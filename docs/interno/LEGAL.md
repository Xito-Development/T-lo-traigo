# Evaluación de riesgos jurídicos (España/UE) · octubre 2026

Marco: Gravedad (1-5) × Probabilidad (1-5). 🟢 1-4 · 🟡 5-9 · 🟠 10-16 · 🔴 17-25. **No sustituye a un abogado**: los riesgos 🔴/🟠 deben revisarse con asesoría externa antes del lanzamiento público.

| # | Riesgo | Norma | G×P | Nivel | Mitigación aplicada | Pendiente |
|---|---|---|---|---|---|---|
| 1 | Repartidores calificados como **falsos autónomos** (laboralidad presunta en reparto por plataforma digital) | DA 23ª ET (Ley 12/2021 «Ley Rider»); Directiva (UE) 2024/2831 | 5×4=20 | 🔴 | Libertad para aceptar/rechazar y fijar disponibilidad y pueblos; declaración de alta en RETA | **Penalizaciones y tarifas fijadas por la plataforma son indicios de dependencia.** Valorar: contratarlos (o vía cooperativa), dejar que fijen su tarifa, o poner la penalización a 0 €. Consultar abogado laboralista |
| 2 | Cobro a clientes por cuenta de terceros sin licencia de pago | RDL 19/2018 (servicios de pago) | 4×3=12 | 🟠 | Pagos con tarjeta/Bizum aún simulados; efectivo lo cobra el repartidor | Usar Stripe Connect (o similar) para que el dinero no pase por la plataforma |
| 3 | Falta de información alimentaria en venta a distancia (alérgenos) | Reglamento (UE) 1169/2011 art. 14 | 3×4=12 | 🟠 | Campos de alérgenos e ingredientes en cada producto | **Rellenar alérgenos de todos los alimentos** en Admin |
| 4 | Fiscalidad: IVA del servicio, facturas a clientes, IRPF de repartidores | Ley 37/1992; RD 1619/2012 | 3×3=9 | 🟡 | Desglose de IVA visible | Emitir factura simplificada por pedido; asesoría fiscal |
| 5 | Geolocalización de repartidores | RGPD; art. 90 LOPDGDD | 4×2=8 | 🟡 | Solo en «en camino», consentimiento en candidatura, aviso destacado, borrado al entregar | Registro de actividades de tratamiento y contrato de encargo con Supabase |
| 6 | Venta de alcohol a menores | Ley 3/1994 de Castilla y León | 4×2=8 | 🟡 | Productos +18, confirmación de edad y DNI en la entrega | — |
| 7 | Requisitos de Google Play (servicios en primer plano, ubicación, borrado de cuenta) | Políticas de Google Play | 3×3=9 | 🟡 | Aviso destacado, sin ubicación en segundo plano, borrado en app y web | Declaraciones en Play Console (ver PLAY_STORE.md) |
| 8 | Información precontractual y botón de pago | RDL 1/2007 arts. 97-98 y 103 | 3×2=6 | 🟡 | Resumen completo, «Confirmar pedido con obligación de pago», condiciones con desistimiento | — |
| 9 | Hojas de reclamaciones | Normativa de consumo de Castilla y León | 2×3=6 | 🟡 | Mención en condiciones | Tener el modelo oficial disponible |
| 10 | Seguridad alimentaria en el transporte | Reglamento (CE) 852/2004 | 3×2=6 | 🟡 | Declaración del repartidor y condiciones | Bolsas isotérmicas para frío |
| 11 | Aviso legal y publicidad identificada | Ley 34/2002 (LSSI-CE) arts. 10 y 20 | 2×2=4 | 🟢 | Aviso legal, banner con «Publicidad» | Completar NIF y domicilio |
| 12 | Cookies y transferencias a terceros en la web | LSSI art. 22; RGPD | 2×2=4 | 🟢 | Sin cookies; fuentes alojadas en la propia web | — |
| 13 | Accesibilidad | Ley 11/2023 / RD 193/2023 | 2×2=4 | 🟢 | Diseño accesible (microempresas exentas) | — |

**Escalar antes de abrir al público:** 1 y 2 (abogado laboralista y asesoría de pagos). Completar los `[completar]` de aviso legal, privacidad y condiciones.

## Actualización octubre 2026 (v0.5)
| Riesgo | Cambio aplicado | Nivel ahora |
|---|---|---|
| 1. Falsos autónomos | Sin penalizaciones económicas; aceptación libre; cobro directo vía Stripe Connect; requisitos de alta en RETA | 🟠 (sigue requiriendo abogado: la plataforma fija las tarifas) |
| 2. Servicios de pago | Stripe Checkout + Connect: el dinero no pasa por la plataforma | 🟢 |
| 3. Alérgenos | Rellenados para los 22 productos (genéricos) y en la «lista fácil» se indica revisar el envase | 🟡 (verificar por marca) |
| 4. Facturación | Factura por pedido: suplidos (compra en el súper) + servicio con IVA 21 %, numeración correlativa y huella encadenada (preparada para Verifactu, obligatorio en 2027 según RDL 15/2025) | 🟡 (asesoría fiscal) |
| 5. Geolocalización | Solo con entrega en curso («en reparto») | 🟢 |
| 9. Reclamaciones | Hoja de reclamaciones en app y web, numerada, plazo de respuesta 10 días hábiles | 🟢 |
| 10. Cadena de frío | Aviso al repartidor en pedidos con refrigerados/congelados | 🟢 |
| 11. Publicidad | Eliminada | — |
| Precios orientativos | Importe máximo informado antes de pedir, foto del ticket y cobro del importe real (arts. 60 y 97 TRLGDCU: forma de cálculo del precio) | 🟡 |

### Otros riesgos a revisar
- **Tarifa de gestión** y no reembolso del servicio: deben estar en las condiciones y no ser desproporcionados (cláusulas abusivas, arts. 82-91 TRLGDCU). Por eso, si nadie ha aceptado el pedido, se devuelve todo.
- **Consumidores vulnerables** (personas mayores; Ley 4/2022): información clara, atención telefónica y no presionar compras.
- **Pedidos por familiares**: el familiar actúa en nombre del mayor; conviene que el mayor dé su consentimiento (código de vinculación).
- **Marcas y precios de supermercados**: no usar logos ni copiar catálogos sin permiso; los precios se presentan como orientativos.
- **Estadísticas/cookies**: consentimiento previo y rechazo igual de fácil (guía AEPD de cookies 2023); registro de actividades de tratamiento.
- **Seguro de responsabilidad civil** de la plataforma y de los repartidores.
- **Prevención de riesgos** (LPRL, coordinación de actividades) aunque sean autónomos.

## Repartidores voluntarios con una pequeña compensación (orientativo, consultar gestoría)
- **Voluntariado (Ley 45/2015):** el voluntario no puede cobrar; solo se le pueden **reembolsar gastos reales justificados** (p. ej. kilometraje). Un pago fijo de 5 € por pedido es **retribución**, no voluntariado.
- **Si cobran:** es una actividad económica → Hacienda (IRPF, rendimientos de actividad) y Seguridad Social (alta en RETA si es habitual). Ganar poco no exime por ley; la «habitualidad» la valoran los tribunales caso a caso.
- **La plataforma** que intermedia pagos a terceros debe informar a Hacienda de lo que cobran los proveedores de servicios (DAC7, Ley 13/2023, modelo 238), y quien cobra la tarifa de gestión también ejerce una actividad económica.
- **Opciones:** (1) asociación sin ánimo de lucro con voluntarios, seguro de voluntariado y solo reembolso de gastos (kilometraje exento hasta 0,26 €/km); (2) repartidores autónomos (cuota reducida el primer año); (3) contratarlos a tiempo parcial; (4) cooperativa. Para la fase de prueba: opción 1 o piloto sin dinero.

### Modelo adoptado (v0.5.5): vecinos colaboradores + plataforma con tarifas
- Repartidor: solo compensación de gastos de desplazamiento (km ida y vuelta × 0,26 €/km) + propinas ocasionales. Sin beneficio → no es actividad económica ni requiere RETA. Debe ser coherente con el gasto real (no inflar el €/km ni pagar «por pedido»).
- Plataforma (tú): cobra servicio y gestión → actividad económica: alta en Hacienda (modelo 036/037) y RETA (cuota reducida), facturas con IVA 21 %. Si trabajas por cuenta ajena: pluriactividad (revisa también la cláusula de exclusividad de tu contrato).
- Riesgos: si el repartidor reparte mucho y de forma organizada, la Inspección puede ver relación laboral encubierta (Ley Rider) aunque solo cobre gastos. Mantener: libertad total para aceptar, sin penalizaciones, sin horarios impuestos, pocos pedidos por persona.
- Seguro: póliza de responsabilidad civil de la plataforma que cubra a los colaboradores, y que el repartidor tenga seguro del vehículo.
