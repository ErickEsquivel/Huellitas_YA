/**
 * Lógica pura de la cuenta (spec 004): eliminar la cuenta y cambiar el correo con código. Sin
 * Firebase, para probarla con `node --test`.
 */
import { CodeRecord } from "./resetCodes";

/** Documento `emailChangeCodes/{uid}`: código enviado al correo nuevo. Solo lo usa el backend. */
export interface EmailChangeRecord extends CodeRecord {
  newEmail: string;
}

/** La app reautentica justo antes de eliminar: un inicio de sesión más viejo se rechaza. */
export const RECENT_LOGIN_MAX_AGE_MS = 5 * 60 * 1000;

/** Margen para relojes desfasados entre el dispositivo y el servidor. */
const CLOCK_SKEW_MS = 60 * 1000;

/**
 * `true` si el token se emitió tras un inicio de sesión reciente.
 *
 * @param authTimeSeconds claim `auth_time` del ID token (segundos desde epoch).
 */
export function isRecentLogin(
  authTimeSeconds: unknown,
  nowMs: number,
  maxAgeMs: number = RECENT_LOGIN_MAX_AGE_MS,
): boolean {
  if (typeof authTimeSeconds !== "number" || !Number.isFinite(authTimeSeconds)) return false;
  const ageMs = nowMs - authTimeSeconds * 1000;
  return ageMs >= -CLOCK_SKEW_MS && ageMs <= maxAgeMs;
}

/** Carpeta de Storage con los archivos del usuario (hoy, `profile.jpg`). */
export function userStoragePrefix(uid: string): string {
  return `users/${uid}/`;
}

/**
 * Oculta parte del correo para el aviso al correo anterior (`nuevo@correo.com` → `n****@correo.com`).
 * Así el aviso no filtra el correo nuevo completo si lo lee otra persona.
 */
export function maskEmail(email: string): string {
  const at = email.lastIndexOf("@");
  if (at <= 0) return "****";
  const user = email.slice(0, at);
  const domain = email.slice(at + 1);
  return `${user[0]}${"*".repeat(Math.max(3, user.length - 1))}@${domain}`;
}
