/**
 * Lógica pura de la recuperación de contraseña con código (spec 002, "Recuperar contraseña").
 * No depende de Firebase para poder probarse con `node --test`.
 */
import { createHash, createHmac, randomBytes, randomInt, timingSafeEqual } from "node:crypto";

export const CODE_LENGTH = 4;
export const CODE_TTL_MS = 10 * 60 * 1000;
export const RESET_TOKEN_TTL_MS = 10 * 60 * 1000;
export const MAX_ATTEMPTS = 5;
export const RESEND_COOLDOWN_MS = 60 * 1000;
export const MAX_SENDS_PER_HOUR = 5;
const HOUR_MS = 60 * 60 * 1000;

/** Documento `passwordResetCodes/{emailKey}`. Solo lo lee y escribe el backend. */
export interface ResetRecord {
  uid: string;
  codeHash: string | null;
  expiresAt: number;
  attempts: number;
  /** Momentos (ms) de los envíos de la última hora, para limitar reenvíos. */
  sends: number[];
  tokenHash?: string | null;
  tokenExpiresAt?: number | null;
}

export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

export function isValidEmail(email: string): boolean {
  return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email);
}

/** Id del documento: no guarda el correo en claro. */
export function emailKey(email: string): string {
  return createHash("sha256").update(normalizeEmail(email)).digest("hex");
}

export function generateCode(): string {
  return randomInt(0, 10 ** CODE_LENGTH).toString().padStart(CODE_LENGTH, "0");
}

export function isValidCodeFormat(code: unknown): code is string {
  return typeof code === "string" && new RegExp(`^\\d{${CODE_LENGTH}}$`).test(code);
}

export function generateResetToken(): string {
  return randomBytes(32).toString("base64url");
}

/** HMAC con un secreto del servidor: un volcado de Firestore no permite adivinar códigos. */
export function hashSecret(value: string, pepper: string): string {
  return createHmac("sha256", pepper).update(value).digest("hex");
}

function safeEqualHex(a: string, b: string): boolean {
  const bufferA = Buffer.from(a, "hex");
  const bufferB = Buffer.from(b, "hex");
  return bufferA.length === bufferB.length && timingSafeEqual(bufferA, bufferB);
}

export type SendDecision = { allowed: true; recentSends: number[] } | { allowed: false; retryAfterSeconds: number };

/** Reenvío: espera de 60 s entre envíos y máximo 5 envíos por hora. */
export function canSendCode(previousSends: number[], now: number): SendDecision {
  const recentSends = previousSends.filter((t) => now - t < HOUR_MS).sort((a, b) => a - b);
  const last = recentSends[recentSends.length - 1];
  if (last !== undefined && now - last < RESEND_COOLDOWN_MS) {
    return { allowed: false, retryAfterSeconds: Math.ceil((RESEND_COOLDOWN_MS - (now - last)) / 1000) };
  }
  if (recentSends.length >= MAX_SENDS_PER_HOUR) {
    return { allowed: false, retryAfterSeconds: Math.ceil((recentSends[0] + HOUR_MS - now) / 1000) };
  }
  return { allowed: true, recentSends };
}

export type CodeCheck = "ok" | "invalid" | "expired" | "too_many_attempts" | "no_code";

export function checkCode(record: ResetRecord | undefined, code: string, pepper: string, now: number): CodeCheck {
  if (!record || !record.codeHash) return "no_code";
  if (record.attempts >= MAX_ATTEMPTS) return "too_many_attempts";
  if (now > record.expiresAt) return "expired";
  return safeEqualHex(hashSecret(code, pepper), record.codeHash) ? "ok" : "invalid";
}

export function checkResetToken(record: ResetRecord | undefined, token: string, pepper: string, now: number): boolean {
  if (!record || !record.tokenHash || !record.tokenExpiresAt) return false;
  if (now > record.tokenExpiresAt) return false;
  return safeEqualHex(hashSecret(token, pepper), record.tokenHash);
}

export type PasswordRule = "min_length" | "uppercase" | "digit_or_symbol";

/** Misma política que la app (`ValidatePasswordUseCase.forRegistration`). */
export function passwordProblems(password: string): PasswordRule[] {
  const problems: PasswordRule[] = [];
  if (password.length < 8) problems.push("min_length");
  if (!/\p{Lu}/u.test(password)) problems.push("uppercase");
  if (!/[\p{Nd}]|[^\p{L}\p{Nd}]/u.test(password)) problems.push("digit_or_symbol");
  return problems;
}
