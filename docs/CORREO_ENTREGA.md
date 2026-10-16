# Que los correos lleguen a «Principal» y con el logo

## 1. Autenticar el dominio en Brevo (imprescindible, 10 min)
Brevo → ⚙ → **Remitentes, dominios e IP dedicadas** → Dominios → `unocerobits.com` → «Autenticar». Brevo te da 3–4 registros DNS.
En **IONOS → Dominios → unocerobits.com → DNS** añade exactamente esos registros:
- `brevo-code` (TXT) · **DKIM** (`brevo1._domainkey` y `brevo2._domainkey`, CNAME)
- **SPF**: si ya tienes un TXT `v=spf1 …` de IONOS, **no crees otro**: añade `include:spf.brevo.com` dentro del mismo
  (ej.: `v=spf1 include:_spf-eu.ionos.com include:spf.brevo.com ~all`)
- **DMARC** (TXT en `_dmarc`): empieza con `v=DMARC1; p=none; rua=mailto:dvg@unocerobits.com`
Vuelve a Brevo y pulsa «Verificar». Sin esto, Gmail desconfía y manda a Spam/Promociones.

## 2. Desactivar el seguimiento de aperturas y clics
Brevo → **Transaccional** → Ajustes → desactiva el seguimiento de aperturas y de clics. El píxel de seguimiento y los enlaces
reescritos son señales típicas de publicidad.

## 3. Logo en la bandeja de entrada
- **Rápido y gratis (Gmail):** crea una **Cuenta de Google con tu correo** `tlotraigo@unocerobits.com`
  (accounts.google.com → Crear cuenta → «Usar mi dirección de correo actual») y pon el logo como foto de perfil
  (`web/t-lo-traigo/favicon-512.png`). Gmail suele mostrarla a los destinatarios.
- **Outlook/Hotmail:** igual con una cuenta Microsoft de ese correo y su foto de perfil.
- **Oficial (BIMI):** logo ya preparado en `https://tlotraigo.unocerobits.com/bimi.svg` (formato SVG Tiny PS).
  1. Cuando lleves unas semanas con DMARC `p=none` sin problemas, cámbialo a `p=quarantine` (afecta a todo el correo de
     unocerobits.com: asegúrate antes de que IONOS y Brevo pasan SPF/DKIM).
  2. Registro TXT en `default._bimi`: `v=BIMI1; l=https://tlotraigo.unocerobits.com/bimi.svg;`
  3. Yahoo, Fastmail y otros ya muestran el logo así. **Gmail y Apple** exigen además un certificado VMC/CMC
     (DigiCert o Sectigo, ~1.000 €/año el VMC; el CMC es más barato y no requiere marca registrada).

## 4. Que vaya a «Principal» y no a «Notificaciones»/«Promociones»
Gmail decide las pestañas automáticamente y **no se puede forzar** desde el envío. Ya se ha hecho lo que más ayuda:
correos personales («Hola, Rosa: …»), versión en texto, «responder a» soporte real, sin etiquetas de campaña ni listas.
Lo que más influye después:
- Que el destinatario **arrastre un correo a «Principal»** y pulse «Sí» a «¿Hacer esto con futuros mensajes?».
- Que **añada `tlotraigo@unocerobits.com` a sus contactos** (podemos recordarlo en la app).
- Que **responda** alguna vez a un correo.
