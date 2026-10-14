# Publicación en Google Play

## Estado de requisitos
| Requisito | Estado |
|---|---|
| `targetSdk` 36 (obligatorio para apps nuevas y actualizaciones desde el 31/08/2026) | ✅ |
| AAB firmado (Play App Signing con clave de subida) | ✅ workflow genera `.aab` |
| `versionCode` creciente | ✅ `github.run_number` |
| Política de privacidad pública | ✅ `web/t-lo-traigo/privacidad.html` → subir a unocerobits.com |
| Eliminación de cuenta en la app y por web | ✅ Ajustes + `privacidad.html#eliminar` |
| Icono 512×512 y gráfico 1024×500 | ✅ `fastlane/metadata/android/es-ES/images/` |
| Capturas de pantalla (mín. 2 de móvil) | ⬜ hacerlas desde el móvil |
| Cuenta de desarrollador (25 USD, verificación de identidad) | ⬜ |
| Sin autoactualización en Play (variante `play` sin `REQUEST_INSTALL_PACKAGES`) | ✅ |
| Pagos de bienes físicos (Stripe/Bizum) | ✅ permitido fuera de Google Play Billing |

## Pasos
1. Crear la cuenta en Play Console y la app «T lo traigo» (idioma es-ES, app gratuita).
2. Subir **manualmente la primera** AAB a *Pruebas internas* (Play exige una subida manual inicial).
3. Completar: clasificación de contenido, público objetivo (mayores de 18), anuncios = no,
   seguridad de los datos (tabla de abajo), acceso a la app (credenciales de revisión, ver más abajo).
4. Crear una cuenta de servicio en Google Cloud con acceso a la app y ejecutar
   `configurar-firma.bat -PlayJson C:\ruta\cuenta.json`. Desde ese momento, cada `publicar.bat` sube la AAB a
   *Pruebas internas* automáticamente.
5. Las cuentas nuevas de desarrollador personal deben hacer una prueba cerrada (12 testers, 14 días)
   antes de producción.

## Declaraciones de permisos (Play Console → Contenido de la app)
| Permiso | Justificación para Google |
|---|---|
| `FOREGROUND_SERVICE_DATA_SYNC` | Comprobar pedidos nuevos y avisar al repartidor mientras se marca como disponible |
| `FOREGROUND_SERVICE_LOCATION` + ubicación precisa | Compartir la posición del repartidor con el cliente solo mientras su pedido está «en camino»; la app muestra un aviso destacado antes de pedir el permiso. No usa `ACCESS_BACKGROUND_LOCATION` |
| `POST_NOTIFICATIONS` | Avisos de pedidos nuevos |
Vídeo para la declaración: grabar el flujo «Estoy disponible → aviso → aceptar → Salgo a repartir → ubicación».

## Seguridad de los datos (formulario de Play)
| Tipo | Recogido | Compartido | Finalidad | Opcional |
|---|---|---|---|---|
| Nombre, correo, teléfono | Sí | No | Funcionamiento, gestión de cuenta | No |
| Dirección | Sí | No (solo la ve el repartidor del pedido) | Funcionamiento | No |
| Historial de compras | Sí | No | Funcionamiento | No |
| Fotos (solo administradores, imágenes de producto) | Sí | No | Funcionamiento | Sí |
| Ubicación precisa (solo repartidores en reparto) | Sí | Con el cliente del pedido, por acción del usuario | Funcionamiento | No (para repartir) |
Cifrado en tránsito: sí (HTTPS). Eliminación: sí, desde la app y por correo.

## Ficha
Correo de contacto de la ficha: **support@tlotraigo.unocerobits.com** · Web: https://tlotraigo.unocerobits.com · Privacidad: https://tlotraigo.unocerobits.com/privacidad.html

## Revisión
Facilitar a Google una cuenta de pruebas de cliente (no la del superadministrador).


## Al publicar en Google Play (actualizaciones)
1. En `app/build.gradle.kts` cambia `PLAY_PUBLISHED` a `true` y publica una versión nueva.
2. En la web: 15 toques en el logo → modo edición → en «Descargar» rellena el enlace de Google Play
   `https://play.google.com/store/apps/details?id=es.unocerobits.tlotraigo` → Guardar. Los botones irán a Play y aparecerá
   «¿No tienes Google Play? Descarga el APK desde GitHub».
3. La versión de Play se actualiza desde la tienda (In-App Updates: descarga en segundo plano e instala). Para forzar una
   actualización urgente, súbela en Play Console con **prioridad 4 o 5**.
4. Quien tenga la versión de GitHub sigue actualizándose desde GitHub; puede pasarse a Play desinstalando antes la app
   (las firmas de GitHub y de Play son distintas si usas la firma de apps de Google Play).
