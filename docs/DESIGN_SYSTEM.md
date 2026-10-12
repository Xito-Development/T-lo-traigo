# Sistema de diseño de T lo traigo

## Tokens
### Color (paleta por defecto «Montaña»)
| Token | Hex | Uso |
|---|---|---|
| pino | `#2E6B57` | Primario, fondo del icono |
| pino-hondo | `#1F4D3F` | Titulares, superficies oscuras |
| mostaza | `#F2B544` | Bolsa del icono, acentos, números de paso |
| mostaza-hondo | `#DE9A2A` | Pliegue de la bolsa, foco |
| hoja | `#7BC9A4` | Verduras del icono, iconografía |
| crema | `#FFF8EC` | Casa del icono, asas |
Las 7 paletas de la app se generan desde 3 colores semilla (`schemeFor()` en `Theme.kt`).
Alto contraste: claro (blanco/negro/azul marino) y oscuro (negro/amarillo), WCAG AAA.

### Tipografía
Atkinson Hyperlegible (por defecto), Andika, Lato, sistema y serif. Escala: display 38, headline 32/28/24,
title 22/18/16, body 18/16/14, label 17/15/13 sp. La letra se multiplica por 1,0 – 1,5 (tope 1,9 con la del sistema).

### Forma y movimiento
Radios 8/12/18/26/34 dp (extra pequeño → extra grande). Transiciones 380 ms `EaseOutCubic`; colores de tema 450 ms.
Áreas táctiles mínimas: 48 dp (normal) y 56–64 dp (modo fácil).

## Icono de la app (extend)
**Problema:** el icono anterior (bolsa con asa centrada) se leía como un candado a tamaño pequeño.
**Diseño:** bolsa de la compra ancha con pliegue superior, dos asas desplazadas, tres hojas verdes que asoman
(producto fresco), una casa en el frontal (entrega a domicilio) y una estela de tres líneas (reparto).
| Capa | Archivo | Notas |
|---|---|---|
| Fondo | `drawable/ic_launcher_background.xml` | Pino con halo `#357762` |
| Primer plano | `drawable/ic_launcher_foreground.xml` | Dentro de la zona segura de 66 dp |
| Monocromo | `drawable/ic_launcher_monochrome.xml` | Iconos temáticos de Android 13+ (casa recortada) |
| Android 5–7 | `mipmap/ic_launcher.xml` | Círculo + primer plano |
| Fuente | `docs/brand/icon.svg` | Exportar a 512 px para tiendas |
**Accesibilidad:** contraste bolsa/fondo 4,6:1; silueta reconocible a 24 px.
