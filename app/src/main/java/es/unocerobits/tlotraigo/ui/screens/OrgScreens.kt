package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

/** Colores de marca disponibles para las entidades (contraste suficiente con texto blanco). */
val brandColors = listOf("#2E6B57", "#1F5F8B", "#7A3E9D", "#B23A48", "#C4661F", "#3C5A14", "#37474F", "#00796B")
fun brand(hex: String) = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(androidx.compose.ui.graphics.Color(0xFF2E6B57))

/** ¿El usuario actual es superadministrador? (para mostrar los ajustes de suscripción) */
val LocalIsSuper = staticCompositionLocalOf { false }

/** Abre el panel de una entidad (superadministrador). */
val LocalOpenOrg = staticCompositionLocalOf<(Long) -> Unit> { {} }

private val kinds = listOf("ayuntamiento" to "Ayuntamiento", "diputacion" to "Diputación / Junta", "asociacion" to "Asociación", "tienda" to "Tienda", "empresa" to "Empresa", "otra" to "Otra")
private val roleNames = mapOf("gestor" to "Administrador/a", "repartidor" to "Repartidor/a", "cliente" to "Usuario/a")
private val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")

fun inviteText(org: String, code: String, role: String) =
    "Te invito a ${org} en T lo traigo como ${roleNames[role]?.lowercase()}.\n1. Descarga la app: https://tlotraigo.unocerobits.com\n2. Crea tu cuenta.\n" +
    "3. En Ajustes → «Tengo un código», escribe: $code\nO abre: https://tlotraigo.unocerobits.com/unirse.html?c=$code"

// =================== Canjear un código de invitación ===================
@Composable
fun RedeemInviteDialog(vm: AppViewModel, initial: String = "", onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(initial.uppercase()) }
    var err by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Filled.VpnKey, null) }, title = { Text("Código de invitación") },
        text = { Column {
            Text("Si una entidad (ayuntamiento, asociación, tienda…) te ha dado un código para administrar, repartir o usar el servicio, escríbelo aquí.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(code, { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) }, Modifier.fillMaxWidth().padding(top = 10.dp),
                label = { Text("Código (8 letras o números)") }, singleLine = true, textStyle = MaterialTheme.typography.headlineSmall)
            ErrorText(err); ok?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium) }
        } },
        confirmButton = { if (ok == null) Button({ scope.launch { try { ok = Locator.repo.redeemInvite(code); vm.refreshProfile() } catch (e: ApiException) { err = e.message } } },
            enabled = code.length == 8) { Text("Unirme") } else Button({ onDismiss(); vm.start() }) { Text("Empezar") } },
        dismissButton = { if (ok == null) TextButton(onDismiss) { Text("Cancelar") } })
}

// =================== Superadministrador: entidades ===================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminOrgs(vm: AppViewModel, onOpen: (Long) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val towns by vm.towns.collectAsState()
    var list by remember { mutableStateOf<List<Organization>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var created by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(reload) { list = runCatching { Locator.repo.organizations() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Cada entidad (ayuntamiento, asociación, tienda…) tiene su administrador, sus pueblos y sus repartidores. Créala y envía el código a su responsable.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp)); BigButton("Nueva entidad", { creating = true }, icon = Icons.Filled.AddBusiness)
            created?.let { (name, code) ->
                Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("✓ $name creada", style = MaterialTheme.typography.titleMedium)
                        Text("Código para su administrador/a:", style = MaterialTheme.typography.bodyMedium)
                        Text(code, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                        Button({ ctx.share(inviteText(name, code, "gestor")) }) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(6.dp)); Text("Enviar invitación") }
                    }
                }
            }
        }
        val l = list
        if (l == null) item { Loading() } else items(l, key = { it.id }) { o ->
            ListItem(headlineContent = { Text(o.name) }, supportingContent = { Text("${kinds.toMap()[o.kind]} · plan ${o.plan}${o.subscriptionUntil?.let { " hasta ${prettyDay(it)}" } ?: ""}") },
                leadingContent = { Icon(if (o.kind == "ayuntamiento" || o.kind == "diputacion") Icons.Filled.AccountBalance else Icons.Filled.Groups, null) },
                trailingContent = { Switch(o.active, { v -> scope.launch { runCatching { Locator.repo.updateOrganization(o.id, buildJsonObject { put("active", v) }) }; reload++ } }) },
                modifier = Modifier.clickable { onOpen(o.id) })
        }
    }
    if (creating) {
        var name by remember { mutableStateOf("") }; var kind by remember { mutableStateOf("ayuntamiento") }
        var cn by remember { mutableStateOf("") }; var ce by remember { mutableStateOf("") }; var cp by remember { mutableStateOf("") }
        val sel = remember { mutableStateListOf<Long>() }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { creating = false }, title = { Text("Nueva entidad") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre (ej.: Ayuntamiento de Valderas)") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) { kinds.forEach { (k, l) -> FilterChip(kind == k, { kind = k }, { Text(l) }) } }
                OutlinedTextField(cn, { cn = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Persona de contacto") }, singleLine = true)
                OutlinedTextField(ce, { ce = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Correo") }, singleLine = true)
                OutlinedTextField(cp, { cp = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Teléfono") }, singleLine = true)
                Text("Pueblos que gestiona (puede añadir más después)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { towns.forEach { t -> FilterChip(t.id in sel, { if (t.id in sel) sel.remove(t.id) else sel.add(t.id) }, { Text(t.name) }) } }
                ErrorText(err)
            } },
            confirmButton = { Button({ scope.launch { try { val (_, code) = Locator.repo.createOrganization(name, kind, cn, ce, cp, sel.toList()); created = name to code; creating = false; reload++ } catch (e: ApiException) { err = e.message } } },
                enabled = name.trim().length >= 2) { Text("Crear") } },
            dismissButton = { TextButton({ creating = false }) { Text("Cancelar") } })
    }
}

// =================== Panel de la entidad (administrador de localidad) ===================
private enum class OTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    RESUMEN("Resumen", Icons.Filled.Dashboard), PEDIDOS("Pedidos", Icons.Filled.ReceiptLong), REPARTIDORES("Repartidores", Icons.Filled.DeliveryDining),
    ZONAS("Pueblos", Icons.Filled.Map), RECLAMACIONES("Reclamaciones", Icons.Filled.Feedback), INVITAR("Invitar", Icons.Filled.PersonAdd), INFORME("Informe", Icons.Filled.Assessment), AJUSTES("Ajustes", Icons.Filled.Tune)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgPanel(vm: AppViewModel, orgId: Long, onBack: (() -> Unit)? = null, onOrder: (Long) -> Unit = {}) {
    var org by remember { mutableStateOf<Organization?>(null) }
    var ov by remember { mutableStateOf<OrgOverview?>(null) }
    var tab by remember { mutableStateOf(OTab.RESUMEN) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(orgId, reload) { while (true) { org = runCatching { Locator.repo.organization(orgId) }.getOrNull(); ov = runCatching { Locator.repo.orgOverview(orgId) }.getOrNull(); delay(30_000) } }
    Column(Modifier.fillMaxSize()) {
        TopAppBar({ Text(org?.name ?: "Mi entidad", maxLines = 1) }, navigationIcon = { onBack?.let { IconButton(it) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } } },
            windowInsets = if (onBack == null) WindowInsets(0) else TopAppBarDefaults.windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = org?.brandColor?.let { brand(it) } ?: MaterialTheme.colorScheme.surface,
                titleContentColor = if (org?.brandColor != null) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = if (org?.brandColor != null) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface))
        ScrollableTabRow(tab.ordinal, edgePadding = 8.dp) { OTab.entries.forEach { t -> Tab(tab == t, { tab = t }, text = { Text(t.label) }, icon = { Icon(t.icon, null) }) } }
        val o = org; val v = ov
        if (o == null || v == null) { Loading(); return@Column }
        if (!o.active) Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
            Text("Suscripción inactiva: los clientes no pueden hacer pedidos. Contacta con T lo traigo.", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
        when (tab) {
            OTab.RESUMEN -> OrgSummary(o, v) { reload++ }
            OTab.PEDIDOS -> OrgOrders(orgId, onOrder)
            OTab.REPARTIDORES -> OrgDrivers(o, v) { reload++ }
            OTab.ZONAS -> OrgTowns(o, v) { reload++ }
            OTab.INVITAR -> OrgInvites(o)
            OTab.INFORME -> OrgReportTab(o)
            OTab.RECLAMACIONES -> OrgComplaints()
            OTab.AJUSTES -> OrgSettings(o) { reload++ }
        }
    }
}

@Composable
private fun OrgSummary(o: Organization, v: OrgOverview, onChange: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(if (o.acceptingOrders) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (o.acceptingOrders) "Aceptando pedidos" else "Pedidos pausados", style = MaterialTheme.typography.titleLarge)
                    Text("Horario de entrega: ${o.openHour}:00–${o.closeHour}:00 · ${o.deliveryDays.sorted().joinToString(" ") { dayNames[it - 1] }}", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(o.acceptingOrders, { a -> scope.launch { runCatching { Locator.repo.updateOrganization(o.id, buildJsonObject { put("accepting_orders", a) }) }; onChange() } })
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Hoy" to v.today, "En curso" to v.active, "Entregados (mes)" to v.monthDelivered).forEach { (l, n) ->
                Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("$n", style = MaterialTheme.typography.headlineMedium); Text(l, style = MaterialTheme.typography.labelMedium) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Usuarios" to v.clients, "Repartidores" to v.drivers.count { it.approved }, "Disponibles ahora" to v.drivers.count { it.available }).forEach { (l, n) ->
                Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("$n", style = MaterialTheme.typography.headlineMedium); Text(l, style = MaterialTheme.typography.labelMedium) } }
            }
        }
        if (v.claims > 0) Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
            Text("⚠️ ${v.claims} pedido(s) reclamado(s) sin entregar: revisa la pestaña Pedidos.", Modifier.padding(14.dp)) }
        SectionTitle("Pueblos que atiende"); Text(v.towns.filter { it.active }.joinToString(", ") { it.name }.ifBlank { "Ninguno todavía: añádelos en «Pueblos»." })
        SectionTitle("Administradores"); v.gestores.forEach { Text("• ${it.name}${it.phone?.let { p -> " · $p" } ?: ""}") }
    }
}

@Composable
private fun OrgOrders(orgId: Long, onOrder: (Long) -> Unit) {
    var list by remember { mutableStateOf<List<Order>?>(null) }
    var filter by remember { mutableStateOf("activos") }
    LaunchedEffect(orgId) { while (true) { list = runCatching { Locator.repo.orgOrders(orgId) }.getOrDefault(list ?: emptyList()); delay(15_000) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("activos" to "En curso", "reclamados" to "Reclamados", "todos" to "Todos").forEach { (k, l) -> FilterChip(filter == k, { filter = k }, { Text(l) }) } } }
        val l = list?.filter { when (filter) { "activos" -> !it.isFinal; "reclamados" -> it.claimedAt != null && !it.isFinal; else -> true } }
        if (l == null) item { Loading() } else if (l.isEmpty()) item { Message(Icons.Filled.Inbox, "No hay pedidos aquí") }
        else items(l, key = { it.id }) { o ->
            ListItem(headlineContent = { Text("Nº ${o.id} · ${o.customer?.fullName ?: ""}") },
                supportingContent = { Text("${OrderStatus.of(o.status).label} · ${productos(o.items.sumOf { it.qty })} · ${prettyDate(o.createdAt)}" +
                    (o.driver?.fullName?.let { "\nRepartidor: $it" } ?: "\nSin repartidor") + (o.claimDeadline?.let { "\n⚠️ Reclamado: plazo ${prettyDate(it)}" } ?: "")) },
                leadingContent = { Icon(AppIcons.vector("cesta"), null) }, modifier = Modifier.clickable { onOrder(o.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun OrgDrivers(o: Organization, v: OrgOverview, onChange: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            BigButton("Invitar repartidores", { scope.launch { code = runCatching { Locator.repo.createInvite(o.id, "repartidor", 20, 30, "Repartidores") }.getOrNull() } }, icon = Icons.Filled.PersonAdd)
            code?.let { c -> Card(Modifier.fillMaxWidth().padding(top = 10.dp)) { Column(Modifier.padding(14.dp)) {
                Text("Código para repartidores (hasta 20 personas, 30 días):"); Text(c, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                TextButton({ ctx.share(inviteText(o.name, c, "repartidor")) }) { Text("Enviar por WhatsApp o correo") } } } }
            Text(if (o.autoApproveDrivers) "Los que usen el código quedan autorizados al momento (puedes cambiarlo en Ajustes)." else "Tendrás que autorizar a cada repartidor.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (v.drivers.isEmpty()) item { Message(Icons.Filled.DeliveryDining, "Aún no hay repartidores", "Envía el código a los vecinos o al personal que vaya a repartir.") }
        items(v.drivers, key = { it.id }) { d ->
            ListItem(headlineContent = { Text(d.name) }, supportingContent = { Text((if (d.available) "🟢 Disponible" else "⚪ No disponible") + (d.phone?.let { " · $it" } ?: "")) },
                trailingContent = { Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(d.approved, { a -> scope.launch { runCatching { Locator.repo.orgSetDriver(d.id, a) }; onChange() } })
                    IconButton({ scope.launch { runCatching { Locator.repo.orgSetDriver(d.id, false, remove = true) }; onChange() } }) { Icon(Icons.Filled.PersonRemove, "Quitar de la entidad") }
                } })
        }
    }
}

@Composable
private fun OrgTowns(o: Organization, v: OrgOverview, onChange: () -> Unit) {
    val scope = rememberCoroutineScope()
    var edit by remember { mutableStateOf<Town?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { BigButton("Añadir pueblo", { edit = Town(0, "", 1.5) }, icon = Icons.Filled.AddLocationAlt)
            Text("Solo los pueblos activos aparecen al registrarse. El envío es lo que paga el cliente (pon 0 si el servicio es gratuito).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
        items(v.towns, key = { it.id }) { t ->
            ListItem(headlineContent = { Text(t.name + if (!t.active) " (desactivado)" else "") },
                supportingContent = { Text("CP ${t.postalCode ?: "—"} · envío ${eur(t.deliveryFee)} · ${fmtNum(t.distanceKm, 0)} km al súper") },
                trailingContent = { Switch(t.active, { a -> scope.launch { runCatching { Locator.repo.orgSaveTown(o.id, t.id, t.name, t.postalCode, t.deliveryFee, t.distanceKm, a) }; onChange() } }) },
                modifier = Modifier.clickable { edit = t })
        }
    }
    edit?.let { t ->
        var name by remember(t) { mutableStateOf(t.name) }; var cp by remember(t) { mutableStateOf(t.postalCode.orEmpty()) }
        var fee by remember(t) { mutableStateOf(fmtNum(t.deliveryFee)) }; var km by remember(t) { mutableStateOf(if (t.id == 0L) "" else fmtNum(t.distanceKm, 0)) }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { edit = null }, title = { Text(if (t.id == 0L) "Nuevo pueblo" else t.name) },
            text = { Column {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Pueblo") }, singleLine = true)
                OutlinedTextField(cp, { cp = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Código postal") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    NumField("Envío", fee, "€", Modifier.weight(1f)) { fee = it }; NumField("Distancia al súper", km, "km", Modifier.weight(1f)) { km = it }
                }
                ErrorText(err)
            } },
            confirmButton = { Button({ scope.launch { try { Locator.repo.orgSaveTown(o.id, t.id.takeIf { it != 0L }, name, cp, parseNum(fee) ?: 0.0, parseNum(km) ?: 0.0); edit = null; onChange() }
                catch (e: ApiException) { err = e.message } } }, enabled = name.isNotBlank()) { Text("Guardar") } },
            dismissButton = { TextButton({ edit = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun OrgInvites(o: Organization) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<OrgInvite>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    var role by remember { mutableStateOf("cliente") }
    var uses by remember { mutableStateOf("50") }
    LaunchedEffect(reload) { list = runCatching { Locator.repo.invites(o.id) }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Crea códigos para que se unan a tu entidad. Los usuarios también pueden registrarse eligiendo uno de tus pueblos.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) { roleNames.forEach { (k, l) -> FilterChip(role == k, { role = k; uses = if (k == "gestor") "1" else "50" }, { Text(l) }) } }
            NumField("Número de personas que pueden usarlo", uses, "", Modifier.fillMaxWidth()) { uses = it.filter(Char::isDigit).take(3) }
            Spacer(Modifier.height(8.dp))
            BigButton("Crear código", { scope.launch { runCatching { val c = Locator.repo.createInvite(o.id, role, uses.toIntOrNull() ?: 1, 30, null); ctx.share(inviteText(o.name, c, role)) }; reload++ } }, icon = Icons.Filled.QrCode)
            SectionTitle("Códigos creados")
        }
        items(list, key = { it.code }) { i ->
            ListItem(headlineContent = { Text(i.code, style = MaterialTheme.typography.titleLarge) },
                supportingContent = { Text("${roleNames[i.role]} · usado ${i.uses}/${i.maxUses} · caduca ${prettyDate(i.expiresAt)}") },
                trailingContent = { IconButton({ ctx.share(inviteText(o.name, i.code, i.role)) }) { Icon(Icons.Filled.Share, "Compartir") } })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrgSettings(o: Organization, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val isSuper = LocalIsSuper.current
    var x by remember(o) { mutableStateOf(o) }
    var msg by remember { mutableStateOf<String?>(null) }
    @Composable fun sw(t: String, d: String, v: Boolean, set: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(t, style = MaterialTheme.typography.titleMedium); Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(v, set)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedTextField(x.name, { x = x.copy(name = it) }, Modifier.fillMaxWidth(), label = { Text("Nombre de la entidad") }, singleLine = true)
        OutlinedTextField(x.publicPhone.orEmpty(), { x = x.copy(publicPhone = it) }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Teléfono de atención (lo ven los usuarios)") }, singleLine = true)
        OutlinedTextField(x.welcomeMessage.orEmpty(), { x = x.copy(welcomeMessage = it.take(300)) }, Modifier.fillMaxWidth().padding(top = 6.dp),
            label = { Text("Mensaje para los usuarios") }, placeholder = { Text("Ej.: Servicio municipal gratuito para mayores de 65 años") }, minLines = 2)
        SectionTitle("Imagen de la entidad")
        Text("Color y logo que verán tus usuarios en la pantalla de compra.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            (listOf(null) + brandColors).forEach { c ->
                Surface(Modifier.size(40.dp).clickable { x = x.copy(brandColor = c) }, shape = androidx.compose.foundation.shape.CircleShape,
                    color = c?.let { brand(it) } ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = if (x.brandColor == c) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null) {
                    if (c == null) Box(contentAlignment = Alignment.Center) { Text("—") }
                }
            }
        }
        OutlinedTextField(x.logoUrl.orEmpty(), { x = x.copy(logoUrl = it.trim()) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Enlace del logo (https://…, opcional)") }, singleLine = true)
        x.logoUrl?.takeIf { it.startsWith("https://") }?.let { coil.compose.AsyncImage(it, "Logo", Modifier.padding(top = 6.dp).size(64.dp)) }
        SectionTitle("Pedidos")
        sw("Aceptar pedidos", "Desactívalo para pausar el servicio (vacaciones, festivos…)", x.acceptingOrders) { x = x.copy(acceptingOrders = it) }
        NumField("Máximo de artículos por pedido", x.maxItems.toString(), "uds", Modifier.fillMaxWidth()) { x = x.copy(maxItems = it.filter(Char::isDigit).toIntOrNull() ?: x.maxItems) }
        SectionTitle("Horario y días de reparto")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Desde", x.openHour.toString(), "h", Modifier.weight(1f)) { x = x.copy(openHour = (it.filter(Char::isDigit).toIntOrNull() ?: 9).coerceIn(0, 23)) }
            NumField("Hasta", x.closeHour.toString(), "h", Modifier.weight(1f)) { x = x.copy(closeHour = (it.filter(Char::isDigit).toIntOrNull() ?: 22).coerceIn(1, 24)) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            dayNames.forEachIndexed { i, d -> val n = i + 1; FilterChip(n in x.deliveryDays, { x = x.copy(deliveryDays = if (n in x.deliveryDays) x.deliveryDays - n else x.deliveryDays + n) }, { Text(d) }) }
        }
        SectionTitle("Formas de pago para los usuarios")
        sw("Efectivo", "El usuario paga el importe del ticket al repartidor al recibir la compra", x.allowCash) { x = x.copy(allowCash = it) }
        sw("Bizum al repartidor", "El usuario devuelve por Bizum al repartidor lo que adelantó (el repartidor pone su número en Ajustes)", x.allowBizumDriver) { x = x.copy(allowBizumDriver = it) }
        sw("Bizum a la entidad", "El usuario paga por Bizum a la entidad (necesita Bizum para empresas de su banco)", x.allowBizumOrg) { x = x.copy(allowBizumOrg = it) }
        if (x.allowBizumOrg) {
            OutlinedTextField(x.bizumOrgPhone.orEmpty(), { x = x.copy(bizumOrgPhone = it.filter { c -> c.isDigit() || c == '+' }.take(15)) }, Modifier.fillMaxWidth(),
                label = { Text("Número de Bizum de la entidad") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            OutlinedTextField(x.bizumOrgName.orEmpty(), { x = x.copy(bizumOrgName = it) }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Nombre que verá el usuario") }, singleLine = true)
        }
        if (!x.allowCash && !x.allowBizumDriver && !(x.allowBizumOrg && !x.bizumOrgPhone.isNullOrBlank()) && x.productsPayment != "entidad")
            Text("⚠️ Activa al menos una forma de pago o los usuarios no podrán pedir.", color = MaterialTheme.colorScheme.error)
        SectionTitle("Quién paga la compra del súper")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(x.productsPayment == "efectivo", { x = x.copy(productsPayment = "efectivo") }, { Text("El usuario paga el ticket al repartidor") })
            FilterChip(x.productsPayment == "entidad", { x = x.copy(productsPayment = "entidad") }, { Text("La entidad se encarga del pago") })
        }
        Text(if (x.feesMode == "gratuito") "Servicio sin tarifas para el usuario (incluido en la suscripción de la entidad)." else "El usuario paga las tarifas del servicio.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle("Repartidores")
        sw("Autorizar automáticamente", "Quien use tu código de repartidor queda autorizado al momento", x.autoApproveDrivers) { x = x.copy(autoApproveDrivers = it) }
        Spacer(Modifier.height(16.dp))
        BigButton("Guardar ajustes", {
            scope.launch {
                msg = try {
                    Locator.repo.updateOrganization(o.id, buildJsonObject {
                        put("name", x.name); put("public_phone", x.publicPhone.orEmpty()); put("welcome_message", x.welcomeMessage.orEmpty())
                        put("accepting_orders", x.acceptingOrders); put("allow_cash", x.allowCash); put("show_prices", x.showPrices)
                        put("max_items", x.maxItems); put("products_payment", x.productsPayment)
                        put("brand_color", x.brandColor.orEmpty()); put("logo_url", x.logoUrl?.takeIf { it.startsWith("https://") }.orEmpty())
                        put("allow_bizum_driver", x.allowBizumDriver); put("allow_bizum_org", x.allowBizumOrg)
                        put("bizum_org_phone", x.bizumOrgPhone.orEmpty()); put("bizum_org_name", x.bizumOrgName.orEmpty())
                        if (isSuper) { put("fees_mode", x.feesMode); put("price_month", x.priceMonth); put("plan", x.plan); put("billing_email", x.billingEmail.orEmpty())
                            put("subscription_until", x.subscriptionUntil.orEmpty()); Unit }
                        put("open_hour", x.openHour); put("close_hour", x.closeHour); put("auto_approve_drivers", x.autoApproveDrivers)
                        putJsonArray("delivery_days") { x.deliveryDays.sorted().forEach { add(it) } }
                    }); onSaved(); "Guardado ✓"
                } catch (e: ApiException) { e.message }
            }
        }, icon = Icons.Filled.Save)
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (isSuper) {
            SectionTitle("Suscripción (solo superadministrador)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(x.feesMode == "gratuito", { x = x.copy(feesMode = "gratuito") }, { Text("Gratis para el usuario") })
                FilterChip(x.feesMode == "tarifas", { x = x.copy(feesMode = "tarifas") }, { Text("El usuario paga tarifas") })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("piloto", "basico", "municipal", "comarcal").forEach { pl -> FilterChip(x.plan == pl, { x = x.copy(plan = pl) }, { Text(pl.replaceFirstChar { it.uppercase() }) }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumField("Cuota mensual", fmtNum(x.priceMonth), "€", Modifier.weight(1f)) { x = x.copy(priceMonth = parseNum(it) ?: x.priceMonth) }
                OutlinedTextField(x.subscriptionUntil.orEmpty(), { x = x.copy(subscriptionUntil = it.take(10)) }, Modifier.weight(1f), label = { Text("Pagada hasta (AAAA-MM-DD)") }, singleLine = true)
            }
            OutlinedTextField(x.billingEmail.orEmpty(), { x = x.copy(billingEmail = it) }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Correo de facturación") }, singleLine = true)
            Text("Se desactiva sola 7 días después de la fecha de pago si no se renueva. Recuerda pulsar «Guardar ajustes».", style = MaterialTheme.typography.bodySmall)
        }
        SectionTitle("Suscripción"); Text("Plan: ${o.plan}${o.subscriptionUntil?.let { " · hasta ${prettyDay(it)}" } ?: ""} · ${if (o.active) "activa" else "inactiva"}")
        Text("Para cambiar de plan escribe a info@unocerobits.com", style = MaterialTheme.typography.bodySmall)
    }
}

// =================== Informe mensual ===================
@Composable
private fun OrgReportTab(o: Organization) {
    val ctx = LocalContext.current
    var month by remember { mutableStateOf(java.time.LocalDate.now().withDayOfMonth(1)) }
    var r by remember { mutableStateOf<OrgReport?>(null) }
    LaunchedEffect(month) { r = null; r = runCatching { Locator.repo.orgReport(o.id, month) }.getOrNull() }
    val label = month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale("es", "ES"))).replaceFirstChar { it.uppercase() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, "Mes anterior") }
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton({ month = month.plusMonths(1) }, enabled = month < java.time.LocalDate.now().withDayOfMonth(1)) { Icon(Icons.Filled.ChevronRight, "Mes siguiente") }
        }
        val x = r ?: return@Column Loading()
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Pedidos" to "${x.total}", "Entregados" to "${x.delivered}", "Usuarios" to "${x.users}").forEach { (l, v) -> Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text(v, style = MaterialTheme.typography.headlineMedium); Text(l, style = MaterialTheme.typography.labelMedium) } } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Cancelados" to "${x.cancelled}", "Reclamados" to "${x.claims}", "Tiempo medio" to (x.avgHours?.let { "${fmtNum(it, 1)} h" } ?: "—")).forEach { (l, v) -> Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text(v, style = MaterialTheme.typography.headlineMedium); Text(l, style = MaterialTheme.typography.labelMedium) } } }
        }
        Text("Compras entregadas (tickets): ${eur(x.tickets)}", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleMedium)
        SectionTitle("Cobros")
        Row(verticalAlignment = Alignment.CenterVertically) { PayBadge("efectivo"); Spacer(Modifier.width(8.dp)); Text(eur(x.cash), style = MaterialTheme.typography.titleMedium) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) { PayBadge("bizum"); Spacer(Modifier.width(8.dp)); Text(eur(x.bizum), style = MaterialTheme.typography.titleMedium) }
        if (x.pending > 0) Text("⚠️ Pendiente de cobro (pedidos entregados): ${eur(x.pending)}", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.error)
        SectionTitle("Por pueblo"); x.byTown.forEach { Text("• ${it.town}: ${it.n} pedidos (${it.delivered} entregados)") }
        SectionTitle("Por repartidor"); x.byDriver.forEach { Text("• ${it.driver}: ${it.n} entregas") }
        Spacer(Modifier.height(16.dp))
        BigButton("Descargar informe (PDF)", { ReportExport.pdf(ctx, x, label) }, icon = Icons.Filled.PictureAsPdf)
        Spacer(Modifier.height(8.dp))
        BigButton("Exportar pedidos (Excel / CSV)", { ReportExport.csv(ctx, x) }, tonal = true, icon = Icons.Filled.TableChart)
    }
}

object ReportExport {
    private fun share(ctx: android.content.Context, f: java.io.File, mime: String, view: Boolean) {
        val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        val i = if (view) android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, mime)
            else android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType(mime).putExtra(android.content.Intent.EXTRA_STREAM, uri), f.name)
        runCatching { ctx.startActivity(i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { share(ctx, f, mime, false) }
    }
    fun csv(ctx: android.content.Context, r: OrgReport) {
        val sb = StringBuilder("\uFEFFPedido;Fecha;Pueblo;Usuario;Repartidor;Estado;Artículos;Ticket (€);Total (€);Pago;Cobrado;Reclamado;Entregado\n")
        r.rows.forEach { w -> sb.append(listOf(w.id, w.created, w.town, w.customer.orEmpty(), w.driver.orEmpty(), OrderStatus.of(w.status).label, w.items,
            w.ticket?.let { fmtNum(it) } ?: "", w.total?.let { fmtNum(it) } ?: "", when (w.method) { "bizum" -> "Bizum"; "tarjeta" -> "Tarjeta"; else -> "Efectivo" },
            if (w.paid) "Sí" else "No", if (w.claimed) "Sí" else "No", w.deliveredAt.orEmpty()).joinToString(";") { it.toString().replace(";", ",") }).append('\n') }
        val f = java.io.File(java.io.File(ctx.cacheDir, "facturas").apply { mkdirs() }, "Informe_${r.month}.csv"); f.writeText(sb.toString())
        share(ctx, f, "text/csv", false)
    }
    fun pdf(ctx: android.content.Context, r: OrgReport, label: String) {
        val doc = android.graphics.pdf.PdfDocument(); var page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()); var c = page.canvas
        val p = android.graphics.Paint().apply { textSize = 10.5f; isAntiAlias = true }; val b = android.graphics.Paint(p).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val h = android.graphics.Paint(b).apply { textSize = 20f; color = android.graphics.Color.rgb(46, 107, 87) }
        var y = 56f; var n = 1
        fun line(t: String, paint: android.graphics.Paint = p, dy: Float = 15f) {
            if (y > 800) { doc.finishPage(page); n++; page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, n).create()); c = page.canvas; y = 56f }
            c.drawText(t.take(100), 48f, y, paint); y += dy }
        line("INFORME MENSUAL · $label", h, 24f); line(r.org, b, 20f)
        line("Pedidos: ${r.total}   Entregados: ${r.delivered}   Cancelados: ${r.cancelled}   Reclamados: ${r.claims}")
        line("Usuarios atendidos: ${r.users}   Tiempo medio de entrega: ${r.avgHours?.let { fmtNum(it, 1) + " h" } ?: "—"}   Compras: ${eur(r.tickets)}")
        line("Cobrado en efectivo: ${eur(r.cash)}   Cobrado por Bizum: ${eur(r.bizum)}   Pendiente de cobro: ${eur(r.pending)}", dy = 22f)
        line("Por pueblo", b); r.byTown.forEach { line("  ${it.town}: ${it.n} pedidos, ${it.delivered} entregados") }
        y += 6; line("Por repartidor", b); r.byDriver.forEach { line("  ${it.driver}: ${it.n} entregas") }
        y += 6; line("Detalle de pedidos", b)
        r.rows.forEach { w -> line("  Nº ${w.id} · ${w.created} · ${w.town} · ${w.customer.orEmpty()} · ${OrderStatus.of(w.status).label}${w.ticket?.let { " · " + eur(it) } ?: ""}${if (w.claimed) " · RECLAMADO" else ""}", dy = 13f) }
        y += 10; line("Generado por T lo traigo (UNOCEROBITS · Xito Development).", android.graphics.Paint(p).apply { textSize = 8.5f })
        doc.finishPage(page)
        val f = java.io.File(java.io.File(ctx.cacheDir, "facturas").apply { mkdirs() }, "Informe_${r.month}.pdf"); f.outputStream().use { doc.writeTo(it) }; doc.close()
        share(ctx, f, "application/pdf", true)
    }
}

/** Hojas de reclamaciones de los pedidos de la entidad (la entidad es quien presta el servicio y debe responder). */
@Composable
private fun OrgComplaints() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Complaint>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { while (true) { list = runCatching { Locator.repo.complaints() }.getOrDefault(list ?: emptyList()); delay(30_000) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { Text("Responde en un máximo de 10 días hábiles. La respuesta queda registrada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        val l = list
        if (l == null) item { Loading() } else if (l.isEmpty()) item { Message(Icons.Filled.Feedback, "No hay reclamaciones") }
        else items(l, key = { it.id }) { c ->
            var ans by remember(c.id) { mutableStateOf(c.response.orEmpty()) }
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Column(Modifier.padding(14.dp)) {
                Text("${c.number} · ${c.status}", style = MaterialTheme.typography.titleMedium)
                Text("${c.fullName} · ${c.contact}${c.orderId?.let { " · pedido nº $it" } ?: ""} · ${prettyDate(c.createdAt)}", style = MaterialTheme.typography.bodySmall)
                Text(c.subject, style = MaterialTheme.typography.titleSmall); Text(c.body)
                OutlinedTextField(ans, { ans = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Respuesta") }, minLines = 2)
                Button({ scope.launch { runCatching { Locator.repo.answerComplaint(c.id, ans) }; reload++ } }, enabled = ans.isNotBlank()) { Text("Guardar respuesta") }
            } }
        }
    }
}
