import assert from "node:assert/strict";
import { describe, it } from "node:test";

import {
  canSendCode,
  checkCode,
  checkResetToken,
  CODE_TTL_MS,
  emailKey,
  generateCode,
  hashSecret,
  isValidCodeFormat,
  MAX_ATTEMPTS,
  passwordProblems,
  RESEND_COOLDOWN_MS,
  ResetRecord,
} from "./resetCodes";

const PEPPER = "pepper-de-prueba";
const NOW = 1_800_000_000_000;

function record(overrides: Partial<ResetRecord> = {}): ResetRecord {
  return {
    uid: "uid-1",
    codeHash: hashSecret("1234", PEPPER),
    expiresAt: NOW + CODE_TTL_MS,
    attempts: 0,
    sends: [NOW],
    ...overrides,
  };
}

describe("código", () => {
  it("siempre tiene 4 dígitos, incluidos los ceros a la izquierda", () => {
    for (let i = 0; i < 500; i++) assert.match(generateCode(), /^\d{4}$/);
  });

  it("valida el formato", () => {
    assert.equal(isValidCodeFormat("0042"), true);
    assert.equal(isValidCodeFormat("123"), false);
    assert.equal(isValidCodeFormat("12a4"), false);
    assert.equal(isValidCodeFormat(1234), false);
  });

  it("acepta el código correcto y rechaza el incorrecto", () => {
    assert.equal(checkCode(record(), "1234", PEPPER, NOW), "ok");
    assert.equal(checkCode(record(), "4321", PEPPER, NOW), "invalid");
  });

  it("rechaza códigos vencidos, sin código o con demasiados intentos", () => {
    assert.equal(checkCode(record(), "1234", PEPPER, NOW + CODE_TTL_MS + 1), "expired");
    assert.equal(checkCode(record({ codeHash: null }), "1234", PEPPER, NOW), "no_code");
    assert.equal(checkCode(undefined, "1234", PEPPER, NOW), "no_code");
    assert.equal(checkCode(record({ attempts: MAX_ATTEMPTS }), "1234", PEPPER, NOW), "too_many_attempts");
  });

  it("no acepta un código hasheado con otro pepper", () => {
    assert.equal(checkCode(record({ codeHash: hashSecret("1234", "otro") }), "1234", PEPPER, NOW), "invalid");
  });
});

describe("reenvío", () => {
  it("exige 60 s entre envíos", () => {
    const decision = canSendCode([NOW], NOW + 10_000);
    assert.deepEqual(decision, { allowed: false, retryAfterSeconds: 50 });
    assert.equal(canSendCode([NOW], NOW + RESEND_COOLDOWN_MS).allowed, true);
  });

  it("permite máximo 5 envíos por hora", () => {
    const sends = [0, 1, 2, 3, 4].map((i) => NOW - 50 * 60_000 + i * 120_000);
    const decision = canSendCode(sends, NOW);
    assert.equal(decision.allowed, false);
    assert.equal(canSendCode(sends, sends[0] + 60 * 60_000 + 1).allowed, true);
  });
});

describe("token de restablecimiento", () => {
  it("solo sirve el token correcto y vigente", () => {
    const r = record({ tokenHash: hashSecret("token", PEPPER), tokenExpiresAt: NOW + 1000 });
    assert.equal(checkResetToken(r, "token", PEPPER, NOW), true);
    assert.equal(checkResetToken(r, "otro", PEPPER, NOW), false);
    assert.equal(checkResetToken(r, "token", PEPPER, NOW + 2000), false);
    assert.equal(checkResetToken(record(), "token", PEPPER, NOW), false);
  });
});

describe("política de contraseña (igual que la app)", () => {
  it("detecta cada regla que falta", () => {
    assert.deepEqual(passwordProblems("abc"), ["min_length", "uppercase", "digit_or_symbol"]);
    assert.deepEqual(passwordProblems("huellitas1"), ["uppercase"]);
    assert.deepEqual(passwordProblems("Huellitas"), ["digit_or_symbol"]);
    assert.deepEqual(passwordProblems("Huellitas!"), []);
    assert.deepEqual(passwordProblems("Huellitas1"), []);
  });
});

describe("clave del documento", () => {
  it("no depende de mayúsculas ni espacios", () => {
    assert.equal(emailKey(" Ana@Correo.com "), emailKey("ana@correo.com"));
  });
});
