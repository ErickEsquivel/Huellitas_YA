/**
 * Recuperación de contraseña con código de 4 dígitos por correo (spec 002).
 *
 * 1. requestPasswordResetCode   → genera el código y lo envía desde el Gmail "noreply".
 * 2. verifyPasswordResetCode    → valida el código y entrega un token de un solo uso.
 * 3. resetPasswordWithToken     → con el token, cambia la contraseña (Admin SDK).
 *
 * Los códigos y tokens se guardan hasheados en `passwordResetCodes/{sha256(email)}`; las reglas
 * de Firestore impiden que los clientes lean esa colección.
 */
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { setGlobalOptions } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { defineSecret } from "firebase-functions/params";
import nodemailer from "nodemailer";

import { buildResetCodeEmail } from "./email";
import {
  canSendCode,
  checkCode,
  checkResetToken,
  CODE_TTL_MS,
  emailKey,
  generateCode,
  generateResetToken,
  hashSecret,
  isValidCodeFormat,
  isValidEmail,
  MAX_ATTEMPTS,
  normalizeEmail,
  passwordProblems,
  RESET_TOKEN_TTL_MS,
  ResetRecord,
} from "./resetCodes";

initializeApp();
setGlobalOptions({ region: "southamerica-east1", maxInstances: 5 });

const SMTP_USER = defineSecret("SMTP_USER");
const SMTP_PASSWORD = defineSecret("SMTP_PASSWORD");
const RESET_CODE_PEPPER = defineSecret("RESET_CODE_PEPPER");

const COLLECTION = "passwordResetCodes";

function requireEmail(data: unknown): string {
  const raw = (data as { email?: unknown } | undefined)?.email;
  if (typeof raw !== "string" || !isValidEmail(raw.trim())) {
    throw new HttpsError("invalid-argument", "Correo no válido.", { reason: "invalid_email" });
  }
  return normalizeEmail(raw);
}

async function findUid(email: string): Promise<string | null> {
  try {
    return (await getAuth().getUserByEmail(email)).uid;
  } catch (error) {
    if ((error as { code?: string }).code === "auth/user-not-found") return null;
    throw error;
  }
}

export const requestPasswordResetCode = onCall(
  { secrets: [SMTP_USER, SMTP_PASSWORD, RESET_CODE_PEPPER], enforceAppCheck: false },
  async (request) => {
    const email = requireEmail(request.data);
    const uid = await findUid(email);
    // Privacidad: si el correo no tiene cuenta, se responde igual que si la tuviera y no se envía nada.
    if (!uid) return { ok: true };

    const ref = getFirestore().collection(COLLECTION).doc(emailKey(email));
    const now = Date.now();
    const code = generateCode();

    await getFirestore().runTransaction(async (tx) => {
      const current = (await tx.get(ref)).data() as ResetRecord | undefined;
      const decision = canSendCode(current?.sends ?? [], now);
      if (!decision.allowed) {
        throw new HttpsError("resource-exhausted", "Espera antes de pedir otro código.", {
          reason: "resend_cooldown",
          retryAfterSeconds: decision.retryAfterSeconds,
        });
      }
      const record: ResetRecord = {
        uid,
        codeHash: hashSecret(code, RESET_CODE_PEPPER.value()),
        expiresAt: now + CODE_TTL_MS,
        attempts: 0,
        sends: [...decision.recentSends, now],
        tokenHash: null,
        tokenExpiresAt: null,
      };
      tx.set(ref, record);
    });

    const message = buildResetCodeEmail(code);
    const transport = nodemailer.createTransport({
      service: "gmail",
      // Google muestra la contraseña de aplicación en grupos de 4 con espacios: se ignoran.
      auth: { user: SMTP_USER.value().trim(), pass: SMTP_PASSWORD.value().replace(/\s+/g, "") },
    });
    try {
      await transport.sendMail({
        from: `"Huellitas YA" <${SMTP_USER.value().trim()}>`,
        to: email,
        subject: message.subject,
        text: message.text,
        html: message.html,
      });
    } catch (error) {
      logger.error("No se pudo enviar el correo con el código", error);
      // El correo no salió: se anula el código y este envío no cuenta para la espera de 60 s.
      await ref.update({ codeHash: null, sends: FieldValue.arrayRemove(now) });
      throw new HttpsError("unavailable", "No se pudo enviar el correo. Intenta de nuevo.", { reason: "email_failed" });
    }
    return { ok: true };
  },
);

export const verifyPasswordResetCode = onCall(
  { secrets: [RESET_CODE_PEPPER], enforceAppCheck: false },
  async (request) => {
    const email = requireEmail(request.data);
    const code = (request.data as { code?: unknown }).code;
    if (!isValidCodeFormat(code)) {
      throw new HttpsError("invalid-argument", "El código debe tener 4 dígitos.", { reason: "invalid_code" });
    }

    const ref = getFirestore().collection(COLLECTION).doc(emailKey(email));
    const pepper = RESET_CODE_PEPPER.value();
    const now = Date.now();

    const outcome = await getFirestore().runTransaction(async (tx) => {
      const record = (await tx.get(ref)).data() as ResetRecord | undefined;
      const check = checkCode(record, code, pepper, now);
      if (check === "invalid" && record) {
        tx.update(ref, { attempts: record.attempts + 1 });
        return { check, attemptsLeft: Math.max(0, MAX_ATTEMPTS - (record.attempts + 1)) };
      }
      if (check === "ok" && record) {
        const resetToken = generateResetToken();
        tx.update(ref, {
          codeHash: null,
          tokenHash: hashSecret(resetToken, pepper),
          tokenExpiresAt: now + RESET_TOKEN_TTL_MS,
        });
        return { check, resetToken };
      }
      return { check };
    });

    switch (outcome.check) {
      case "ok":
        return { resetToken: outcome.resetToken };
      case "invalid":
        throw new HttpsError("invalid-argument", "El código ingresado no es correcto.", {
          reason: "invalid_code",
          attemptsLeft: outcome.attemptsLeft,
        });
      case "too_many_attempts":
        throw new HttpsError("resource-exhausted", "Demasiados intentos. Pide un código nuevo.", {
          reason: "too_many_attempts",
        });
      default:
        // "expired" o "no_code": se pide un código nuevo.
        throw new HttpsError("deadline-exceeded", "El código venció. Pide uno nuevo.", { reason: "expired" });
    }
  },
);

export const resetPasswordWithToken = onCall(
  { secrets: [RESET_CODE_PEPPER], enforceAppCheck: false },
  async (request) => {
    const email = requireEmail(request.data);
    const { resetToken, newPassword } = request.data as { resetToken?: unknown; newPassword?: unknown };
    if (typeof resetToken !== "string" || typeof newPassword !== "string") {
      throw new HttpsError("invalid-argument", "Datos incompletos.", { reason: "invalid_request" });
    }
    const problems = passwordProblems(newPassword);
    if (problems.length > 0) {
      throw new HttpsError("invalid-argument", "La contraseña no cumple los requisitos.", {
        reason: "weak_password",
        problems,
      });
    }

    const ref = getFirestore().collection(COLLECTION).doc(emailKey(email));
    const record = (await ref.get()).data() as ResetRecord | undefined;
    if (!record || !checkResetToken(record, resetToken, RESET_CODE_PEPPER.value(), Date.now())) {
      throw new HttpsError("permission-denied", "La verificación venció. Pide un código nuevo.", {
        reason: "invalid_token",
      });
    }

    await getAuth().updateUser(record.uid, { password: newPassword });
    // Cierra las sesiones abiertas en otros dispositivos.
    await getAuth().revokeRefreshTokens(record.uid);
    await ref.delete();
    return { ok: true };
  },
);
