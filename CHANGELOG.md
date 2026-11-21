# Historial de cambios

Todas las versiones de T lo traigo. Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/), versiones [semánticas](https://semver.org/lang/es/).

## [0.2.3] - 2026-11-21
### Cambiado
- Ayuda: «Escribir a soporte» pasa a un botón ✉️ en la cabecera; «Hablar con una persona» lleva directamente a escribir a soporte.
- Cliente: el bloque del gestor en Comprar tiene el aspecto de «Así te ven tus vecinos», con botón de llamada.
- Lista fácil: todos los bloques miden lo mismo.
### Corregido
- Visor de fotos y firmas: los botones Guardar y Compartir ya no quedan bajo la barra de navegación.
- Aviso de versión nueva: llega también con datos móviles (antes solo con wifi), reintenta si no hay conexión y se recuerda cada 3 días hasta actualizar.

## [0.2.2] - 2026-11-21
### Añadido
- Animación de carga entre pantallas y transiciones suaves; datos en caché para abrir más rápido.
- Aviso para guardar o descartar al salir de un formulario con cambios.
- Gestor: en cada pueblo, «Tarifa general» (por defecto) o «Tarifa propia».
- Cliente: bloque animado con el pedido en curso bajo la Lista fácil (lleva al seguimiento).
- Repartidor: aviso emergente al llegar un pedido nuevo (varios, uno a uno) y la cabecera cambia de color; bloque de repartos en curso e insignias en Resumen y Repartos.
- Panel de control: «Buscar versiones nuevas» en Ajustes; la actualización se descarga dentro de la app con progreso.
### Cambiado
- Todas las pantallas a pantalla completa y barra de estado con el color adecuado en cada tema; el teclado se oculta al tocar fuera.
- Textos largos se desplazan en vez de cortarse.
- Superadmin: los bloques de Gestores, Repartidores y Usuarios filtran la lista; orden de pestañas Gestores · Repartidores · Usuarios.
- Gestor: datos fiscales plegados por defecto mostrando si está verificado y solo la forma jurídica elegida.
- Repartidor: Mis repartos destaca cuánto cobrar y cuándo entregar; «Fuera de horario» es un botón.
- Abre Google Authenticator (no Samsung Pass) para los códigos de seguridad.
- Panel de control: aviso de versión nueva compacto.
### Corregido
- Guardar la tarifa general ya no convierte los pueblos en «tarifa propia».
- Pantalla intermedia «Mi gestor» al cambiar entre Pedidos y Gestión.
- Superadmin: al salir de la simulación de gestor ya no queda en «Sin gestor asignado».
- Cesta: el desplazamiento ya no salta entre el paso 1 y el 2.
- Mapa de la ruta: encuadre y zoom estables (no se mueve al actualizar la posición tras tocarlo).
- Botones de Seguimiento y de Familia equilibrados y sin solaparse.

## [0.2.1] - 2026-11-19
### Añadido
- Repartidor: pestaña «Ganancias» con tus repartos y ganancias, ganancias por semana, estimación de gastos (combustible y otros), ingresos frente a gastos por mes, beneficio final del mes y kilómetros por mes.
- Repartidor: datos personales y fiscales (nombre, DNI/NIE/NIF, dirección fiscal y si eres autónomo o por cuenta ajena) para que tu gestor pueda pagarte; se piden al darte de alta y están en Ajustes → Mis datos.
- Repartidor: opiniones de los clientes en la cabecera del Historial.
- Gestor: ficha de cada repartidor con sus datos, alta, beneficios, forma de cobro, pueblos asignados (con acceso a cada pueblo), rendimiento (entregas, cancelaciones y km por mes), actividad y ganancias.
- Gestor: tarifa propia por repartidor (si no tiene, se usa la general de la entidad).
- Gestor: «Repartidores activos» en el Resumen, con acceso a Repartidores.
- Gestor: «Ejemplo de tarifa» y «Tarifas personalizadas» (pueblos con tarifas propias) en Tarifas.
- Gestor: con la entidad verificada, los datos fiscales se cambian con una solicitud que autoriza el equipo de T lo traigo.
- Pedidos: botón «Contacta» para avisar al equipo de soporte de cualquier incidencia con el pedido.
- Superadministrador: ficha de cada usuario y repartidor (toca en Gestores → Usuarios o Repartidores).
- Superadministrador: cabecera y menú inferior con o sin curvatura y con o sin sombra, para todos los usuarios.
### Cambiado
- Buscador: encuentra productos, pedidos, ajustes y respuestas de la guía de ayuda sin IA; el asistente con IA solo aparece cuando está activado.
- «Buscar en la ayuda» con los colores del tema y el icono de la lupa.
- Ajustes: «Cuenta y seguridad» y «Acerca de, créditos y privacidad» salen al menú principal; «Tengo el código de invitación del gestor» va antes de Familia; se quita el pie con la versión y las firmas.
- Familia: dos botones, «Hago la compra» (vincular con el código de un familiar) y «Me hacen la compra» (mi código para familiares).
- Comprar: el bloque del servicio del gestor muestra el importe del servicio.
- Pedido: el chat va junto a la llamada del repartidor, vuelve «Ver el recorrido» y el chat queda solo para leer al cerrar el pedido.
- Gestor: se quita «Cómo se paga al repartidor» de Repartidores; «Tarifas personalizadas» incluye a los repartidores con tarifa propia.
- Informes, facturas y justificantes en PDF con cabecera de T lo traigo, datos del gestor, cifras, gráficas de barras, tablas y pie con numeración y logos.
- Repartidor: los pedidos nuevos están en el Resumen, bajo «Estás disponible»; la pestaña Resumen avisa y cambia de color cuando llega uno.
- Repartidor: «Servicio gratuito de tu gestor» pasa a Ajustes.
- Repartidor: «Tus repartos y ganancias» al final del Resumen (lleva a Ganancias) y datos fiscales en Mis datos y dirección.
- Repartidor: en los repartos, «Pagado» en verde (rojo si falta cobrar) y el margen en verde si ganas o rojo si no; el icono de entrega explica si es «lo antes posible» o su fecha, y la ruta recorrida se abre desde su icono.
- Superadministrador: el Resumen de cada gestor tiene el mismo diseño que los demás y cada bloque lleva a su pestaña.
- Gestor: en Repartidores, una barra de color indica la disponibilidad; «Invitar al gestor» pasa a llamarse «Enviar invitaciones» y ahí está también el pago general a repartidores.
- Pedidos: el detalle se ordena en bloques: datos del pedido, proceso, lista y pago, justificante, repartidor y chat, valoración y, al final, reclamaciones, repetir y contacta.
- Gráficas más claras en tema oscuro.
- Avisos emergentes con los colores del tema.
- Pestañas superiores separadas del contenido y sin rectángulo al pulsarlas.
- Firma con sombra más marcada en tema claro; «UNOCEROBITS» y «Xito Development» abren sus webs.
### Corregido
- Al aplicar la tarifa general, los pueblos ya no aparecen como «Tarifas personalizadas» y los que tienen tarifas propias las conservan.
- El interruptor de curvatura y sombra (superadministrador) ya se guarda.
- «Mis consultas» y otras pantallas a pantalla completa ya no se ven transparentes.
- Privacidad: los vecinos y repartidores ya no pueden ver los datos privados de su entidad (CIF, DNI y nombre del representante, domicilio fiscal ni datos de facturación).

## [0.2.0] - 2026-11-19
### Añadido
- Ficha de cada repartidor para su gestor: datos, pueblos, actividad, ganancias, repartos por cliente, recorridos y últimos repartos.
- Seguimiento (pedidos en curso) e Historial (pedidos terminados) por separado.
- Explicación de cada tarifa (botón ⓘ): qué sirve para pagar al repartidor y qué es para la entidad.
- Suscripción del gestor visible en Gestión → Entidad (plan, cuota, renovación) con opción de cancelarla, indicando el motivo, o mantenerla.
### Cambiado
- Nuevo diseño: cabeceras a toda la pantalla, Resumen y gráficas con el color elegido en Ajustes, y pestañas y tarjetas más claras.
- Ajustes reorganizado: «Ajustes de la aplicación» reúne pantalla, colores, avisos, ayuda y cuenta.
- La lupa reúne el buscador y el asistente, e indica si el asistente con IA está disponible.
- Panel del gestor reorganizado: Resumen, Pedidos (con informes), Gestión (repartidores, pueblos, tarifas y entidad) y Ajustes. Invitar va dentro de Repartidores.
- Estadísticas con gráficas y nombres de pantalla claros.
- Animación de inicio y pie de Ajustes más legibles.
- Las gráficas usan el mismo color que los botones.
- En la ficha del repartidor: «Desactivar repartidor» y pestaña «Pedidos».
### Corregido
- Cierre inesperado al cambiar de pestaña en el panel de administración.
- Las tarifas propias de cada pueblo no se veían al editarlas ni en la lista.
- «Colores vivos» apagaba los colores en el tema oscuro.
- El aviso de «Ver como» seguía visible al volver.
- Los informes de un gestor cambiaban al pasar un pueblo a otra entidad.

## [0.1.14] - 2026-11-18
### Cambiado
- Mejoras y correcciones.

## [0.1.13] - 2026-11-17
### Corregido
- Panel de control: ya no da error al simular un rol en el móvil, y avisa de que la simulación está activa.
- La actualización obligatoria se aplica a todas las cuentas.
### Cambiado
- Mejoras de seguridad en las cuentas de administración.

## [0.1.12] - 2026-11-15
### Añadido
- Ajustes de la entidad: vista previa en directo de cómo ven los vecinos vuestra cabecera (color, logo, nombre, horario y mensaje).
- Ajustes de la entidad: color propio con código #RRGGBB (por ejemplo, el del escudo) y cuatro colores nuevos.
- Ajustes de la entidad: atajos para los días de reparto («Lunes a viernes», «Lunes a sábado», «Todos los días»).
- Ajustes de la entidad: barra de «Cambios sin guardar» con Guardar y Descartar, y aviso de lo que falta antes de guardar.
### Cambiado
- Ajustes de la entidad reordenados en secciones claras (Datos, Imagen, Pedidos, Horario, Cobros, Repartidores) con un resumen visible aunque estén plegadas.
- Las pestañas del panel de la entidad crecen con el tamaño de letra elegido: el texto ya no se corta con letra grande.
- Web: nueva animación de «Así funciona» y marca UNOCEROBITS con su tipografía (Raleway fina).
- Web: el ticket de ejemplo muestra el desglose de la tarifa y su subtotal antes del total.
### Corregido
- La sección «Repartidores» salía dos veces en los ajustes de la entidad.
- Se podía guardar un horario con la hora de cierre anterior a la de apertura.

## [0.1.11] - 2026-11-08
### Corregido
- Parpadeo al cambiar de pantalla (parecía que la página se recargaba): ahora es un fundido suave, y las tarjetas de «cargando» solo aparecen si la carga tarda.

## [0.1.10] - 2026-11-08
### Cambiado
- Las versiones nuevas ya no se descargan solas en segundo plano: llega un aviso y, al tocarlo, la app se actualiza con la persona delante.
- Web: la animación de la portada cuenta cómo funciona de verdad (pides, el repartidor acepta, compra y envía el ticket, lo apruebas, te lo lleva y cobra su parte) y las páginas cargan más rápido al volver a ellas.

## [0.1.9] - 2026-11-08

### Añadido
- Buscador: si no encuentra lo que buscas, puedes preguntárselo al asistente con un toque.
- El asistente con IA puede consultar tus últimos pedidos o repartos y tus ajustes para responderte (sin tu nombre, dirección, teléfono ni correo) y sus respuestas traen un botón que te lleva al sitio que menciona.

### Cambiado
- El asistente solo responde con su guía cuando la pregunta está clarísima; si hay duda, contesta la IA, y solo sobre lo que corresponde a tu rol.

### Corregido
- Ajustes: el botón del panel de pruebas y «Cerrar sesión» quedaban pegados.
- Web: el logo de la animación de la portada se veía con un recuadro de fondo; y la web ya no se recolorea con el modo oscuro forzado de Chrome ni con extensiones como Dark Reader.

## [0.1.8] - 2026-11-08
### Cambiado
- Ajustes reordenado por temas: mis datos, pantalla y accesibilidad, avisos y privacidad, ayuda, cuenta y seguridad y, al final, lo de superadministrador y las herramientas de prueba.
- El botón de ayuda tiene ahora el mismo estilo que la lupa del buscador (un bocadillo con interrogación).
### Corregido
- Parpadeo al cambiar de pantalla: la nueva aparece mientras se va la anterior, sin un instante vacío.
- Error al simular un rol («column role is of type user_role…»).

## [0.1.7] - 2026-11-08
### Añadido
- Administración: el superadministrador puede «simular» el rol de cliente, repartidor o entidad de una entidad concreta para probar la app como esa persona, y volver desde Ajustes. Mientras simula, sus pedidos son de prueba.
### Cambiado
- Las actualizaciones automáticas solo se descargan con wifi y batería suficiente (para no gastar datos del móvil).
- Transiciones entre pantallas más limpias: la pantalla que sale desaparece antes de que entre la nueva (sin verse las dos a la vez ni efecto borroso).
- Nueva foto del asistente de ayuda, más fácil de reconocer: un bocadillo con una interrogación.

## [0.1.6] - 2026-11-07
### Añadido
- El asistente de ayuda ahora usa inteligencia artificial (gratuita) cuando su guía no tiene la respuesta: entiende cualquier forma de preguntar y responde según tu rol, con un «escribiendo…» mientras piensa. Si no hay conexión o se agota el cupo del día, sigue respondiendo como antes.
### Cambiado
- Asistente de ayuda renovado: con su propia foto, fondo opaco (antes se veía transparente), burbujas de chat, botones que te llevan al sitio que explica (Lista fácil, Mis pedidos, Ajustes…) y más ayuda para cada rol. El superadministrador puede desactivarlo para todos.
- Temas de color más distintos entre sí y más vistosos.
- Al desplazarte, lo que se sale de la pantalla se desvanece en vez de cortarse en seco (arriba, abajo y en las filas que se deslizan).
- Web: iconos sin aro blanco en Google, la animación de la portada con el logo real y nombre del sitio «T Lo Traigo · UNOCEROBITS»; información más clara para buscadores con IA.
### Corregido
- Ajustes: el botón del asistente chocaba con el siguiente.
- Texto de los botones grandes a su tamaño (con letra grande se salía).

## [0.1.5] - 2026-11-07
### Añadido
- Asistente de ayuda dentro de la app (Ajustes → Ayuda): responde al momento según seas cliente, repartidor, entidad o administración, entiende preguntas escritas a tu manera y, si no sabe la respuesta, te deja escribir a soporte (te contestamos por correo). Es gratuito y tus preguntas no salen de T lo traigo.
- Entidades: en «Repartidores» deciden lo que gana cada repartidor (porcentaje del servicio y envío, gastos por kilómetro o importe fijo) con un ejemplo claro de lo que paga el vecino, lo que gana el repartidor y lo que le queda a la entidad.
- Web: Google Analytics (mediante Google Tag Manager) solo si aceptas las cookies, con el modo de consentimiento de Google; política de cookies y de privacidad actualizadas.
### Cambiado
- Notificación en vivo del pedido: se sigue actualizando cada 5 s aunque cierres la app y usa el color de tu tema (también en la Super Isla de HyperOS).
- Mapa del seguimiento: se mueve y se acerca con dos dedos, y el fondo toma el color de la app.
- Botones principales más visibles: más altos, con sombra, esquinas redondeadas y el icono en su círculo; los del repartidor, rellenos.
- Avisos emergentes más bonitos: el icono entra con un salto, una barra muestra el tiempo que les queda y se pueden quitar deslizando hacia arriba.
- Web: nueva imagen para Google y redes con el logo real, y el nombre «T Lo Traigo» en buscadores.
### Corregido
- Lista fácil: el precio no cambiaba con el formato (p. ej. «Pack de 6» costaba lo mismo que un brick); ahora se calcula según el formato, también en el servidor.
- Ayuda más a mano: botón «?» en la portada, en «Mis repartos», en el panel de la entidad y en Administración.
- El asistente entiende mucho mejor: preguntas sin tildes o con faltas, palabras parecidas («anular», «encargo», «clave», «mi hijo»…), saludos y agradecimientos; si duda, te propone las respuestas más probables y tiene muchas más respuestas (cambiar un pedido, pedir para otro día, productos agotados, propinas, avisos, contraseña, huella…).
- Temas de color renovados: nuevos Cereza, Miel y Grafito; retocados Mar, Lavanda y Rosa; quitados Girasol, Melocotón y Pizarra (quien los usaba pasa a Montaña).
- El aviso de que una fila se puede deslizar es ahora una flecha en un círculo pequeño, sin el fondo que tapaba el contenido; también avisa a la izquierda y aparece y desaparece con un fundido.

## [0.1.4] - 2026-11-04
### Cambiado
- Al elegir una pestaña de la barra inferior, el icono da un pequeño salto.
### Corregido
- Administración: el logo de cada entidad llena su recuadro (sin marco blanco).
- La app podía cerrarse en móviles con Android 5 al mostrar la notificación en vivo del pedido.

## [0.1.3] - 2026-11-03
### Cambiado
- Avisos de la entidad (tarifas, verificación…): ahora van dentro de la tarjeta de la entidad, pequeños y con icono, duran 3 días como mucho y se cierran con la ✕.
- Los avisos emergentes («Guardado…») duran 4 segundos, entran con un rebote y llevan un icono; se pueden cerrar tocándolos.
- El logo de la entidad llena su recuadro (sin marco blanco) y aparece con un fundido.
### Corregido
- No se podía guardar el logo subido desde el móvil («algún valor está fuera de los límites»).
- Los avisos emergentes se quedaban fijos en pantalla.
- Recuadros negros en las listas con el modo oscuro (usuarios, repartidores, pueblos…).
- «Sesiones abiertas» mostraba sesiones ya cerradas: al cerrar sesión ahora también se cierra en el servidor y solo salen las activas.

## [0.1.2] - 2026-11-02
### Añadido
- Al guardar ajustes o datos aparece un aviso durante 5 segundos y, debajo del botón, la fecha y hora del último guardado.
- Pedidos: tarjeta clara con la forma de pago, el ticket guardado (ver, ampliar, guardar) y el desglose hasta el total; el repartidor ve destacado su margen.
- Estadísticas: el superadministrador puede limpiar los errores y reiniciar las estadísticas.
### Cambiado
- Entidades (superadministrador): resumen arriba, buscador, filtros (activas, suspendidas, por verificar…) y una tarjeta por entidad con su color, estado y menú de acciones; mantenimiento plegado al final.
- Ajustes de la entidad: cada apartado es plegable; solo «Pedidos» empieza abierto.
- Sesiones abiertas: solo se muestran las activas.

## [0.1.1] - 2026-11-02
### Cambiado
- Mejoras y correcciones.

## [0.1.0] - 2026-11-02
### Añadido
- Cuenta: lista de sesiones abiertas; «Cerrar sesión en los otros dispositivos» solo se puede pulsar si hay otras.
- Avisos de versión nueva aunque la app esté cerrada (cada 6 horas, una vez por versión).
- La entidad decide si sus vecinos pagan tarifas o el servicio es gratuito (con aviso a los vecinos al cambiarlo).
- Mapa del seguimiento con botones para acercar, alejar y volver al encuadre automático; foto del repartidor ampliable.
- Repartidor: en el historial puede ver la ruta recorrida de cada reparto.
- Avisos animados al guardar (bajan desde arriba y desaparecen solos).
### Cambiado
- Repartidor: la tarjeta de cada reparto muestra arriba el paso en que va (Aceptado → Comprar → Llevar → Entregado), luego contacto y ruta, la compra y al final el botón del siguiente paso.
- Administración: secciones con icono y agrupadas (gente, catálogo, actividad, facturación); el superadministrador entra directo a Entidades.
- Más avisos animados al guardar (ajustes de la entidad y datos de facturación).
- Notificación en vivo del pedido: se actualiza cada 5 s con pedido en curso, con icono por estado, número de productos, importe del ticket y la foto del repartidor.
- El ticket se ve igual para cliente y repartidor (visor con zoom, guardar y compartir).
### Corregido
- Al girar el móvil ya no se pierden las ventanas abiertas ni se sale de la pantalla en la que estabas.
- Passkeys («RP ID cannot be validated») y botón del correo que no abría la app: la web no tenía registrada la firma real de la app.
- Logo de la entidad que no carga: se muestra un icono en lugar de un recuadro blanco.

## [0.0.18] - 2026-11-02
### Añadido
- Notificación en vivo del pedido en curso (Android 16: en la barra de estado y la pantalla de bloqueo; en HyperOS 3, en la Super Isla), con su barra de progreso; desaparece al entregar.
- Foto de perfil (en Ajustes → Mis datos, con recorte redondo): el cliente ve la del repartidor en el seguimiento y en el mapa.
- Logo de la entidad con recorte y zoom antes de guardarlo; se aceptan todos los formatos de imagen (también HEIC y AVIF).
- Entidades: pestaña «Tarifas» con tarifa propia para todos sus pueblos (sus vecinos reciben un aviso al cambiarla), elección de las tarifas adicionales que se cobran y precios propios para la Lista fácil.
- Visor de imágenes en la app para el ticket y la firma: zoom con dos dedos, guardar en la galería y compartir la imagen (el repartidor ya no sale a la web).
- Lista fácil: si «Leer en voz alta» está activado, al tocar un producto se dice su nombre y el precio orientativo.
### Cambiado
- Efecto al pulsar en los bloques principales (Lista fácil, pedidos, portada y pedidos disponibles del repartidor): se hunden un poco y vuelven con un rebote suave.
- Repartos del repartidor: el detalle se abre y se cierra con su botón (ya no al tocar cualquier parte) y los terminados se pliegan solos a los 10 minutos.
- Mapa del seguimiento: siempre cubierto entero, se acerca solo a medida que llega el repartidor, punto del repartidor más grande y claro, se actualiza cada 5 s y sin bocadillos encima.
- Tiempos de más de 60 minutos se muestran como h:mm (p. ej. «1:05 h»).
- Mis pedidos: los pedidos en curso salen desplegados con su progreso; los terminados se pliegan solos a los 10 minutos y se pueden abrir o cerrar con un botón.
- Respuestas rápidas del chat distintas para cliente y repartidor según cómo va el pedido.
- Cabecera de la entidad con un degradado más vistoso.
- Seguimiento: primero «Coste estimado»; con el ticket, el estimado pasa a pequeño y destacan «Coste real del súper» y «Coste total».
- Mis listas: cada artículo muestra su dibujo (naranjas, patatas, pan…) en lugar del icono genérico de cesta.
- El botón de los correos intenta abrir la app al momento, sin pasar por la página.
- Letra: nunca por debajo de un tamaño mínimo y, en pantallas pequeñas, al menos el tamaño normal; títulos sin letras apretadas.
### Corregido
- Tarifas: con el servicio en modo gratuito ya no se cobraba el servicio pero sí el envío; ahora es gratuito del todo. Un pueblo sin distancia o envío configurado ya no deja el total en blanco.
- La firma ya no se sale del recuadro y la copia se puede ampliar (también la ve la entidad desde el pedido).

## [0.0.17] - 2026-11-01
### Añadido
- Verificación en dos pasos por correo para todas las cuentas: al entrar en un móvil nuevo llega un correo con un código de 6 cifras y un botón «Sí, soy yo» que abre la app; pantalla guiada paso a paso y el móvil queda recordado 30 días.
- Entidades: pestaña «Recorridos» para ver en el mapa por dónde ha ido cada repartidor en sus entregas (últimos 30 días).
- Entidades: en «Informe», gráfica de compras por día del mes, totales de compras, tarifas y pagos a repartidores, y ranking de repartidores.
- Entidades: subir el logo desde el móvil (se comprime y se guarda en la base de datos) y límite de pedidos a la vez por cliente.
- Temas y colores: vista previa en vivo del tema y paletas con animación al elegirlas.
- Confirmar el pedido espera 2 segundos para dar tiempo a revisar el resumen.
### Cambiado
- El color de la entidad se muestra con un degradado suave y el logo sobre fondo blanco.
- Lista fácil: todas las tarjetas miden lo mismo, con botón «Añadir» y borde al elegir (sin saltos ni tarjetas descuadradas).
- Cargas más suaves: tarjetas de espera que laten y elementos que aparecen escalonados en lugar de en bloque.
- Transiciones entre pantallas con un ligero zoom además del fundido.
### Corregido
- Privacidad: al descargar tus datos desde la cuenta ahora también salen tus repartos (sin datos de los clientes), tus recorridos y tus vínculos familiares; al borrar la cuenta se borran también tus recorridos.
- Una entidad suspendida ya no se queda cargando: ve el aviso con el motivo y cómo contactar; si falla la conexión, sale «Reintentar».

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
