# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

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
