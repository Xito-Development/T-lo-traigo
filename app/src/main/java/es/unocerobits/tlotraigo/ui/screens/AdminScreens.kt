package es.unocerobits.tlotraigo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class ASec(val label: String, val superOnly: Boolean = false) {
    PRODUCTOS("Productos"), TARIFAS("Tarifas"), OPINIONES("Opiniones"),
    REPARTIDORES("Repartidores", true), USUARIOS("Usuarios", true)
}

fun parseNum(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()
fun fmtNum(v: Double, dec: Int = 2): String = String.format(Locale("es", "ES"), "%.${dec}f", v)

@Composable
fun AdminHome(vm: AppViewModel, onEditProduct: (Long?) -> Unit) {
    val p by vm.profile.collectAsState()
    val isSuper = p?.isSuper == true
    var sec by rememberSaveable { mutableStateOf(ASec.PRODUCTOS) }
    Column(Modifier.fillMaxSize()) {
        Text("Administración", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
        Text(if (isSuper) "Superadministrador" else "Administrador", Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ASec.entries.filter { !it.superOnly || isSuper }) { s -> FilterChip(sec == s, { sec = s }, { Text(s.label) }) }
        }
        AnimatedContent(sec, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "asec") { s ->
            when (s) {
                ASec.PRODUCTOS -> AdminProducts(onEditProduct)
                ASec.TARIFAS -> AdminFees()
                ASec.OPINIONES -> AdminReviews()
                ASec.REPARTIDORES -> AdminDrivers(vm)
                ASec.USUARIOS -> AdminUsers()
            }
        }
    }
}

// ---------------- Productos ----------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdminProducts(onEdit: (Long?) -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    var list by remember { mutableStateOf<List<Product>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(q) {
        if (q.isNotEmpty()) kotlinx.coroutines.delay(300)
        try { list = Locator.repo.adminProducts(q); err = null } catch (e: ApiException) { err = e.message }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
        item {
            BigButton("Añadir producto nuevo", { onEdit(null) }, icon = Icons.Filled.AddCircle)
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(vertical = 10.dp), placeholder = { Text("Buscar producto") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = CircleShape)
            ErrorText(err)
        }
        val l = list
        if (l == null) item { Loading() }
        else items(l, key = { it.id }) { p ->
            Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).animateItem().clip(MaterialTheme.shapes.large).clickable { onEdit(p.id) },
                shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb(p, 52)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("PVP ${eur(p.price)} · IVA ${p.vatRate.toInt()} % · base ${eur(p.basePrice)}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!p.active) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = CircleShape) {
                        Text("Oculto", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Icon(Icons.Filled.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductEditorScreen(id: Long?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(id == null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var market by remember { mutableLongStateOf(1L) }
    var name by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var ean by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf<Long?>(null) }
    var unit by rememberSaveable { mutableStateOf("ud") }
    var priceTxt by rememberSaveable { mutableStateOf("") }
    var vat by rememberSaveable { mutableDoubleStateOf(10.0) }
    var icon by rememberSaveable { mutableStateOf<String?>(null) }
    var image by rememberSaveable { mutableStateOf<String?>(null) }
    var active by rememberSaveable { mutableStateOf(true) }
    var pickIcon by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        runCatching { categories = Locator.repo.categories() }
        runCatching { Locator.repo.supermarkets().firstOrNull()?.let { market = it.id } }
        if (id != null) try {
            Locator.repo.product(id)?.let { p ->
                name = p.name; brand = p.brand.orEmpty(); ean = p.ean.orEmpty(); cat = p.categoryId; unit = p.unit
                priceTxt = fmtNum(p.price); vat = p.vatRate; icon = p.icon; image = p.imageUrl; active = p.active; market = p.supermarketId
            }
            loaded = true
        } catch (e: ApiException) { err = e.message }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            uploading = true; err = null
            try { image = Locator.repo.uploadProductImage(withContext(Dispatchers.IO) { ImageUtil.prepare(ctx, uri) }) }
            catch (e: Exception) { err = e.message ?: "No se pudo subir la imagen" }
            uploading = false
        }
    }
    val price = parseNum(priceTxt)

    Scaffold(topBar = {
        TopAppBar({ Text(if (id == null) "Nuevo producto" else "Editar producto") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } })
    }) { pad ->
        if (!loaded) { Box(Modifier.padding(pad)) { Loading() }; return@Scaffold }
        Column(Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // Imagen / icono
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(104.dp).clip(MaterialTheme.shapes.large), Alignment.Center) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {}
                    if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(AppIcons.vector(icon ?: categories.firstOrNull { it.id == cat }?.icon), null, Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (uploading) CircularProgressIndicator()
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    OutlinedButton({ pickIcon = true }) { Icon(Icons.Filled.Category, null); Spacer(Modifier.width(6.dp)); Text("Elegir icono") }
                    OutlinedButton({ picker.launch("image/*") }, enabled = !uploading) { Icon(Icons.Filled.AddPhotoAlternate, null); Spacer(Modifier.width(6.dp)); Text("Poner foto") }
                    if (image != null) TextButton({ image = null }) { Text("Quitar foto") }
                }
            }
            Text("Si hay foto se muestra la foto; si no, el icono.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))

            SectionTitle("Datos")
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre *") }, singleLine = true, shape = MaterialTheme.shapes.medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(brand, { brand = it }, Modifier.weight(1f), label = { Text("Marca") }, singleLine = true, shape = MaterialTheme.shapes.medium)
                OutlinedTextField(ean, { ean = it.filter(Char::isLetterOrDigit).take(20) }, Modifier.weight(1f), label = { Text("EAN / código") },
                    singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Text("Sección", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c ->
                    FilterChip(cat == c.id, { cat = c.id; if (id == null) vat = c.vatRate }, { Text(c.name) },
                        leadingIcon = { Icon(AppIcons.vector(c.icon), null, Modifier.size(18.dp)) })
                }
            }
            Text("Se vende por", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ud" to "Unidad", "kg" to "Kilo", "pack" to "Paquete", "l" to "Litro").forEach { (k, l) -> FilterChip(unit == k, { unit = k }, { Text(l) }) }
            }

            SectionTitle("Precio")
            OutlinedTextField(priceTxt, { priceTxt = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8) }, Modifier.fillMaxWidth(),
                label = { Text("Precio en tienda (PVP, IVA incluido) *") }, suffix = { Text("€") }, singleLine = true,
                isError = priceTxt.isNotEmpty() && price == null, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Text("Tipo de IVA", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4.0 to "4 % básico", 10.0 to "10 % reducido", 21.0 to "21 % general").forEach { (v, l) -> FilterChip(vat == v, { vat = v }, { Text(l) }) }
            }
            Text("4 %: pan, leche, huevos, queso, fruta, verdura, legumbres, cereales y aceite de oliva · 10 %: resto de alimentos y bebidas sin alcohol · 21 %: limpieza, higiene, alcohol y no alimentarios.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            if (price != null) {
                val base = price / (1 + vat / 100)
                Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Desglose", style = MaterialTheme.typography.titleMedium)
                        BreakRow("Base imponible", base)
                        BreakRow("IVA (${vat.toInt()} %)", price - base)
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        BreakRow("PVP", price, strong = true)
                        Text("Al cliente se le suman el servicio y el envío en la cesta.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Visible en el catálogo", style = MaterialTheme.typography.titleMedium)
                    Text(if (active) "Los clientes pueden comprarlo" else "Oculto para los clientes", style = MaterialTheme.typography.bodySmall)
                }
                Switch(active, { active = it })
            }
            ErrorText(err)
            Spacer(Modifier.height(12.dp))
            BigButton("Guardar producto", {
                scope.launch {
                    busy = true; err = null
                    try {
                        Locator.repo.saveProduct(id, market, cat, name, brand, unit, price!!, vat, icon, image, ean, active)
                        onBack()
                    } catch (e: ApiException) { err = e.message }
                    busy = false
                }
            }, enabled = name.isNotBlank() && price != null && price > 0 && cat != null && !uploading, loading = busy, icon = Icons.Filled.Save)
            Spacer(Modifier.height(40.dp))
        }
    }
    if (pickIcon) IconPickerDialog(icon, { icon = it; pickIcon = false }) { pickIcon = false }
}

@Composable
private fun BreakRow(label: String, v: Double, strong: Boolean = false) = Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
    Text(label, Modifier.weight(1f), style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
    Text(eur(v), style = if (strong) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
}

// ---------------- Tarifas ----------------
@Composable
private fun NumField(label: String, value: String, suffix: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) =
    OutlinedTextField(value, { onChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8)) }, modifier, label = { Text(label) },
        suffix = { Text(suffix) }, singleLine = true, isError = parseNum(value) == null, shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

@Composable
private fun AdminFees() {
    val scope = rememberCoroutineScope()
    var f by remember { mutableStateOf<FeeConfig?>(null) }
    var towns by remember { mutableStateOf<List<Town>>(emptyList()) }
    val v = remember { mutableStateMapOf<String, String>() }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val c = Locator.repo.fees(); f = c
            v["pct"] = fmtNum(c.servicePct); v["min"] = fmtNum(c.minService); v["item"] = fmtNum(c.perItem); v["free"] = c.freeItems.toString()
            v["km"] = fmtNum(c.perKm, 3); v["exp"] = fmtNum(c.express); v["pen"] = fmtNum(c.stalePenalty); v["hrs"] = c.staleHours.toString()
            towns = Locator.repo.towns()
            towns.forEach { t -> v["tf${t.id}"] = fmtNum(t.deliveryFee); v["tk${t.id}"] = fmtNum(t.distanceKm, 0) }
        } catch (e: ApiException) { err = e.message }
    }
    if (f == null) { ErrorText(err); if (err == null) Loading(); return }
    fun n(k: String) = parseNum(v[k].orEmpty())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("El coste del servicio depende del importe, de la cantidad de productos, de la distancia al pueblo y de si es para hoy. Todos los importes incluyen IVA (21 %).",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle("Servicio")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Porcentaje", v["pct"].orEmpty(), "%", Modifier.weight(1f)) { v["pct"] = it }
            NumField("Mínimo", v["min"].orEmpty(), "€", Modifier.weight(1f)) { v["min"] = it }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Productos incluidos", v["free"].orEmpty(), "uds", Modifier.weight(1f)) { v["free"] = it }
            NumField("Por cada ud. extra", v["item"].orEmpty(), "€", Modifier.weight(1f)) { v["item"] = it }
        }
        SectionTitle("Distancia y rapidez")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Por km", v["km"].orEmpty(), "€/km", Modifier.weight(1f)) { v["km"] = it }
            NumField("Entrega en el día", v["exp"].orEmpty(), "€", Modifier.weight(1f)) { v["exp"] = it }
        }
        SectionTitle("Penalización a repartidores")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Tras", v["hrs"].orEmpty(), "horas", Modifier.weight(1f)) { v["hrs"] = it }
            NumField("Importe", v["pen"].orEmpty(), "€", Modifier.weight(1f)) { v["pen"] = it }
        }
        SectionTitle("Pueblos (envío base y distancia al súper)")
        towns.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text(t.name, Modifier.weight(1.2f), style = MaterialTheme.typography.bodyLarge)
                NumField("Base", v["tf${t.id}"].orEmpty(), "€", Modifier.weight(1f)) { v["tf${t.id}"] = it }
                Spacer(Modifier.width(6.dp))
                NumField("Dist.", v["tk${t.id}"].orEmpty(), "km", Modifier.weight(1f)) { v["tk${t.id}"] = it }
            }
        }
        // Ejemplo en vivo
        val ex = runCatching {
            val sub = 30.0; val units = 12; val km = 45.0
            val s = maxOf(sub * n("pct")!! / 100, n("min")!!); val it = maxOf(units - n("free")!!.toInt(), 0) * n("item")!!
            val d = 1.5 + km * n("km")!!; val e = n("exp")!!
            "Ejemplo: compra de ${eur(sub)} con $units productos a $km km para hoy → servicio ${eur(s)} + cantidad ${eur(it)} + envío ${eur(d)} + en el día ${eur(e)} = ${eur(sub + s + it + d + e)}"
        }.getOrNull()
        ex?.let { Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
            Text(it, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium) } }
        ErrorText(err)
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(12.dp))
        val valid = v.values.all { parseNum(it) != null }
        BigButton("Guardar tarifas", {
            scope.launch {
                busy = true; err = null; msg = null
                try {
                    Locator.repo.saveFeeConfig(FeeConfig(n("pct")!!, n("min")!!, n("item")!!, n("free")!!.toInt(), n("km")!!, n("exp")!!, n("pen")!!, n("hrs")!!.toInt()))
                    towns.forEach { t ->
                        val fee = n("tf${t.id}")!!; val km = n("tk${t.id}")!!
                        if (fee != t.deliveryFee || km != t.distanceKm) Locator.repo.saveTown(t.id, fee, km)
                    }
                    msg = "Tarifas guardadas."
                } catch (e: ApiException) { err = e.message }
                busy = false
            }
        }, enabled = valid, loading = busy, icon = Icons.Filled.Save)
        Spacer(Modifier.height(40.dp))
    }
}

// ---------------- Opiniones ----------------
@Composable
private fun AdminReviews() {
    var list by remember { mutableStateOf<List<Review>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { try { list = Locator.repo.recentReviews() } catch (e: ApiException) { err = e.message; list = emptyList() } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { ErrorText(err) }
        val l = list
        when {
            l == null -> item { Loading() }
            l.isEmpty() -> item { Message(Icons.Filled.StarBorder, "Aún no hay opiniones", "Aparecerán cuando los clientes valoren sus pedidos entregados.") }
            else -> {
                item {
                    Text("Media: repartidor ${fmtNum(l.map { it.driverRating }.average(), 1)} ★ · entrega ${fmtNum(l.map { it.deliveryRating }.average(), 1)} ★ (${l.size})",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(l, key = { it.orderId }) { r ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Pedido nº ${r.orderId} · ${prettyDate(r.createdAt)}", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) { Text("Repartidor ", style = MaterialTheme.typography.bodyMedium); Stars(r.driverRating.toDouble(), 18) }
                            Row(verticalAlignment = Alignment.CenterVertically) { Text("Entrega ", style = MaterialTheme.typography.bodyMedium); Stars(r.deliveryRating.toDouble(), 18) }
                            r.comment?.let { Text("«$it»", style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Repartidores (superadmin) ----------------
@Composable
private fun AdminDrivers(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<DriverApplication>?>(null) }
    var drivers by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var rejecting by remember { mutableStateOf<DriverApplication?>(null) }
    LaunchedEffect(reload) {
        try { apps = Locator.repo.applications(); drivers = Locator.repo.adminUsers().filter { it.role == "repartidor" }; err = null }
        catch (e: ApiException) { err = e.message; apps = apps ?: emptyList() }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { ErrorText(err) }
        val pending = apps?.filter { it.status == "pendiente" }.orEmpty()
        item { SectionTitle("Candidaturas pendientes (${pending.size})") }
        if (apps == null) item { Loading() }
        else if (pending.isEmpty()) item { Text("No hay candidaturas pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(pending, key = { "a${it.id}" }) { a ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(a.driver?.fullName?.ifBlank { null } ?: "Candidato", style = MaterialTheme.typography.titleLarge)
                    a.driver?.phone?.let { Text("Tel.: $it", style = MaterialTheme.typography.bodyMedium) }
                    Text("Vehículo: ${a.vehicle}", style = MaterialTheme.typography.bodyMedium)
                    Text("Pueblos: ${a.townIds.joinToString { vm.townName(it) }}", style = MaterialTheme.typography.bodyMedium)
                    a.availability?.takeIf { it.isNotBlank() }?.let { Text("Disponibilidad: $it", style = MaterialTheme.typography.bodyMedium) }
                    a.experience?.takeIf { it.isNotBlank() }?.let { Text("Experiencia: $it", style = MaterialTheme.typography.bodyMedium) }
                    a.message?.takeIf { it.isNotBlank() }?.let { Text("«$it»", style = MaterialTheme.typography.bodyMedium) }
                    Text("Enviada el ${prettyDate(a.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ rejecting = a }, Modifier.weight(1f)) { Text("Rechazar") }
                        Button({ scope.launch { try { Locator.repo.reviewApplication(a.id, true, null); reload++ } catch (e: ApiException) { err = e.message } } },
                            Modifier.weight(1f)) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp)); Text("Autorizar") }
                    }
                }
            }
        }
        item { SectionTitle("Repartidores (${drivers.size})") }
        items(drivers, key = { "d${it.id}" }) { d ->
            ListItem(
                headlineContent = { Text(d.fullName.ifBlank { d.email }) },
                supportingContent = { Text(listOfNotNull(d.email, d.phone, d.rating?.let { "★ $it" }).joinToString(" · ")) },
                trailingContent = {
                    Switch(d.approved == true, { v -> scope.launch { try { Locator.repo.setDriverApproved(d.id, v); reload++ } catch (e: ApiException) { err = e.message } } })
                },
            )
        }
        item { Text("El interruptor autoriza o retira el permiso para aceptar pedidos. Para dar de alta a alguien como repartidor directamente usa «Usuarios».",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
    }
    rejecting?.let { a ->
        var note by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { rejecting = null }, title = { Text("Rechazar candidatura") },
            text = { OutlinedTextField(note, { note = it }, label = { Text("Motivo (lo verá el candidato)") }, minLines = 2) },
            confirmButton = { TextButton({
                rejecting = null
                scope.launch { try { Locator.repo.reviewApplication(a.id, false, note.ifBlank { null }); reload++ } catch (e: ApiException) { err = e.message } }
            }) { Text("Rechazar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ rejecting = null }) { Text("Cancelar") } })
    }
}

// ---------------- Usuarios (superadmin) ----------------
private val roleLabels = linkedMapOf("cliente" to "Cliente", "repartidor" to "Repartidor", "admin" to "Administrador")

@Composable
private fun AdminUsers() {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<AdminUser>?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("admin") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(reload) { try { users = Locator.repo.adminUsers() } catch (e: ApiException) { err = e.message; users = users ?: emptyList() } }
    fun assign(mail: String, r: String) = scope.launch {
        busy = true; err = null; msg = null
        try { Locator.repo.setRole(mail, r); msg = "${roleLabels[r]} asignado a $mail."; email = ""; reload++ } catch (e: ApiException) { err = e.message }
        busy = false
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Añadir administrador o repartidor", style = MaterialTheme.typography.titleMedium)
            Text("La persona debe haberse registrado antes en la app con su correo.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Correo de la persona") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Email, null) }, shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                roleLabels.forEach { (k, l) -> FilterChip(role == k, { role = k }, { Text(l) }) }
            }
            BigButton("Asignar rol", { assign(email, role) }, enabled = email.isNotBlank(), loading = busy, icon = Icons.Filled.PersonAdd)
            ErrorText(err)
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)) }
            SectionTitle("Todas las cuentas")
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("Buscar por nombre o correo") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }, shape = CircleShape)
        }
        val l = users?.filter { q.isBlank() || q.lowercase() in it.fullName.lowercase() || q.lowercase() in it.email.lowercase() }
        if (l == null) item { Loading() }
        else items(l, key = { it.id }) { u ->
            var menu by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(u.fullName.ifBlank { u.email }) },
                supportingContent = { Text(u.email) },
                leadingContent = { Icon(when (u.role) { "superadmin" -> Icons.Filled.Shield; "admin" -> Icons.Filled.AdminPanelSettings
                    "repartidor" -> Icons.Filled.LocalShipping; else -> Icons.Filled.Person }, null) },
                trailingContent = {
                    if (u.role == "superadmin") Text("Superadmin", style = MaterialTheme.typography.labelMedium)
                    else Box {
                        AssistChip({ menu = true }, { Text(roleLabels[u.role] ?: u.role) }, trailingIcon = { Icon(Icons.Filled.ExpandMore, null) })
                        DropdownMenu(menu, { menu = false }) {
                            roleLabels.forEach { (k, lbl) -> DropdownMenuItem({ Text(lbl) }, { menu = false; if (k != u.role) assign(u.email, k) }) }
                        }
                    }
                },
            )
        }
    }
}
