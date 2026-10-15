package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.ui.components.*
import es.unocerobits.tlotraigo.ui.theme.LocalSenior
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 6..13 -> "Buenos días"; in 14..20 -> "Buenas tardes"; else -> "Buenas noches"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatalogScreen(vm: AppViewModel, onOpenCart: () -> Unit, onNeeds: () -> Unit = {}) {
    val profile by vm.profile.collectAsState()
    val cartLines by vm.cart.lines.collectAsState()
    val senior = LocalSenior.current
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>?>(null) }
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var lastOrder by remember { mutableStateOf<Order?>(null) }
    var repeating by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(reload) {
        runCatching { categories = Locator.repo.categories(); CategoryIcons.map = categories.associate { it.id to it.icon } }
        runCatching { lastOrder = Locator.repo.myOrders().firstOrNull { it.status == "entregado" } }
    }
    LaunchedEffect(selected, query, reload) {
        if (query.isNotEmpty()) delay(350)
        err = null
        try { products = Locator.repo.products(selected, query) } catch (e: ApiException) { err = e.message; products = emptyList() }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
                Text("${greeting()}${profile?.firstName?.let { if (it.isNotBlank()) ", $it" else "" } ?: ""}",
                    style = MaterialTheme.typography.headlineLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text(vm.townName(profile?.townId).let { if (it.isBlank()) "Entrega a domicilio" else "Te lo llevamos a $it" }, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Buscar: leche, pan, aceite…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = CircleShape,
                    trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Filled.Close, "Borrar búsqueda") } },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }
        item { OrgWelcome(vm) }
        item { val openList = LocalOpenList.current; ListsRow(vm) { id -> openList(id, emptyList()) } }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).clickable(onClick = onNeeds), shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Checklist, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Haz tu lista fácil", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Pan, leche, fruta… y dinos cómo lo quieres", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Text("Precios orientativos: pagarás lo que marque el ticket del súper.", Modifier.padding(horizontal = 22.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        lastOrder?.let { lo ->
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer), shape = MaterialTheme.shapes.large,
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Replay, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Repetir mi última compra", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("${productos(lo.items.sumOf { it.qty })} · ${eur(lo.subtotal)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Button({
                            scope.launch {
                                repeating = true
                                runCatching { Locator.repo.repeatOrder(lo); onOpenCart() }
                                repeating = false
                            }
                        }, enabled = !repeating, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)) {
                            Text(if (repeating) "…" else "Repetir")
                        }
                    }
                }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { CategoryTile("Todo", Icons.Filled.Apps, selected == null) { selected = null } }
                items(categories, key = { it.id }) { c -> CategoryTile(c.name, categoryIcon(c.icon), selected == c.id) { selected = c.id } }
            }
        }
        item { ErrorText(err) }
        when {
            products == null -> item { Loading() }
            products!!.isEmpty() && err == null -> item {
                Message(Icons.Filled.SearchOff, "No encontramos productos", "Prueba con otra palabra o elige otra sección.", "Ver todo") { query = ""; selected = null }
            }
            else -> items(products!!, key = { it.id }) { p ->
                ProductRow(p, cartLines[p.id]?.qty ?: 0, senior, Modifier.animateItem().clickable { detail = p }, onAdd = { vm.cart.add(p) }, onRemove = { vm.cart.remove(p) })
            }
        }
    }
    detail?.let { p -> ProductSheet(p, cartLines[p.id]?.qty ?: 0, { vm.cart.add(p) }, { vm.cart.remove(p) }) { detail = null } }
}

@Composable
private fun CategoryTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, label = "s")
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.width(((if (LocalSenior.current) 96 else 86) * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceIn(1f, 1.6f)).dp).scale(scale).clip(MaterialTheme.shapes.medium)
            .background(bg).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(32.dp), tint = if (selected) fg else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun ProductThumb(p: Product, size: Int) {
    Box(Modifier.size(size.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f)), Alignment.Center) {
        if (p.imageUrl != null) AsyncImage(p.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(productIcon(p, CategoryIcons.map[p.categoryId]), null, Modifier.size((size * .5f).dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProductRow(p: Product, qty: Int, senior: Boolean, modifier: Modifier, onAdd: () -> Unit, onRemove: () -> Unit) {
    val unitLabel = when (p.unit) { "kg" -> "el kilo"; "pack" -> "el paquete"; else -> "la unidad" }
    Surface(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductThumb(p, if (senior) 76 else 64)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                p.brand?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(eur(p.price), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text(" $unitLabel aprox.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (senior) SpeakButton("${p.name}. ${eur(p.price)} $unitLabel")
                AnimatedContent(qty > 0, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "add") { inCart ->
                    if (inCart) QtyStepper(qty, onRemove, onAdd)
                    else FilledIconButton(onAdd, Modifier.size(if (senior) 56.dp else 48.dp)) { Icon(Icons.Filled.AddShoppingCart, "Añadir ${p.name}") }
                }
            }
        }
    }
}

/** Iconos de categoría en memoria para usarlos como icono por defecto del producto. */
object CategoryIcons { @Volatile var map: Map<Long, String> = emptyMap() }

/** Ficha del producto: precio con IVA desglosado, alérgenos e ingredientes (Reglamento UE 1169/2011, art. 14). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSheet(p: Product, qty: Int, onAdd: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductThumb(p, 96); Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleLarge)
                    p.brand?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (p.ageRestricted) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape, modifier = Modifier.padding(top = 6.dp)) {
                        Text("+18", Modifier.padding(horizontal = 10.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(eur(p.price), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("IVA incluido (${p.vatRate.toInt()} %): base ${eur(p.basePrice)} + IVA ${eur(p.vatAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Alérgenos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text(p.allergens ?: "Consulta el envase o pregunta al repartidor antes de consumirlo.", style = MaterialTheme.typography.bodyLarge)
            p.ingredients?.let { Text("Ingredientes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)); Text(it, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(20.dp))
            if (qty > 0) Row(verticalAlignment = Alignment.CenterVertically) { Text("En tu cesta:", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); QtyStepper(qty, onRemove, onAdd) }
            else BigButton("Añadir a la cesta", onAdd, icon = Icons.Filled.AddShoppingCart)
        }
    }
}

/** Mensaje y teléfono de la entidad que presta el servicio en el pueblo del usuario. */
@Composable
private fun OrgWelcome(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val orgId = vm.profile.collectAsState().value?.orgId ?: return
    var org by remember { mutableStateOf<Organization?>(null) }
    var notices by remember { mutableStateOf<List<OrgNotice>>(emptyList()) }
    LaunchedEffect(orgId) {
        org = runCatching { Locator.repo.organization(orgId) }.getOrNull()
        val weekAgo = java.time.OffsetDateTime.now().minusDays(7)
        notices = runCatching { Locator.repo.orgNotices(orgId) }.getOrDefault(emptyList()).filter { runCatching { java.time.OffsetDateTime.parse(it.createdAt).isAfter(weekAgo) }.getOrDefault(false) }.take(3)
    }
    notices.forEach { n -> Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp), shape = MaterialTheme.shapes.medium,
        color = if (n.kind == "cierre") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer) {
        Text(n.body, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) } }
    val o = org ?: return

    val bc = o.brandColor?.let { brand(it) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), colors = CardDefaults.cardColors(
        if (!o.acceptingOrders) MaterialTheme.colorScheme.errorContainer else bc ?: MaterialTheme.colorScheme.surfaceContainerHigh,
        if (!o.acceptingOrders) MaterialTheme.colorScheme.onErrorContainer else if (bc != null) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                o.logoUrl?.let { coil.compose.AsyncImage(it, null, Modifier.size(44.dp).clip(MaterialTheme.shapes.small)); Spacer(Modifier.width(10.dp)) }
                Column { Text("Servicio de", style = MaterialTheme.typography.labelMedium); Text(o.name, style = MaterialTheme.typography.titleMedium) }
            }
            if (!o.acceptingOrders) Text("Ahora mismo no se aceptan pedidos.", style = MaterialTheme.typography.bodyMedium)
            val days = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
            Text("Reparto: ${if (o.deliveryDays.size == 7) "todos los días" else o.deliveryDays.sorted().joinToString(", ") { days[it - 1] }} · de ${o.openHour}:00 a ${o.closeHour}:00",
                style = MaterialTheme.typography.bodySmall)
            o.closedDates.filter { it >= java.time.LocalDate.now().toString() }.sorted().take(3).takeIf { it.isNotEmpty() }?.let { cd ->
                Text("Sin reparto: " + cd.joinToString(", ") { prettyDay(it) }, style = MaterialTheme.typography.bodySmall) }
            o.welcomeMessage?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            o.publicPhone?.takeIf { it.isNotBlank() }?.let { p -> TextButton({ ctx.dial(p) }) { Icon(Icons.Filled.Call, null); Spacer(Modifier.width(6.dp)); Text("Llamar: $p") } }
        }
    }
}
