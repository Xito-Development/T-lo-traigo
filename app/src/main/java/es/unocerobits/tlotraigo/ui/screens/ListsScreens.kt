package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.launch

/** Abre una lista (id) o crea una nueva (-1) con artículos iniciales. */
val LocalOpenList = staticCompositionLocalOf<(Long, List<ListItem>) -> Unit> { { _, _ -> } }

private val listColors = listOf("#2E6B57", "#1F5F8B", "#7A3E9D", "#B23A48", "#C4661F", "#3C5A14", "#00796B", "#5D4037")
private val listEmojis = listOf("🛒", "🥖", "🥛", "🍎", "🥦", "🧀", "🍪", "🧴", "🐾", "👵", "🏠", "⭐", "🎉", "☕", "🥩", "🐟")
private fun hex(c: String) = runCatching { Color(android.graphics.Color.parseColor(c)) }.getOrDefault(Color(0xFF2E6B57))

/** Portada de una lista: degradado de su color, emoji grande y botón redondo para comprarla entera. */
@Composable
fun ListCover(l: ShoppingList, size: androidx.compose.ui.unit.Dp, onOpen: () -> Unit, onBuy: () -> Unit) {
    val c = hex(l.color)
    var pressed by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (pressed) 0.94f else 1f, Motion.bouncy(), label = "cover")
    Column(Modifier.width(size).graphicsLayer { scaleX = sc; scaleY = sc }) {
        Box(Modifier.size(size).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.65f), Color(0xFF1B2428))))
            .clickable { pressed = true; onOpen() }) {
            Text(l.emoji, Modifier.align(Alignment.Center), fontSize = (size.value * 0.36f).sp)
            Text("${l.items.sumOf { it.qty }}", Modifier.align(Alignment.TopStart).padding(10.dp).background(Color.Black.copy(alpha = .25f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
            FilledIconButton(onBuy, Modifier.align(Alignment.BottomEnd).padding(8.dp).size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF2B544), contentColor = Color(0xFF1B2428))) {
                Icon(Icons.Filled.AddShoppingCart, "Comprar la lista ${l.name}")
            }
        }
        Text(l.name, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (l.repeatDays.isNotEmpty()) "🔁 " + repeatText(l.repeatDays) else productos(l.items.sumOf { it.qty }), style = MaterialTheme.typography.bodySmall,
            color = if (l.repeatDays.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

/** Fila «Mis listas» de la pantalla de compra. */
@Composable
fun ListsRow(vm: AppViewModel, onOpen: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    val celebrate = LocalCelebrate.current
    var lists by remember { mutableStateOf<List<ShoppingList>?>(null) }
    LaunchedEffect(Unit) { lists = runCatching { Locator.repo.lists() }.getOrDefault(emptyList()) }
    Column(Modifier.padding(vertical = 8.dp)) {
        val today = java.time.LocalDate.now()
        lists?.filter { l -> today.dayOfWeek.value in l.repeatDays && l.lastOrderedAt?.take(10) != today.toString() }?.forEach { due ->
            val c = hex(due.color)
            Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(c.copy(alpha = 0.14f))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(due.emoji, fontSize = 28.sp); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Hoy toca tu compra recurrente", style = MaterialTheme.typography.labelMedium, color = c)
                        Text("«${due.name}» · ${productos(due.items.sumOf { it.qty })}", style = MaterialTheme.typography.titleSmall) }
                    FilledTonalButton({ scope.launch { val miss = runCatching { Locator.repo.listToCart(due) }.getOrDefault(0)
                        lists = lists?.map { if (it.id == due.id) it.copy(lastOrderedAt = today.toString()) else it }
                        celebrate(Celebration("«${due.name}» en la cesta", if (miss > 0) "$miss artículo(s) ya no están disponibles" else "Revísala y haz el pedido", due.emoji)) } }) { Text("Añadir") }
                }
            }
        }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Mis listas", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton({ onOpen(-1) }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Nueva") }
        }
        val l = lists
        if (l != null && l.isEmpty()) Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onOpen(-1) }, shape = MaterialTheme.shapes.large) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🛒", fontSize = 32.sp); Spacer(Modifier.width(12.dp))
                Column { Text("Crea tu primera lista", style = MaterialTheme.typography.titleMedium)
                    Text("Guarda tu compra de siempre y repítela con un toque", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        else LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(l ?: emptyList(), key = { it.id }) { list ->
                Box(Modifier.animateItem()) {
                    ListCover(list, 128.dp, { onOpen(list.id) }) {
                        scope.launch {
                            val miss = runCatching { Locator.repo.listToCart(list) }.getOrDefault(0)
                            celebrate(Celebration("«${list.name}» en la cesta", if (miss > 0) "$miss artículo(s) ya no están disponibles" else "Revísala y haz el pedido", list.emoji))
                        }
                    }
                }
            }
        }
    }
}

/** Detalle y edición de una lista. id = -1 para crear una nueva (con [initial] si viene de la cesta o de un pedido). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListDetailScreen(vm: AppViewModel, id: Long, initial: List<ListItem> = emptyList(), onBack: () -> Unit, onCart: () -> Unit) {
    val scope = rememberCoroutineScope()
    val celebrate = LocalCelebrate.current
    var list by remember { mutableStateOf<ShoppingList?>(if (id < 0) ShoppingList(name = "", items = initial) else null) }
    var needs by remember { mutableStateOf<List<Need>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var addText by remember { mutableStateOf("") }
    LaunchedEffect(id) {
        needs = runCatching { Locator.repo.needs() }.getOrDefault(emptyList())
        if (id > 0) list = runCatching { Locator.repo.lists().firstOrNull { it.id == id } }.getOrNull()
    }
    val l = list ?: return Loading()
    val c = hex(l.color)
    fun save(then: () -> Unit = {}) = scope.launch {
        saving = true; err = null
        try { list = Locator.repo.saveList(l.copy(name = l.name.ifBlank { "Mi lista" })); then() } catch (e: ApiException) { err = e.message }
        saving = false
    }
    Scaffold(topBar = {
        TopAppBar({ Text(if (id < 0) "Nueva lista" else "Lista") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            actions = { if (l.id > 0) IconButton({ askDelete = true }) { Icon(Icons.Filled.DeleteOutline, "Borrar lista") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = c, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White))
    }, bottomBar = {
        Surface(tonalElevation = 6.dp) { Row(Modifier.navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Guardar", { save { celebrate(Celebration("Lista guardada", l.name.ifBlank { "Mi lista" }, l.emoji)) } }, Modifier.weight(1f), loading = saving, tonal = true, icon = Icons.Filled.Save)
            BigButton("Comprar", { scope.launch {
                if (l.id == 0L) runCatching { list = Locator.repo.saveList(l.copy(name = l.name.ifBlank { "Mi lista" })) }
                val miss = runCatching { Locator.repo.listToCart(list!!) }.getOrDefault(0)
                celebrate(Celebration("Lista en la cesta", if (miss > 0) "$miss artículo(s) ya no están disponibles" else null, l.emoji)); onCart()
            } }, Modifier.weight(1f), enabled = l.items.isNotEmpty(), icon = Icons.Filled.AddShoppingCart)
        } }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(c, c.copy(alpha = 0.15f), Color.Transparent))).padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(c, Color(0xFF1B2428)))), Alignment.Center) {
                                AnimatedContent(l.emoji, transitionSpec = { (scaleIn(Motion.bouncy()) + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "emoji") { Text(it, fontSize = 46.sp) }
                            }
                            Spacer(Modifier.width(14.dp))
                            OutlinedTextField(l.name, { list = l.copy(name = it.take(60)) }, Modifier.weight(1f), placeholder = { Text("Nombre: «Compra semanal»") },
                                singleLine = true, textStyle = MaterialTheme.typography.titleLarge, shape = MaterialTheme.shapes.medium)
                        }
                        FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listEmojis.forEach { e -> Surface(Modifier.size(40.dp).clickable { list = l.copy(emoji = e) }, shape = CircleShape,
                                color = if (l.emoji == e) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) { Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 20.sp) } } }
                        }
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listColors.forEach { col -> Box(Modifier.size(32.dp).clip(CircleShape).background(hex(col)).clickable { list = l.copy(color = col) }, Alignment.Center) {
                                if (l.color == col) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = Color.White) } }
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("Compra recurrente", style = MaterialTheme.typography.titleSmall)
                    Text("Elige qué días te la recordamos para pedirla con un toque.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, d -> val day = i + 1; val on = day in l.repeatDays
                            val bg by animateColorAsState(if (on) c else MaterialTheme.colorScheme.surfaceContainerHigh, label = "dia")
                            Box(Modifier.size(38.dp).clip(CircleShape).background(bg).clickable { list = l.copy(repeatDays = if (on) l.repeatDays - day else l.repeatDays + day) },
                                Alignment.Center) { Text(d, color = if (on) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge) } }
                    }
                }
            }
            item { ErrorText(err) }
            item {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${productos(l.items.sumOf { it.qty })} · ≈ ${eur(l.items.sumOf { it.price * it.qty })}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    val cart by vm.cart.lines.collectAsState()
                    if (cart.isNotEmpty()) TextButton({ list = l.copy(items = mergeItems(l.items, cart.values.map { it.toListItem() })) }) { Icon(Icons.Filled.ShoppingCart, null); Spacer(Modifier.width(4.dp)); Text("Añadir mi cesta") }
                }
            }
            if (l.items.isEmpty()) item { Message(Icons.Filled.PlaylistAdd, "Lista vacía", "Escribe abajo lo que quieres o añade lo que tengas en la cesta.") }
            items(l.items.withIndex().toList(), key = { (i, it) -> "${it.productId}-${it.needId}-${it.detail}-$i" }) { (i, it) ->
                Row(Modifier.animateItem().fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.14f)), Alignment.Center) {
                        Icon(AppIcons.vector(it.icon ?: "cesta"), null, tint = c, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(it.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        it.detail?.takeIf { d -> d.isNotBlank() }?.let { d -> Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
                    }
                    QtyStepper(it.qty, { list = l.copy(items = if (it.qty <= 1) l.items.filterIndexed { j, _ -> j != i } else l.items.mapIndexed { j, x -> if (j == i) x.copy(qty = x.qty - 1) else x }) },
                        { list = l.copy(items = l.items.mapIndexed { j, x -> if (j == i) x.copy(qty = (x.qty + 1).coerceAtMost(99)) else x }) })
                }
            }
            item {
                Card(Modifier.fillMaxWidth().padding(20.dp), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Añadir a la lista", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(addText, { addText = it }, Modifier.weight(1f).padding(top = 6.dp), placeholder = { Text("Ej.: 2 barras de pan, leche, 6 huevos") }, minLines = 1, shape = MaterialTheme.shapes.medium)
                            Spacer(Modifier.width(8.dp))
                            FilledIconButton({
                                val add = parseShoppingList(addText, needs).map { (n, q, d) -> ListItem(needId = n.id, name = n.name, detail = d, qty = q, icon = n.icon, price = n.estPrice) }
                                if (add.isNotEmpty()) { list = l.copy(items = mergeItems(l.items, add)); addText = "" }
                            }, enabled = addText.isNotBlank()) { Icon(Icons.Filled.Add, "Añadir") }
                        }
                    }
                }
            }
        }
    }
    if (askDelete) AlertDialog(onDismissRequest = { askDelete = false }, title = { Text("¿Borrar la lista?") }, text = { Text("«${l.name}» se borrará. Tus pedidos no cambian.") },
        confirmButton = { Button({ askDelete = false; scope.launch { runCatching { Locator.repo.deleteList(l.id) }; onBack() } }) { Text("Borrar") } },
        dismissButton = { TextButton({ askDelete = false }) { Text("Cancelar") } })
}

fun CartLine.toListItem() = ListItem(productId = product.id.takeIf { it > 0 }, needId = needId, name = product.name, detail = detail ?: product.brand,
    qty = qty, icon = product.icon, price = product.price)
fun OrderItem.toListItem() = ListItem(productId = productId, needId = needId, name = name, detail = detail, qty = qty, price = unitPrice)

/** Une artículos iguales sumando cantidades. */
fun mergeItems(a: List<ListItem>, b: List<ListItem>): List<ListItem> {
    val out = a.toMutableList()
    b.forEach { n -> val i = out.indexOfFirst { it.productId == n.productId && it.needId == n.needId && (it.detail ?: "") == (n.detail ?: "") }
        if (i >= 0) out[i] = out[i].copy(qty = (out[i].qty + n.qty).coerceAtMost(99)) else out += n }
    return out
}

fun repeatText(days: List<Int>): String {
    val n = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábados", "domingos")
    return when { days.size == 7 -> "todos los días"; days.sorted() == listOf(1, 2, 3, 4, 5) -> "de lunes a viernes"; else -> "los " + days.sorted().joinToString(", ") { n[it - 1].let { w -> if (w.endsWith("s")) w else w } } }
}
