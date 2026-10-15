package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import es.unocerobits.tlotraigo.ui.TabLabel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.payments.PayMethod
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class DTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    NUEVOS("Nuevos", Icons.Filled.NotificationsActive), MIOS("Repartos", Icons.Filled.LocalShipping),
    GANANCIAS("Ganancias", Icons.Filled.Savings), AJUSTES("Ajustes", Icons.Filled.Settings)
}

@Composable
fun DriverHome(vm: AppViewModel, onDebug: () -> Unit = {}) {
    var tab by rememberSaveable { mutableStateOf(DTab.NUEVOS) }
    BackHandler(tab != DTab.NUEVOS) { tab = DTab.NUEVOS }
    Scaffold(bottomBar = {
        NavigationBar {
            DTab.entries.forEach { t -> NavigationBarItem(tab == t, { tab = t }, { Icon(t.icon, null) }, label = { TabLabel(t.label) }, alwaysShowLabel = true) }
        }
    }) { pad ->
        AnimatedContent(tab, Modifier.padding(pad), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "dtab") { t ->
            when (t) {
                DTab.NUEVOS -> DriverPending(vm, onApply = { tab = DTab.AJUSTES }) { tab = DTab.MIOS }
                DTab.MIOS -> DriverMine()
                DTab.GANANCIAS -> DriverEarningsScreen()
                DTab.AJUSTES -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("Ajustes", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(start = 20.dp, top = 12.dp))
                    DriverSettingsPanel(vm); SettingsScreen(vm, showFamily = false, onDebug = onDebug)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DriverPending(vm: AppViewModel, onApply: () -> Unit, onAccepted: () -> Unit) {
    var rejecting by remember { mutableStateOf<Order?>(null) }
    var fees by remember { mutableStateOf(FeeConfig()) }
    LaunchedEffect(Unit) { runCatching { fees = Locator.repo.fees() } }
    var ds by remember { mutableStateOf<DriverSettings?>(null) }
    var orders by remember { mutableStateOf<List<Order>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val ctxS = androidx.compose.ui.platform.LocalContext.current
    val perms = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) {
        es.unocerobits.tlotraigo.DriverService.start(ctxS)
    }
    var disclosure by remember { mutableStateOf(false) }
    var pendientes by remember { mutableStateOf<List<String>>(emptyList()) }
    // Aviso destacado antes de pedir permisos (política de Google Play sobre ubicación)
    if (disclosure) AlertDialog(
        onDismissRequest = { disclosure = false },
        icon = { Icon(Icons.Filled.NotificationsActive, null) },
        title = { Text("Avisos y ubicación") },
        text = { Text("Para avisarte de pedidos nuevos, T lo traigo mostrará notificaciones mientras estés disponible.\n\n" +
            "Tu ubicación solo se usa mientras llevas un pedido «en camino», para que el cliente vea por dónde vas y para la ruta. " +
            "Se deja de compartir al marcarlo como entregado y no se usa con la app cerrada fuera de un reparto.") },
        confirmButton = { Button({ disclosure = false; perms.launch(pendientes.toTypedArray()) }) { Text("Continuar") } },
        dismissButton = { TextButton({ disclosure = false; es.unocerobits.tlotraigo.DriverService.start(ctxS) }) { Text("Ahora no") } },
    )
    fun pedirPermisosYArrancar() {
        val faltan = buildList {
            if (android.os.Build.VERSION.SDK_INT >= 33) add(android.Manifest.permission.POST_NOTIFICATIONS)
            add(android.Manifest.permission.ACCESS_FINE_LOCATION); add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }.filter { androidx.core.content.ContextCompat.checkSelfPermission(ctxS, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (faltan.isEmpty()) es.unocerobits.tlotraigo.DriverService.start(ctxS) else { pendientes = faltan; disclosure = true }
    }
    LaunchedEffect(Unit) {
        runCatching { ds = Locator.repo.driverSettings() }
        if (ds?.available == true && ds?.approved == true) pedirPermisosYArrancar()
    }
    LaunchedEffect(ds?.available) {
        while (ds?.available == true) {
            try { orders = Locator.repo.pendingOrders(); err = null } catch (e: ApiException) { err = e.message }
            delay(15_000)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            val on = ds?.available == true
            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (on) "Estás disponible" else "No estás disponible", style = MaterialTheme.typography.titleLarge)
                        Text(if (on) "Recibirás pedidos de tus pueblos" else "Actívalo para ver pedidos nuevos", style = MaterialTheme.typography.bodyMedium)
                    }
                    AppSwitch(on, { v -> ds?.let { cur -> val n = cur.copy(available = v); ds = n
                        scope.launch { runCatching { Locator.repo.saveDriverSettings(n) } }
                        if (v) pedirPermisosYArrancar() else es.unocerobits.tlotraigo.DriverService.stop(ctxS) } })
                }
            }
            if (ds != null && ds?.approved == false) Surface(Modifier.fillMaxWidth().padding(top = 10.dp), color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(14.dp)) {
                    Text("Aún no estás autorizado para repartir. Envía tu candidatura y el responsable la revisará.",
                        color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onApply, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer)) {
                        Icon(Icons.Filled.AssignmentInd, null); Spacer(Modifier.width(6.dp)); Text("Ver mi candidatura")
                    }
                }
            }
            if (ds?.townIds?.isEmpty() == true) Text("Elige tus pueblos en Ajustes para recibir pedidos.", Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error)
        }
        item { ErrorText(err) }
        val list = orders
        when {
            ds?.available != true -> {}
            list == null -> item { Loading() }
            list.isEmpty() -> item { Message(Icons.Filled.HourglassEmpty, "No hay pedidos nuevos", "Te avisaremos aquí en cuanto llegue uno.") }
            else -> items(list, key = { it.id }) { o ->
                val celebrate = LocalCelebrate.current
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).animateItem(), shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.secondary)
                            Text(vm.townName(o.townId), Modifier.weight(1f).padding(start = 6.dp), style = MaterialTheme.typography.titleLarge)
                            if (!o.freeService) Text("+${eur(o.driverEarnings)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${productos(o.items.sumOf { it.qty })} · compra ${eur(o.subtotal)} · ${PayMethod.of(o.paymentMethod).label}", style = MaterialTheme.typography.bodyMedium)
                        ScheduleBadge(o.scheduledDate, o.scheduledSlot)
                        val h = hoursSince(o.createdAt)
                        val stale = h >= fees.staleHours
                        Text(if (h < 1) "Recibido hace un momento" else "En espera desde hace ${waitLabel(h)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (stale && fees.stalePenalty > 0) Text("Más de ${fees.staleHours} h sin aceptar: si no lo aceptas se aplicará una penalización de ${eur(fees.stalePenalty)}.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton({ rejecting = o }, Modifier.weight(1f).heightIn(min = bigH()), shape = MaterialTheme.shapes.medium) {
                                Icon(Icons.Filled.Close, null); Spacer(Modifier.width(6.dp)); Text("Rechazar")
                            }
                            BigButton("Aceptar", {
                                scope.launch {
                                    busyId = o.id
                                    try { Locator.repo.acceptOrder(o.id); celebrate(Celebration("¡Pedido aceptado!", "Lo tienes en «Mis repartos»")); onAccepted() } catch (e: ApiException) { err = e.message; orders = runCatching { Locator.repo.pendingOrders() }.getOrNull() }
                                    busyId = null
                                }
                            }, Modifier.weight(1.4f), loading = busyId == o.id, icon = Icons.Filled.TaskAlt)
                        }
                    }
                }
            }
        }
    }
    rejecting?.let { o ->
        val stale = hoursSince(o.createdAt) >= fees.staleHours
        AlertDialog(
            onDismissRequest = { rejecting = null },
            icon = { Icon(if (stale) Icons.Filled.Warning else Icons.Filled.Close, null) },
            title = { Text("¿Rechazar este pedido?") },
            text = { Text(if (stale && fees.stalePenalty > 0) "Lleva más de ${fees.staleHours} h esperando. Si lo rechazas se te aplicará una penalización de ${eur(fees.stalePenalty)} en el pago final."
                          else "Dejará de aparecerte en la lista y otro repartidor podrá aceptarlo.") },
            confirmButton = { TextButton({
                rejecting = null
                scope.launch {
                    try {
                        val pen = Locator.repo.rejectOrder(o.id)
                        orders = orders?.filterNot { it.id == o.id }
                        err = if (pen) "Pedido rechazado. Se ha aplicado una penalización de ${eur(fees.stalePenalty)}." else null
                    } catch (e: ApiException) { err = e.message }
                }
            }) { Text("Sí, rechazar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ rejecting = null }) { Text("No") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DriverMine() {
    var orders by remember { mutableStateOf<List<Order>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        while (true) {   // refresco automático (pagos del cliente, cambios de estado…)
            try { orders = Locator.repo.myDeliveries(); err = null } catch (e: ApiException) { err = e.message; orders = orders ?: emptyList() }
            delay(12_000)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { Text("Mis repartos", style = MaterialTheme.typography.headlineLarge) }
        item { ErrorText(err) }
        val list = orders
        when {
            list == null -> item { Loading() }
            list.isEmpty() -> item { Message(Icons.Filled.LocalShipping, "Sin repartos todavía", "Acepta un pedido en «Nuevos».") }
            else -> items(list, key = { it.id }) { o -> DeliveryCard(o, Modifier.animateItem()) { reload++ } }
        }
    }
}

@Composable
private fun DeliveryCard(o: Order, modifier: Modifier, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable(o.id) { mutableStateOf(!o.isFinal) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val checked = remember(o.id) { mutableStateMapOf<Long, Boolean>() }
    val next = when (o.status) { "aceptado" -> "comprando" to "Empiezo a comprar"; "comprando" -> "en_camino" to "Salgo a repartir"; "en_camino" -> "entregado" to "Pedido entregado"; else -> null }
    Card(modifier.fillMaxWidth().padding(vertical = 6.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.clickable { expanded = !expanded }.padding(18.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(o.customer?.fullName?.ifBlank { null } ?: "Pedido nº ${o.id}", style = MaterialTheme.typography.titleLarge)
                    Text(o.address, style = MaterialTheme.typography.bodyMedium)
                }
                StatusChip(o.status)
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Text(if (o.isPaid) "Ya pagado (${PayMethod.of(o.paymentMethod).label}) · ${eur(o.total)}" else "Cobrar: ${eur(o.total)} · ${PayMethod.of(o.paymentMethod).label}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                ScheduleBadge(o.scheduledDate, o.scheduledSlot)
                o.claimDeadline?.let { Text("⚠️ Pedido reclamado: entrégalo antes de las ${prettyDate(it).substringAfterLast(", ")}", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp)) }
                if (o.hasCold) Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("❄ Lleva refrigerados o congelados: usa bolsa isotérmica y no rompas la cadena de frío.", Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                o.notes?.let { Text("Nota: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) }
                Spacer(Modifier.height(6.dp))
                o.items.forEach { i ->
                    val c = checked[i.id] == true
                    Row(Modifier.fillMaxWidth().clickable(enabled = o.status == "comprando") { checked[i.id] = !c }, verticalAlignment = Alignment.CenterVertically) {
                        if (o.status == "comprando") Checkbox(c, { checked[i.id] = it })
                        Text("${i.qty} × ${i.name}", Modifier.weight(1f).padding(vertical = 6.dp), style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (c) TextDecoration.LineThrough else null)
                        Text(eur(i.unitPrice * i.qty), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                ErrorText(err)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    o.customer?.phone?.let { ph -> OutlinedButton({ ctx.dial(ph) }, Modifier.weight(1f)) { Icon(Icons.Filled.Call, null); Spacer(Modifier.width(6.dp)); Text("Llamar") } }
                    val openChat = LocalOpenChat.current
                    OutlinedButton({ openChat(o.id) }, Modifier.weight(1f)) { Icon(Icons.Filled.Forum, null); Spacer(Modifier.width(6.dp)); Text("Chat") }
                    OutlinedButton({ ctx.navigateTo(o.address) }, Modifier.weight(1f)) { Icon(Icons.Filled.Navigation, null); Spacer(Modifier.width(6.dp)); Text("Ruta") }
                }
                val celebrate = LocalCelebrate.current
                val locPerm = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) {
                    es.unocerobits.tlotraigo.DriverService.start(ctx)
                }
                if (o.status == "en_camino") Text("📍 Tu ubicación se comparte con el cliente hasta que marques «entregado».",
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (o.ticketTotal != null) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    PayBadge(o.paymentMethod); Spacer(Modifier.width(8.dp))
                    Text(when { o.isPaid -> "✓ Cobrado ${eur(o.amount)}"; o.paymentMethod == "bizum" && o.clientPaidAt != null -> "El cliente dice que ha enviado ${eur(o.amount)}"
                        o.paymentMethod == "bizum" -> "Esperando el Bizum de ${eur(o.amount)}"; else -> "Cobrar ${eur(o.amount)} en la entrega" },
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                if (o.ticketTotal != null && !o.isPaid && o.paymentMethod == "bizum" && o.bizumTo == "repartidor") TextButton({
                    scope.launch { runCatching { Locator.repo.markCollected(o.id) }; onChanged() } }) { Icon(Icons.Filled.DoneAll, null); Spacer(Modifier.width(6.dp)); Text("He recibido el Bizum") }
                if (o.status == "comprando" && o.ticketPath == null) TicketSection(o) { onChanged() }
                else if (o.ticketTotal != null) Text("🧾 Ticket guardado: ${eur(o.ticketTotal)} · total cliente ${eur(o.amount)}" + (if (o.freeService) "" else " · ganas ${eur(o.driverEarnings)}") +
                    (if (o.tipStatus == "pagada") " + propina ${eur(o.tipAmount)} 💛" else ""),
                    Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                next?.let { (st, label) ->
                    Spacer(Modifier.height(10.dp))
                    BigButton(label, {
                        scope.launch {
                            busy = true
                            try {
                                Locator.repo.setStatus(o.id, st)
                                when (st) { "entregado" -> celebrate(Celebration("¡Entregado!", "Gracias por hacer la compra a tu vecino")); "en_camino" -> celebrate(Celebration("¡En camino!", "El cliente ya puede ver por dónde vas")) }
                                if (st == "entregado" && o.paymentMethod == "tarjeta") Locator.repo.payout(o.id)
                                if (st == "en_camino") {
                                    if (es.unocerobits.tlotraigo.DriverService.hasLocation(ctx)) es.unocerobits.tlotraigo.DriverService.start(ctx)
                                    else locPerm.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                                onChanged()
                            } catch (e: ApiException) { err = e.message }
                            busy = false
                        }
                    }, loading = busy, icon = Icons.Filled.ArrowForward, enabled = st != "en_camino" || (o.ticketTotal != null && (o.amountConfirmed || o.isPaid)))
                    if (st == "en_camino" && o.ticketTotal != null && !(o.amountConfirmed || o.isPaid))
                        Text("⏳ Esperando a que el cliente pague o confirme el importe (${eur(o.amount)}). Si tarda, llámale.",
                            Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverSettingsPanel(vm: AppViewModel) {
    val towns by vm.towns.collectAsState()
    var ds by remember { mutableStateOf<DriverSettings?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { ds = Locator.repo.driverSettings() } }
    fun save(n: DriverSettings) { ds = n; scope.launch { try { Locator.repo.saveDriverSettings(n); err = null } catch (e: ApiException) { err = e.message } } }
    val s = ds ?: return
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionTitle("Cobros que aceptas")
        Text("Los clientes solo verán los métodos que aceptan los repartidores disponibles en su pueblo.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            Triple(PayMethod.CASH, s.cash) { v: Boolean -> s.copy(cash = v) },
            Triple(PayMethod.BIZUM, s.bizum) { v: Boolean -> s.copy(bizum = v) },
            Triple(PayMethod.CARD, s.card) { v: Boolean -> s.copy(card = v) },
        ).forEach { (m, on, upd) ->
            Row(Modifier.fillMaxWidth().clickable { save(upd(!on)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(m.label, style = MaterialTheme.typography.titleMedium); Text(m.hint, style = MaterialTheme.typography.bodySmall) }
                AppSwitch(on, { save(upd(it)) })
            }
        }
        SectionTitle("Tu Bizum (opcional)")
        Text("Si tu entidad lo permite, los clientes te devolverán por Bizum lo que adelantes en el súper.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        var bz by remember(s.bizumPhone) { mutableStateOf(s.bizumPhone.orEmpty()) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(bz, { bz = it.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16) }, Modifier.weight(1f), label = { Text("Teléfono con Bizum") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone), leadingIcon = { Icon(Icons.Filled.Smartphone, null) })
            Spacer(Modifier.width(8.dp))
            Button({ save(s.copy(bizumPhone = bz)) }) { Text("Guardar") }
        }
        SectionTitle("Cobros con tarjeta")
        val ctxP = androidx.compose.ui.platform.LocalContext.current
        Text("Para recibir tu parte de los pedidos pagados con tarjeta, conecta tu cuenta de cobro (Stripe). Es gratis y la verificación la hace Stripe.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        var stripeMsg by remember { mutableStateOf<String?>(null) }
        OutlinedButton({ scope.launch { try { val u = Locator.repo.connectStripe(); if (u != null) ctxP.openUrl(u) else stripeMsg = "Tu cuenta de cobro ya está lista ✓" } catch (e: ApiException) { stripeMsg = e.message } } }) {
            Icon(Icons.Filled.AccountBalance, null); Spacer(Modifier.width(6.dp)); Text("Configurar cobros")
        }
        stripeMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        SectionTitle("Pueblos donde repartes")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            towns.forEach { t ->
                val sel = t.id in s.townIds
                FilterChip(sel, { save(s.copy(townIds = if (sel) s.townIds - t.id else s.townIds + t.id)) }, { Text(t.name) },
                    leadingIcon = if (sel) { { Icon(Icons.Filled.Check, null) } } else null)
            }
        }
        ErrorText(err)
    }
}

@Composable
private fun DriverEarningsScreen() {
    var e by remember { mutableStateOf<Earnings?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { try { e = Locator.repo.earnings() } catch (x: ApiException) { err = x.message } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { Text("Mis ganancias", style = MaterialTheme.typography.headlineLarge) }
        item { ErrorText(err) }
        val x = e
        if (x == null) { if (err == null) item { Loading() } }
        else {
            item {
                Text("Desde el ${prettyDay(x.from)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(Modifier.fillMaxWidth().padding(vertical = 12.dp), shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("A cobrar", style = MaterialTheme.typography.titleMedium)
                        Text(eur(x.net), style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        EarnRow("${x.delivered} pedidos entregados", x.gross)
                        if (x.tips > 0) EarnRow("Propinas 💛", x.tips)
                        if (x.penalties > 0) EarnRow("Penalizaciones", -x.penalties)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stars(x.rating ?: 0.0)
                    Spacer(Modifier.width(8.dp))
                    Text(if (x.reviews == 0) "Sin valoraciones todavía" else "${x.rating} de 5 · ${x.reviews} opiniones", style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (x.penaltyList.isNotEmpty()) {
                item { SectionTitle("Penalizaciones") }
                items(x.penaltyList, key = { it.orderId }) { p ->
                    ListItem(headlineContent = { Text("Pedido nº ${p.orderId} · −${eur(p.amount)}") },
                        supportingContent = { Text("${p.reason}\n${prettyDate(p.createdAt)}") },
                        leadingContent = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
            item {
                Text("Eres independiente: aceptas los pedidos que quieras. Los pagos con tarjeta te llegan a tu cuenta de Stripe; el efectivo lo cobras en la entrega.",
                    Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EarnRow(label: String, v: Double) = Row(Modifier.fillMaxWidth()) {
    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    Text((if (v < 0) "−" else "") + eur(kotlin.math.abs(v)), style = MaterialTheme.typography.bodyLarge)
}

/** Candidatura para ser repartidor (solo cuentas registradas como repartidor). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriverApplicationSection(vm: AppViewModel) {
    val towns by vm.towns.collectAsState()
    val scope = rememberCoroutineScope()
    var app by remember { mutableStateOf<DriverApplication?>(null) }
    var approved by remember { mutableStateOf<Boolean?>(null) }
    var editing by remember { mutableStateOf(false) }
    var vehicle by remember { mutableStateOf("Coche") }
    val sel = remember { mutableStateListOf<Long>() }
    var availability by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var dAdult by remember { mutableStateOf(false) }
    var dRETA by remember { mutableStateOf(false) }
    var dSeguro by remember { mutableStateOf(false) }
    var dAlimentos by remember { mutableStateOf(false) }
    var dTerms by remember { mutableStateOf(false) }
    var dGeo by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        runCatching { approved = Locator.repo.driverSettings()?.approved }
        runCatching {
            app = Locator.repo.myApplication()
            app?.let { a -> vehicle = a.vehicle; sel.clear(); sel.addAll(a.townIds); availability = a.availability.orEmpty()
                experience = a.experience.orEmpty(); message = a.message.orEmpty() }
        }
    }
    SectionTitle("Candidatura de repartidor")
    if (approved == true) {
        Text("Estás autorizado para repartir. ¡Gracias por ayudar a tus vecinos!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        return
    }
    val a = app
    if (a != null && !editing) {
        val (txt, color) = when (a.status) {
            "pendiente" -> "Enviada. El responsable la está revisando." to MaterialTheme.colorScheme.secondary
            "rechazada" -> "No aprobada${a.reviewNote?.let { ": $it" } ?: "."} Puedes corregirla y enviarla de nuevo." to MaterialTheme.colorScheme.error
            else -> "Aprobada." to MaterialTheme.colorScheme.primary
        }
        Text(txt, style = MaterialTheme.typography.bodyLarge, color = color)
        Text("Vehículo: ${a.vehicle} · ${a.townIds.size} pueblos", style = MaterialTheme.typography.bodyMedium)
        TextButton({ editing = true }) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("Modificar candidatura") }
        return
    }
    Text("Cuéntanos cómo puedes repartir. El responsable revisará tu solicitud.", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Vehículo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Coche", "Furgoneta", "Moto", "Bicicleta", "A pie").forEach { v -> FilterChip(vehicle == v, { vehicle = v }, { Text(v) }) }
    }
    Text("Pueblos donde puedes repartir", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        towns.forEach { t -> val on = t.id in sel
            FilterChip(on, { if (on) sel.remove(t.id) else sel.add(t.id) }, { Text(t.name) }, leadingIcon = if (on) { { Icon(Icons.Filled.Check, null) } } else null) }
    }
    OutlinedTextField(availability, { availability = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Disponibilidad (días y horas)") },
        placeholder = { Text("Ej.: lunes a viernes por la mañana") }, shape = MaterialTheme.shapes.medium)
    OutlinedTextField(experience, { experience = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Experiencia (opcional)") },
        minLines = 2, shape = MaterialTheme.shapes.medium)
    OutlinedTextField(message, { message = it }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Mensaje para el responsable (opcional)") },
        minLines = 2, shape = MaterialTheme.shapes.medium)
    Text("Declaraciones", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
    @Composable fun Decl(v: Boolean, set: (Boolean) -> Unit, t: String) = Row(Modifier.fillMaxWidth().clickable { set(!v) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(v, set); Text(t, style = MaterialTheme.typography.bodyMedium)
    }
    Decl(dAdult, { dAdult = it }, "Soy mayor de 18 años y tengo DNI o NIE en vigor. *")
    Decl(dRETA, { dRETA = it }, "Estoy dado de alta como autónomo (RETA) o me daré de alta antes de empezar a repartir.")
    Decl(dSeguro, { dSeguro = it }, "Mi vehículo tiene seguro en vigor y, si es necesario, carné de conducir.")
    Decl(dAlimentos, { dAlimentos = it }, "Transportaré los alimentos limpios y separados, y los refrigerados o congelados sin romper la cadena de frío.")
    Decl(dTerms, { dTerms = it }, "He leído y acepto las condiciones para repartidores. *")
    TextButton({ ctx.openUrl(es.unocerobits.tlotraigo.BuildConfig.WEB_URL.trimEnd('/') + "/repartidores.html") }) { Text("Leer las condiciones para repartidores") }
    Decl(dGeo, { dGeo = it }, "Acepto que mi ubicación se comparta con el cliente solo mientras llevo su pedido (ruta «en camino»). *")
    Text("* Obligatorio. El alta como autónomo y el seguro son tu responsabilidad; consulta a una gestoría si tienes dudas.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ErrorText(err)
    Spacer(Modifier.height(10.dp))
    BigButton("Enviar candidatura", {
        scope.launch {
            busy = true; err = null
            try { Locator.repo.submitApplication(vehicle, sel.toList(), availability, experience, message, dAdult, dRETA, dSeguro, dAlimentos, dTerms, dGeo); editing = false; reload++ }
            catch (e: ApiException) { err = e.message }
            busy = false
        }
    }, enabled = sel.isNotEmpty() && dAdult && dTerms && dGeo, loading = busy, icon = Icons.AutoMirrored.Filled.Send)
}
