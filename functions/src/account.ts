/**
 * Lógica pura de la eliminación de cuenta (spec 004, "Eliminar cuenta"). Sin Firebase, para
 * probarla con `node --test`.
 */

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
