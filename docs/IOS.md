# Llevar T lo traigo a iPhone

## Recomendación: Kotlin Multiplatform + Compose Multiplatform
La app ya está escrita en Kotlin y Jetpack Compose. Compose Multiplatform permite compartir la mayor
parte de la interfaz y toda la lógica con iOS (versión estable para iOS desde 2025).

| Capa | Reutilizable | Trabajo |
|---|---|---|
| Modelos, tarifas, repositorio (kotlinx.serialization) | ~100 % | Pasar a módulo `shared` |
| Cliente HTTP (OkHttp) | Sustituir | Ktor Client (motor Darwin en iOS) |
| Pantallas Compose | ~85 % | `androidx.*` → equivalentes multiplataforma; Coil 3 |
| DataStore / SharedPreferences | Sustituir | DataStore multiplataforma o `multiplatform-settings` |
| Lectura en voz alta, llamar, mapas, galería | Nativo | `expect/actual` (AVSpeechSynthesizer, `tel:`, Apple Maps, PHPicker) |
| Backend Supabase | 100 % | Sin cambios |

## Requisitos de Apple
- Mac con Xcode y cuenta Apple Developer (99 €/año).
- App Store: eliminación de cuenta dentro de la app (✅ ya implementada), política de privacidad (✅),
  etiqueta de privacidad, iniciar sesión con Apple solo si se añaden logins de terceros.
- Bienes físicos: se pueden cobrar con Stripe/Apple Pay (no requiere compras integradas).
- Ajustar `minSdk` equivalente: iOS 15 o superior.

## Plan por fases
1. Extraer `shared` (modelos, repositorio, tarifas) con Ktor; Android sigue igual.
2. Mover pantallas a `composeApp` multiplataforma.
3. Implementar `actual` de iOS (voz, llamadas, mapas, fotos) y probar en TestFlight.
4. Publicar en App Store.
Alternativa más rápida pero menos nativa: PWA basada en la landing (sin voz ni notificaciones fiables en iOS).
