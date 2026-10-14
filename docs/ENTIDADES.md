# Modelo de entidades (suscripción)

**Quién es quién**
- **Superadministrador (T lo traigo):** crea entidades, activa o desactiva su suscripción y les da el primer código.
- **Administrador de localidad (gestor):** gestiona su entidad: pueblos, repartidores, códigos, horarios y si acepta pedidos.
- **Repartidores:** se unen con un código de la entidad; ven solo los pedidos de los pueblos de su entidad.
- **Usuarios:** se registran eligiendo su pueblo (quedan en la entidad de ese pueblo) o con un código.

**Alta de una entidad (2 minutos)**
1. Admin → Entidades → «Nueva entidad»: nombre, tipo, contacto y pueblos.
2. Se genera un código de 8 caracteres: «Enviar invitación» (WhatsApp/correo).
3. El responsable instala la app, crea su cuenta y escribe el código en Ajustes → «Tengo un código». Ya ve la pestaña «Mi entidad».

**Panel «Mi entidad»:** Resumen (aceptar/pausar pedidos, contadores), Pedidos (en curso y reclamados), Repartidores (invitar, autorizar, quitar),
Pueblos (añadir, envío, distancia, activar), Invitar (códigos para administradores, repartidores o usuarios) y Ajustes (mensaje, teléfono,
ticket obligatorio, efectivo, máximo de artículos, horario y días de reparto, autorización automática).

**Reglas que aplica el servidor:** entidad inactiva o pausada → no se puede pedir; horario y días de reparto; ticket obligatorio según la entidad;
máximo de artículos; cada gestor solo ve los datos de su entidad.

## Pagos en el modelo de suscripción
- **La entidad paga a T lo traigo** una cuota mensual (Admin → Entidades → entidad → Ajustes → «Suscripción»: plan, cuota, «pagada hasta»). Se cobra por **factura y transferencia/domiciliación**, fuera de la app. Si pasan 7 días de la fecha sin renovar, la entidad se desactiva sola.
- **Servicio «Gratis para el usuario»** (por defecto en entidades nuevas): sin tarifas de servicio, gestión, distancia ni propinas; solo el envío que fije la entidad (normalmente 0).
- **La compra del súper**: o el usuario paga el ticket en efectivo al repartidor, o «la entidad se encarga del pago» (bonos sociales, cuenta municipal…). La app guarda el ticket y genera un **justificante** de cada pedido.
- **Repartidores**: los organiza y, si procede, los compensa la entidad (voluntarios o personal propio). La app no les paga nada.
- **Informe mensual** (pestaña «Informe»): totales, por pueblo, por repartidor, tiempos y reclamaciones; descargable en PDF y en Excel (CSV).

## Efectivo y Bizum
- La entidad elige en Ajustes: **Efectivo**, **Bizum al repartidor** (el repartidor pone su número en sus Ajustes) y/o **Bizum a la entidad** (número de Bizum para empresas de su banco).
- Tras el ticket, el usuario ve el importe real. Con Bizum: número, importe y concepto «Pedido N» para copiar → «Ya he enviado el Bizum» → el repartidor puede salir → repartidor o entidad pulsan «Cobrado».
- Con efectivo: se marca cobrado al entregar. El informe mensual separa efectivo, Bizum y pendiente.
- La app no se conecta a Bizum ni toca el dinero: el pago se hace desde la app del banco del usuario.
