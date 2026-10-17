package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.payments.PayMethod
import es.unocerobits.tlotraigo.payments.PayResult
import es.unocerobits.tlotraigo.payments.Payments
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chipFmt = DateTimeFormatter.ofPattern("EEE d", Locale("es", "ES"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CartScreen(vm: AppViewModel, onBack: () -> Unit, onPlaced: (Long) -> Unit) {
    val lines by vm.cart.lines.collectAsState()
    val me by vm.profile.collectAsState()
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val celebrate = LocalCelebrate.current

    var seniors by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var target by remember(me) { mutableStateOf(me) }
    var address by remember(target) { mutableStateOf(target?.let { t -> composeAddress(t, vm.townName(t.townId)) }.orEmpty()) }
    var notes by remember { mutableStateOf("") }
    var methods by remember { mutableStateOf<List<String>?>(null) }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var date by remember { mutableStateOf<LocalDate?>(null) }   // null = lo antes posible
    var slot by remember { mutableStateOf<String?>(null) }
    var quote by remember { mutableStateOf<Quote?>(null) }
    var quoteErr by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var cardOn by remember { mutableStateOf(false) }
    // Días de reparto de la entidad del pueblo (los demás días no se pueden elegir)
    var orgDays by remember { mutableStateOf<List<Int>?>(null) }
    var orgClosed by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(target?.orgId) { target?.orgId?.let { runCatching { Locator.repo.organization(it) }.getOrNull() }?.let { orgDays = it.deliveryDays; orgClosed = it.closedDates } }
    LaunchedEffect(Unit) { cardOn = runCatching { Locator.repo.fees().cardEnabled }.getOrDefault(false); if (!cardOn && method == PayMethod.CARD) method = PayMethod.CASH }
    LaunchedEffect(quote?.allowCash, quote?.allowBizum) { if (quote?.allowCash == false && quote?.allowBizum == true) method = PayMethod.BIZUM }
    var payConfirm by remember { mutableStateOf(false) }
    var adultOk by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var payingMsg by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { seniors = Locator.repo.linkedSeniors() }
        // Refresca precios de la cesta guardada (pueden haber cambiado desde que se añadió)
        runCatching {
            val fresh = Locator.repo.productsByIds(lines.keys).associateBy { it.id }
            var changed = 0; var gone = 0
            lines.values.filter { it.needId == null }.forEach { l ->
                val f = fresh[l.product.id]
                when {
                    f == null -> { vm.cart.set(l.product, 0); gone++ }
                    f.price != l.product.price -> { vm.cart.set(f, l.qty); changed++ }
                }
            }
            notice = listOfNotNull(
                if (changed > 0) "Han cambiado los precios de ${productos(changed)}." else null,
                if (gone > 0) "${productos(gone)} ya no están disponibles y se han quitado." else null,
            ).joinToString(" ").ifBlank { null }
        }
    }
    LaunchedEffect(target?.townId) {
        val t = target?.townId ?: return@LaunchedEffect
        methods = runCatching { Locator.repo.paymentMethods(t) }.getOrDefault(emptyList())
        val av = methods.orEmpty()
        if (av.isNotEmpty() && method.db !in av) method = PayMethod.of(av.first())
        if (av.isEmpty()) method = PayMethod.CASH
    }
    // Presupuesto en servidor (misma fórmula que el pedido final)
    LaunchedEffect(lines, target?.id, date) {
        val t = target ?: return@LaunchedEffect
        if (lines.isEmpty()) { quote = null; return@LaunchedEffect }
        delay(300)
        try {
            quote = Locator.repo.quote(t.id, lines.values.first().product.supermarketId, lines.mapValues { it.value.qty }, date?.toString())
            quoteErr = null
        } catch (e: ApiException) { quoteErr = e.message }
    }
    val total = quote?.total ?: lines.values.sumOf { it.total }
    fun placeOrder() = scope.launch {
        busy = true; err = null
        try {
            val t = target ?: error("Sin cliente")
            val id = Locator.repo.createOrder(t.id, lines.values.first().product.supermarketId,
                lines.mapValues { it.value.qty }, method.db, address, notes, date?.toString(), if (date == null) null else slot, adultOk)
            // No se cobra ahora: el cliente paga cuando el repartidor sube el ticket con el importe real
            celebrate(Celebration("¡Pedido hecho!", "Te avisaremos cuando un repartidor lo acepte", "🛍️"))
            vm.cart.clear(); onPlaced(id)
        } catch (e: Exception) { err = e.message; payingMsg = null }
        busy = false
    }


    Scaffold(
        topBar = { TopAppBar({ Text("Mi cesta") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) },
        bottomBar = {
            AnimatedVisibility(lines.isNotEmpty(), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                Surface(tonalElevation = 6.dp, shadowElevation = 12.dp) {
                    Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                        BigButton("Revisar y pedir · ${eur(total)}", { confirm = true }, icon = Icons.Filled.CheckCircle,
                            enabled = address.isNotBlank() && target?.townId != null && quote != null && (date == null || slot != null), loading = busy)
                    }
                }
            }
        },
    ) { pad ->
        if (lines.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), Alignment.Center) {
                Message(Icons.Filled.ShoppingCart, "Tu cesta está vacía", notice ?: "Añade productos desde el catálogo.", "Ir al catálogo", onBack)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            notice?.let { n -> item {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(n, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            } }
            items(lines.values.toList(), key = { it.product.id }) { l ->
                Row(Modifier.fillMaxWidth().animateItem().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb(l.product, 56)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l.product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        l.detail?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
                        Text(eur(l.total), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    QtyStepper(l.qty, { vm.cart.remove(l.product) }, { vm.cart.add(l.product) })
                }
            }
            if (seniors.isNotEmpty()) item {
                SectionTitle("¿Para quién es la compra?")
                me?.let { m -> ChoiceRow("Para mí", vm.townName(m.townId), target?.id == m.id, icon = Icons.Filled.Person) { target = m } }
                seniors.forEach { s -> ChoiceRow(s.fullName.ifBlank { "Familiar" }, vm.townName(s.townId), target?.id == s.id, icon = Icons.Filled.Elderly) { target = s } }
            }
            item {
                SectionTitle("¿Cuándo lo quieres?")
                val today = LocalDate.now(ZoneId.of("Europe/Madrid"))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(date == null, { date = null; slot = null }, { Text("Lo antes posible") }, leadingIcon = { Icon(Icons.Filled.Bolt, null, Modifier.size(18.dp)) }) }
                    items((0L..14L).toList()) { d ->
                        val day = today.plusDays(d)
                        val label = when (d) { 0L -> "Hoy"; 1L -> "Mañana"; else -> day.format(chipFmt).replaceFirstChar { it.uppercase() } }
                        val open = day.dayOfWeek.value in (orgDays ?: (1..7).toList()) && day.toString() !in orgClosed
                        FilterChip(date == day, { date = day; if (slot == null) slot = "manana" }, { Text(label) }, enabled = open)
                    }
                }
                AnimatedVisibility(date != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        FilterChip(slot == "manana", { slot = "manana" }, { Text("9–14 h") }, leadingIcon = { Icon(Icons.Filled.WbSunny, null, Modifier.size(18.dp)) })
                        FilterChip(slot == "tarde", { slot = "tarde" }, { Text("14–18 h") }, leadingIcon = { Icon(Icons.Filled.WbTwilight, null, Modifier.size(18.dp)) })
                        FilterChip(slot == "noche", { slot = "noche" }, { Text("18–22 h") }, leadingIcon = { Icon(Icons.Filled.NightsStay, null, Modifier.size(18.dp)) })
                    }
                }
                val hour = java.time.LocalTime.now(ZoneId.of("Europe/Madrid")).hour
                if (date == null && (hour < 9 || hour >= 22)) Text("🌙 Fuera del horario de entrega (9:00 a 22:00): te lo llevarán a partir de las 9:00.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                if (date == null || date == today) Text("La entrega en el día lleva un pequeño suplemento.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                SectionTitle("Entrega")
                OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Dirección de entrega") },
                    minLines = 2, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(notes, { notes = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Notas para el repartidor (opcional)") },
                    placeholder = { Text("Ej.: el pan poco hecho, llamar al timbre de abajo") },
                    minLines = 2, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium)
            }
            item {
                SectionTitle("Forma de pago")
                val av = methods
                if (quote?.productsPayment == "entidad") Text("La compra la gestiona ${quote?.orgName ?: "tu entidad"}: no pagas nada al repartidor.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                else if (av == null) Loading()
                else {
                    if (av.isEmpty()) Text("Ahora mismo no hay repartidores activos en tu pueblo. Tu pedido quedará en espera y se pagará en efectivo.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PayMethod.entries.forEach { m ->
                        val enabled = when (m) { PayMethod.BIZUM -> quote?.allowBizum == true; PayMethod.CARD -> false; PayMethod.CASH -> quote?.allowCash != false }
                        if ((!enabled && m != PayMethod.CASH) || m == PayMethod.CARD) return@forEach   // solo se muestran las formas que ofrece la entidad
                        val icon = when (m) { PayMethod.CASH -> Icons.Filled.Payments; PayMethod.BIZUM -> Icons.Filled.Smartphone; PayMethod.CARD -> Icons.Filled.CreditCard }
                        ChoiceRow(m.label, when { m == PayMethod.BIZUM -> "Desde la app de tu banco, cuando el repartidor suba el ticket con el importe real"
                            m == PayMethod.CASH && !enabled -> "No disponible en este servicio"; m == PayMethod.CASH -> "Al recibir la compra, con el importe del ticket"; else -> m.hint }, method == m, enabled, icon) { method = m }
                    }
                }
            }
            item {
                SectionTitle("Resumen")
                ErrorText(quoteErr)
                val q = quote
                if (q == null) Loading()
                else {
                    // Productos bien visibles: icono o foto, cantidad, nombre, detalle y precio
                    Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            lines.values.forEachIndexed { i, l ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box { ProductThumb(l.product, 48)
                                        Surface(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp), shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary) {
                                            Text("${l.qty}", Modifier.padding(horizontal = 7.dp, vertical = 1.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary) } }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(l.product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                        (l.detail ?: l.product.brand)?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
                                        Text("${l.qty} × ${eur(l.product.price)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(eur(l.total), style = MaterialTheme.typography.titleMedium)
                                }
                                if (i < lines.size - 1) HorizontalDivider(Modifier.padding(start = 74.dp))
                            }
                        }
                    }
                    val openList = LocalOpenList.current
                    TextButton({ openList(-1, lines.values.map { it.toListItem() }) }) { Icon(Icons.Filled.PlaylistAdd, null); Spacer(Modifier.width(6.dp)); Text("Guardar como lista") }
                    SummaryLine("Productos (precio orientativo)", q.subtotal)
                    SummaryLine("   de los que IVA", q.productsVat, muted = true)
                    SummaryLine((if (q.payoutMode == "gastos") "Servicio de T lo traigo" else "Servicio del repartidor") + " (${q.servicePct.toInt()} %)", q.service)
                    SummaryLine("Gestión del pedido (${q.gestionPct.toInt()} %)", q.gestion)
                    if (q.itemsFee > 0) SummaryLine("Por cantidad (${q.units - q.freeItems} uds. más de ${q.freeItems})", q.itemsFee)
                    SummaryLine("Envío a ${q.town}", q.deliveryBase)
                    SummaryLine(if (q.payoutMode == "gastos") "Gastos de desplazamiento del repartidor (${q.distanceKm.toInt()} km, ida y vuelta)" else "Distancia (${q.distanceKm.toInt()} km)", q.distanceFee)
                    if (q.expressFee > 0) SummaryLine("Entrega en el día", q.expressFee)
                    q.extras.filter { it.amount > 0 }.forEach { SummaryLine(it.name, it.amount) }
                    SummaryLine("   IVA del servicio (21 %)", q.feesVat, muted = true)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    if (q.feesMode == "gratuito") Surface(Modifier.fillMaxWidth().padding(vertical = 6.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                        Text("✓ Servicio gratuito ofrecido por ${q.orgName ?: "tu entidad"}: no pagas tarifas.", Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                    SummaryLine("Total estimado (IVA incluido)", q.total, strong = true)
                    Surface(Modifier.fillMaxWidth().padding(top = 8.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                        Text("Los precios de los productos son orientativos. El repartidor hará una foto al ticket del súper y pagarás lo real. " +
                            "Como máximo ${eur(q.maxTotal)} (un ${q.marginPct.toInt()} % más en productos); si se pasara, te llamará antes.",
                            Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                ErrorText(err)
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val needsAdult = lines.values.any { it.product.ageRestricted }
    if (confirm) {
        // Paso 1: revisión completa del pedido. Botón con "obligación de pago" (art. 98.2 TRLGDCU).
        var reviewed by remember { mutableStateOf(false) }
        var adult by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Filled.ShoppingBag, null) },
            title = { Text("Revisa tu pedido") },
            text = {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    ConfirmRow(Icons.Filled.ShoppingBasket, productos(lines.values.sumOf { it.qty }))
                    ConfirmRow(Icons.Filled.Event, date?.let { prettyDay(it.toString()) + ", " + slotLabel(slot) } ?: "Lo antes posible")
                    ConfirmRow(Icons.Filled.Home, address)
                    ConfirmRow(Icons.Filled.Payments, method.label + if (method == PayMethod.CASH) " (al recibirlo)" else "")
                    Text("Total: ${eur(total)}", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text("IVA incluido. No pagas ahora: cuando el repartidor haga la compra verás la foto del ticket con el importe real y entonces pagarás. Puedes cancelar mientras no esté en el súper.", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(reviewed, { reviewed = it }); Text("He revisado los productos, la dirección y el importe.", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (needsAdult) Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(adult, { adult = it }); Text("Soy mayor de 18 años. El repartidor me pedirá el DNI al entregar las bebidas alcohólicas.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button({ confirm = false; adultOk = adult; placeOrder() },
                    enabled = reviewed && (!needsAdult || adult)) { Text("Confirmar pedido con obligación de pago") }
            },
            dismissButton = { TextButton({ confirm = false }) { Text("Volver") } },
        )
    }
    // Paso 2 (tarjeta o Bizum): confirmación explícita del cobro
    if (payConfirm) AlertDialog(
        onDismissRequest = { payConfirm = false },
        icon = { Icon(if (method == PayMethod.CARD) Icons.Filled.CreditCard else Icons.Filled.PhoneAndroid, null) },
        title = { Text("¿Pagar con ${method.label}?") },
        text = { Text("Se reservará en tu tarjeta un máximo de ${eur(quote?.maxTotal ?: total)} y solo se cobrará el importe real del ticket más las tarifas. Se abrirá la página de pago seguro de Stripe.") },
        confirmButton = { Button({ payConfirm = false; placeOrder() }) { Text("Sí, continuar al pago") } },
        dismissButton = { TextButton({ payConfirm = false }) { Text("No, volver") } },
    )
    payingMsg?.let { msg ->
        AlertDialog({}, confirmButton = {}, icon = { CircularProgressIndicator() }, title = { Text("Pagando") }, text = { Text(msg) })
    }
}

@Composable
private fun SummaryLine(label: String, value: Double, strong: Boolean = false, muted: Boolean = false) {
    if (value == 0.0 && !strong && !muted) return   // las tarifas a 0 no se muestran
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1f),
            style = if (strong) MaterialTheme.typography.titleMedium else if (muted) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        Text(eur(value), style = if (strong) MaterialTheme.typography.titleLarge else if (muted) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
            color = if (strong) MaterialTheme.colorScheme.primary else if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ConfirmRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) =
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text(text, style = MaterialTheme.typography.bodyLarge)
    }
