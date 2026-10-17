@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.foundation.combinedClickable

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// =================== Bienvenida (onboarding) ===================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingWizard(vm: AppViewModel) {
    val p by vm.profile.collectAsState()
    val s by vm.settings.collectAsState()
    val driver = p?.isDriver == true
    var step by rememberSaveable { mutableIntStateOf(if (driver) 1 else 0) }
    var usage by rememberSaveable { mutableStateOf(p?.usage ?: "propio") }
    val scope = rememberCoroutineScope()
    val total = 5
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Wordmark(width = 170.dp)
        LinearProgressIndicator({ (step + 1f) / total }, Modifier.fillMaxWidth().padding(vertical = 16.dp))
        AnimatedContent(step, transitionSpec = { (slideInHorizontally { (if (targetState > initialState) 1 else -1) * it / 3 } + fadeIn(androidx.compose.animation.core.tween(220))) togetherWith ExitTransition.None }, label = "ob") { st ->
            Column {
                when (st) {
                    0 -> {
                        Text("¿Quién va a usar T lo traigo?", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        ChoiceRow("Hago mi propia compra", "Pido para mi casa", usage == "propio", icon = Icons.Filled.Person) { usage = "propio" }
                        ChoiceRow("Pido para un familiar", "Soy hijo, hija o familiar de alguien mayor", usage == "familiar", icon = Icons.Filled.FamilyRestroom) { usage = "familiar" }
                        ChoiceRow("Un familiar pedirá por mí", "Mi hijo o hija me ayudará desde su móvil", usage == "asistido", icon = Icons.Filled.Elderly) { usage = "asistido" }
                        var redeem by remember { mutableStateOf(false) }
                        if (redeem) RedeemInviteDialog(vm) { redeem = false }
                        TextButton({ redeem = true }) { Icon(Icons.Filled.VpnKey, null); Spacer(Modifier.width(6.dp)); Text("Tengo un código de una entidad (administro o reparto)") }
                        Spacer(Modifier.height(16.dp))
                        BigButton("Siguiente", { step = 1 }, icon = Icons.Filled.ArrowForward)
                    }
                    1 -> {
                        Text("Ajusta cómo se ve", style = MaterialTheme.typography.headlineMedium)
                        Text("Podrás cambiarlo cuando quieras en Ajustes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tamaño de letra", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.85f to "Pequeña", 1f to "Normal", 1.15f to "Grande", 1.3f to "Muy grande", 1.5f to "Enorme").forEach { (v, l) ->
                                val on = kotlin.math.abs(s.textScale - v) < .01f
                                FilterChip(on, { vm.updateSettings { it.copy(textScale = v) } }, { Text(l, maxLines = 1) }, leadingIcon = if (on) { { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) } } else null)
                            }
                        }
                        ToggleRow("Modo fácil", "Botones y letras más grandes", s.senior, Icons.Filled.Elderly) { v -> vm.updateSettings { it.copy(senior = v) } }
                        ToggleRow("Leer en voz alta", "Botón de altavoz en los productos", s.speak, Icons.Filled.RecordVoiceOver) { v -> vm.updateSettings { it.copy(speak = v) } }
                        ToggleRow("Alto contraste", "Máxima legibilidad", s.highContrast, Icons.Filled.Contrast) { v -> vm.updateSettings { it.copy(highContrast = v) } }
                        Surface(Modifier.fillMaxWidth().padding(top = 12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                            Text("Así se verán los textos de la app.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(16.dp))
                        BigButton("Siguiente", { step = 2 }, icon = Icons.Filled.ArrowForward)
                    }
                    2 -> {
                        Text("¿Nos ayudas a mejorar?", style = MaterialTheme.typography.headlineMedium)
                        Text("Si aceptas, enviaremos datos anónimos de uso (qué pantallas se abren) y de errores para arreglarlos antes. " +
                            "Se guardan en la Unión Europea, no se comparten con nadie y puedes cambiarlo en Ajustes.", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        BigButton("Sí, acepto", { Analytics.consent = true; scope.launch { runCatching { Locator.repo.saveUsage(usage, true) } }; step = 3 }, icon = Icons.Filled.Check)
                        Spacer(Modifier.height(8.dp))
                        BigButton("No, gracias", { Analytics.consent = false; scope.launch { runCatching { Locator.repo.saveUsage(usage, false) } }; step = 3 }, tonal = true)
                    }
                    3 -> {
                        ProfileForm(vm, if (driver) "¿Dónde vives y repartes?" else if (usage == "familiar") "Tus datos" else "¿Dónde te llevamos la compra?", "Guardar y continuar",
                            onBack = if (driver) null else ({ step = 2 })) {
                            if (usage == "propio" || driver) vm.start() else step = 4
                        }
                        if (usage == "familiar") Text("Después podrás vincular a tu familiar para pedir a su casa.", Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> FamilyStep(vm, usage) { vm.start() }
                }
            }
        }
        if (step > 0 && step != 3 && !(driver && step == 1)) TextButton({ step -= 1 }, Modifier.align(Alignment.CenterHorizontally)) { Text("Atrás") }
    }
}

@Composable
private fun FamilyStep(vm: AppViewModel, usage: String, onDone: () -> Unit) {
    val p by vm.profile.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refreshProfile() }
    if (usage == "asistido") {
        Text("Dale este código a tu familiar", style = MaterialTheme.typography.headlineMedium)
        Text(p?.linkCode ?: "…", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp))
        Text("Con él podrá hacerte los pedidos desde su móvil. Sirve una sola vez.", style = MaterialTheme.typography.bodyLarge)
        TextButton({ ctx.share("Mi código de T lo traigo es ${p?.linkCode}. Descarga la app: https://tlotraigo.unocerobits.com") }) { Text("Enviar el código") }
    } else {
        Text("Vincula a tu familiar", style = MaterialTheme.typography.headlineMedium)
        Text("Pídele el código de 6 cifras que aparece en su app (Ajustes → Familia).", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(code, { code = it.uppercase().take(6) }, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Código") }, singleLine = true, shape = MaterialTheme.shapes.medium)
        ErrorText(err)
        if (ok) Text("¡Vinculado! Ya puedes pedir para tu familiar.", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        BigButton("Vincular", { scope.launch { try { Locator.repo.linkSenior(code); ok = true; err = null } catch (e: ApiException) { err = e.message } } },
            enabled = code.length == 6 && !ok, icon = Icons.Filled.Link)
    }
    Spacer(Modifier.height(12.dp))
    BigButton(if (usage == "asistido" || ok) "Empezar" else "Lo haré más tarde", onDone, tonal = !(usage == "asistido" || ok), icon = Icons.Filled.Check)
}

// =================== Lista fácil (pedido guiado) ===================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsScreen(vm: AppViewModel, onBack: () -> Unit, onCart: () -> Unit) {
    var needs by remember { mutableStateOf<List<Need>?>(null) }
    var sel by remember { mutableStateOf<Need?>(null) }
    var iconFor by remember { mutableStateOf<Need?>(null) }
    val lines by vm.cart.lines.collectAsState()
    var q by rememberSaveable { mutableStateOf("") }
    var usual by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        needs = runCatching { Locator.repo.needs() }.getOrDefault(emptyList())
        // «Lo de siempre»: artículos de la lista fácil de pedidos anteriores
        usual = runCatching { Locator.repo.myOrders().flatMap { it.items }.filter { it.needId != null }.distinctBy { it.needId to it.detail }.take(6) }.getOrDefault(emptyList())
    }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun added(t: String) = scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar("Añadido a la cesta: $t") }
    Scaffold(topBar = { TopAppBar({ Text("Lista fácil") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = { if (lines.isNotEmpty()) Surface(tonalElevation = 6.dp) { Box(Modifier.navigationBarsPadding().padding(16.dp)) {
            BigButton("Ver mi cesta (${productos(lines.values.sumOf { it.qty })})", onCart, icon = Icons.Filled.ShoppingCart) } } }) { pad ->
        val n = needs?.filter { q.isBlank() || q.trim().lowercase() in it.name.lowercase() || it.options.any { o -> o.v.any { v -> q.trim().lowercase() in v.lowercase() } } }
        // Una sola lista desplazable: cabecera, buscador, dictado, «lo de siempre» y la cuadrícula (antes solo se desplazaba la cuadrícula)
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp, 4.dp, 12.dp, 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val full: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> androidx.compose.foundation.lazy.grid.GridItemSpan = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
            item(span = full) {
                ExpandableSearch(q, { q = it }, "Buscar: leche, pan, pañales…") {
                    Text("Toca lo que necesites · mantén pulsado para cambiar el icono", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            if (q.isBlank()) needs?.let { all -> item(span = full) { VoiceListCard(all) { items -> items.forEach { (nd, qty, d) -> vm.cart.addNeed(nd, d, qty) }; added(productos(items.sumOf { it.second })) } } }
            if (usual.isNotEmpty() && q.isBlank()) item(span = full) {
                Column {
                    Text("Lo de siempre", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(usual.size) { i -> val it = usual[i]
                            AssistChip({ needs?.firstOrNull { nd -> nd.id == it.needId }?.let { nd -> vm.cart.addNeed(nd, it.detail.orEmpty(), it.qty); added("${it.qty} × ${it.name}") } },
                                { Text("${it.name}${it.detail?.let { d -> " · $d" } ?: ""}", maxLines = 1) }, leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) })
                        }
                    }
                }
            }
            if (n == null) item(span = full) { Loading() }
            else if (n.isEmpty()) item(span = full) { Message(Icons.Filled.SearchOff, "No lo encuentro", "Usa «Otra cosa» o escríbelo en la lista de arriba.") }
            else items(n, key = { it.id }) { need ->
                val inCart = lines.values.filter { it.needId == need.id }.sumOf { it.qty }
                val tileColor by androidx.compose.animation.animateColorAsState(if (inCart > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, label = "tile")
                val pop by androidx.compose.animation.core.animateFloatAsState(if (inCart > 0) 1f else 0f, Motion.bouncy(), label = "pop")
                val custom by es.unocerobits.tlotraigo.data.NeedIcons.map.collectAsState()
                Surface(Modifier.animateItem().graphicsLayer { scaleX = 1f + pop * 0.03f; scaleY = 1f + pop * 0.03f }.clip(MaterialTheme.shapes.large)
                    .combinedClickable(onClick = { sel = need }, onLongClick = { iconFor = need }),
                    shape = MaterialTheme.shapes.large, color = tileColor) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(AppIcons.vector(custom[need.id] ?: need.icon), null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(need.name, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Text("≈ ${eur(need.estPrice)} / ${need.unitLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (inCart > 0) Text("En la cesta: $inCart", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
    // Mantener pulsado un artículo: cambiar su icono (solo en este móvil)
    val ctxI = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { es.unocerobits.tlotraigo.data.NeedIcons.load(ctxI) }
    iconFor?.let { nd -> IconPickerDialog(es.unocerobits.tlotraigo.data.NeedIcons.map.value[nd.id] ?: nd.icon, { k ->
        es.unocerobits.tlotraigo.data.NeedIcons.set(ctxI, nd.id, k); iconFor = null; added("icono de ${nd.name} cambiado".removePrefix("")) }) { iconFor = null } }
    sel?.let { nd -> NeedDialog(nd, { d, qq -> vm.cart.addNeed(nd, d, qq); added("$qq × ${nd.name}"); sel = null }) { sel = null } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NeedDialog(n: Need, onAdd: (String, Int) -> Unit, onDismiss: () -> Unit) {
    val picks = remember { mutableStateMapOf<String, String>() }
    var brand by remember { mutableStateOf("Cualquiera") }
    var note by remember { mutableStateOf("") }
    var qty by remember { mutableIntStateOf(1) }
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(AppIcons.vector(n.icon), null) }, title = { Text(n.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                n.options.forEach { o ->
                    Text(o.t, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { o.v.forEach { v -> FilterChip(picks[o.t] == v, { picks[o.t] = v }, { Text(v) }) } }
                }
                Text("Marca", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Cualquiera", "La más barata", "Marca blanca", "La de siempre").forEach { v -> FilterChip(brand == v, { brand = v }, { Text(v) }) } }
                OutlinedTextField(note, { note = it.take(120) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("¿Algo más? (opcional)") },
                    placeholder = { Text(if (n.options.isEmpty()) "Escribe qué necesitas: ej. pilas AA, velas…" else "Ej.: marca Pascual, que no esté muy maduro") }, shape = MaterialTheme.shapes.medium)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Cantidad", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    QtyStepper(qty, { if (qty > 1) qty-- }, { qty++ })
                }
                Text("Precio orientativo: ${eur(n.estPrice * qty)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button({ onAdd((n.options.mapNotNull { picks[it.t] } + listOfNotNull(brand.takeIf { it != "Cualquiera" }, note.trim().ifBlank { null })).joinToString(" · "), qty) },
                enabled = n.options.all { picks[it.t] != null } && (n.options.isNotEmpty() || note.trim().length >= 3)) { Text("Añadir a la cesta") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } })
}

// =================== Ticket de compra (repartidor) ===================
/** Foto del ticket + total (obligatorio) y precio real de cada línea (opcional). Una vez guardado no se puede cambiar. */
@Composable
fun TicketSection(o: Order, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var file by remember { mutableStateOf<File?>(null) }
    var shot by remember { mutableStateOf<Uri?>(null) }
    val reals = remember { mutableStateMapOf<Long, String>() }
    var total by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var askSave by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) shot = file?.let { Uri.fromFile(it) } }
    fun takePhoto() {
        val dir = File(ctx.cacheDir, "tickets").apply { mkdirs() }
        val f = File(dir, "ticket_${o.id}_${System.currentTimeMillis()}.jpg"); file = f
        cam.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f))
    }
    val sumReals = reals.values.mapNotNull { parseNum(it) }.sum()
    var swap by remember { mutableStateOf<OrderItem?>(null) }
    swap?.let { item -> SubstituteDialog(item, { swap = null; onSaved() }) { swap = null } }
    Surface(Modifier.fillMaxWidth().padding(top = 10.dp), color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(14.dp)) {
            Text("🧾 Ticket de compra (obligatorio)", style = MaterialTheme.typography.titleMedium)
            Text("Apunta el precio real de cada cosa si quieres (así el cliente ve qué salió más caro o barato), haz la foto y escribe el total.",
                style = MaterialTheme.typography.bodySmall)
            o.items.forEach { it ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    IconButton({ swap = it }, Modifier.size(40.dp)) { Icon(Icons.Filled.SwapHoriz, "No hay ${it.name}: cambiar o quitar", tint = MaterialTheme.colorScheme.primary) }
                    Column(Modifier.weight(1f)) {
                        Text("${it.qty} × ${it.name}", style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (it.unavailable) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                        when {
                            it.unavailable -> Text("❌ No había · quitado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            it.substitute != null -> Text("🔁 Cambiado por: ${it.substitute}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            else -> Text("Estimado ${eur(it.unitPrice * it.qty)}${it.detail?.let { d -> " · $d" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(reals[it.id].orEmpty(), { v -> reals[it.id] = v.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7) },
                        Modifier.width(110.dp), placeholder = { Text("Real") }, suffix = { Text("€") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
            if (sumReals > 0) TextButton({ total = fmtNum(sumReals) }) { Text("Usar la suma como total (${eur(sumReals)})") }
            shot?.let { AsyncImage(it, null, Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton({ takePhoto() }) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text(if (shot == null) "Foto" else "Repetir") }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(total, { total = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7) }, Modifier.weight(1f), label = { Text("Total ticket") },
                    suffix = { Text("€") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            parseNum(total)?.let { t -> val d = t - o.subtotal
                Text(if (kotlin.math.abs(d) < 0.01) "Igual que lo estimado" else "${if (d > 0) "▲ " + eur(d) + " más caro" else "▼ " + eur(-d) + " más barato"} que lo estimado (${eur(o.subtotal)})",
                    style = MaterialTheme.typography.bodySmall, color = if (d > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            ErrorText(err)
            msg?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(6.dp))
            BigButton("Guardar ticket", { askSave = true }, enabled = shot != null && parseNum(total) != null, loading = busy, icon = Icons.Filled.Save)
        }
    }
    if (askSave) AlertDialog(onDismissRequest = { askSave = false }, title = { Text("¿Guardar el ticket?") },
        text = { Text("Total: ${eur(parseNum(total) ?: 0.0)}. Una vez guardado no se puede cambiar y el cliente recibirá el importe para pagar.") },
        confirmButton = { Button({
            askSave = false
            scope.launch {
                busy = true; err = null; msg = null
                try {
                    val uri = shot ?: throw ApiException("Haz primero la foto del ticket")
                    val bytes = withContext(Dispatchers.IO) { ImageUtil.prepare(ctx, uri, 1600) }
                    val path = Locator.repo.uploadTicket(o.id, bytes)
                    val r = Locator.repo.setTicket(o.id, path, parseNum(total)!!, reals.mapNotNull { (k, v) -> parseNum(v)?.let { k to it } }.toMap())
                    if (r["over_max"]?.toString() == "true") msg = "El total supera el máximo orientativo. Avisa al cliente por teléfono."
                    onSaved()
                } catch (e: ApiException) { err = e.message }
                busy = false
            }
        }) { Text("Sí, guardar") } }, dismissButton = { TextButton({ askSave = false }) { Text("Revisar") } })
}

/** Lista de productos con lo estimado y, si se conoce, lo real (▲ más caro / ▼ más barato). */
@Composable
fun ItemsWithDiff(o: Order) {
    o.items.forEach { i ->
        val est = i.unitPrice * i.qty
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${i.qty} ×", Modifier.width(40.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(i.name, style = MaterialTheme.typography.bodyLarge, textDecoration = if (i.unavailable) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                i.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (i.unavailable) Text("No había en el súper", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                i.substitute?.let { Text("🔁 Cambiado por: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            }
            Column(horizontalAlignment = Alignment.End) {
                val r = i.realTotal
                if (r == null) Text(eur(est), style = MaterialTheme.typography.bodyLarge)
                else {
                    Text(eur(r), style = MaterialTheme.typography.bodyLarge)
                    val d = r - est
                    if (kotlin.math.abs(d) >= 0.01) Text((if (d > 0) "▲ +" else "▼ −") + eur(kotlin.math.abs(d)), style = MaterialTheme.typography.labelSmall,
                        color = if (d > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    o.ticketDiff?.let { d -> if (kotlin.math.abs(d) >= 0.01) Surface(Modifier.fillMaxWidth().padding(top = 6.dp), shape = MaterialTheme.shapes.small,
        color = if (d > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer) {
        Text("La compra salió ${eur(kotlin.math.abs(d))} ${if (d > 0) "más cara" else "más barata"} que lo estimado (${eur(o.subtotal)} → ${eur(o.ticketTotal!!)}).",
            Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
    } }
}

// =================== Factura: visor en la app y PDF ===================
@Composable
fun InvoiceDialog(inv: Invoice, o: Order, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    androidx.compose.ui.window.Dialog(onDismiss, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).widthIn(max = 640.dp).fillMaxHeight(.9f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text("Factura", style = MaterialTheme.typography.headlineMedium)
                Text("Nº ${inv.number} · ${prettyDate(inv.issuedAt)}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Text("Emisor", style = MaterialTheme.typography.titleSmall); Text("${inv.issuer["name"]} · NIF ${inv.issuer["nif"]}\n${inv.issuer["address"]}", style = MaterialTheme.typography.bodySmall)
                Text("Cliente", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)); Text("${inv.customerName.orEmpty()}\n${inv.customerAddress.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                SectionTitle("Productos")
                ItemsWithDiff(o)
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                @Composable fun r(a: String, v: String, strong: Boolean = false) = Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(a, Modifier.weight(1f), style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
                    Text(v, style = if (strong) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium) }
                r("Compra en el súper (suplido, sin IVA de la plataforma)", eur(inv.suplidos))
                r("Servicio, gestión y envío — base", eur(inv.base))
                r("IVA ${inv.vatRate.toInt()} %", eur(inv.vat))
                r("TOTAL", eur(inv.total), true)
                if (o.refundAmount > 0) r("Reembolsado", "−" + eur(o.refundAmount))
                Text("El suplido es lo pagado al supermercado en tu nombre (art. 78.Tres.3º Ley del IVA); su factura es el ticket.", Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                BigButton("Abrir en PDF", { InvoicePdf.open(ctx, inv, o, share = false) }, icon = Icons.Filled.PictureAsPdf)
                Spacer(Modifier.height(8.dp))
                BigButton("Compartir", { InvoicePdf.open(ctx, inv, o, share = true) }, tonal = true, icon = Icons.Filled.Share)
                TextButton(onDismiss, Modifier.align(Alignment.CenterHorizontally)) { Text("Cerrar") }
            }
        }
    }
}

object InvoicePdf {
    fun open(ctx: Context, inv: Invoice, o: Order, share: Boolean) {
        val doc = PdfDocument(); val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()); val c = page.canvas
        val p = Paint().apply { textSize = 10.5f; isAntiAlias = true }; val b = Paint(p).apply { typeface = Typeface.DEFAULT_BOLD }
        val small = Paint(p).apply { textSize = 8.5f; color = android.graphics.Color.DKGRAY }
        val h = Paint(b).apply { textSize = 22f; color = android.graphics.Color.rgb(46, 107, 87) }
        val right = Paint(p).apply { textAlign = Paint.Align.RIGHT }; val rightB = Paint(b).apply { textAlign = Paint.Align.RIGHT }
        var y = 56f
        fun line(t: String, paint: Paint = p, x: Float = 48f, dy: Float = 15f) { c.drawText(t.take(95), x, y, paint); y += dy }
        fun row(a: String, v: String, bold: Boolean = false) { c.drawText(a.take(80), 48f, y, if (bold) b else p); c.drawText(v, 547f, y, if (bold) rightB else right); y += 16f }
        line("FACTURA", h, dy = 24f); line("Nº ${inv.number}   ·   ${prettyDate(inv.issuedAt)}", b, dy = 22f)
        line("EMISOR", b); line("${inv.issuer["name"]}  ·  NIF ${inv.issuer["nif"]}"); line(inv.issuer["address"] ?: ""); line(inv.issuer["email"] ?: "", dy = 20f)
        line("CLIENTE", b); line(inv.customerName ?: ""); line(inv.customerAddress ?: ""); line("Pedido nº ${o.id}", dy = 22f)
        row("Productos (estimado → real)", "Importe", true)
        o.items.forEach { i -> val est = i.unitPrice * i.qty
            val d = i.realTotal?.let { it - est }
            row("${i.qty} × ${i.name}${i.detail?.let { " · $it" } ?: ""}" + (d?.takeIf { kotlin.math.abs(it) >= 0.01 }?.let { if (it > 0) "  (+${eur(it)})" else "  (−${eur(-it)})" } ?: ""), eur(i.realTotal ?: est)) }
        o.ticketDiff?.takeIf { kotlin.math.abs(it) >= 0.01 }?.let { line("La compra salió ${eur(kotlin.math.abs(it))} ${if (it > 0) "más cara" else "más barata"} que lo estimado (${eur(o.subtotal)}).", small) }
        y += 8; c.drawLine(48f, y - 10, 547f, y - 10, p)
        row("Compra en supermercado por cuenta del cliente (suplido*)", eur(inv.suplidos))
        row("Servicio de compra, gestión y entrega — base imponible", eur(inv.base))
        row("IVA ${inv.vatRate.toInt()} %", eur(inv.vat))
        row("TOTAL", eur(inv.total), true)
        if (o.refundAmount > 0) row("Reembolsado", "−" + eur(o.refundAmount))
        y += 14
        line("* Pagado al supermercado en nombre del cliente (art. 78.Tres.3º Ley 37/1992): no forma parte de la base imponible.", small, dy = 12f)
        line("  El ticket del supermercado está disponible en la app.   Huella: ${inv.hash.take(24)}…", small)
        doc.finishPage(page)
        val dir = File(ctx.cacheDir, "facturas").apply { mkdirs() }
        val f = File(dir, "Factura_${inv.number}.pdf"); f.outputStream().use { doc.writeTo(it) }; doc.close()
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        val i = if (share) Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Compartir factura")
        else Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: android.content.ActivityNotFoundException) { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Abrir factura").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/** Justificante del pedido (servicios gratuitos): productos, ticket, fechas y repartidor. Sirve como trazabilidad para la entidad. */
object ReceiptPdf {
    fun open(ctx: Context, o: Order) {
        val doc = PdfDocument(); val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()); val c = page.canvas
        val p = Paint().apply { textSize = 10.5f; isAntiAlias = true }; val b = Paint(p).apply { typeface = Typeface.DEFAULT_BOLD }
        val h = Paint(b).apply { textSize = 20f; color = android.graphics.Color.rgb(46, 107, 87) }; val right = Paint(p).apply { textAlign = Paint.Align.RIGHT }
        var y = 56f
        fun line(t: String, paint: Paint = p, dy: Float = 15f) { c.drawText(t.take(95), 48f, y, paint); y += dy }
        line("JUSTIFICANTE DE PEDIDO", h, 24f); line("Pedido nº ${o.id} · T lo traigo", b, 20f)
        line("Fecha del pedido: ${prettyDate(o.createdAt)}"); line("Estado: ${OrderStatus.of(o.status).label}${if (o.status == "entregado") " · " + prettyDate(o.updatedAt) else ""}")
        line("Usuario: ${o.customer?.fullName.orEmpty()}"); line("Dirección: ${o.address}"); line("Repartidor: ${o.driver?.fullName ?: "—"}", dy = 22f)
        line("Productos", b)
        o.items.forEach { i -> c.drawText("${i.qty} × ${i.name}${i.detail?.let { " · $it" } ?: ""}".take(80), 48f, y, p)
            (i.realTotal ?: (i.unitPrice * i.qty)).let { c.drawText(eur(it) + if (i.realTotal == null) " (est.)" else "", 547f, y, right) }; y += 15f }
        y += 8; line("Importe del ticket del supermercado: ${o.ticketTotal?.let { eur(it) } ?: "—"}", b)
        if (o.amount > (o.ticketTotal ?: 0.0)) line("Otros importes: ${eur(o.amount - (o.ticketTotal ?: 0.0))}")
        o.claimedAt?.let { line("Reclamación: ${prettyDate(it)}") }
        y += 10; line("Documento de trazabilidad generado por la app. El ticket original del supermercado está disponible en el pedido.", Paint(p).apply { textSize = 8.5f })
        doc.finishPage(page)
        val dir = File(ctx.cacheDir, "facturas").apply { mkdirs() }; val f = File(dir, "Justificante_pedido_${o.id}.pdf"); f.outputStream().use { doc.writeTo(it) }; doc.close()
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Justificante").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

// =================== Reembolso y reclamación ===================
@Composable
fun RefundDialog(o: Order, onDone: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val max = (o.amount - o.serviceFee - o.gestionFee - o.refundAmount).coerceAtLeast(0.0)
    AlertDialog(onDismissRequest = { onDone(null) }, icon = { Icon(Icons.Filled.CurrencyExchange, null) }, title = { Text("Solicitar reembolso") },
        text = { Column {
            Text("Cuéntanos qué ha pasado (producto en mal estado, que faltaba…). La tarifa de servicio y gestión no se reembolsa. Máximo: ${eur(max)}.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(reason, { reason = it.take(1000) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Motivo") }, minLines = 3)
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(7) }, Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Importe (opcional)") }, suffix = { Text("€") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            ErrorText(err)
        } },
        confirmButton = { Button({ scope.launch { try { Locator.repo.requestRefund(o.id, reason, parseNum(amount)); onDone("Solicitud enviada. Te responderemos en un máximo de 7 días.") } catch (e: ApiException) { err = e.message } } },
            enabled = reason.trim().length >= 3) { Text("Enviar") } },
        dismissButton = { TextButton({ onDone(null) }) { Text("Cancelar") } })
}

@Composable
fun ComplaintDialog(vm: AppViewModel, orderId: Long?, onDone: (String?) -> Unit) {
    val p by vm.profile.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(p?.fullName.orEmpty()) }
    var contact by remember { mutableStateOf(p?.phone.orEmpty()) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { onDone(null) }, icon = { Icon(Icons.Filled.Feedback, null) }, title = { Text("Hoja de reclamaciones") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Recibirás un número de reclamación y te responderemos en un plazo máximo de 10 días hábiles. También puedes escribir a tlotraigo@unocerobits.com o usar la hoja oficial de la Junta de Castilla y León.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Nombre y apellidos") }, singleLine = true)
            OutlinedTextField(contact, { contact = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Teléfono o correo") }, singleLine = true)
            OutlinedTextField(subject, { subject = it.take(120) }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Asunto") }, singleLine = true)
            OutlinedTextField(body, { body = it.take(4000) }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Hechos y lo que solicitas") }, minLines = 4)
            ErrorText(err)
        } },
        confirmButton = { Button({ scope.launch { try { val n = Locator.repo.submitComplaint(orderId, name, contact, subject, body); onDone("Reclamación registrada con el número $n.") } catch (e: ApiException) { err = e.message } } },
            enabled = name.isNotBlank() && contact.isNotBlank() && subject.isNotBlank() && body.length >= 10) { Text("Presentar") } },
        dismissButton = { TextButton({ onDone(null) }) { Text("Cancelar") } })
}

/** El repartidor no encuentra un producto: lo cambia por otro o lo quita. El cliente recibe un mensaje en el chat (con notificación). */
@Composable
fun SubstituteDialog(item: OrderItem, onDone: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun send(unavailable: Boolean) = scope.launch {
        busy = true; err = null
        try { Locator.repo.substituteItem(item.id, text.takeIf { !unavailable }, unavailable); onDone() } catch (e: ApiException) { err = e.message }
        busy = false
    }
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Filled.SwapHoriz, null) }, title = { Text("No hay «${item.name}»") },
        text = { Column {
            item.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("Escribe por qué lo cambias o quítalo. El cliente recibirá un aviso y podrá contestarte por el chat.", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(text, { text = it.take(200) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Lo cambio por…") },
                placeholder = { Text("Ej.: leche semidesnatada de otra marca") }, singleLine = true)
            ErrorText(err)
        } },
        confirmButton = { Button({ send(false) }, enabled = text.isNotBlank() && !busy) { Text("Cambiar y avisar") } },
        dismissButton = { Row { TextButton({ send(true) }, enabled = !busy) { Text("Quitar (no hay)", color = MaterialTheme.colorScheme.error) }; TextButton(onDismiss) { Text("Cancelar") } } })
}
