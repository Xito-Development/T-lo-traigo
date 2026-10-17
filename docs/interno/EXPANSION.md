# Expansión futura: mascotas, hogar y compras en varias tiendas

## 1. Nuevas categorías (mascotas, hogar, droguería, farmacia sin receta)
- **Catálogo por tipo de tienda**: añadir a `products` y `needs` un campo `store_kind` (`super`, `mascotas`, `hogar`, `farmacia`, `ferreteria`…).
- **Lista fácil por pestañas**: «Súper · Mascotas · Hogar», con los iconos ya disponibles (pienso, juguetes, bombillas, pilas, herramientas, limpieza…).
- **Reglas propias por categoría**: peso/volumen máximo (sacos de pienso de 15 kg, garrafas), productos que requieren vehículo, y +18/medicamentos con avisos.
- **Entidad decide** qué categorías ofrece y con qué tiendas colabora (ajuste por entidad).

## 2. Compras en varias tiendas en un mismo pedido
**Modelo de datos**
- `stores` (tienda): nombre, tipo, dirección, horario, pueblo, entidad, coordenadas.
- `order_stores` (parada del pedido): `order_id`, `store_id`, orden de visita, estado (`pendiente`, `comprando`, `comprado`), **ticket propio** (`ticket_path`, `ticket_total`), notas.
- `order_items.store_id`: cada artículo sabe en qué tienda se compra (asignado al pedir o por el repartidor).

**Flujo**
1. El usuario hace una sola lista; la app agrupa por tienda («Súper Día · Tienda de piensos · Ferretería»).
2. El repartidor ve las paradas en orden (ruta optimizada), marca cada tienda, **sube un ticket por tienda** y puede sustituir o marcar «no había» por tienda.
3. El cliente paga la **suma de todos los tickets** (efectivo/Bizum); la factura/justificante lista los tickets como suplidos separados.
4. Tarifas opcionales por parada extra (la entidad decide; por defecto gratis).

**App**
- Cesta agrupada por tienda con subtotal estimado de cada una.
- Seguimiento con la parada actual («En la ferretería · 2 de 3»).
- Informe mensual por tienda (útil para acuerdos con comercios locales).

## 3. Comercios locales como socios (siguiente paso)
- Panel ligero para la tienda: recibe la lista de su parte, la **prepara** y el repartidor solo recoge (menos tiempo).
- Precios del catálogo actualizados por la propia tienda.
- Modelo de negocio adicional: cuota pequeña por comercio o comisión, sin manejar el dinero del cliente.

## 4. Orden recomendado
1. `store_kind` + pestañas en la Lista fácil (rápido, sin cambiar el flujo).
2. Varias paradas con un ticket por tienda.
3. Panel para comercios.
