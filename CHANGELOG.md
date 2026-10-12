# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

## [0.4.0] - 2026-10-12
### Añadido
- Nuevo icono de la app (bolsa con verduras y casa, con estela de reparto), icono monocromo para Android 13+ y gráficos para Google Play.
- Pantalla «Acerca de» con los logos de Xito Development y UNOCEROBITS, contacto, licencia y software de terceros.
- Eliminación de cuenta desde Ajustes (requisito de Google Play y App Store); los pedidos se conservan anonimizados.
- Landing page y política de privacidad para `unocerobits.com/t-lo-traigo`.
- Ficha de Google Play (fastlane) y subida opcional a Play desde GitHub Actions.
- Publicación en un paso: `publicar.bat` sube la versión, actualiza el CHANGELOG y hace push; GitHub Actions crea la Release sola.
- `configurar-firma.bat`: crea la clave de firma y guarda los secretos en GitHub en un paso.
### Cambiado
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
