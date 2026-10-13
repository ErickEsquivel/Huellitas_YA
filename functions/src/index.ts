/**
 * Backend de Huellitas YA! (Cloud Functions callable, Admin SDK).
 *
 * Recuperación de contraseña con código de 4 dígitos por correo (spec 002):
 * 1. requestPasswordResetCode   → genera el código y lo envía desde el Gmail "noreply".
 * 2. verifyPasswordResetCode    → valida el código y entrega un token de un solo uso.
 * 3. resetPasswordWithToken     → con el token, cambia la contraseña (Admin SDK).
 *
 * Los códigos y tokens se guardan hasheados en `passwordResetCodes/{sha256(email)}`; las reglas
 * de Firestore impiden que los clientes lean esa colección.
 *
 * Cuenta (spec 004):
 * - deleteAccount           → borra la foto, `users/{uid}` (con subcolecciones) y el usuario de Auth.
 * - requestEmailChangeCode  → envía un código de 4 dígitos al correo nuevo (`emailChangeCodes/{uid}`).
 * - confirmEmailChange      → con el código, cambia el correo en Auth y en `users/{uid}`, y avisa al anterior.
 * - notifyPasswordChanged   → avisa por correo que la contraseña cambió (también tras restablecerla).
 */
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { setGlobalOptions } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";
import { defineSecret } from "firebase-functions/params";
import nodemailer from "nodemailer";

import { EmailChangeRecord, isRecentLogin, maskEmail, userStoragePrefix } from "./account";
import {
  AppEmail,
  buildEmailChangeCodeEmail,
  buildEmailChangedEmail,
  buildPasswordChangedEmail,
  buildResetCodeEmail,
} from "./email";
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
const EMAIL_CHANGE_COLLECTION = "emailChangeCodes";
const PASSWORD_NOTICE_COLLECTION = "passwordChangeNotices";

function requireEmail(data: unknown): string {
  const raw = (data as { email?: unknown } | undefined)?.email;
  if (typeof raw !== "string" || !isValidEmail(raw.trim())) {
    throw new HttpsError("invalid-argument", "Correo no válido.", { reason: "invalid_email" });
  }
  return normalizeEmail(raw);
}

/** Envía un correo desde el Gmail "noreply" (secretos SMTP_USER y SMTP_PASSWORD). */
async function sendAppEmail(to: string, message: AppEmail): Promise<void> {
  const transport = nodemailer.createTransport({
    service: "gmail",
    // Google muestra la contraseña de aplicación en grupos de 4 con espacios: se ignoran.
    auth: { user: SMTP_USER.value().trim(), pass: SMTP_PASSWORD.value().replace(/\s+/g, "") },
  });
  await transport.sendMail({
    from: `"Huellitas YA" <${SMTP_USER.value().trim()}>`,
    to,
    subject: message.subject,
    text: message.text,
    html: message.html,
  });
}

/** Igual que [sendAppEmail], pero un aviso que no sale no debe romper la operación principal. */
async function sendNotice(to: string | undefined, message: AppEmail): Promise<void> {
  if (!to) return;
  try {
    await sendAppEmail(to, message);
  } catch (error) {
    logger.error("No se pudo enviar el aviso por correo", error);
  }
}

/** Exige sesión e inicio de sesión reciente (≤ 5 min): la app reautentica antes si hace falta. */
function requireRecentAuth(request: { auth?: { uid: string; token: { auth_time?: unknown } } }): string {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Inicia sesión para continuar.");
  }
  if (!isRecentLogin(request.auth.token.auth_time, Date.now())) {
    throw new HttpsError("failed-precondition", "Vuelve a confirmar tu identidad.", {
      reason: "requires_recent_login",
    });
  }
  return request.auth.uid;
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

    try {
      await sendAppEmail(email, buildResetCodeEmail(code));
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
  { secrets: [SMTP_USER, SMTP_PASSWORD, RESET_CODE_PEPPER], enforceAppCheck: false },
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
    // Spec 004: aviso por si no fue el dueño de la cuenta.
    await sendNotice(email, buildPasswordChangedEmail());
    return { ok: true };
  },
);

/**
 * Elimina la cuenta del usuario que llama (spec 004, "Eliminar cuenta"). Es idempotente: si
 * falla a la mitad, al reintentar se salta lo que ya no existe.
 *
 * Contrato con la app (`FunctionsAccountRepository`):
 * - sin sesión → `unauthenticated`;
 * - inicio de sesión de hace más de 5 min → `failed-precondition` con `reason: "requires_recent_login"`;
 * - éxito → `{ deleted: true }`.
 */
export const deleteAccount = onCall({ enforceAppCheck: false }, async (request) => {
  const uid = requireRecentAuth(request);

  // 1. Foto (y cualquier archivo futuro del usuario). Sin bucket o sin archivos no hay nada que borrar.
  try {
    await getStorage().bucket().deleteFiles({ prefix: userStoragePrefix(uid) });
  } catch (error) {
    if ((error as { code?: number }).code !== 404) throw error;
  }

  // 2. Perfil con sus subcolecciones (p. ej. mascotas, cuando exista su spec).
  const firestore = getFirestore();
  await firestore.recursiveDelete(firestore.collection("users").doc(uid));

  // 3. Usuario de Auth. Va al final: si algo falla antes, la cuenta sigue usable para reintentar.
  try {
    await getAuth().deleteUser(uid);
  } catch (error) {
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
  }

  logger.info("Cuenta eliminada", { uid });
  return { deleted: true };
});

/**
 * Paso 1 del cambio de correo (spec 004): envía un código de 4 dígitos al correo nuevo.
 *
 * Contrato con la app (`FunctionsEmailChangeRepository`):
 * - inicio de sesión de hace más de 5 min → `failed-precondition`, `reason: "requires_recent_login"`;
 * - correo inválido o igual al actual → `invalid-argument`, `reason: "invalid_email"`;
 * - correo de otra cuenta → `already-exists`, `reason: "email_in_use"`;
 * - reenvío antes de tiempo → `resource-exhausted`, `reason: "resend_cooldown"` + `retryAfterSeconds`;
 * - el correo no salió → `unavailable`, `reason: "email_failed"`.
 */
export const requestEmailChangeCode = onCall(
  { secrets: [SMTP_USER, SMTP_PASSWORD, RESET_CODE_PEPPER], enforceAppCheck: false },
  async (request) => {
    const uid = requireRecentAuth(request);
    const raw = (request.data as { newEmail?: unknown } | undefined)?.newEmail;
    if (typeof raw !== "string" || !isValidEmail(raw.trim())) {
      throw new HttpsError("invalid-argument", "Correo no válido.", { reason: "invalid_email" });
    }
    const newEmail = normalizeEmail(raw);
    const currentEmail = normalizeEmail(request.auth?.token.email ?? "");
    if (newEmail === currentEmail) {
      throw new HttpsError("invalid-argument", "Es el mismo correo.", { reason: "invalid_email" });
    }
    if ((await findUid(newEmail)) !== null) {
      throw new HttpsError("already-exists", "Este correo ya está registrado.", { reason: "email_in_use" });
    }

    const ref = getFirestore().collection(EMAIL_CHANGE_COLLECTION).doc(uid);
    const now = Date.now();
    const code = generateCode();
    await getFirestore().runTransaction(async (tx) => {
      const current = (await tx.get(ref)).data() as EmailChangeRecord | undefined;
      const decision = canSendCode(current?.sends ?? [], now);
      if (!decision.allowed) {
        throw new HttpsError("resource-exhausted", "Espera antes de pedir otro código.", {
          reason: "resend_cooldown",
          retryAfterSeconds: decision.retryAfterSeconds,
        });
      }
      const record: EmailChangeRecord = {
        newEmail,
        codeHash: hashSecret(code, RESET_CODE_PEPPER.value()),
        expiresAt: now + CODE_TTL_MS,
        attempts: 0,
        sends: [...decision.recentSends, now],
      };
      tx.set(ref, record);
    });

    try {
      await sendAppEmail(newEmail, buildEmailChangeCodeEmail(code));
    } catch (error) {
      logger.error("No se pudo enviar el código de cambio de correo", error);
      await ref.update({ codeHash: null, sends: FieldValue.arrayRemove(now) });
      throw new HttpsError("unavailable", "No se pudo enviar el correo. Intenta de nuevo.", { reason: "email_failed" });
    }
    return { ok: true };
  },
);

/**
 * Paso 2 del cambio de correo (spec 004): valida el código y cambia el correo en Auth y en
 * `users/{uid}` a la vez. Después avisa al correo anterior.
 *
 * Contrato con la app: los mismos códigos que `verifyPasswordResetCode` (`invalid_code`,
 * `too_many_attempts`, `expired`) y `email_in_use` si otra cuenta tomó el correo entretanto.
 * Éxito → `{ email }` con el correo nuevo.
 */
export const confirmEmailChange = onCall(
  { secrets: [SMTP_USER, SMTP_PASSWORD, RESET_CODE_PEPPER], enforceAppCheck: false },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Inicia sesión para continuar.");
    }
    const uid = request.auth.uid;
    const oldEmail = request.auth.token.email;
    const code = (request.data as { code?: unknown } | undefined)?.code;
    if (!isValidCodeFormat(code)) {
      throw new HttpsError("invalid-argument", "El código debe tener 4 dígitos.", { reason: "invalid_code" });
    }

    const ref = getFirestore().collection(EMAIL_CHANGE_COLLECTION).doc(uid);
    const pepper = RESET_CODE_PEPPER.value();
    const now = Date.now();
    const outcome = await getFirestore().runTransaction(async (tx) => {
      const record = (await tx.get(ref)).data() as EmailChangeRecord | undefined;
      const check = checkCode(record, code, pepper, now);
      if (check === "invalid" && record) {
        tx.update(ref, { attempts: record.attempts + 1 });
      }
      if (check === "ok" && record) {
        // El código es de un solo uso aunque falle lo que sigue.
        tx.update(ref, { codeHash: null });
        return { check, newEmail: record.newEmail };
      }
      return { check };
    });

    switch (outcome.check) {
      case "ok":
        break;
      case "invalid":
        throw new HttpsError("invalid-argument", "El código ingresado no es correcto.", { reason: "invalid_code" });
      case "too_many_attempts":
        throw new HttpsError("resource-exhausted", "Demasiados intentos. Pide un código nuevo.", {
          reason: "too_many_attempts",
        });
      default:
        throw new HttpsError("deadline-exceeded", "El código venció. Pide uno nuevo.", { reason: "expired" });
    }

    const newEmail = outcome.newEmail as string;
    try {
      await getAuth().updateUser(uid, { email: newEmail, emailVerified: true });
    } catch (error) {
      if ((error as { code?: string }).code === "auth/email-already-exists") {
        throw new HttpsError("already-exists", "Este correo ya está registrado.", { reason: "email_in_use" });
      }
      throw error;
    }
    const firestore = getFirestore();
    await firestore.collection("users").doc(uid).set(
      { email: newEmail, updatedAt: FieldValue.serverTimestamp() },
      { merge: true },
    );
    await ref.delete();

    await sendNotice(oldEmail, buildEmailChangedEmail(maskEmail(newEmail)));
    logger.info("Correo cambiado", { uid });
    return { email: newEmail };
  },
);

/**
 * Avisa por correo que la contraseña cambió (spec 004, Seguridad). La app la llama justo después
 * de `updatePassword`, con la sesión recién reautenticada. Máximo un aviso por minuto.
 */
export const notifyPasswordChanged = onCall(
  { secrets: [SMTP_USER, SMTP_PASSWORD], enforceAppCheck: false },
  async (request) => {
    const uid = requireRecentAuth(request);
    const email = request.auth?.token.email;
    const ref = getFirestore().collection(PASSWORD_NOTICE_COLLECTION).doc(uid);
    const now = Date.now();
    const lastSentAt = ((await ref.get()).data()?.lastSentAt as number | undefined) ?? 0;
    if (now - lastSentAt < 60 * 1000) return { sent: false };

    await ref.set({ lastSentAt: now });
    await sendNotice(email, buildPasswordChangedEmail());
    return { sent: true };
  },
);
