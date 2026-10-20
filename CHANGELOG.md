# Historial de cambios

Todas las versiones de T lo traigo. Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/), versiones [semánticas](https://semver.org/lang/es/).

## [0.0.2] - 2026-10-20
### Cambiado
- Textos de la app sin referencias a una provincia concreta; en la dirección, la provincia solo aparece si la escribes.
- Las reclamaciones remiten a la hoja oficial de consumo de tu comunidad autónoma.
### Corregido
- Administración (superadministrador): las secciones (Entidades, Productos, Lista fácil…) se veían vacías.
- Inicio de sesión y registro: los campos de usuario y contraseña ya se pueden autocompletar con el gestor de contraseñas.
- El registro ya no rechaza códigos postales de otras provincias (sigue comprobando que coincidan con el pueblo elegido).
- La búsqueda de la casa en el mapa ya no da por hecho una provincia.

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
