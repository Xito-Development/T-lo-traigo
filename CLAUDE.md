# T lo traigo — contexto para Claude Code

App Android (Kotlin + Jetpack Compose) tipo "Uber Eats de supermercado" para personas mayores de pueblos de León
(zona de lanzamiento: Villaquejida y alrededores). Propiedad: Xito Development y UNOCEROBITS. Licencia propietaria.

## Repositorio y publicación
- Remoto: https://github.com/Xito-Development/T-lo-traigo (rama `main`).
- Versión: `version.properties` (`VERSION_NAME=x.y.z`). `publicar.bat` calcula la siguiente a partir del tag más alto publicado.
- Notas de la próxima versión: escribirlas en `CAMBIOS_PENDIENTES.md` (### Añadido/Cambiado/Corregido + líneas `- `); publicar.bat las pasa al CHANGELOG y vacía el archivo. No editar a mano versiones ya publicadas del CHANGELOG.
- Publicar: subir la versión, añadir la entrada en `CHANGELOG.md` (Keep a Changelog, en español), actualizar
  `fastlane/metadata/android/es-ES/changelogs/default.txt` y hacer push a `main`. GitHub Actions crea el tag `vX.Y.Z`
  y la Release con `TLoTraigo-x.y.z.apk`, `TLoTraigo.apk` (nombre fijo para la web y el actualizador) y el AAB.
  Atajo en Windows: `publicar.bat`.
- Firma: secretos `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (`configurar-firma.bat`).
  Clave local en `%USERPROFILE%\TLoTraigo-firma\` — nunca subirla al repo. Opcional `PLAY_SERVICE_ACCOUNT_JSON`.
- `instalar-vigilante.bat`: los ZIP descargados de Claude reemplazan esta carpeta (copia previa en %LOCALAPPDATA%\TLoTraigo\copias; se conserva .git). Cambiar carpeta: `cambiar-ubicacion.bat`.

## Build
- JDK 17, Android SDK 36, AGP 8.11.1, Gradle 8.13, Kotlin 2.0.21. `minSdk 21`, `targetSdk 36`.
- No subir versiones de AndroidX que exijan minSdk > 21 (core 1.15, activity 1.9.3, lifecycle 2.8.7, navigation 2.8.5, Compose BOM 2024.12.01).
- Variantes: `github` (actualizador integrado, `REQUEST_INSTALL_PACKAGES` y FileProvider en `src/github/`) y `play` (sin actualizador; Play no permite autoactualizarse).
- `./gradlew assembleGithubDebug` · release: `./gradlew assembleGithubRelease bundlePlayRelease`.

## Backend (Supabase)
- Proyecto `qqqudthqgpojbipsevna` (eu-west-3). Cliente propio en `data/Api.kt` (OkHttp → GoTrue/PostgREST/Storage).
- Precios y tarifas solo en servidor: `_order_quote`, `quote_order`, `create_order`. RLS en todas las tablas.
- Roles: cliente, repartidor, admin, superadmin (`DNL-Owner` = dnl-owner@tlotraigo.app).
- Textos de la web en `site_content` (lectura pública, escritura solo admin).
- Migraciones en `supabase/migrations/` (las posteriores a v0.2 están en remoto: `supabase db pull`).

## Convenciones
- Textos en castellano de España, claros y sencillos para personas mayores.
- Accesibilidad primero: áreas táctiles ≥ 48 dp (56–64 en modo fácil), alto contraste, lectura en voz alta.
- Colores desde `MaterialTheme.colorScheme` (paletas en `ui/theme/Theme.kt`); iconos desde `AppIcons`.
- Web en `web/t-lo-traigo/` (→ unocerobits.com/t-lo-traigo/). Elementos con `data-k` son editables desde la propia web
  (15 toques rápidos en el logo + cuenta admin).
