package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.payments.PayMethod
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", Locale("es", "ES"))
private val dayFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
fun prettyDay(isoDate: String): String = runCatching { java.time.LocalDate.parse(isoDate.take(10)).format(dayFmt) }.getOrDefault(isoDate)
fun slotLabel(slot: String?) = when (slot) { "manana" -> "por la mañana (9–13 h)"; "tarde" -> "por la tarde (16–20 h)"; else -> "a cualquier hora" }
fun hoursSince(iso: String): Long = runCatching {
    java.time.Duration.between(OffsetDateTime.parse(iso).toInstant(), java.time.Instant.now()).toHours()
}.getOrDefault(0)
fun waitLabel(h: Long) = when { h < 24 -> "$h h"; h < 48 -> "1 día y ${h - 24} h"; else -> "${h / 24} días" }

/** Fecha programada de entrega o «lo antes posible». */
@Composable
fun ScheduleBadge(date: String?, slot: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(if (date == null) Icons.Filled.Bolt else Icons.Filled.Event, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.width(6.dp))
        Text(if (date == null) "Lo antes posible" else prettyDay(date).replaceFirstChar { it.uppercase() } + ", " + slotLabel(slot),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
fun Stars(value: Double, size: Int = 22) = Row {
    (1..5).forEach { i ->
        Icon(when { value >= i -> Icons.Filled.Star; value >= i - .5 -> Icons.Filled.StarHalf; else -> Icons.Filled.StarBorder }, null,
            Modifier.size(size.dp), tint = MaterialTheme.colorScheme.secondary)
    }
}

/** Selector de 1 a 5 estrellas con botones grandes. */
@Composable
fun StarPicker(label: String, value: Int, onChange: (Int) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    Row {
        (1..5).forEach { i ->
            IconButton({ onChange(i) }) {
                Icon(if (i <= value) Icons.Filled.Star else Icons.Filled.StarBorder, "$i estrellas", Modifier.size(36.dp),
                    tint = if (i <= value) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline)
            }
        }
    }
}

fun prettyDate(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(dateFmt).replaceFirstChar { it.uppercase() }
}.getOrDefault(iso)

@Composable
fun StatusChip(status: String) {
    val s = OrderStatus.of(status)
    val (bg, fg) = when (s) {
        OrderStatus.ENTREGADO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.CANCELADO -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = bg, shape = CircleShape) { Text(s.label, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = fg, style = MaterialTheme.typography.labelMedium) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrdersScreen(onOpen: (Long) -> Unit) {
    var orders by remember { mutableStateOf<List<Order>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(tick) {
        while (true) {
            try { orders = Locator.repo.myOrders(); err = null } catch (e: ApiException) { err = e.message; if (orders == null) orders = emptyList() }
            delay(20_000)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { Text("Mis pedidos", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 8.dp)) }
        item { ErrorText(err) }
        when {
            orders == null -> item { Loading() }
            orders!!.isEmpty() -> item { Message(Icons.Filled.ReceiptLong, "Aún no tienes pedidos", "Cuando hagas tu primera compra la verás aquí.") }
            else -> items(orders!!, key = { it.id }) { o ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).animateItem().clickable { onOpen(o.id) }, shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(o.status)
                                if (o.status == "entregado" && !o.hasReview) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                                        Text("★ Valóralo", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("${productos(o.items.sumOf { it.qty })} · ${eur(o.total)}", style = MaterialTheme.typography.titleMedium)
                            Text(prettyDate(o.createdAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTimeline(status: String) {
    val cur = OrderStatus.of(status)
    if (cur == OrderStatus.CANCELADO) {
        Message(Icons.Filled.Cancel, cur.label, cur.clientText); return
    }
    val idx = OrderStatus.flow.indexOf(cur)
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(1f, 1.35f, infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse")
    Column {
        OrderStatus.flow.forEachIndexed { i, s ->
            val done = i <= idx
            val dot by animateColorAsState(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, tween(500), label = "d")
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(28.dp).scale(if (i == idx && i < OrderStatus.flow.lastIndex) pulse else 1f).clip(CircleShape).background(dot), Alignment.Center) {
                        if (done) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    if (i < OrderStatus.flow.lastIndex) {
                        val fill by animateFloatAsState(if (i < idx) 1f else 0f, tween(600), label = "f")
                        Box(Modifier.width(4.dp).height(34.dp).background(MaterialTheme.colorScheme.outlineVariant)) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(fill).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.padding(top = 2.dp)) {
                    Text(s.label, style = MaterialTheme.typography.titleMedium, color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (i == idx) Text(s.clientText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(vm: AppViewModel, id: Long, onBack: () -> Unit, onRepeat: () -> Unit) {
    var order by remember { mutableStateOf<Order?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var askCancel by remember { mutableStateOf(false) }
    var showRefund by remember { mutableStateOf(false) }
    var showComplaint by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    LaunchedEffect(id) {
        while (true) {
            try { order = Locator.repo.order(id); err = null } catch (e: ApiException) { err = e.message }
            if (order?.isFinal == true) break
            delay(10_000) // TODO: sustituir por Supabase Realtime
        }
    }
    Scaffold(topBar = {
        TopAppBar({ Text("Pedido nº $id") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } })
    }) { pad ->
        val o = order
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            ErrorText(err)
            if (o == null) { Loading(); return@Column }
            Text(prettyDate(o.createdAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
                Box(Modifier.fillMaxWidth().padding(20.dp)) { StatusTimeline(o.status) }
            }
            Spacer(Modifier.height(10.dp))
            ScheduleBadge(o.scheduledDate, o.scheduledSlot)
            o.driver?.let { d -> o.driverId?.let { DriverCard(d, it) } }
            if (o.status == "en_camino") TrackingCard(o)
            if (o.awaitingPayment) Spacer(Modifier.height(4.dp))
            if (o.status == "entregado") ReviewSection(o)
            SectionTitle("Productos")
            o.items.forEach { i ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${i.qty} ×", Modifier.width(44.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(i.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(eur(i.unitPrice * i.qty), style = MaterialTheme.typography.bodyLarge)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row { Text("Productos (IVA incl.)", Modifier.weight(1f)); Text(eur(o.subtotal)) }
            Row { Text("Servicio del repartidor", Modifier.weight(1f)); Text(eur(o.serviceFee)) }
            Row { Text("Envío", Modifier.weight(1f)); Text(eur(o.deliveryFee)) }
            if (o.expressFee > 0) Row { Text("Entrega en el día", Modifier.weight(1f)); Text(eur(o.expressFee)) }
            Row { Text("Total", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text(eur(o.total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(6.dp))
            Text("Pago: ${PayMethod.of(o.paymentMethod).label} · ${if (o.isPaid) "pagado" else if (o.paymentMethod == "efectivo") "al recibirlo" else "pendiente"}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Entrega: ${o.address}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            o.ticketPath?.let { TicketView(o) }
            if (o.awaitingPayment) PayNowCard(o) { scope.launch { order = Locator.repo.order(id) } }
            else if (o.ticketTotal != null && !o.isFinal) Text(if (o.isPaid) "✓ Pagado: ${eur(o.amount)}" else "✓ Pagarás ${eur(o.amount)} en efectivo al recibirlo",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            if (o.canCancel) BigButton("Cancelar pedido", { askCancel = true }, tonal = true, icon = Icons.Filled.Close)
            else if (!o.isFinal) Text("El repartidor ya está en el súper o de camino: ya no se puede cancelar. Si hay un problema, llama al teléfono de ayuda.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (o.status == "entregado") {
                BigButton("Descargar factura", { scope.launch { Locator.repo.invoice(id)?.let { InvoicePdf.share(ctx, it, o) } ?: run { err = "La factura aún no está disponible" } } }, tonal = true, icon = Icons.Filled.Receipt)
                Spacer(Modifier.height(10.dp))
                BigButton("Solicitar reembolso", { showRefund = true }, tonal = true, icon = Icons.Filled.CurrencyExchange)
            }
            if (o.refundAmount > 0) Text("Reembolso: ${eur(o.refundAmount)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            TextButton({ showComplaint = true }) { Icon(Icons.Filled.Feedback, null); Spacer(Modifier.width(6.dp)); Text("Hoja de reclamaciones") }
            if (o.isFinal) BigButton("Repetir esta compra", {
                scope.launch {
                    runCatching {
                        val q = o.items.mapNotNull { i -> i.productId?.let { it to i.qty } }.toMap()
                        vm.cart.replaceWith(Locator.repo.productsByIds(q.keys).map { it to q.getValue(it.id) })
                        onRepeat()
                    }
                }
            }, icon = Icons.Filled.Replay)
            Spacer(Modifier.height(10.dp))
            BigButton("Llamar para pedir ayuda", { ctx.dial(es.unocerobits.tlotraigo.BuildConfig.SUPPORT_PHONE) }, tonal = true, icon = Icons.Filled.Call)
        }
    }
    order?.let { o ->
        if (showRefund) RefundDialog(o) { m -> showRefund = false; m?.let { info = it } }
        if (showComplaint) ComplaintDialog(vm, o.id) { m -> showComplaint = false; m?.let { info = it } }
    }
    if (askCancel) AlertDialog(
        onDismissRequest = { askCancel = false },
        title = { Text("¿Cancelar el pedido?") },
        text = { Text(if (order?.status == "pendiente") "Nadie lo ha aceptado todavía: se te devolverá todo lo pagado."
                      else "Un repartidor ya lo había aceptado: se devolverá lo pagado salvo la tarifa de servicio y gestión.") },
        confirmButton = { TextButton({
            askCancel = false
            scope.launch { try { Locator.repo.setStatus(id, "cancelado"); Locator.repo.refundCard(id); order = Locator.repo.order(id) } catch (e: ApiException) { err = e.message } }
        }) { Text("Sí, cancelar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ askCancel = false }) { Text("No") } },
    )
}

@Composable
private fun DriverCard(d: PersonLite, driverId: String) {
    val ctx = LocalContext.current
    var r by remember { mutableStateOf<Rating?>(null) }
    LaunchedEffect(driverId) { r = runCatching { Locator.repo.driverRating(driverId) }.getOrNull() }
    Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalShipping, null, Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Tu repartidor", style = MaterialTheme.typography.labelMedium)
                Text(d.fullName.ifBlank { "Repartidor" }, style = MaterialTheme.typography.titleMedium)
                r?.rating?.let { Row(verticalAlignment = Alignment.CenterVertically) { Stars(it, 16); Text(" $it (${r?.count})", style = MaterialTheme.typography.bodySmall) } }
            }
            d.phone?.let { ph -> FilledIconButton({ ctx.dial(ph) }) { Icon(Icons.Filled.Call, "Llamar al repartidor") } }
        }
    }
}

@Composable
private fun ReviewSection(o: Order) {
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<Review?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var driver by remember { mutableIntStateOf(0) }
    var delivery by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(o.id) {
        existing = runCatching { Locator.repo.review(o.id) }.getOrNull()
        existing?.let { driver = it.driverRating; delivery = it.deliveryRating; comment = it.comment.orEmpty() }
        loaded = true
    }
    if (!loaded) return
    Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            val e = existing
            if (e != null && !editing) {
                Text("Tu opinión", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Repartidor ", style = MaterialTheme.typography.bodyMedium); Stars(e.driverRating.toDouble(), 18) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Entrega ", style = MaterialTheme.typography.bodyMedium); Stars(e.deliveryRating.toDouble(), 18) }
                e.comment?.let { Text("«$it»", style = MaterialTheme.typography.bodyMedium) }
                TextButton({ editing = true }) { Text("Cambiar mi opinión") }
            } else {
                Text("¿Qué tal ha ido?", style = MaterialTheme.typography.titleLarge)
                StarPicker("El repartidor (trato, puntualidad)", driver) { driver = it }
                StarPicker("La entrega (productos, estado)", delivery) { delivery = it }
                OutlinedTextField(comment, { comment = it.take(1000) }, Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("Tu opinión (opcional)") }, minLines = 3, shape = MaterialTheme.shapes.medium)
                ErrorText(err)
                Spacer(Modifier.height(8.dp))
                BigButton("Enviar valoración", {
                    scope.launch {
                        busy = true; err = null
                        try {
                            Locator.repo.submitReview(o.id, driver, delivery, comment)
                            existing = Review(o.id, o.driverId, driver, delivery, comment.ifBlank { null }); editing = false
                        } catch (x: ApiException) { err = x.message }
                        busy = false
                    }
                }, enabled = driver > 0 && delivery > 0, loading = busy, icon = Icons.Filled.Star)
            }
        }
    }
}

/** Dónde está el repartidor (solo mientras el pedido va en camino). */
@Composable
private fun TrackingCard(o: Order) {
    val ctx = LocalContext.current
    var loc by remember { mutableStateOf<DriverLocation?>(null) }
    var dest by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(o.address) {
        dest = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                android.location.Geocoder(ctx, Locale("es", "ES")).getFromLocationName("${o.address}, León, España", 1)?.firstOrNull()?.let { it.latitude to it.longitude }
            }.getOrNull()
        }
    }
    LaunchedEffect(o.id) { while (true) { loc = runCatching { Locator.repo.driverLocation(o.id) }.getOrNull(); delay(10_000) } }
    val l = loc ?: return
    val km = dest?.let { (la, lo) -> FloatArray(1).also { android.location.Location.distanceBetween(l.lat, l.lng, la, lo, it) }[0] / 1000f }
    Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NearMe, null, Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(km?.let { if (it < 0.3f) "¡Tu repartidor está llegando!" else "Tu repartidor está a ${"%.1f".format(Locale("es", "ES"), it)} km" } ?: "Tu repartidor va de camino",
                    style = MaterialTheme.typography.titleMedium)
                Text("Actualizado hace ${hoursSinceSec(l.updatedAt)}", style = MaterialTheme.typography.bodySmall)
            }
            TextButton({ ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("geo:${l.lat},${l.lng}?q=${l.lat},${l.lng}(Repartidor)")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Ver mapa") }
        }
    }
}
private fun hoursSinceSec(iso: String): String = runCatching {
    val s = java.time.Duration.between(OffsetDateTime.parse(iso).toInstant(), java.time.Instant.now()).seconds
    if (s < 60) "$s s" else "${s / 60} min"
}.getOrDefault("un momento")

@Composable
private fun TicketView(o: Order) {
    var url by remember { mutableStateOf<String?>(null) }
    var big by remember { mutableStateOf(false) }
    LaunchedEffect(o.ticketPath) { url = o.ticketPath?.let { Locator.repo.ticketUrl(it) } }
    Card(Modifier.fillMaxWidth().padding(vertical = 10.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            url?.let { coil.compose.AsyncImage(it, "Ticket", Modifier.size(72.dp).clip(MaterialTheme.shapes.small).clickable { big = true }, contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Ticket del súper: ${eur(o.ticketTotal ?: 0.0)}", style = MaterialTheme.typography.titleMedium)
                Text("Estimado: ${eur(o.subtotal)} · Total final: ${eur(o.amount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (big) androidx.compose.ui.window.Dialog({ big = false }) { coil.compose.AsyncImage(url, "Ticket", Modifier.fillMaxWidth().clickable { big = false }) }
}

/** Cuando el repartidor sube el ticket: el cliente ve el importe real y paga (tarjeta) o confirma efectivo. */
@Composable
private fun PayNowCard(o: Order, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var err by remember { mutableStateOf<String?>(null) }
    var demo by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 10.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(18.dp)) {
            Text("¡Tu compra está hecha!", style = MaterialTheme.typography.titleLarge)
            Text("Ticket del súper: ${eur(o.ticketTotal ?: 0.0)} (estimado ${eur(o.subtotal)})", style = MaterialTheme.typography.bodyMedium)
            Text("Servicio, gestión y envío: ${eur(o.serviceFee + o.gestionFee + o.deliveryFee + o.expressFee)}", style = MaterialTheme.typography.bodyMedium)
            Text("Total a pagar: ${eur(o.amount)}", Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("El repartidor saldrá hacia tu casa en cuanto pagues o confirmes que pagarás en efectivo.", style = MaterialTheme.typography.bodySmall)
            ErrorText(err)
            Spacer(Modifier.height(8.dp))
            BigButton("Pagar ${eur(o.amount)} con tarjeta", {
                scope.launch {
                    busy = true; err = null
                    try { Locator.repo.cardCheckoutUrl(o.id)?.let { ctx.openUrl(it) } }
                    catch (e: ApiException) { err = e.message; demo = e.code == 503 }
                    busy = false
                }
            }, loading = busy, icon = Icons.Filled.CreditCard)
            if (demo) TextButton({ scope.launch { runCatching { Locator.repo.mockMarkPaid(o.id) }; onDone() } }) { Text("Simular pago con tarjeta (demo)") }
            Spacer(Modifier.height(8.dp))
            BigButton("Pagaré en efectivo al recibirlo", {
                scope.launch { try { Locator.repo.confirmCash(o.id); onDone() } catch (e: ApiException) { err = e.message } }
            }, tonal = true, icon = Icons.Filled.Payments)
        }
    }
}
