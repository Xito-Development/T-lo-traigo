# Historial de cambios

Todas las versiones de T lo traigo. Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/), versiones [semánticas](https://semver.org/lang/es/).

## [0.0.16] - 2026-10-31
### Añadido
- Accesos directos al mantener pulsado el icono de la app: Lista fácil, Mi cesta y Mis pedidos.
- Web: logo de VirusTotal junto al enlace al informe de seguridad de cada versión.
- Web: cuatro preguntas frecuentes nuevas (qué es, si hace falta saber usar el móvil, en qué pueblos funciona y cómo hacerse repartidor).
### Corregido
- Un doble toque rápido en «Hacer pedido» ya no puede crear dos pedidos iguales (también lo impide el servidor).
- Web: el modo oscuro forzado del navegador ya no estropea los colores (la web trae su propio modo oscuro) y la barra del navegador toma el color del tema.
- Web: mejor contraste de textos y botones en modo claro y oscuro en todas las páginas (revisión de accesibilidad sin fallos).

## [0.0.15] - 2026-10-30
### Cambiado
- Mejoras y correcciones.

## [0.0.14] - 2026-10-30
### Corregido
- Web: sin desplazamiento lateral en el móvil y logo nítido y sin fondo en la pestaña del navegador, en Google y al añadirla a la pantalla de inicio.

## [0.0.13] - 2026-10-30
### Cambiado
- Web: Google mostrará el nombre «T Lo Traigo • UNOCEROBITS» en los resultados (en lugar de la dirección).

## [0.0.12] - 2026-10-30
### Cambiado
- Transiciones más suaves entre pantallas y pestañas (fundido encadenado y desplazamiento corto) y botón de cesta con rebote.
- Fondo animado más fluido y con menos consumo de batería: aparece con un fundido y ya no da saltos cada 24 segundos.

## [0.0.11] - 2026-10-29
### Cambiado
- Antes de hacer un pedido, la app comprueba si hay versión nueva y obliga a actualizar (la cesta se queda guardada).
- Las novedades del aviso de actualización se pueden deslizar cuando son largas.
### Corregido
- En Android, el botón del correo ya no gasta el enlace en la web antes de abrir la app (antes la app decía «enlace caducado»); sin la app instalada se verifica en la web.

## [0.0.10] - 2026-10-29
### Cambiado
- Las filas que se pueden deslizar (por ejemplo, las secciones de Administración) avisan con la flecha unos segundos y luego se quedan quietas: menos gasto de batería.
### Corregido
- Con mala cobertura, la app podía quedarse varios segundos en la pantalla verde antes de mostrar la entrada.

## [0.0.9] - 2026-10-27

### Cambiado
- Fondo animado: los círculos se ven un poco más y el resto de efectos son más suaves; las figuras ya no se cruzan entre sí.
- Buscar «Tema y colores» abre esa sección de Ajustes ya desplegada.
- Web: títulos y descripciones correctos en todas las páginas, enlaces canónicos, vista previa al compartir, datos estructurados, mapa del sitio actualizado y páginas de uso interno fuera de los buscadores.

## [0.0.8] - 2026-10-25

### Añadido
- Fondo animado a elegir: círculos, burbujas, hojas, estrellas u olas; con negro puro (OLED) los efectos lucen más.

### Cambiado
- «Te lo llevamos a…» abre directamente tu dirección en Ajustes.
- Actualizar es obligatorio para seguir usando la app (en Google Play, actualización inmediata).
- Los avisos de la portada salen de uno en uno y por orden (actualización, permisos, tutorial, novedades).
- Portada más sencilla y con menos desplazamiento: la Lista fácil arriba y un dibujo nuevo para su botón.
- Ajustes ordenados: tus datos y dirección primero y «Colores, tema y letra» plegado.
- El negro puro solo se ofrece con el tema oscuro.

### Corregido
- El fondo animado ya no se dibuja encima de menús y tarjetas.

## [0.0.7] - 2026-10-25

### Cambiado
- Lista fácil: nueva revisión de la lista escrita o dictada, con una tarjeta grande por artículo (dibujo, nombre, + y − y «Quitar»), «Dictar más» y «Cambiar lo escrito».
- Cabeceras ilustradas en la Lista fácil y en Mis pedidos, y confirmaciones con dibujo (borrar, cancelar, vaciar la cesta, pagar…).

## [0.0.6] - 2026-10-25
### Añadido
- Botón grande «Lista fácil» en la portada y el catálogo completo debajo.
- Dictado que entiende mejor a las personas mayores (muletillas, «media docena», «un par», diminutivos) y escucha continua hasta «ya está».
- Pantalla de permisos explicada según el rol, también desde Ajustes.
- Repartidor: «Llamar al mayor» y «Llamar al familiar», y «Pedir entregar ahora» fuera de horario (lo aprueba el cliente).
- Tarifas propias por pueblo y máximo de pedidos a la vez por repartidor, editables por la entidad.
- Actualizaciones al momento (tiempo real) y deslizar hacia abajo para refrescar en toda la app.
- Emoji propio para cada artículo de la Lista fácil.
- Aviso de actualización obligatoria cuando la versión es demasiado antigua.
### Cambiado
- Sugerencias de dirección en toda España, con prioridad a lo cercano y aviso si no hay servicio en la zona.
- Solo el superadministrador añade o quita pueblos de una entidad; borrar un pueblo con pedidos lo oculta.
- Firmas y tickets en almacenamiento privado, con enlaces temporales de 60 segundos.
### Corregido
- La Lista fácil se cerraba al dictar o escribir la lista.
- Entidades y repartidores ya no pasan por el registro de dirección ni ven el tutorial al empezar.
- Ajustes: los botones ya no se solapan con letra grande.
- Resumen de la entidad: ya se puede desplazar.
- La lista del repartidor conserva lo marcado al salir.
- Los errores se muestran con frases sencillas.
- Los correos automáticos dejaban de enviarse por un tiempo de espera demasiado corto.
- El repartidor con un pedido en curso necesita la ubicación y el GPS activados.
### Añadido (tanda 2)
- Portada nueva: dirección de entrega, Mis listas, repetir la última compra y botón grande e ilustrado de la Lista fácil.
- Lista fácil más cómoda: categorías con dibujo, iconos grandes, + y − grandes, lo más pedido primero, buscador y «Añadir todo lo de siempre».
- Buscador de la portada que encuentra artículos, ajustes y pedidos anteriores.
- Estados vacíos ilustrados y animados (respetan «reducir animaciones»).
- Superadministrador: suspender o reactivar entidades con motivo, nombrar o quitar superadministradores y borrar cuentas desde el panel.
### Cambiado (tanda 3)
- Temas: colores de tu fondo de pantalla (Material You), colores vivos, negro puro en modo oscuro, cinco paletas nuevas y animaciones de fondo opcionales.
- Pantalla de permisos más clara, con dibujo y progreso.
- Deslizar para refrescar con los colores del tema y vibración.
- Panel de Android: barra inferior fija, menú «Más» con título que se cierra con atrás y refresco que respeta el desplazamiento.
- El modo familia solo aparece para clientes.
### Cambiado (tanda 2)
- Los permisos solo se piden si faltan, y cuando hacen falta.
- Panel de Android: sin solapes con las barras del sistema y con deslizar para refrescar.

## [0.0.1] - 2026-10-20
Primera versión pública de T lo traigo.
### Añadido
- Pedir la compra con la Lista fácil (iconos a color, kilos o unidades, opciones y detalles), el catálogo, listas guardadas y compras recurrentes.
- Dictado por voz que entiende el habla natural, con animación y productos reconocidos en directo.
- Pedido en dos pasos con sugerencias de dirección, precios orientativos y pago en efectivo o Bizum.
- Seguimiento del repartidor en el mapa con la ruta y la llegada estimada, notificaciones de cada paso y chat.
- Revisión del ticket por el cliente y entrega con firma en el móvil del repartidor.
- Entidades: invitaciones por correo, verificación, pueblos por código postal, repartidores, días de reparto, informes y primeros pasos guiados.
- Seguridad: límites de peticiones, sesión cifrada en el móvil, cabeceras de seguridad en la web y registro de intentos sospechosos.
- Accesibilidad: tamaño de letra con deslizador, modo fácil, alto contraste, lectura en voz alta, passkeys y tutoriales.
