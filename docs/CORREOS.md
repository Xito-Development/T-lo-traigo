# Correos de compras y facturas (Brevo)

La base de datos avisa a la función `correos` al crear un pedido y al entregarlo; la función envía el correo con Brevo.

**Activar (una vez):**
1. Brevo → rueda ⚙ → **SMTP y API** → pestaña **Claves API** → «Generar una nueva clave API» → nombre `Supabase correos` → copia la clave (`xkeysib-…`).
2. Supabase → **Edge Functions** → **Secrets** → añade `BREVO_API_KEY` con esa clave → Save.

Sin la clave, la app funciona igual y simplemente no envía correos.
- Se envía a la persona mayor y a quien hizo el pedido (familiar), si tienen correo real y no lo han desactivado en Ajustes.
- Las cuentas cortas internas (`usuario@tlotraigo.app`) no reciben correos.
- Remitente: T Lo Traigo UNOCEROBITS <tlotraigo@unocerobits.com> · Límite del plan gratuito de Brevo: 300 correos al día.
