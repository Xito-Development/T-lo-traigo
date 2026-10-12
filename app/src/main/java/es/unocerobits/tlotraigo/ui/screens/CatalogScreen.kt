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
fun CatalogScreen(vm: AppViewModel, onOpenCart: () -> Unit) {
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
                                runCatching {
                                    val qty = lo.items.mapNotNull { i -> i.productId?.let { it to i.qty } }.toMap()
                                    val ps = Locator.repo.productsByIds(qty.keys)
                                    vm.cart.replaceWith(ps.map { it to qty.getValue(it.id) })
                                    onOpenCart()
                                }
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
                ProductRow(p, cartLines[p.id]?.qty ?: 0, senior, Modifier.animateItem(), onAdd = { vm.cart.add(p) }, onRemove = { vm.cart.remove(p) })
            }
        }
    }
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
                    Text(" $unitLabel · IVA incl.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
