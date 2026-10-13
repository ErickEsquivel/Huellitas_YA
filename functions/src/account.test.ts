import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { isRecentLogin, maskEmail, RECENT_LOGIN_MAX_AGE_MS, userStoragePrefix } from "./account";

const NOW = 1_800_000_000_000;
const seconds = (ms: number) => ms / 1000;

describe("isRecentLogin", () => {
  it("acepta un inicio de sesión de hace un momento", () => {
    assert.equal(isRecentLogin(seconds(NOW - 10_000), NOW), true);
  });

  it("acepta el límite exacto de 5 minutos", () => {
    assert.equal(isRecentLogin(seconds(NOW - RECENT_LOGIN_MAX_AGE_MS), NOW), true);
  });

  it("rechaza un inicio de sesión de hace más de 5 minutos", () => {
    assert.equal(isRecentLogin(seconds(NOW - RECENT_LOGIN_MAX_AGE_MS - 1000), NOW), false);
  });

  it("tolera un reloj adelantado hasta un minuto", () => {
    assert.equal(isRecentLogin(seconds(NOW + 30_000), NOW), true);
    assert.equal(isRecentLogin(seconds(NOW + 120_000), NOW), false);
  });

  it("rechaza un auth_time ausente o inválido", () => {
    assert.equal(isRecentLogin(undefined, NOW), false);
    assert.equal(isRecentLogin("1800000000", NOW), false);
    assert.equal(isRecentLogin(Number.NaN, NOW), false);
  });
});

describe("userStoragePrefix", () => {
  it("limita el borrado a la carpeta del usuario", () => {
    assert.equal(userStoragePrefix("uid-1"), "users/uid-1/");
  });
});

describe("maskEmail", () => {
  it("deja ver solo la primera letra del usuario y el dominio", () => {
    assert.equal(maskEmail("nuevo@correo.com"), "n****@correo.com");
  });

  it("usa al menos tres asteriscos aunque el usuario sea corto", () => {
    assert.equal(maskEmail("ab@x.co"), "a***@x.co");
  });

  it("no revela nada si el correo no tiene formato", () => {
    assert.equal(maskEmail("sin-arroba"), "****");
  });
});
