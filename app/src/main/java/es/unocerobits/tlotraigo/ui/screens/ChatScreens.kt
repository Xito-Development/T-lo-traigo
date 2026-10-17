package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.core.*

import androidx.compose.ui.graphics.graphicsLayer

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
                        val dictar = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
                            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { t -> text = (if (text.isBlank()) t else "$text $t").replaceFirstChar { it.uppercase() } } }
                        OutlinedTextField(text, { text = it.take(1000) }, Modifier.weight(1f), placeholder = { Text("Escribe un mensaje…") }, maxLines = 4,
                            shape = MaterialTheme.shapes.large, trailingIcon = { IconButton({ runCatching { dictar.launch(dictationIntent("Di tu mensaje")) } }) { Icon(Icons.Filled.Mic, "Dictar mensaje") } })
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
                Box(Modifier.fillMaxWidth().animateItem(fadeInSpec = androidx.compose.animation.core.tween(300), placementSpec = androidx.compose.animation.core.spring(0.8f, 400f)),
                    contentAlignment = if (system) Alignment.Center else if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
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
private val numeros = mapOf("un" to 1, "una" to 1, "uno" to 1, "unos" to 1, "unas" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7,
    "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15, "dieciséis" to 16, "dieciseis" to 16,
    "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20, "veinticinco" to 25, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50,
    "medio" to 1, "media" to 1, "par" to 2)
private val sinonimos = mapOf(
    "Pan" to listOf("pan", "barra", "hogaza", "chapata", "baguette", "pan de molde", "bollo"), "Leche" to listOf("leche"), "Huevos" to listOf("huevo"),
    "Fruta" to listOf("manzana", "plátano", "platano", "naranja", "pera", "uva", "fruta", "mandarina", "melón", "melon", "sandía", "kiwi", "fresa", "limón", "limon", "melocot"),
    "Verdura" to listOf("patata", "tomate", "cebolla", "lechuga", "zanahoria", "pimiento", "verdura", "ajo", "calabacín", "puerro", "judías verdes", "pepino", "berenjena"),
    "Agua" to listOf("agua"), "Cereales" to listOf("cereal", "avena", "muesli"), "Galletas" to listOf("galleta", "magdalena", "bizcocho"), "Café" to listOf("café", "cafe", "cápsula"),
    "Aceite" to listOf("aceite"), "Arroz y pasta" to listOf("arroz", "macarr", "espagueti", "pasta ", "fideo"), "Legumbres" to listOf("lenteja", "garbanzo", "alubia"),
    "Yogures" to listOf("yogur", "natilla", "flan"), "Carne" to listOf("pollo", "ternera", "cerdo", "carne", "filete", "chuleta", "pavo", "hamburguesa"),
    "Pescado" to listOf("merluza", "pescado", "salmón", "salmon", "sardina", "bacalao", "lenguado"),
    "Embutido y queso" to listOf("jamón", "jamon", "chorizo", "queso", "cecina", "salchichón", "lomo", "embutido", "mortadela"),
    "Limpieza" to listOf("lejía", "lejia", "friegasuelos", "lavavajillas", "detergente", "bayeta", "suavizante", "estropajo"),
    "Papel e higiene" to listOf("papel", "gel", "champú", "champu", "pasta de dientes", "desodorante", "jabón", "jabon", "servilleta"),
    "Zumos y refrescos" to listOf("zumo", "refresco", "coca", "gaseosa", "fanta", "cerveza", "vino"), "Conservas" to listOf("atún", "atun", "conserva", "tomate frito", "bonito"),
    "Congelados" to listOf("congelad", "pizza", "croqueta", "helado"), "Azúcar, sal y harina" to listOf("azúcar", "azucar", " sal ", "harina", "pan rallado"),
    "Mascotas" to listOf("perro", "gato", "pienso"), "Bebé" to listOf("pañal", "toallita", "potito"),
)
/** Muletillas y relleno del habla natural que no son productos. */
private val muletilla = Regex("^(e+h*m*|e+m+|m+|h+m+|a+h+|o+h+|uh+|este|pues|bueno|vale|oye|venga|vamos|nada|y?eso|mira|quiero|necesito|ponme|pon|tráeme|traeme|trae|dame|compra|comprar|apunta|añade|anade|también|tambien|además|ademas|luego|después|despues|gracias|porfa|hola|quería|queria|querría|querria|apúntame|apuntame|cómprame|comprame|traer|algo|cosas)$")
/** Palabras con las que se corrige lo que se acaba de decir. */
private val correccion = setOf("perdón", "perdon", "perdona", "espera", "rectifico", "digo", "mejor", "corrijo", "error")
private val sobranAlFinal = setOf("de", "del", "la", "el", "los", "las", "y", "con", "un", "una", "unos", "unas", "o")
private val sobranAlPrincipio = setOf("de", "del", "la", "el", "los", "las", "y", "o", "que")

/**
 * Entiende listas escritas o dictadas de forma natural, aunque vengan sin comas y con dudas:
 * «quiero dos litros de leche, también un par de barras de pan y luego unas manzanas» o
 * «2 barras de pan blanquinas ehhhm 6 bricks de no no 5 bricks de leche».
 * - Quita muletillas y relleno («eh», «bueno», «quiero», «ponme», «también», «luego», «por favor», «nada más»…).
 * - «no no», «perdón», «espera», «digo», «mejor» anulan lo que se estaba diciendo; «…, no, mejor 3» corrige la cantidad.
 * - Cada número (2, «dos», «un par de», «unas»…) empieza un artículo nuevo; también las comas, los puntos y la «y».
 */
fun parseShoppingList(text: String, needs: List<Need>): List<Triple<Need, Int, String>> {
    val otra = needs.firstOrNull { it.name == "Otra cosa" } ?: return emptyList()
    val clean = (" " + text.lowercase() + " ").replace(Regex("[\\n.;:!?¿¡]+"), " , ").replace(",", " , ")
        .replace(Regex(" (o sea|a ver|por favor|quiero decir|es decir|nada más|nada mas|y ya está|y ya esta|eso es todo|y ya|me hace falta|hace falta|me falta|otra cosa|un poquito de|un poco de) "), " , ")
        .replace(Regex(" y (medio|media|cuarto) "), " y_$1 ").replace(" y ", " , ").replace(" un par de ", " 2 ").replace(" par de ", " 2 ")
    val tokens = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
    val items = mutableListOf<MutableList<String>>(); val cur = mutableListOf<String>()
    var corregible: MutableList<String>? = null   // artículo anulado con «mejor…»: si después solo viene un número, se corrige su cantidad
    fun isQty(t: String) = t.toIntOrNull() != null || t in numeros
    fun hasWords(l: List<String>) = l.any { !isQty(it) }
    fun flush() {
        val c = corregible
        if (!hasWords(cur) && cur.isNotEmpty() && c != null) { items += (listOf(cur.first()) + c.dropWhile(::isQty)).toMutableList(); corregible = null }
        else if (hasWords(cur)) { items += cur.toMutableList(); corregible = null }
        cur.clear()
    }
    var k = 0
    while (k < tokens.size) {
        val t = tokens[k]; val next = tokens.getOrNull(k + 1)
        when {
            t == "," -> flush()
            t in correccion || (t == "no" && (next == "no" || next in correccion || (next != null && isQty(next)) ||
                    ((tokens.getOrNull(k - 1) ?: ",") == "," && (next == null || next == ",")))) -> {
                if (hasWords(cur)) { corregible = cur.toMutableList(); cur.clear() }
                else { cur.clear(); if (corregible == null && items.isNotEmpty()) corregible = items.removeAt(items.lastIndex) }
                while (tokens.getOrNull(k + 1) == "no" || tokens.getOrNull(k + 1) in correccion) k++
            }
            muletilla.matches(t) -> {}
            isQty(t) && hasWords(cur) -> { flush(); cur += t }
            else -> cur += t
        }
        k++
    }
    flush()
    return items.mapNotNull { words ->
        val w = words.map { it.replace("y_", "y ") }.toMutableList()
        while (w.isNotEmpty() && w.last() in sobranAlFinal) w.removeAt(w.lastIndex)
        if (w.isEmpty()) return@mapNotNull null
        val first = w.first()
        val qty = first.toIntOrNull() ?: numeros[first]?.takeIf { first != "medio" && first != "media" }
        val detailWords = (if (qty != null) w.drop(1) else w).dropWhile { it in sobranAlPrincipio }
        if (detailWords.isEmpty()) return@mapNotNull null
        val detail = detailWords.joinToString(" ").replaceFirstChar { it.uppercase() }
        val low = " ${detail.lowercase()} "
        val need = sinonimos.entries.firstOrNull { (_, ks) -> ks.any { it in low } }?.key?.let { n -> needs.firstOrNull { it.name == n } } ?: otra
        Triple(need, (qty ?: 1).coerceIn(1, 99), detail)
    }
}

private class Draft(val need: Need, qty: Int, detail: String) { var qty by mutableIntStateOf(qty); var detail by mutableStateOf(detail) }

/** Intent de dictado configurado para frases largas con pausas. */
fun dictationIntent(prompt: String) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
    .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt).putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
    .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)

@Composable
fun VoiceListCard(needs: List<Need>, onAdd: (List<Triple<Need, Int, String>>) -> Unit) {
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val drafts = remember { mutableStateListOf<Draft>() }
    fun review() { drafts.clear(); parseShoppingList(text, needs).forEach { (n, q, d) -> drafts += Draft(n, q, d) }; if (drafts.isEmpty() && text.isNotBlank()) err = "No he entendido ningún producto. Prueba con «2 barras de pan, leche…»" else err = null }
    var dictating by remember { mutableStateOf(false) }
    if (dictating) DictationDialog("Di lo que necesitas", { t -> dictating = false; if (t.isNotBlank()) { text = if (text.isBlank()) t else "$text, $t"; review() } }) { dictating = false }
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Text("Escribe o dicta tu lista", style = MaterialTheme.typography.titleMedium)
            Text("Ej.: «2 barras de pan, 5 bricks de leche y pilas». Puedes decir «no, no» o «perdón» para corregir.", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(text, { text = it; drafts.clear() }, Modifier.weight(1f), placeholder = { Text("Tu lista…") }, minLines = 2, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.width(8.dp))
                FilledIconButton({ dictating = true }, Modifier.size(56.dp)) { Icon(Icons.Filled.Mic, "Dictar la lista por voz") }
            }
            if (text.isNotBlank() && drafts.isEmpty()) TextButton(::review) { Icon(Icons.Filled.Checklist, null); Spacer(Modifier.width(6.dp)); Text("Revisar la lista") }
            ErrorText(err)
            if (drafts.isNotEmpty()) {
                Text("Revisa y corrige antes de añadir:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                drafts.toList().forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        QtyStepper(d.qty, { if (d.qty > 1) d.qty-- }, { if (d.qty < 99) d.qty++ })
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            androidx.compose.foundation.text.BasicTextField(d.detail, { d.detail = it.take(120) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSecondaryContainer), modifier = Modifier.fillMaxWidth())
                            Text(d.need.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton({ drafts.remove(d) }) { Icon(Icons.Filled.Close, "Quitar ${d.detail}") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                BigButton("Añadir ${productos(drafts.sumOf { it.qty })} a la cesta", {
                    onAdd(drafts.filter { it.detail.isNotBlank() }.map { Triple(it.need, it.qty, it.detail.trim()) }); drafts.clear(); text = ""
                }, icon = Icons.Filled.AddShoppingCart)
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
