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

## Seguridad de los datos (formulario de Play)
| Tipo | Recogido | Compartido | Finalidad | Opcional |
|---|---|---|---|---|
| Nombre, correo, teléfono | Sí | No | Funcionamiento, gestión de cuenta | No |
| Dirección | Sí | No (solo la ve el repartidor del pedido) | Funcionamiento | No |
| Historial de compras | Sí | No | Funcionamiento | No |
| Fotos (solo administradores, imágenes de producto) | Sí | No | Funcionamiento | Sí |
Cifrado en tránsito: sí (HTTPS). Eliminación: sí, desde la app y por correo.

## Revisión
Facilitar a Google una cuenta de pruebas de cliente (no la del superadministrador).
