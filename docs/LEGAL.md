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
