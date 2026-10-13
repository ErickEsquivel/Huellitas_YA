import assert from "node:assert/strict";
import { describe, it } from "node:test";

import {
  buildEmailChangeCodeEmail,
  buildEmailChangedEmail,
  buildPasswordChangedEmail,
  buildResetCodeEmail,
} from "./email";

describe("plantillas de correo", () => {
  it("el código de cambio de correo aparece en el texto y en el HTML", () => {
    const message = buildEmailChangeCodeEmail("4821");
    assert.equal(message.subject, "Confirma tu nuevo correo");
    assert.match(message.text, /4821/);
    for (const digit of "4821") assert.match(message.html, new RegExp(`>${digit}</span>`));
  });

  it("el aviso de correo cambiado muestra el correo enmascarado y escapa el HTML", () => {
    const message = buildEmailChangedEmail("n****@<b>x</b>.com");
    assert.match(message.text, /n\*\*\*\*@/);
    assert.doesNotMatch(message.html, /<b>x<\/b>/);
    assert.match(message.html, /&lt;b&gt;/);
  });

  it("el aviso de contraseña cambiada no lleva código", () => {
    const message = buildPasswordChangedEmail();
    assert.equal(message.subject, "Tu contraseña cambió");
    assert.doesNotMatch(message.html, /border:1px solid #588157/);
  });

  it("el código de recuperación sigue igual que antes", () => {
    const message = buildResetCodeEmail("0042");
    assert.equal(message.subject, "Tu código para restablecer la contraseña");
    assert.match(message.text, /0042/);
    assert.match(message.html, /Verifica tu correo/);
  });
});
