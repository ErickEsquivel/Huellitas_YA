/**
 * Correos "noreply" de Huellitas YA! (texto en español), todos con la misma plantilla:
 * - código para restablecer la contraseña (spec 002);
 * - código para confirmar un correo nuevo, aviso de correo cambiado y aviso de contraseña cambiada (spec 004).
 */
import { CODE_TTL_MS } from "./resetCodes";

export interface AppEmail {
  subject: string;
  text: string;
  html: string;
}

const NO_REPLY = "Este es un mensaje automático, por favor no respondas.";

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function codeDigits(code: string): string {
  return code
    .split("")
    .map(
      (d) =>
        `<span style="display:inline-block;width:44px;height:56px;line-height:56px;margin:0 4px;border:1px solid #588157;border-radius:10px;font-size:28px;font-weight:800;color:#000;">${d}</span>`,
    )
    .join("");
}

/**
 * Plantilla común: logo, título, párrafo, bloque opcional (p. ej. el código) y notas al pie.
 * Los textos se escapan: pueden incluir correos escritos por el usuario.
 */
function layout(title: string, intro: string, block: string, notes: string[]): string {
  const noteRows = notes
    .map((note) => `<tr><td align="center" style="padding-top:16px;font-size:14px;color:#8e8e93;">${escapeHtml(note)}</td></tr>`)
    .join("\n          ");
  const blockRow = block ? `<tr><td align="center" style="padding:24px 0;">${block}</td></tr>` : "";
  return `<!doctype html>
<html lang="es">
  <body style="margin:0;padding:24px;background:#f2f2f2;font-family:Nunito,Arial,sans-serif;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
      <tr><td align="center">
        <table role="presentation" width="420" cellspacing="0" cellpadding="0" style="background:#ffffff;border-radius:16px;padding:32px;">
          <tr><td align="center" style="font-size:28px;font-weight:800;color:#000;">
            Huellitas <span style="color:#59ad57;">YA!</span>
          </td></tr>
          <tr><td align="center" style="padding-top:16px;font-size:22px;font-weight:800;color:#000;">${escapeHtml(title)}</td></tr>
          <tr><td align="center" style="padding-top:8px;font-size:16px;color:#8e8e93;">${escapeHtml(intro)}</td></tr>
          ${blockRow}
          ${noteRows}
          <tr><td align="center" style="padding-top:24px;font-size:12px;color:#aeaeb2;">${NO_REPLY}</td></tr>
        </table>
      </td></tr>
    </table>
  </body>
</html>`;
}

function minutes(): number {
  return Math.round(CODE_TTL_MS / 60000);
}

export function buildResetCodeEmail(code: string): AppEmail {
  const subject = "Tu código para restablecer la contraseña";
  const text = [
    "Hola,",
    "",
    `Tu código para restablecer la contraseña de Huellitas YA! es: ${code}`,
    `Vence en ${minutes()} minutos.`,
    "",
    "Si no lo pediste, ignora este correo: tu contraseña no cambiará.",
    "",
    NO_REPLY,
  ].join("\n");
  const html = layout(
    "Verifica tu correo",
    "Usa este código para restablecer tu contraseña:",
    codeDigits(code),
    [`Vence en ${minutes()} minutos.`, "Si no lo pediste, ignora este correo: tu contraseña no cambiará."],
  );
  return { subject, text, html };
}

/** Código que se envía al correo nuevo para confirmar que es del usuario (spec 004). */
export function buildEmailChangeCodeEmail(code: string): AppEmail {
  const subject = "Confirma tu nuevo correo";
  const text = [
    "Hola,",
    "",
    `Tu código para cambiar el correo de tu cuenta de Huellitas YA! es: ${code}`,
    `Vence en ${minutes()} minutos.`,
    "",
    "Si no lo pediste, ignora este correo: tu cuenta no cambiará.",
    "",
    NO_REPLY,
  ].join("\n");
  const html = layout(
    "Confirma tu nuevo correo",
    "Usa este código para cambiar el correo de tu cuenta:",
    codeDigits(code),
    [`Vence en ${minutes()} minutos.`, "Si no lo pediste, ignora este correo: tu cuenta no cambiará."],
  );
  return { subject, text, html };
}

/** Aviso al correo anterior: si no fue el usuario, se entera de que le cambiaron el correo. */
export function buildEmailChangedEmail(maskedNewEmail: string): AppEmail {
  const subject = "El correo de tu cuenta cambió";
  const intro = `El correo de tu cuenta de Huellitas YA! ahora es ${maskedNewEmail}.`;
  const warning = "Si no fuiste tú, escribe cuanto antes a huellitasyasupport@gmail.com.";
  const text = ["Hola,", "", intro, "", warning, "", NO_REPLY].join("\n");
  const html = layout("Tu correo cambió", intro, "", [warning]);
  return { subject, text, html };
}

/** Aviso tras cambiar o restablecer la contraseña. */
export function buildPasswordChangedEmail(): AppEmail {
  const subject = "Tu contraseña cambió";
  const intro = "La contraseña de tu cuenta de Huellitas YA! se cambió hace un momento.";
  const warning = "Si no fuiste tú, restablécela desde la app y escribe a huellitasyasupport@gmail.com.";
  const text = ["Hola,", "", intro, "", warning, "", NO_REPLY].join("\n");
  const html = layout("Tu contraseña cambió", intro, "", [warning]);
  return { subject, text, html };
}
