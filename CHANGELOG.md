# Historial de cambios

Todas las versiones de T lo traigo. Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/), versiones [semánticas](https://semver.org/lang/es/).

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
