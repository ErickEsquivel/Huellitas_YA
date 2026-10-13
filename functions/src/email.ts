/** Correo "noreply" con el código de recuperación (texto en español). */
import { CODE_TTL_MS } from "./resetCodes";

export interface ResetCodeEmail {
  subject: string;
  text: string;
  html: string;
}

export function buildResetCodeEmail(code: string): ResetCodeEmail {
  const minutes = Math.round(CODE_TTL_MS / 60000);
  const subject = "Tu código para restablecer la contraseña";
  const text = [
    "Hola,",
    "",
    `Tu código para restablecer la contraseña de Huellitas YA! es: ${code}`,
    `Vence en ${minutes} minutos.`,
    "",
    "Si no lo pediste, ignora este correo: tu contraseña no cambiará.",
    "",
    "Este es un mensaje automático, por favor no respondas.",
  ].join("\n");

  const digits = code
    .split("")
    .map(
      (d) =>
        `<span style="display:inline-block;width:44px;height:56px;line-height:56px;margin:0 4px;border:1px solid #588157;border-radius:10px;font-size:28px;font-weight:800;color:#000;">${d}</span>`,
    )
    .join("");

  const html = `<!doctype html>
<html lang="es">
  <body style="margin:0;padding:24px;background:#f2f2f2;font-family:Nunito,Arial,sans-serif;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
      <tr><td align="center">
        <table role="presentation" width="420" cellspacing="0" cellpadding="0" style="background:#ffffff;border-radius:16px;padding:32px;">
          <tr><td align="center" style="font-size:28px;font-weight:800;color:#000;">
            Huellitas <span style="color:#59ad57;">YA!</span>
          </td></tr>
          <tr><td align="center" style="padding-top:16px;font-size:22px;font-weight:800;color:#000;">Verifica tu correo</td></tr>
          <tr><td align="center" style="padding-top:8px;font-size:16px;color:#8e8e93;">
            Usa este código para restablecer tu contraseña:
          </td></tr>
          <tr><td align="center" style="padding:24px 0;">${digits}</td></tr>
          <tr><td align="center" style="font-size:14px;color:#8e8e93;">Vence en ${minutes} minutos.</td></tr>
          <tr><td align="center" style="padding-top:16px;font-size:14px;color:#8e8e93;">
            Si no lo pediste, ignora este correo: tu contraseña no cambiará.
          </td></tr>
          <tr><td align="center" style="padding-top:24px;font-size:12px;color:#aeaeb2;">
            Este es un mensaje automático, por favor no respondas.
          </td></tr>
        </table>
      </td></tr>
    </table>
  </body>
</html>`;

  return { subject, text, html };
}
