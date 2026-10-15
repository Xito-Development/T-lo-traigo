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
fun slotLabel(slot: String?) = when (slot) { "manana" -> "por la mañana (9–14 h)"; "tarde" -> "por la tarde (14–18 h)"; "noche" -> "por la noche (18–22 h)"; else -> "de 9 a 22 h" }
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
                                if (o.awaitingPayment) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape) {
                                        Text("💳 Pagar ${eur(o.amount)}", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
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
    var showEdit by remember { mutableStateOf(false) }
    var invoice by remember { mutableStateOf<Invoice?>(null) }
    var showComplaint by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    LaunchedEffect(id) {
        while (true) {
            try { order = Locator.repo.order(id); err = null } catch (e: ApiException) { err = e.message }
            delay(if (order?.isFinal == true) 30_000 else 8_000)   // refresco automático
        }
    }
    Scaffold(topBar = {
        TopAppBar({ Text("Pedido nº $id") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } })
    }) { pad ->
        val o = order
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            val viewer = vm.profile.collectAsState().value
            val customerView = viewer == null || !(viewer.isGestor || viewer.isAdmin) || o?.customerId == viewer.id || o?.placedBy == viewer.id
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
            if (o.driverId != null && (!o.isFinal || o.status == "entregado")) {
                val openChat = LocalOpenChat.current
                OutlinedButton({ openChat(o.id) }, Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.Forum, null); Spacer(Modifier.width(8.dp)); Text("Chat con el repartidor")
                }
            }
            if (!o.isFinal && (vm.profile.collectAsState().value?.let { !(it.isGestor || it.isAdmin) || o.customerId == it.id || o.placedBy == it.id } != false)) ClaimSection(o) { scope.launch { order = Locator.repo.order(id) } }
            if (o.awaitingPayment) Spacer(Modifier.height(4.dp))
            if (o.status == "entregado" && o.driverId != null && !o.freeService && customerView) TipCard(o) { scope.launch { order = Locator.repo.order(id) } }
            if (o.status == "entregado") ReviewSection(o)
            SectionTitle("Productos")
            ItemsWithDiff(o)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row { Text(if (o.ticketTotal != null) "Productos (ticket del súper)" else "Productos (estimado)", Modifier.weight(1f)); Text(eur(o.ticketTotal ?: o.subtotal)) }
            Row { Text("Servicio del repartidor", Modifier.weight(1f)); Text(eur(o.serviceFee)) }
            Row { Text("Gestión", Modifier.weight(1f)); Text(eur(o.gestionFee)) }
            if (o.extraFee > 0) Row { Text("Tarifas adicionales", Modifier.weight(1f)); Text(eur(o.extraFee)) }
            Row { Text("Envío", Modifier.weight(1f)); Text(eur(o.deliveryFee)) }
            if (o.expressFee > 0) Row { Text("Entrega en el día", Modifier.weight(1f)); Text(eur(o.expressFee)) }
            Row { Text(if (o.finalTotal != null) "Total" else "Total estimado", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text(eur(o.amount), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(6.dp))
            Text("Pago: ${PayMethod.of(o.paymentMethod).label} · ${if (o.isPaid) "pagado" else if (o.paymentMethod == "efectivo") "al recibirlo" else "pendiente"}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Entrega: ${o.address}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            o.ticketPath?.let { TicketView(o) }
            if (o.awaitingPayment && customerView) PayNowCard(o) { scope.launch { order = Locator.repo.order(id) } }
            else if (o.paymentMethod == "bizum" && o.clientPaidAt != null && !o.isPaid) Text("📲 Bizum enviado. Falta que ${o.bizumName ?: "lo"} confirme el cobro.",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))
            val me = vm.profile.collectAsState().value
            if (o.ticketTotal != null && !o.isPaid && (me?.isGestor == true || me?.isAdmin == true)) OutlinedButton({
                scope.launch { try { Locator.repo.markCollected(o.id); order = Locator.repo.order(id) } catch (e: ApiException) { err = e.message } } },
                Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Icon(Icons.Filled.DoneAll, null); Spacer(Modifier.width(6.dp)); Text("Marcar como cobrado (${eur(o.amount)})") }
            else if (o.ticketTotal != null && !o.isFinal) Text(if (o.isPaid) "✓ Pagado: ${eur(o.amount)}" else "✓ Pagarás ${eur(o.amount)} en efectivo al recibirlo",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            val meP = vm.profile.collectAsState().value
            val asCustomer = meP == null || !(meP.isGestor || meP.isAdmin) || o.customerId == meP.id || o.placedBy == meP.id
            if (o.canCancel && asCustomer) { BigButton("Editar pedido", { showEdit = true }, tonal = true, icon = Icons.Filled.EditNote); Spacer(Modifier.height(10.dp)) }
            if (o.canCancel && asCustomer) BigButton("Cancelar pedido", { askCancel = true }, tonal = true, icon = Icons.Filled.Close)
            else if (!o.isFinal) Text("El repartidor ya está en el súper o de camino: ya no se puede cancelar. Si hay un problema, llama al teléfono de ayuda.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (o.status == "entregado") {
                BigButton(if (o.freeService) "Justificante del pedido" else "Ver factura", { scope.launch {
                    invoice = if (o.freeService) null else Locator.repo.invoice(id)
                    if (invoice == null) ReceiptPdf.open(ctx, o) } }, tonal = true, icon = Icons.Filled.Receipt)
                Spacer(Modifier.height(10.dp))
                if (customerView) BigButton("Solicitar reembolso", { showRefund = true }, tonal = true, icon = Icons.Filled.CurrencyExchange)
            }
            if (o.refundAmount > 0) Text("Reembolso: ${eur(o.refundAmount)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            TextButton({ showComplaint = true }) { Icon(Icons.Filled.Feedback, null); Spacer(Modifier.width(6.dp)); Text("Hoja de reclamaciones") }
            if (o.isFinal) BigButton("Repetir esta compra", {
                scope.launch {
                    runCatching { Locator.repo.repeatOrder(o); onRepeat() }
                }
            }, icon = Icons.Filled.Replay)
            Spacer(Modifier.height(10.dp))
            BigButton("Llamar para pedir ayuda", { ctx.dial(es.unocerobits.tlotraigo.BuildConfig.SUPPORT_PHONE) }, tonal = true, icon = Icons.Filled.Call)
        }
    }
    order?.let { o ->
        if (showRefund) RefundDialog(o) { m -> showRefund = false; m?.let { info = it } }
        invoice?.let { InvoiceDialog(it, o) { invoice = null } }
        if (showEdit) EditOrderDialog(o) { m -> showEdit = false; m?.let { info = it; scope.launch { order = Locator.repo.order(id) } } }
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
    val celebrateReview = LocalCelebrate.current
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
                            celebrateReview(Celebration("¡Gracias por tu opinión!", "Nos ayuda a mejorar el servicio", "⭐"))
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
    if (o.paymentMethod == "bizum") { BizumPayCard(o, onDone); return }
    var demo by remember { mutableStateOf(false) }
    var cardOn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { Locator.repo.fees() }.getOrNull()?.let { demo = it.demoPayments && it.cardEnabled; cardOn = it.cardEnabled } }
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
            if (cardOn) BigButton("Pagar ${eur(o.amount)} con tarjeta", {
                scope.launch {
                    busy = true; err = null
                    try { Locator.repo.cardCheckoutUrl(o.id)?.let { ctx.openUrl(it) } }
                    catch (e: ApiException) { err = e.message }
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

/** Propina al terminar: 100 % para el repartidor, fuera de la factura. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TipCard(o: Order, onDone: () -> Unit) {
    val celebrateTip = LocalCelebrate.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf<Double?>(null) }
    var other by remember { mutableStateOf("") }
    var demo by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var cardOn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { Locator.repo.fees() }.getOrNull()?.let { demo = it.demoPayments && it.cardEnabled; cardOn = it.cardEnabled } }
    val name = o.driver?.fullName?.substringBefore(' ')?.ifBlank { null } ?: "tu repartidor"
    Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(18.dp).animateContentSize()) {
            if (o.tipStatus == "pagada") {
                Text("💛 ¡Gracias por tu propina de ${eur(o.tipAmount)}!", style = MaterialTheme.typography.titleMedium)
                Text("Es íntegra para $name.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Text("¿Quieres dejarle propina a $name?", style = MaterialTheme.typography.titleLarge)
            Text("Es voluntaria y va entera para el repartidor.", style = MaterialTheme.typography.bodyMedium)
            if (o.tipStatus == "pendiente") {
                Text("Propina de ${eur(o.tipAmount)} pendiente de pago con tarjeta.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
                BigButton("Pagar propina ${eur(o.tipAmount)}", { scope.launch { try { Locator.repo.tipCheckoutUrl(o.id)?.let { ctx.openUrl(it) } } catch (e: ApiException) { err = e.message } } }, icon = Icons.Filled.CreditCard)
                if (demo) TextButton({ scope.launch { runCatching { Locator.repo.mockTipPaid(o.id) }; onDone() } }) { Text("Simular pago de la propina (demo)") }
                ErrorText(err); return@Column
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf(1.0, 2.0, 3.0, 5.0).forEach { v -> FilterChip(amount == v && other.isBlank(), { amount = v; other = "" }, { Text(eur(v)) }) }
            }
            OutlinedTextField(other, { other = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5); amount = parseNum(other) }, Modifier.fillMaxWidth().padding(top = 6.dp),
                label = { Text("Otra cantidad") }, suffix = { Text("€") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            ErrorText(err)
            val a = amount
            Spacer(Modifier.height(8.dp))
            if (cardOn) BigButton(if (a != null) "Dar ${eur(a)} con tarjeta" else "Dar propina con tarjeta", {
                scope.launch {
                    busy = true; err = null
                    try { Locator.repo.giveTip(o.id, a!!, "tarjeta"); onDone()
                        try { Locator.repo.tipCheckoutUrl(o.id)?.let { ctx.openUrl(it) } } catch (e: ApiException) { if (!demo) err = e.message } }
                    catch (e: ApiException) { err = e.message }
                    busy = false
                }
            }, enabled = a != null && a >= 0.5, loading = busy, icon = Icons.Filled.Favorite)
            if (cardOn) TextButton({ scope.launch { try { Locator.repo.giveTip(o.id, a!!, "efectivo"); onDone() } catch (e: ApiException) { err = e.message } } },
                enabled = a != null && a >= 0.5) { Text("Ya se la di en efectivo") }
            else BigButton(if (a != null) "Le he dado ${eur(a)} en efectivo" else "Apuntar propina en efectivo", {
                scope.launch { try { Locator.repo.giveTip(o.id, a!!, "efectivo"); celebrateTip(Celebration("¡Gracias por tu propina!", "Va entera para el repartidor", "💛")); onDone() } catch (e: ApiException) { err = e.message } } },
                enabled = a != null && a >= 0.5, icon = Icons.Filled.Favorite)
        }
    }
}

/** Bizum: importe, a quién y concepto, con botones para copiar. El usuario confirma el envío y el repartidor/entidad confirma el cobro. */
@Composable
private fun BizumPayCard(o: Order, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var err by remember { mutableStateOf<String?>(null) }
    val clip = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember { mutableStateOf<String?>(null) }
    Card(Modifier.fillMaxWidth().padding(vertical = 10.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("¡Tu compra está hecha!", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge); PayBadge("bizum") }
            Text("Ticket del súper: ${eur(o.ticketTotal ?: 0.0)}" + if (o.amount > (o.ticketTotal ?: 0.0)) " + tarifas ${eur(o.amount - (o.ticketTotal ?: 0.0))}" else "", style = MaterialTheme.typography.bodyMedium)
            if (o.bizumPhone == null) {
                Text("El repartidor no tiene Bizum configurado. Paga en efectivo al recibir la compra.", Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyLarge)
                BigButton("Pagaré en efectivo", { scope.launch { try { Locator.repo.confirmCash(o.id); onDone() } catch (e: ApiException) { err = e.message } } }, icon = Icons.Filled.Payments)
                ErrorText(err); return@Column
            }
            Text("Envía por Bizum desde la app de tu banco:", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleMedium)
            @Composable fun copyRow(label: String, value: String, big: Boolean = false) = Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium) }
                TextButton({ clip.setText(androidx.compose.ui.text.AnnotatedString(value)); copied = label }) { Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(if (copied == label) "Copiado" else "Copiar") }
            }
            copyRow("Importe", fmtNum(o.amount) + " €", big = true)
            copyRow("A (${if (o.bizumTo == "entidad") "entidad" else "repartidor"}: ${o.bizumName.orEmpty()})", o.bizumPhone)
            copyRow("Concepto", "Pedido ${o.id}")
            Text("El repartidor saldrá hacia tu casa cuando confirmes que lo has enviado.", style = MaterialTheme.typography.bodySmall)
            ErrorText(err)
            Spacer(Modifier.height(8.dp))
            val celebrate = LocalCelebrate.current
            BigButton("Ya he enviado el Bizum", { scope.launch { try { Locator.repo.clientMarkPaid(o.id); celebrate(Celebration("¡Bizum enviado!", "El repartidor sale hacia tu casa")); onDone() } catch (e: ApiException) { err = e.message } } }, icon = Icons.Filled.Send)
            TextButton({ scope.launch { try { Locator.repo.confirmCash(o.id); onDone() } catch (e: ApiException) { err = e.message } } }) { Text("Prefiero pagar en efectivo al recibirlo") }
        }
    }
}
