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

    var seniors by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var target by remember(me) { mutableStateOf(me) }
    var address by remember(target) { mutableStateOf(target?.address.orEmpty()) }
    var notes by remember { mutableStateOf("") }
    var methods by remember { mutableStateOf<List<String>?>(null) }
    var method by remember { mutableStateOf(PayMethod.CASH) }
    var date by remember { mutableStateOf<LocalDate?>(null) }   // null = lo antes posible
    var slot by remember { mutableStateOf<String?>(null) }
    var quote by remember { mutableStateOf<Quote?>(null) }
    var quoteErr by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
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
            lines.values.forEach { l ->
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
            when (method) {
                PayMethod.CARD -> {   // Pasarela Stripe: se autoriza el máximo y se cobra lo real del ticket
                    payingMsg = "Abriendo el pago seguro…"
                    try { Locator.repo.cardCheckoutUrl(id)?.let { ctx.openUrl(it) } }
                    catch (e: ApiException) { err = "Pedido hecho, pero no se pudo abrir el pago: ${e.message}" }
                }
                PayMethod.BIZUM -> { payingMsg = "Procesando Bizum (simulado)…"; if (Payments.provider(method).pay(id, total) == PayResult.Paid) runCatching { Locator.repo.mockMarkPaid(id) } }
                PayMethod.CASH -> {}
            }
            payingMsg = null; vm.cart.clear(); onPlaced(id)
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
                        FilterChip(date == day, { date = day; if (slot == null) slot = "manana" }, { Text(label) })
                    }
                }
                AnimatedVisibility(date != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        FilterChip(slot == "manana", { slot = "manana" }, { Text("Mañana · 9–13 h") }, leadingIcon = { Icon(Icons.Filled.WbSunny, null, Modifier.size(18.dp)) })
                        FilterChip(slot == "tarde", { slot = "tarde" }, { Text("Tarde · 16–20 h") }, leadingIcon = { Icon(Icons.Filled.WbTwilight, null, Modifier.size(18.dp)) })
                    }
                }
                if (date == null || date == today) Text("La entrega en el día lleva un pequeño suplemento.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            item {
                SectionTitle("Entrega")
                OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Dirección en ${vm.townName(target?.townId)}") },
                    minLines = 2, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(notes, { notes = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Notas para el repartidor (opcional)") },
                    placeholder = { Text("Ej.: el pan poco hecho, llamar al timbre de abajo") },
                    minLines = 2, textStyle = MaterialTheme.typography.bodyLarge, shape = MaterialTheme.shapes.medium)
            }
            item {
                SectionTitle("Forma de pago")
                val av = methods
                if (av == null) Loading()
                else {
                    if (av.isEmpty()) Text("Ahora mismo no hay repartidores activos en tu pueblo. Tu pedido quedará en espera y se pagará en efectivo.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PayMethod.entries.forEach { m ->
                        val enabled = if (av.isEmpty()) m == PayMethod.CASH else m.db in av
                        val icon = when (m) { PayMethod.CASH -> Icons.Filled.Payments; PayMethod.BIZUM -> Icons.Filled.PhoneAndroid; PayMethod.CARD -> Icons.Filled.CreditCard }
                        ChoiceRow(m.label, if (enabled) m.hint else "No disponible ahora en tu pueblo", method == m, enabled, icon) { method = m }
                    }
                }
            }
            item {
                SectionTitle("Resumen")
                ErrorText(quoteErr)
                val q = quote
                if (q == null) Loading()
                else {
                    SummaryLine("Productos (precio del súper)", q.subtotal)
                    SummaryLine("   de los que IVA", q.productsVat, muted = true)
                    SummaryLine("Servicio del repartidor (${q.servicePct.toInt()} %)", q.service)
                    SummaryLine("Gestión del pedido (${q.gestionPct.toInt()} %)", q.gestion)
                    if (q.itemsFee > 0) SummaryLine("Por cantidad (${q.units - q.freeItems} uds. más de ${q.freeItems})", q.itemsFee)
                    SummaryLine("Envío a ${q.town}", q.deliveryBase)
                    SummaryLine("Distancia (${q.distanceKm.toInt()} km)", q.distanceFee)
                    if (q.expressFee > 0) SummaryLine("Entrega en el día", q.expressFee)
                    SummaryLine("   IVA del servicio (21 %)", q.feesVat, muted = true)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
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
                    Text("IVA incluido. Puedes cancelarlo gratis mientras ningún repartidor lo haya aceptado.", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(reviewed, { reviewed = it }); Text("He revisado los productos, la dirección y el importe.", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (needsAdult) Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(adult, { adult = it }); Text("Soy mayor de 18 años. El repartidor me pedirá el DNI al entregar las bebidas alcohólicas.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button({ confirm = false; adultOk = adult; if (method == PayMethod.CASH) placeOrder() else payConfirm = true },
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
