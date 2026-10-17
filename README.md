# T lo traigo

<p><img src="docs/brand/icon.svg" width="72" alt="T lo traigo"></p>

**La compra del súper, en la puerta de casa.** App para que las personas mayores de los pueblos (o sus familiares) pidan la compra y un repartidor de su servicio se la lleve a casa, con ticket, seguimiento y reclamaciones.

[![Última versión](https://img.shields.io/github/v/release/Xito-Development/T-lo-traigo?label=versi%C3%B3n&color=2E6B57)](https://github.com/Xito-Development/T-lo-traigo/releases/latest)
[![Compilación](https://github.com/Xito-Development/T-lo-traigo/actions/workflows/android.yml/badge.svg)](https://github.com/Xito-Development/T-lo-traigo/actions/workflows/android.yml)
[![Analizada con VirusTotal](https://img.shields.io/badge/VirusTotal-analizada-2E6B57?logo=virustotal&logoColor=white)](https://tlotraigo.unocerobits.com/seguridad.html)

**Descargar:** [TLoTraigo.apk](https://github.com/Xito-Development/T-lo-traigo/releases/latest/download/TLoTraigo.apk) (Android 5.0 o superior) · Web: [tlotraigo.unocerobits.com](https://tlotraigo.unocerobits.com)

## Qué ofrece
- **Pedir fácil:** Lista fácil con iconos, dictado por voz, listas guardadas y compras recurrentes, o catálogo.
- **Confianza:** foto del ticket con el precio real, seguimiento en el mapa, chat con el repartidor y reclamaciones.
- **Pago sencillo:** efectivo o Bizum, según el servicio de cada pueblo.
- **Accesible:** modo fácil, letra de pequeña a enorme, alto contraste, lectura en voz alta y passkeys.
- **Para entidades:** ayuntamientos y asociaciones ofrecen el servicio con sus propios repartidores, horarios y pueblos.

## Estructura del repositorio
| Carpeta | Contenido |
|---|---|
| `app/` | App Android (Kotlin + Jetpack Compose) |
| `web/t-lo-traigo/` | Web pública |
| `supabase/` | Base de datos (migraciones) y funciones del servidor |
| `control/`, `desktop/` | Herramientas internas del equipo (se publican como *pre-release*) |
| `docs/` | Marca, plantillas de correo y documentación (`docs/interno/`: notas del equipo) |
| `scripts/` | Publicación y utilidades |

## Versiones
- **Versiones estables** (`vX.Y.Z`): las que descargan los usuarios; la app se actualiza sola.
- **Pre-releases**: versiones de prueba (`vX.Y.Z-beta.N`) y herramientas internas (`panel-v…`, `desktop-v…`). No aparecen como «última versión».
- Historial completo: [CHANGELOG.md](CHANGELOG.md).

## Contribuir
Abre una [incidencia](https://github.com/Xito-Development/T-lo-traigo/issues/new/choose) para fallos o ideas. Para vulnerabilidades, lee [SECURITY.md](SECURITY.md).

Desarrollada por **Xito Development** y **UNOCEROBITS** · tlotraigo@unocerobits.com
