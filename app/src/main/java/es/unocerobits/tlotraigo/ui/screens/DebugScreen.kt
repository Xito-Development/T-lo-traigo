package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.BuildConfig
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.payments.PayMethod
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DebugScreen(vm: AppViewModel, onBack: () -> Unit) {
    val repo = Locator.repo
    val towns by vm.towns.collectAsState()
    val profile by vm.profile.collectAsState()
    val log by DebugTools.log.collectAsState()
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }

    fun run(label: String, block: suspend () -> String) = scope.launch {
        busy = true
        val msg = try { block() } catch (e: Exception) { "Error en $label: ${e.message}" }
        busy = false
        snack.showSnackbar(msg)
    }

    // Estado de los formularios
    var count by remember { mutableIntStateOf(3) }
    var town by remember { mutableStateOf<Long?>(null) }
    var method by remember { mutableStateOf<PayMethod?>(null) }
    var drivers by remember { mutableStateOf<List<DriverRow>>(emptyList()) }
    var feePct by remember { mutableStateOf("") }
    var feeMin by remember { mutableStateOf("") }
    var latency by remember { mutableLongStateOf(DebugTools.extraLatencyMs) }
    var reloadDrivers by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { runCatching { repo.fees() }.onSuccess { feePct = it.servicePct.toString(); feeMin = it.minService.toString() } }
    LaunchedEffect(reloadDrivers) { runCatching { drivers = repo.drivers() } }

    Scaffold(
        topBar = {
            TopAppBar({ Text("Panel de pruebas") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = { if (busy) CircularProgressIndicator(Modifier.size(24.dp).padding(end = 4.dp)) })
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

            // ---------- Pedidos de prueba
            SectionTitle("Pedidos de prueba")
            Text("Se crean a tu nombre, marcados como [PRUEBA]. Míralos en «Pedidos» o acéptalos en la vista de repartidor.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Label("Cantidad")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10).forEach { n -> FilterChip(count == n, { count = n }, { Text("$n") }) }
            }
            Label("Pueblo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(town == null, { town = null }, { Text("Aleatorio") })
                towns.forEach { t -> FilterChip(town == t.id, { town = t.id }, { Text(t.name) }) }
            }
            Label("Pago")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(method == null, { method = null }, { Text("Aleatorio") })
                PayMethod.entries.forEach { m -> FilterChip(method == m, { method = m }, { Text(m.label) }) }
            }
            Spacer(Modifier.height(12.dp))
            BigButton("Generar ${productosOrPedidos(count)}", { run("generar") { "Creados ${repo.debugSpawn(count, town, method?.db)} pedidos de prueba" } },
                icon = Icons.Filled.AddBox, enabled = !busy)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ run("antiguos") { "Creados ${repo.debugSpawn(count, town, method?.db, 50)} pedidos con 50 h de antigüedad" } },
                    Modifier.weight(1f), enabled = !busy) { Icon(Icons.Filled.History, null); Spacer(Modifier.width(6.dp)); Text("Pedidos de hace 50 h") }
                OutlinedButton({ run("penalizar") { "Penalizaciones aplicadas: ${repo.debugRunPenalties()}" } },
                    Modifier.weight(1f), enabled = !busy) { Icon(Icons.Filled.Gavel, null); Spacer(Modifier.width(6.dp)); Text("Aplicar penalizaciones") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ run("avanzar") { "Avanzados ${repo.debugAdvance()} pedidos un paso" } }, Modifier.weight(1f), enabled = !busy) {
                    Icon(Icons.Filled.FastForward, null); Spacer(Modifier.width(6.dp)); Text("Avanzar estado")
                }
                OutlinedButton({ run("borrar") { "Borrados ${repo.debugClear()} pedidos de prueba" } }, Modifier.weight(1f), enabled = !busy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Filled.DeleteSweep, null); Spacer(Modifier.width(6.dp)); Text("Borrar todos")
                }
            }

            // ---------- Repartidores
            SectionTitle("Repartidores")
            if (drivers.isEmpty()) Text("No hay repartidores registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            drivers.forEach { d ->
                ListItem(
                    headlineContent = { Text(d.profile?.fullName?.ifBlank { null } ?: d.driverId.take(8)) },
                    supportingContent = { Text((if (d.approved) "Aprobado" else "Pendiente de aprobación") + (if (d.available) " · disponible" else "") + (d.profile?.phone?.let { " · $it" } ?: "")) },
                    trailingContent = {
                        AppSwitch(d.approved, { v -> run("aprobar") { repo.setDriverApproved(d.driverId, v); reloadDrivers++; if (v) "Repartidor aprobado" else "Aprobación retirada" } })
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                )
            }

            // ---------- Tarifas
            SectionTitle("Tarifas")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(feePct, { feePct = it.replace(',', '.') }, Modifier.weight(1f), label = { Text("Servicio %") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(feeMin, { feeMin = it.replace(',', '.') }, Modifier.weight(1f), label = { Text("Mínimo €") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Spacer(Modifier.height(8.dp))
            BigButton("Guardar tarifas", {
                run("tarifas") {
                    val p = feePct.toDoubleOrNull() ?: error("porcentaje no válido")
                    val m = feeMin.toDoubleOrNull() ?: error("mínimo no válido")
                    repo.saveFees(p, m); "Tarifas guardadas: $p % (mín. ${eur(m)})"
                }
            }, tonal = true, icon = Icons.Filled.Euro, enabled = !busy)

            // ---------- Red
            SectionTitle("Red")
            Label("Latencia añadida")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0L to "Ninguna", 1000L to "1 s", 3000L to "3 s", 8000L to "8 s").forEach { (v, l) ->
                    FilterChip(latency == v, { latency = v; DebugTools.extraLatencyMs = v }, { Text(l) })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton({ DebugTools.failNext = true; scope.launch { snack.showSnackbar("La próxima petición fallará") } }, Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.WifiOff, null); Spacer(Modifier.width(6.dp)); Text("Hacer fallar la próxima petición")
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Últimas peticiones (${log.size})", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton({ DebugTools.clearLog() }) { Text("Limpiar") }
            }
            val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    if (log.isEmpty()) Text("Sin peticiones todavía.", style = MaterialTheme.typography.bodySmall)
                    log.take(30).forEach { l ->
                        val ok = l.code in 200..299
                        Text(
                            "${fmt.format(Date(l.time))} ${l.method} ${l.code.takeIf { it > 0 } ?: "ERR"} ${l.ms}ms ${l.path}" + (l.error?.let { "\n   → $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }

            // ---------- Sesión y app
            SectionTitle("Sesión y app")
            val s = Locator.api.session
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Text(
                    listOf(
                        "Usuario: ${s?.user?.email}", "ID: ${s?.user?.id}", "Rol: ${profile?.role}",
                        "Token caduca: ${s?.expiresAt?.let { fmt.format(Date(it * 1000)) }}",
                        "Versión: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", "Backend: ${BuildConfig.SUPABASE_URL}",
                        "Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT}) · ${android.os.Build.MODEL}",
                    ).joinToString("\n"),
                    Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ Locator.api.debugExpireSession(); scope.launch { snack.showSnackbar("Token marcado como caducado: se refrescará en la próxima petición") } }) { Text("Caducar token") }
                OutlinedButton({ vm.cart.clear(); scope.launch { snack.showSnackbar("Cesta vaciada") } }) { Text("Vaciar cesta") }
                OutlinedButton({ vm.updateSettings { UiSettings() }; scope.launch { snack.showSnackbar("Ajustes visuales restablecidos") } }) { Text("Restablecer vista") }
                OutlinedButton({ vm.start() }) { Text("Recargar perfil") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun productosOrPedidos(n: Int) = if (n == 1) "1 pedido" else "$n pedidos"

@Composable
private fun Label(text: String) = Text(text, Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall)
