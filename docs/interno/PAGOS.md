# Pasarela de pago (Stripe) · paso a paso

**Por qué Stripe:** es entidad de pago autorizada en la UE (Stripe Payments Europe), acepta tarjetas, Apple Pay y Google Pay, cumple PSD2 (autenticación reforzada) y su **Stripe Connect** reparte el dinero a los repartidores sin que pase por tu cuenta (evita necesitar licencia de pago propia).

## Cómo funciona en T lo traigo
1. El cliente hace el pedido **sin pagar** (los precios son orientativos).
2. El repartidor compra, hace la **foto del ticket** y escribe el total.
3. El cliente ve el ticket y el **importe real** y entonces paga: con tarjeta (página segura de Stripe) o confirmando que pagará en efectivo.
4. Solo entonces el repartidor puede salir a repartir.
5. Al entregar, Stripe **transfiere al repartidor** su parte: servicio + envío + suplemento **menos la comisión de la plataforma** (20 % por defecto, en Admin → Tarifas). La gestión y las tarifas adicionales quedan para T lo traigo.
6. Reembolsos aprobados → devolución automática en Stripe, **sin la tarifa de servicio y gestión** (salvo pedido sin aceptar).

En el prototipo, sin claves de Stripe, aparece el botón «Simular pago con tarjeta (demo)».

## Qué tienes que hacer tú (una vez)
1. **Date de alta** como autónomo o crea una sociedad: Stripe exige una actividad económica real y el IBAN de cobro.
2. Crea la cuenta en https://dashboard.stripe.com (país: España) y completa la verificación.
3. En Stripe → **Connect** → activa *Express*, perfil de plataforma «marketplace».
4. En Stripe → Desarrolladores → **Claves API**: copia la clave secreta (`sk_test_…` para pruebas, `sk_live_…` en real).
5. En Stripe → Desarrolladores → **Webhooks** → «Añadir destino»:
   - URL: `https://qqqudthqgpojbipsevna.supabase.co/functions/v1/stripe-webhook`
   - Eventos: `checkout.session.completed`, `checkout.session.expired`, `account.updated`
   - Copia el «secreto de firma» (`whsec_…`).
6. En Supabase → Edge Functions → **Secrets**, añade `STRIPE_SECRET_KEY` y `STRIPE_WEBHOOK_SECRET`.
7. Prueba con la tarjeta de pruebas `4242 4242 4242 4242` (cualquier fecha futura y CVC) usando claves `sk_test_`.

Hasta que pongas las claves, la app avisa de que la pasarela no está configurada y el pago en efectivo funciona igual.
Comisiones orientativas de Stripe para tarjetas europeas: ~1,5 % + 0,25 € por pago (consúltalas en su web).
