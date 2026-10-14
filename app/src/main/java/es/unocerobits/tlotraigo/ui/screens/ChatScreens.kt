package es.unocerobits.tlotraigo.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Abre el chat de un pedido desde cualquier pantalla (lo proporciona la navegación). */
val LocalOpenChat = staticCompositionLocalOf<(Long) -> Unit> { {} }

// =================== Chat del pedido ===================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: AppViewModel, orderId: Long, onBack: () -> Unit) {
    val me = vm.profile.collectAsState().value
    val myRole = when { me?.isDriver == true || vm.driverView.collectAsState().value -> "repartidor"; me?.isGestor == true || me?.isAdmin == true -> "admin"; else -> "cliente" }
    var msgs by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    DisposableEffect(orderId) { ChatNotifier.openChatOrder = orderId; onDispose { ChatNotifier.openChatOrder = null } }
    LaunchedEffect(Unit) { if (android.os.Build.VERSION.SDK_INT >= 33) perm.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(orderId) {
        while (true) {
            runCatching { val n = Locator.repo.messages(orderId); if (n.size != msgs.size) { msgs = n; Locator.repo.markRead(orderId) } }
            delay(4_000)
        }
    }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.lastIndex) }
    val quick = if (myRole == "cliente") listOf("¿Cuánto tardas?", "Estoy en casa", "Llama al timbre", "¡Gracias!")
                else listOf("Estoy en el súper", "No hay este producto, ¿lo cambio?", "Llego en 10 minutos", "Estoy en la puerta")
    fun send(t: String) = scope.launch {
        if (t.isBlank()) return@launch
        try { Locator.repo.sendMessage(orderId, t); text = ""; err = null; msgs = Locator.repo.messages(orderId) } catch (e: ApiException) { err = e.message }
    }
    Scaffold(topBar = { TopAppBar({ Text("Chat · pedido nº $orderId") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) },
        bottomBar = {
            Surface(tonalElevation = 6.dp) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(8.dp)) {
                    ErrorText(err)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quick) { q -> AssistChip({ send(q) }, { Text(q) }) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(text, { text = it.take(1000) }, Modifier.weight(1f), placeholder = { Text("Escribe un mensaje…") }, maxLines = 4,
                            shape = MaterialTheme.shapes.large)
                        Spacer(Modifier.width(6.dp))
                        FilledIconButton({ send(text) }, enabled = text.isNotBlank(), modifier = Modifier.size(52.dp)) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
                    }
                }
            }
        }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), state = list, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (msgs.isEmpty()) item { Message(Icons.Filled.Forum, "Sin mensajes todavía", "Escribe aquí para hablar con ${if (myRole == "cliente") "tu repartidor" else "el cliente"}.") }
            items(msgs, key = { it.id }) { m ->
                val mine = m.role == myRole; val system = m.role == "sistema"
                Box(Modifier.fillMaxWidth(), contentAlignment = if (system) Alignment.Center else if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(shape = RoundedCornerShape(18.dp, 18.dp, if (mine) 4.dp else 18.dp, if (mine) 18.dp else 4.dp),
                        color = when { system -> MaterialTheme.colorScheme.tertiaryContainer; mine -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.surfaceContainerHigh },
                        modifier = Modifier.widthIn(max = 320.dp)) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                            Text(m.body, style = MaterialTheme.typography.bodyLarge,
                                color = if (mine && !system) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            Text(prettyDate(m.createdAt).substringAfterLast(", "), style = MaterialTheme.typography.labelSmall,
                                color = (if (mine && !system) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = .8f))
                        }
                    }
                }
            }
        }
    }
}

// =================== Lista por texto o voz ===================
private val numeros = mapOf("un" to 1, "una" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "doce" to 12)
private val sinonimos = mapOf(
    "Pan" to listOf("pan", "barra", "hogaza", "chapata"), "Leche" to listOf("leche"), "Huevos" to listOf("huevo"),
    "Fruta" to listOf("manzana", "plátano", "platano", "naranja", "pera", "uva", "fruta", "mandarina", "melón", "sandía"),
    "Verdura" to listOf("patata", "tomate", "cebolla", "lechuga", "zanahoria", "pimiento", "verdura", "ajo", "calabacín"),
    "Agua" to listOf("agua"), "Cereales" to listOf("cereal", "avena", "muesli"), "Galletas" to listOf("galleta"), "Café" to listOf("café", "cafe"),
    "Aceite" to listOf("aceite"), "Arroz y pasta" to listOf("arroz", "macarr", "espagueti", "pasta", "fideo"), "Legumbres" to listOf("lenteja", "garbanzo", "alubia", "judía"),
    "Yogures" to listOf("yogur"), "Carne" to listOf("pollo", "ternera", "cerdo", "carne", "filete", "chuleta"), "Pescado" to listOf("merluza", "pescado", "salmón", "sardina", "bacalao"),
    "Embutido y queso" to listOf("jamón", "chorizo", "queso", "cecina", "salchichón", "lomo", "embutido"),
    "Limpieza" to listOf("lejía", "friegasuelos", "lavavajillas", "detergente", "bayeta", "suavizante"),
    "Papel e higiene" to listOf("papel", "gel", "champú", "pasta de dientes", "desodorante", "jabón"),
    "Zumos y refrescos" to listOf("zumo", "refresco", "coca", "gaseosa", "fanta"), "Conservas" to listOf("atún", "conserva", "tomate frito", "bonito"),
    "Congelados" to listOf("congelad", "pizza", "croqueta", "helado"), "Azúcar, sal y harina" to listOf("azúcar", "azucar", "sal ", "harina", "pan rallado"),
    "Mascotas" to listOf("perro", "gato", "pienso"), "Bebé" to listOf("pañal", "toallita", "potito"),
)

/** «2 litros de leche, pan y 6 huevos» → [(Leche, 2, "litros de leche"), (Pan, 1, "pan"), (Huevos, 6, "huevos")]. */
fun parseShoppingList(text: String, needs: List<Need>): List<Triple<Need, Int, String>> {
    val otra = needs.firstOrNull { it.name == "Otra cosa" } ?: return emptyList()
    return text.replace(" y ", ",").split('\n', ',', ';', '.').map { it.trim() }.filter { it.length >= 2 }.map { raw ->
        val words = raw.split(' ')
        val first = words.first().lowercase()
        val qty = first.toIntOrNull() ?: numeros[first]
        val detail = if (qty != null) words.drop(1).joinToString(" ") else raw
        val low = " ${detail.lowercase()} "
        val need = sinonimos.entries.firstOrNull { (_, ks) -> ks.any { it in low } }?.key?.let { n -> needs.firstOrNull { it.name == n } } ?: otra
        Triple(need, (qty ?: 1).coerceIn(1, 99), detail.replaceFirstChar { it.uppercase() })
    }
}

@Composable
fun VoiceListCard(needs: List<Need>, onAdd: (List<Triple<Need, Int, String>>) -> Unit) {
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text = if (text.isBlank()) it else "$text, $it" }
    }
    val preview = remember(text, needs) { parseShoppingList(text, needs) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Text("Escribe o dicta tu lista", style = MaterialTheme.typography.titleMedium)
            Text("Ej.: «2 litros de leche, pan, 6 huevos y pilas AA»", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("Tu lista…") }, minLines = 2, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.width(8.dp))
                FilledIconButton({
                    try {
                        voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES").putExtra(RecognizerIntent.EXTRA_PROMPT, "Di lo que necesitas"))
                    } catch (e: ActivityNotFoundException) { err = "Este móvil no tiene dictado por voz. Escribe la lista." }
                }, Modifier.size(56.dp)) { Icon(Icons.Filled.Mic, "Dictar por voz") }
            }
            ErrorText(err)
            if (preview.isNotEmpty()) {
                Text("Se añadirá:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                preview.forEach { (n, q, d) -> Text("• $q × $d  (${n.name})", style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(8.dp))
                BigButton("Añadir ${productos(preview.sumOf { it.second })} a la cesta", { onAdd(preview); text = "" }, icon = Icons.Filled.AddShoppingCart)
            }
        }
    }
}

// =================== Editar pedido (antes de que el repartidor llegue al súper) ===================
@Composable
fun EditOrderDialog(o: Order, onDone: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val qty = remember { mutableStateMapOf<Long, Int>().apply { o.items.forEach { put(it.id, it.qty) } } }
    var extra by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(o.notes.orEmpty()) }
    var needs by remember { mutableStateOf<List<Need>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { needs = runCatching { Locator.repo.needs() }.getOrDefault(emptyList()) }
    val added = remember(extra, needs) { parseShoppingList(extra, needs) }
    AlertDialog(onDismissRequest = { onDone(null) }, icon = { Icon(Icons.Filled.EditNote, null) }, title = { Text("Editar pedido") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Puedes cambiarlo hasta que el repartidor llegue al súper.", style = MaterialTheme.typography.bodySmall)
            o.items.forEach { i ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Column(Modifier.weight(1f)) { Text(i.name, style = MaterialTheme.typography.bodyLarge); i.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
                    val q = qty[i.id] ?: 0
                    if (q == 0) TextButton({ qty[i.id] = 1 }) { Text("Recuperar") } else QtyStepper(q, { qty[i.id] = q - 1 }, { qty[i.id] = q + 1 })
                }
            }
            OutlinedTextField(extra, { extra = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Añadir más (escribe la lista)") },
                placeholder = { Text("Ej.: 1 kilo de naranjas, sal") }, minLines = 2)
            added.forEach { (n, q, d) -> Text("+ $q × $d", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            OutlinedTextField(notes, { notes = it.take(500) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Notas para el repartidor") }, minLines = 2)
            ErrorText(err)
        } },
        confirmButton = { Button({
            scope.launch {
                busy = true; err = null
                try {
                    val lines = o.items.mapNotNull { i -> (qty[i.id] ?: 0).takeIf { it > 0 }?.let { Triple(i, it, null as String?) } } +
                        added.map { (n, q, d) -> Triple(OrderItem(id = 0, needId = n.id, name = n.name, unitPrice = n.estPrice, qty = q, detail = d), q, d) }
                    if (lines.isEmpty()) throw ApiException("El pedido no puede quedar vacío (para eso, cancélalo)")
                    Locator.repo.editOrder(o.id, lines, notes); onDone("Pedido actualizado.")
                } catch (e: ApiException) { err = e.message }
                busy = false
            }
        }, enabled = !busy) { Text("Guardar cambios") } },
        dismissButton = { TextButton({ onDone(null) }) { Text("Cancelar") } })
}

// =================== Reclamar pedido y 112 ===================
@Composable
fun ClaimSection(o: Order, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ask by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    if (o.claimDeadline != null) {
        val left = runCatching { java.time.Duration.between(java.time.Instant.now(), java.time.OffsetDateTime.parse(o.claimDeadline).toInstant()) }.getOrNull()
        Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text("⚠️ Pedido reclamado", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(if (left != null && !left.isNegative) "Debe entregarse en las próximas ${left.toHours()} h ${(left.toMinutes() % 60)} min." else "Se ha superado el plazo de 6 horas. El equipo de T lo traigo está avisado.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.height(10.dp))
                Button({ ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                    Modifier.fillMaxWidth().heightIn(min = 60.dp), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError),
                    shape = MaterialTheme.shapes.medium) { Icon(Icons.Filled.Emergency, null); Spacer(Modifier.width(8.dp)); Text("Llamar al 112", style = MaterialTheme.typography.titleMedium) }
                Text("Solo emergencias: por ejemplo, si la persona mayor no responde o necesita ayuda urgente.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 6.dp))
            }
        }
        return
    }
    TextButton({ ask = true }) { Icon(Icons.Filled.ReportProblem, null); Spacer(Modifier.width(6.dp)); Text("Reclamar: no me ha llegado") }
    if (ask) AlertDialog(onDismissRequest = { ask = false }, title = { Text("Reclamar el pedido") },
        text = { Column {
            Text("El repartidor y el equipo tendrán 6 horas para entregarlo. Te mostraremos también un botón para llamar al 112 por si hay una emergencia.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(reason, { reason = it.take(500) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("¿Qué ha pasado? (opcional)") }, minLines = 2)
            ErrorText(err)
        } },
        confirmButton = { Button({ scope.launch { try { Locator.repo.claimOrder(o.id, reason); ask = false; onDone() } catch (e: ApiException) { err = e.message } } }) { Text("Reclamar") } },
        dismissButton = { TextButton({ ask = false }) { Text("Cancelar") } })
}

// =================== Admin: editor de la lista fácil ===================
@Composable
fun AdminNeeds() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Need>?>(null) }
    var edit by remember { mutableStateOf<Need?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { list = runCatching { Locator.repo.allNeeds() }.getOrDefault(emptyList()) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { BigButton("Añadir artículo básico", { edit = Need(0, "", "cesta", 1.0) }, icon = Icons.Filled.AddCircle) }
        val l = list
        if (l == null) item { Loading() } else items(l, key = { it.id }) { n ->
            ListItem(headlineContent = { Text(n.name + if (!n.active) " (oculto)" else "") },
                supportingContent = { Text("≈ ${eur(n.estPrice)} / ${n.unitLabel} · IVA ${n.vatRate.toInt()} % · ${n.options.joinToString(" · ") { it.t }}") },
                leadingContent = { Icon(AppIcons.vector(n.icon), null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Filled.Edit, "Editar") }, modifier = Modifier.clickable { edit = n })
        }
    }
    edit?.let { n -> NeedEditor(n, { reload++; edit = null }) { edit = null } }
}

@Composable
private fun NeedEditor(n: Need, onSaved: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(n.name) }
    var icon by remember { mutableStateOf(n.icon) }
    var price by remember { mutableStateOf(if (n.id == 0L) "" else fmtNum(n.estPrice)) }
    var unit by remember { mutableStateOf(n.unitLabel) }
    var vat by remember { mutableDoubleStateOf(n.vatRate) }
    var cold by remember { mutableStateOf(n.coldChain) }
    var active by remember { mutableStateOf(n.active) }
    var sort by remember { mutableStateOf(n.sort.toString()) }
    var opts by remember { mutableStateOf(n.options.joinToString("\n") { "${it.t}: ${it.v.joinToString(", ")}" }) }
    var pick by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (n.id == 0L) "Nuevo artículo" else "Editar ${n.name}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre") }, singleLine = true)
            OutlinedButton({ pick = true }, Modifier.padding(top = 6.dp)) { Icon(AppIcons.vector(icon), null); Spacer(Modifier.width(6.dp)); Text("Icono") }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedTextField(price, { price = it }, Modifier.weight(1f), label = { Text("Precio orientativo") }, suffix = { Text("€") }, singleLine = true)
                OutlinedTextField(unit, { unit = it }, Modifier.weight(1f), label = { Text("Unidad") }, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) { listOf(4.0, 10.0, 21.0).forEach { v -> FilterChip(vat == v, { vat = v }, { Text("IVA ${v.toInt()} %") }) } }
            OutlinedTextField(opts, { opts = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Opciones (una por línea)") },
                placeholder = { Text("Tipo: Entera, Semidesnatada\nFormato: 1 litro, Pack de 6") }, minLines = 3)
            OutlinedTextField(sort, { sort = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Orden en la lista") }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(cold, { cold = it }); Text("Refrigerado o congelado") }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(active, { active = it }); Text("Visible para los clientes") }
            ErrorText(err)
            if (n.id != 0L) TextButton({ scope.launch { runCatching { Locator.repo.deleteNeed(n.id) }; onSaved() } }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button({
            scope.launch {
                try {
                    val options = opts.lines().mapNotNull { l -> l.split(':', limit = 2).takeIf { it.size == 2 }?.let { (t, v) ->
                        NeedOption(t.trim(), v.split(',').map { it.trim() }.filter { it.isNotEmpty() }) } }.filter { it.t.isNotBlank() && it.v.isNotEmpty() }
                    Locator.repo.saveNeed(n.id.takeIf { it != 0L }, n.copy(name = name, icon = icon, estPrice = parseNum(price) ?: throw ApiException("Precio no válido"),
                        unitLabel = unit.ifBlank { "ud" }, vatRate = vat, coldChain = cold, active = active, sort = sort.toIntOrNull() ?: 50, options = options))
                    onSaved()
                } catch (e: ApiException) { err = e.message }
            }
        }, enabled = name.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } })
    if (pick) IconPickerDialog(icon, { icon = it ?: "cesta"; pick = false }) { pick = false }
}
