# Correos de compras y facturas (Brevo)

La base de datos avisa a la función `correos` al crear un pedido y al entregarlo; la función envía el correo con Brevo.

**Activar (una vez):**
1. Brevo → rueda ⚙ → **SMTP y API** → pestaña **Claves API** → «Generar una nueva clave API» → nombre `Supabase correos` → copia la clave (`xkeysib-…`).
2. En la app, con la cuenta de superadministrador: **Ajustes → Cuenta → Correo de compras (Brevo)** → pega la clave → «Guardar clave» → «Enviar prueba».
   (También vale el secreto `BREVO_API_KEY` en Supabase → Edge Functions → Secrets.)

Sin la clave, la app funciona igual y simplemente no envía correos.
- Se envía a la persona mayor y a quien hizo el pedido (familiar), si tienen correo real y no lo han desactivado en Ajustes.
- Las cuentas creadas con nombre de usuario (`usuario@tlotraigo.app`) reciben los correos en el «correo para avisos» que añadan en Ajustes → Cuenta.
- Remitente: T Lo Traigo UNOCEROBITS <tlotraigo@unocerobits.com> · Límite del plan gratuito de Brevo: 300 correos al día.
