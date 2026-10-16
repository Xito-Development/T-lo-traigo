# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

## [0.6.11] - 2026-10-16
### Añadido
- En el móvil del superadministrador, el aviso de versión nueva actualiza también el Panel de control.
- El Panel de control para Android avisa al abrirlo si hay una versión nueva y se actualiza desde ahí.
### Cambiado
- El Panel de control para Android lleva el mismo número de versión que la app.
### Corregido
- Correos: Brevo rechazaba los envíos sin nombre de destinatario (error 400).
- Correos más personales (saludo con el nombre, versión en texto y respuesta a soporte) para que lleguen a la bandeja principal.
- Web: logo para la bandeja de entrada (BIMI) y guía docs/CORREO_ENTREGA.md.

## [0.6.10] - 2026-10-16
### Añadido
- Panel de control para Android (superadministrador), con la misma estética y funciones que el de Windows: servicios, pedidos, actividad, errores, sugerencias, entidades, pendientes, usuarios y correo. Se publica con cada versión como TLoTraigo-Control.apk.
- Ajustes (superadministrador): «Panel de control» para descargarlo en Android o Windows.

## [0.6.9] - 2026-10-16
### Añadido
- Vigilante: el superadministrador recibe por correo las caídas de la web o de la compilación, errores de la app, pedidos sin repartidor o reclamados, reclamaciones, reembolsos, entidades por verificar o vencidas y fallos de correo (lo urgente al momento, recordatorio cada 24 h y aviso de «resuelto») y un resumen cada mañana.
- Ajustes → Cuenta (superadministrador): activar los avisos, enviar el resumen al momento y ver los avisos activos.
### Corregido
- Al abrir la app vuelve a avisar de que hay una versión nueva (versión de GitHub).

## [0.6.8] - 2026-10-16
### Añadido
- Ajustes → Cuenta: correo de acceso, correo para avisos, correos de compras, cambiar contraseña (con medidor de seguridad) y correo, descargar mis datos, cerrar sesión en todos los dispositivos y eliminar cuenta.
- Superadministrador: configuración del correo de compras (clave de Brevo), correo de prueba y últimos envíos.
- Programa de Windows 1.1: pedidos en curso con detalle y filtros, usuarios, correo de compras, acciones sobre entidades (verificar, renovar, activar, pausar), responder reclamaciones y resolver reembolsos, búsqueda rápida (Ctrl+K), gráficas por estado y por entidad, cuotas al mes, historial y disponibilidad de cada servicio en 24 h, exportar a Excel y aviso de versión nueva.
### Corregido
- Los correos de compras no se enviaban porque faltaba la clave de Brevo; ahora se puede guardar desde la app o el programa de Windows. Las cuentas creadas con nombre de usuario pueden añadir un correo para recibirlos.
- La compilación del programa de Windows en GitHub fallaba porque la carpeta de recursos del instalador no se subía al repositorio.

## [0.6.7] - 2026-10-15
### Añadido
- Web: preguntas frecuentes que se abren y cierran con animación, de una en una.
### Corregido
- Web: en modo oscuro, los botones «Escríbenos» y «Solicitar una demostración» se volvían ilegibles al pulsarlos.
- Compilación del programa de Windows en GitHub más robusta (el instalador queda también como archivo de la ejecución).

## [0.6.6] - 2026-10-15
### Añadido
- Web con más vida: franja de cifras animadas, pueblos en movimiento, tarjetas de entidades con iconos e inclinación 3D, brillo en los botones y preguntas que se despliegan con suavidad.
- Barra de desplazamiento de la web con los colores de la marca (también en modo oscuro).
- Claude Code en GitHub: escribe «@claude» en una incidencia para encargarle tareas (docs/CLAUDE_CODE.md).
### Corregido
- Web: varios títulos no llegaban a aparecer (Así de sencillo, Para mayores, Entidades, Preguntas frecuentes…).
- Web: el texto de «Para ayuntamientos y entidades» salía palabra a palabra.

## [0.6.5] - 2026-10-15
- El repartidor puede cambiar un producto que no hay por otro, o quitarlo; el cliente recibe un aviso en el chat y lo ve en su pedido y en el correo.
- Correos de las compras: resumen al hacer el pedido y factura o justificante al entregarlo (se puede desactivar en Ajustes).
- Verificación de entidades: datos fiscales (razón social, CIF/NIF validado, domicilio, representante) y documento acreditativo; sin verificar no reciben pedidos.
### Cambiado / - Icono de la web: el logo redondo sin fondo.
### Corregido
- «Sin conexión» al volver a la app: la conexión se renueva y las peticiones se reintentan solas. y líneas '- ') serán las notas de la próxima versión. publicar.bat lo vacía al publicar. -->
### Añadido
- Mis listas: listas de la compra guardadas, como listas de reproducción (nombre, emoji y color). Se compran enteras con un toque; se crean desde cero, desde la cesta o desde un pedido anterior.
- Seguimiento con mapa en directo: el cliente ve dónde está el repartidor (y su casa) y a cuántos kilómetros.
- La entidad elige los días de entrega y puede marcar días sin reparto (festivos, vacaciones); en la cesta no se pueden elegir.
- Nuevo programa para Windows «T lo traigo Control» (carpeta desktop/), que se publica aparte como «desktop-v1.0.0».
### Cambiado
- Resumen de la cesta con los productos bien visibles: foto o icono, cantidad, detalle y precio.
- Línea de seguimiento rediseñada: barra de progreso con la bolsa que avanza, pasos que se van completando y aviso de pago pendiente.
### Corregido
- La línea de seguimiento se descuadraba cuando el paso actual ocupaba dos líneas.
- La ubicación del repartidor no llegaba al cliente si el repartidor no estaba «disponible», si estaba parado o mientras el GPS buscaba señal; ahora se envía al momento y cada 10 segundos.

## [0.6.4] - 2026-10-15
- Transiciones suaves entre pantallas y entre pestañas (según la dirección), botones que se hunden al pulsarlos, mensajes del chat que aparecen con suavidad y tarjetas de la Lista fácil que cambian de color al añadir.
- Si en Android están desactivadas las animaciones (accesibilidad), la app no anima.
- Animación de arranque: la bolsa cae con rebote, brotan las hojas, aparece la casa, estela de reparto y el nombre se revela; sin destellos blancos al abrir.
- Interruptores animados: icono ✓/✕ que gira, pequeño rebote y vibración al cambiar.
- Actualización automática: la versión de GitHub descarga la nueva versión en segundo plano cuando la app no se usa (1 minuto sin tocarla o al salir) y avisa con una notificación y un aviso «Instalar»; la versión de Google Play la descarga desde la tienda y la instala al salir de la app, avisando.
### Corregido
- Ajustes: las opciones de tamaño de letra se salían de la pantalla con letra grande; ahora se reparten en varias líneas y hay vista previa. y líneas '- ') serán las notas de la próxima versión. publicar.bat lo vacía al publicar. -->
### Añadido
- Lista dictada más inteligente: entiende listas sin comas, quita muletillas («eh», «mmm», «bueno»…) y obedece correcciones («no no», «perdón», «espera»).
- Antes de añadir la lista dictada se puede revisar y corregir: cantidad, texto y quitar artículos.
- Dictado por voz también en el chat.
- Aviso «Añadido a la cesta» en la Lista fácil.
- Versión de Google Play: «Buscar actualizaciones» en Acerca de.
- Pruebas automáticas antes de publicar cada versión.
### Corregido
- Lista fácil: toda la pantalla se desplaza a la vez (antes solo se movía la cuadrícula y quedaba atascada).

## [0.6.3] - 2026-10-14
### Añadido
- Versión de Google Play: las actualizaciones se descargan e instalan desde la tienda (In-App Updates); «Valorar en Google Play» en Acerca de.
- Versión de GitHub: opción de pasarse a Google Play cuando esté publicada.
- Web: cuando la app esté en Google Play, «Descargar» lleva a la tienda y ofrece la alternativa del APK de GitHub (se configura desde el modo edición).

## [0.6.2] - 2026-10-14
- El administrador de la entidad recibe avisos de reclamaciones y cambios de pedidos.
- Web: preguntas frecuentes e imagen para compartir en WhatsApp y redes.
### Cambiado / ### Corregido
- Doble margen superior en la pestaña «Mi entidad».
- El botón «Atrás» de un administrador de entidad volvía a «Comprar» en lugar de a su panel.
- En pedidos ajenos, la entidad ya no ve botones de pagar, propina ni reembolso; en el chat escribe como administración. y líneas '- ') serán las notas de la próxima versión. publicar.bat lo vacía al publicar. -->
### Añadido
- Mi entidad → Reclamaciones: la entidad ve y responde las hojas de reclamaciones de sus pedidos.
- La pantalla de compra muestra los días y el horario de reparto de la entidad; en la cesta solo se pueden elegir días con reparto.
- Web: sección «Para ayuntamientos y entidades», declaración de accesibilidad y política de privacidad ampliada (entidades, Bizum y plazos de conservación).
### Cambiado
- La foto del ticket es siempre obligatoria (base de la trazabilidad).
- Condiciones actualizadas: hay que volver a aceptarlas.
- Los administradores no ven botones de cliente (editar, cancelar, reclamar) en pedidos ajenos.
### Corregido
- Formas de pago, chat y tickets visibles para el administrador de la entidad en sus pedidos.

## [0.6.1] - 2026-10-14
### Añadido
- Formas de pago por entidad: efectivo, Bizum al repartidor y Bizum a la entidad.
- Con Bizum, el usuario ve importe, número y concepto con botones para copiar y confirma el envío; el repartidor o la entidad confirma el cobro.
- Número de Bizum del repartidor en sus Ajustes.
- Estado del cobro en cada pedido y en el informe mensual (cobrado en efectivo, por Bizum y pendiente), también en el Excel.
- Botón «Marcar como cobrado» para administradores de la entidad.
### Corregido
- Al marcar un pedido entregado en efectivo queda registrado quién lo cobró y cuándo.

## [0.6.0] - 2026-10-14
### Añadido
- Entidades (modelo de suscripción): el superadministrador crea entidades (ayuntamientos, asociaciones, tiendas) y envía un código a su administrador.
- Panel «Mi entidad» para el administrador de localidad: aceptar o pausar pedidos, pueblos, repartidores, códigos de invitación, horario, días y reglas del servicio.
- Códigos de invitación para administradores, repartidores y usuarios (en Ajustes, en la bienvenida o con el enlace de la web).
- Mensaje y teléfono de la entidad en la pantalla de compra.
- Servicio «gratis para el usuario» por entidad: sin tarifas ni propinas; la compra la paga el usuario en efectivo o la gestiona la entidad.
- Informe mensual de la entidad (totales, pueblos, repartidores, tiempos y reclamaciones) en PDF y Excel (CSV).
- Justificante en PDF de cada pedido en los servicios gratuitos.
- Suscripción de cada entidad (plan, cuota, «pagada hasta»), con desactivación automática a los 7 días de vencer.
### Cambiado
- El horario, los días de reparto, el ticket obligatorio y el máximo de artículos los decide cada entidad.

## [0.5.5] - 2026-10-13
### Añadido
- Lista fácil: escribe o dicta por voz tu lista («2 litros de leche, pan y 6 huevos») y se añade sola a la cesta.
- Administración → Lista fácil: crear, editar, ocultar y borrar los artículos básicos (precio, IVA, icono, opciones).
- Chat en directo entre cliente y repartidor durante el pedido, con mensajes rápidos y notificaciones.
- Editar el pedido (cantidades, quitar, añadir más y notas) hasta que el repartidor llega al súper.
- Reclamar un pedido no recibido: plazo de 6 horas para entregarlo y botón para llamar al 112 en emergencias.
- Modo oscuro en la web, automático o con el botón 🌙.
- Propinas al terminar el pedido (1, 2, 3, 5 € u otra cantidad), íntegras para el repartidor; se ven en sus ganancias.
- Interruptores en Administración → Tarifas: «Pagos con tarjeta» y «Modo demostración».
- Correos de la app con diseño nuevo y contactos de soporte y de Xito Development.
### Cambiado
- Horario de entrega de 9:00 a 22:00, con franjas de mañana, tarde y noche.
- Pagos con tarjeta desactivados por ahora (solo efectivo); se activan desde Administración → Tarifas.
### Corregido
- Seguridad: los pagos simulados solo funcionan en modo demostración.
- Seguridad: un repartidor ya no puede cambiar su cuenta de cobro ni su autorización.
- Estadísticas: límite contra envíos masivos de eventos.
- Modelo «vecinos colaboradores»: el repartidor recibe solo sus gastos de desplazamiento (km ida y vuelta) y no necesita ser autónomo; servicio y gestión son de la plataforma (Administración → Tarifas).

## [0.5.4] - 2026-10-13
### Añadido
- Correos de la app con diseño nuevo (código de verificación, contraseña, acceso, cambio de correo, invitación y seguridad), enviados desde tlotraigo@unocerobits.com.
- Contacto de soporte (support@tlotraigo.unocerobits.com) y de Xito Development (xito@unocerobits.com) en la app y en la web.
### Cambiado
- Política de privacidad: Brevo como servicio de envío de correos.

## [0.5.3] - 2026-10-13
### Añadido
- Dirección paso a paso: destinatario, teléfono, pueblo (código postal y provincia), calle y número, piso e indicaciones.
- Lista fácil: buscador, «Lo de siempre», más artículos (zumos, conservas, congelados, mascotas, bebé…) y «Otra cosa» con texto libre.
- Resumen de la cesta con el desglose de cada producto.
- El repartidor puede apuntar el precio real de cada producto; el cliente y la factura muestran si salió más caro (▲) o más barato (▼).
- Factura visible en la app, con botones «Abrir en PDF» y «Compartir».
- Administración → Tarifas: añadir y quitar pueblos, añadir y eliminar tarifas adicionales, gestión, margen y comisión de la plataforma.
- Diseño adaptable a tablets, plegables y pantallas grandes (app y web).
### Cambiado
- El repartidor recibe sus tarifas menos la comisión de la plataforma (20 % por defecto); la gestión y las tarifas adicionales son de la plataforma.
- El ticket, una vez guardado, no se puede modificar.
- Pedidos, repartos, reembolsos y reclamaciones se actualizan solos.
- Bizum aparece como «Próximamente».
### Corregido
- Los artículos de la lista fácil desaparecían al abrir la cesta.
- «Repetir compra» no incluía los artículos de la lista fácil.

## [0.5.2] - 2026-10-13
### Cambiado
- El cliente ya no paga al hacer el pedido: paga cuando el repartidor sube la foto del ticket con el importe real (tarjeta o efectivo).
- El repartidor sale a repartir cuando el cliente ha pagado o confirmado el pago en efectivo.
- Al entregar un pedido pagado con tarjeta, se transfiere automáticamente su parte al repartidor (Stripe Connect).
### Añadido
- Prototipo de demostración: botón «Simular pago con tarjeta (demo)» mientras no esté configurada la pasarela.

## [0.5.1] - 2026-10-13
### Añadido
- Pasarela de pago con Stripe: reserva del importe máximo, cobro del importe real del ticket y reparto al repartidor (Stripe Connect).
- Cancelación con reembolso (sin tarifa de servicio y gestión una vez aceptado) y solicitud de reembolso tras la entrega.
- Precios orientativos: foto del ticket obligatoria, importe máximo y tarifa de gestión variable.
- «Lista fácil»: elige pan, leche, fruta… y concreta tipo, formato, marca y cantidad.
- Bienvenida al registrarse: para quién es la app, ajustes de accesibilidad, estadísticas y vínculo familiar.
- Facturas en PDF por pedido, hojas de reclamaciones y estadísticas/errores propios con consentimiento (web y app).
- Aviso de cadena de frío al repartidor; alérgenos rellenados.
### Cambiado
- Sin penalizaciones a repartidores. No se puede cancelar cuando el repartidor ya está en el súper o en reparto.
- Quitado el espacio publicitario.

## [0.5.0] - 2026-10-13
### Añadido
- Repartidores: avisos de pedidos nuevos en el móvil mientras están disponibles.
- Repartidores: botón «Ruta» con navegación GPS y ubicación compartida con el cliente solo durante el reparto.
- Clientes: ven a qué distancia está su repartidor cuando el pedido va en camino.
- Ficha de producto con IVA desglosado, alérgenos e ingredientes; productos +18 con confirmación de edad.
- Confirmación del pedido en dos pasos y botón «Confirmar pedido con obligación de pago».
- Condiciones de uso, condiciones para repartidores y aviso legal; aceptación obligatoria en la app.
- Candidatura de repartidor con declaraciones (mayoría de edad, autónomo, seguro, alimentos, geolocalización).
- Nuevo logotipo de texto «T lo traigo» y espacio «¡TU ANUNCIO AQUÍ!».
### Cambiado
- Web con la estética de la app, logotipo animado y fuentes propias (sin cookies ni conexiones a terceros).
- Web propia en https://tlotraigo.unocerobits.com

## [0.4.1] - 2026-10-12
### Añadido
- Actualizaciones dentro de la app (versión GitHub) con animación de descarga e instalación.
- Web: modo edición oculto, descarga directa del último APK y nuevas animaciones.
- Vigilante de Descargas con carpeta configurable y reemplazo completo del proyecto.

## [0.4.0] - 2026-10-12
### Añadido
- Nuevo icono de la app (bolsa con verduras y casa, con estela de reparto), icono monocromo para Android 13+ y gráficos para Google Play.
- Pantalla «Acerca de» con los logos de Xito Development y UNOCEROBITS, contacto, licencia y software de terceros.
- Eliminación de cuenta desde Ajustes (requisito de Google Play y App Store); los pedidos se conservan anonimizados.
- Landing page y política de privacidad para `unocerobits.com/t-lo-traigo`, con la estética de unocerobits.com (pizarra, turquesa y Raleway), red de puntos animada y animaciones al desplazarse.
- Web: mezcla de la estética de la app (Atkinson, pino y mostaza) con la de UNOCEROBITS; animaciones mejoradas (letras de la portada, capas con parallax, títulos que se despliegan, total que rueda, mapa con pines que caen).
- Web: modo edición oculto (15 toques rápidos en el logo) para cambiar los textos con una cuenta de administrador; se guardan en Supabase.
- Web: «Descargar» baja directamente el último APK publicado y muestra versión, tamaño y fecha.
- App: actualizaciones dentro de la app (versión GitHub) con animación de descarga e instalación.
- Zona de lanzamiento: Villaquejida, Villafer, Cimanes de la Vega, Villamandos, Campazas, Villaornate y Castro, Valderas y La Antigua.
- Ficha de Google Play (fastlane) y subida opcional a Play desde GitHub Actions.
- Publicación en un paso: `publicar.bat` sube la versión, actualiza el CHANGELOG y hace push; GitHub Actions crea la Release sola.
- `instalar-vigilante.bat`: los ZIP descargados de Claude reemplazan solos la carpeta del proyecto (con copia de seguridad previa); `cambiar-ubicacion.bat` para elegir otra carpeta.
- `configurar-firma.bat`: crea la clave de firma y guarda los secretos en GitHub en un paso.
### Cambiado
- Dos variantes: `github` (APK de las Releases, con actualizador) y `play` (AAB para Google Play, sin actualizador, como exige Play).
- Cada Release incluye también `TLoTraigo.apk` con nombre fijo para la descarga directa.
- Repositorio en la organización Xito-Development (`github.com/Xito-Development/T-lo-traigo`).
- La versión se define en `version.properties` (fuente única para la app y el workflow).
### Cambiado
- `compileSdk`/`targetSdk` 36 (Android 16), AGP 8.11.1 y Gradle 8.13; sigue funcionando desde Android 5.0.
- Licencia propietaria (todos los derechos reservados).

## [0.3.0] - 2026-10-12
### Añadido
- Panel de administración: alta y edición de productos, 87 iconos con buscador o foto propia, precio con desglose de IVA (4/10/21 %).
- Tarifas configurables: servicio por importe y cantidad, envío por distancia, suplemento por entrega en el día.
- Superadministrador (DNL-Owner): gestión de roles, autorización de repartidores y revisión de candidaturas.
- Candidatura de repartidor desde Ajustes (solo cuentas de repartidor).
- Repartidores: aceptar o rechazar pedidos, penalización si un pedido supera 48 h sin aceptar, pantalla de ganancias.
- Valoraciones del repartidor y de la entrega, con opiniones.
- Entregas programadas: lo antes posible o día (hasta 14 días) y franja de mañana o tarde.
- 7 paletas de color, modo claro/oscuro, alto contraste claro y oscuro y 5 tipos de letra.
### Corregido
- Precios de la cesta guardada se actualizan al abrirla; productos retirados se eliminan.
- Sesión caducada devuelve a la pantalla de entrada.
- El cliente ve y puede llamar a su repartidor.

## [0.2.0] - 2026-10-12
### Añadido
- Verificación del correo con código de 6 cifras y recuperación de contraseña por código.
- Aprobación de repartidores, cesta persistente, pago simulado marcado como pagado.
### Corregido
- Texto negro en temas oscuros, doble margen superior, destello blanco al abrir y desbordes con letra grande.

## [0.1.0] - 2026-10-08
- Primera versión: catálogo, cesta, pedidos, panel de repartidor, Supabase y CI/CD.
