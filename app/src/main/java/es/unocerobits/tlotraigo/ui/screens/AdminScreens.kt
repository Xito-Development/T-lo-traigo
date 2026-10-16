package es.unocerobits.tlotraigo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
    ENTIDADES("Entidades", true), PRODUCTOS("Productos"), LISTA("Lista fácil"), TARIFAS("Tarifas"), ESTADISTICAS("Estadísticas"), REEMBOLSOS("Reembolsos"),
    RECLAMACIONES("Reclamaciones"), OPINIONES("Opiniones"), EMPRESA("Facturación", true),
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
                ASec.LISTA -> AdminNeeds()
                ASec.ENTIDADES -> AdminOrgs(vm, LocalOpenOrg.current)
                ASec.ESTADISTICAS -> AdminStats()
                ASec.REEMBOLSOS -> AdminRefunds()
                ASec.RECLAMACIONES -> AdminComplaints()
                ASec.EMPRESA -> AdminCompany()
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
    var allergens by rememberSaveable { mutableStateOf("") }
    var ingredients by rememberSaveable { mutableStateOf("") }
    var adultOnly by rememberSaveable { mutableStateOf(false) }
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
                allergens = p.allergens.orEmpty(); ingredients = p.ingredients.orEmpty(); adultOnly = p.ageRestricted
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
            SectionTitle("Información alimentaria")
            OutlinedTextField(allergens, { allergens = it }, Modifier.fillMaxWidth(), label = { Text("Alérgenos (obligatorio si es alimento)") },
                placeholder = { Text("Ej.: Contiene gluten y leche. Puede contener frutos secos.") }, minLines = 2, shape = MaterialTheme.shapes.medium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(ingredients, { ingredients = it }, Modifier.fillMaxWidth(), label = { Text("Ingredientes (opcional)") }, minLines = 2, shape = MaterialTheme.shapes.medium)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Solo mayores de 18", style = MaterialTheme.typography.titleMedium)
                    Text("Bebidas alcohólicas: el cliente confirma su edad y el repartidor pide el DNI", style = MaterialTheme.typography.bodySmall)
                }
                AppSwitch(adultOnly, { adultOnly = it })
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Visible en el catálogo", style = MaterialTheme.typography.titleMedium)
                    Text(if (active) "Los clientes pueden comprarlo" else "Oculto para los clientes", style = MaterialTheme.typography.bodySmall)
                }
                AppSwitch(active, { active = it })
            }
            ErrorText(err)
            Spacer(Modifier.height(12.dp))
            BigButton("Guardar producto", {
                scope.launch {
                    busy = true; err = null
                    try {
                        Locator.repo.saveProduct(id, market, cat, name, brand, unit, price!!, vat, icon, image, ean, active, allergens, ingredients, adultOnly)
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
fun NumField(label: String, value: String, suffix: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) =
    OutlinedTextField(value, { onChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(8)) }, modifier, label = { Text(label) },
        suffix = { Text(suffix) }, singleLine = true, isError = parseNum(value) == null, shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

@Composable
private fun AdminFees() {
    val scope = rememberCoroutineScope()
    var f by remember { mutableStateOf<FeeConfig?>(null) }
    var extras by remember { mutableStateOf<List<ExtraFee>>(emptyList()) }
    var reloadX by remember { mutableIntStateOf(0) }
    LaunchedEffect(reloadX) { extras = runCatching { Locator.repo.extraFees() }.getOrDefault(emptyList()) }
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
            v["pp"] = fmtNum(c.platformPct); v["ckm"] = fmtNum(c.costPerKm); v["gp"] = fmtNum(c.gestionPct); v["gm"] = fmtNum(c.gestionMin); v["mg"] = fmtNum(c.marginPct)
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
        var card by remember { mutableStateOf(f!!.cardEnabled) }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pagos con tarjeta", style = MaterialTheme.typography.titleMedium)
                Text("Próximamente: ahora mismo no se gestionan cobros con tarjeta (solo efectivo y Bizum).", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppSwitch(false, null, enabled = false)
        }
        var demo by remember { mutableStateOf(f!!.demoPayments) }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Modo demostración", style = MaterialTheme.typography.titleMedium)
                Text("Permite «Simular pago». Desactívalo cuando configures Stripe: si no, cualquiera podría marcar pedidos como pagados.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppSwitch(demo, { demo = it; f = f!!.copy(demoPayments = it) })
        }
        SectionTitle("Modelo de repartidores")
        var mode by remember { mutableStateOf(f!!.payoutMode) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == "gastos", { mode = "gastos"; f = f!!.copy(payoutMode = "gastos"); v["km"] = fmtNum((parseNum(v["ckm"].orEmpty()) ?: 0.26) * 2) },
                { Text("Vecinos: solo gastos") })
            FilterChip(mode == "porcentaje", { mode = "porcentaje"; f = f!!.copy(payoutMode = "porcentaje") }, { Text("Profesionales: % de tarifas") })
        }
        Text(if (mode == "gastos") "El repartidor recibe solo sus gastos de desplazamiento (km ida y vuelta × €/km), sin beneficio: no necesita ser autónomo. Servicio, gestión y tarifas adicionales son de la plataforma. El «por km» que paga el cliente se pone al doble del coste por km."
             else "El repartidor (autónomo o contratado) recibe servicio + envío + suplemento menos la comisión de la plataforma.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (mode == "gastos") NumField("Coste por km del vehículo (exento hasta 0,26 €)", v["ckm"].orEmpty(), "€/km", Modifier.fillMaxWidth()) { v["ckm"] = it; parseNum(it)?.let { c -> v["km"] = fmtNum(c * 2) } }
        SectionTitle("Reparto de ganancias")
        NumField("Comisión de la plataforma sobre las tarifas del repartidor", v["pp"].orEmpty(), "%", Modifier.fillMaxWidth()) { v["pp"] = it }
        Text("El repartidor recibe servicio + envío + suplemento menos esta comisión. La gestión y las tarifas adicionales son de la plataforma.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle("Gestión y precios orientativos")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Gestión", v["gp"].orEmpty(), "%", Modifier.weight(1f)) { v["gp"] = it }
            NumField("Mínimo", v["gm"].orEmpty(), "€", Modifier.weight(1f)) { v["gm"] = it }
            NumField("Margen máx.", v["mg"].orEmpty(), "%", Modifier.weight(1f)) { v["mg"] = it }
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
        SectionTitle("Tarifas adicionales")
        Text("Se suman al pedido como servicio de la plataforma (IVA 21 % incluido). Fijas en € o en % de la compra.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        extras.forEach { e ->
            ListItem(headlineContent = { Text(e.name) }, supportingContent = { Text(if (e.kind == "fijo") eur(e.amount) else "${fmtNum(e.amount)} % de la compra") },
                trailingContent = { IconButton({ scope.launch { runCatching { Locator.repo.deleteExtraFee(e.id) }; reloadX++ } }) { Icon(Icons.Filled.Delete, "Eliminar tarifa") } })
        }
        var exName by remember { mutableStateOf("") }; var exKind by remember { mutableStateOf("fijo") }; var exAmt by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(exName, { exName = it }, Modifier.weight(1.4f), label = { Text("Nombre") }, singleLine = true)
            NumField("Importe", exAmt, if (exKind == "fijo") "€" else "%", Modifier.weight(1f)) { exAmt = it }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(exKind == "fijo", { exKind = "fijo" }, { Text("Fija (€)") }); FilterChip(exKind == "porcentaje", { exKind = "porcentaje" }, { Text("Porcentaje") })
            Spacer(Modifier.weight(1f))
            Button({ scope.launch { try { Locator.repo.addExtraFee(exName, exKind, parseNum(exAmt)!!); exName = ""; exAmt = ""; reloadX++ } catch (e: Exception) { err = e.message } } },
                enabled = exName.trim().length >= 2 && parseNum(exAmt) != null) { Text("Añadir") }
        }
        SectionTitle("Pueblos (envío base y distancia al súper)")
        towns.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                IconButton({ scope.launch { runCatching { Locator.repo.deactivateTown(t.id) }; towns = towns - t } }) { Icon(Icons.Filled.RemoveCircleOutline, "Quitar ${t.name}", tint = MaterialTheme.colorScheme.error) }
                Text(t.name, Modifier.weight(1.2f), style = MaterialTheme.typography.bodyLarge)
                NumField("Base", v["tf${t.id}"].orEmpty(), "€", Modifier.weight(1f)) { v["tf${t.id}"] = it }
                Spacer(Modifier.width(6.dp))
                NumField("Dist.", v["tk${t.id}"].orEmpty(), "km", Modifier.weight(1f)) { v["tk${t.id}"] = it }
            }
        }
        var tName by remember { mutableStateOf("") }; var tCp by remember { mutableStateOf("") }; var tFee by remember { mutableStateOf("1,50") }; var tKm by remember { mutableStateOf("") }
        Text("Añadir pueblo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
        LaunchedEffect(tCp) { if (tCp.length == 5 && tName.isBlank()) es.unocerobits.tlotraigo.data.Geo.placesByCp(tCp).firstOrNull()?.town?.let { tName = it } }
        Text("Escribe el código postal y el nombre se rellena solo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(tName, { tName = it }, Modifier.weight(1.5f), label = { Text("Pueblo") }, singleLine = true)
            OutlinedTextField(tCp, { tCp = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), label = { Text("CP") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumField("Base", tFee, "€", Modifier.weight(1f)) { tFee = it }; NumField("Distancia", tKm, "km", Modifier.weight(1f)) { tKm = it }
            Button({ scope.launch { try { Locator.repo.addTown(tName, tCp, parseNum(tFee)!!, parseNum(tKm)!!); towns = Locator.repo.towns()
                towns.forEach { t -> v["tf${t.id}"] = fmtNum(t.deliveryFee); v["tk${t.id}"] = fmtNum(t.distanceKm, 0) }; tName = ""; tCp = ""; tKm = "" } catch (e: Exception) { err = e.message } } },
                enabled = tName.isNotBlank() && parseNum(tFee) != null && parseNum(tKm) != null) { Text("Añadir") }
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
        val valid = v.entries.all { parseNum(it.value) != null }
        BigButton("Guardar tarifas", {
            scope.launch {
                busy = true; err = null; msg = null
                try {
                    Locator.repo.saveFeeConfig(FeeConfig(n("pct")!!, n("min")!!, n("item")!!, n("free")!!.toInt(), n("km")!!, n("exp")!!, n("pen")!!, n("hrs")!!.toInt(), n("gp")!!, n("gm")!!, n("mg")!!, n("pp")!!, f!!.demoPayments, f!!.cardEnabled, f!!.payoutMode, n("ckm") ?: 0.26))
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
                    AppSwitch(d.approved == true, { v -> scope.launch { try { Locator.repo.setDriverApproved(d.id, v); reload++ } catch (e: ApiException) { err = e.message } } })
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

// ---------------- Estadísticas y errores ----------------
@Composable
private fun AdminStats() {
    var days by remember { mutableIntStateOf(30) }
    var a by remember { mutableStateOf<AnalyticsSummary?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(days) { try { a = Locator.repo.analytics(days); err = null } catch (e: ApiException) { err = e.message } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(7, 30, 90).forEach { d -> FilterChip(days == d, { days = d }, { Text("$d días") }) } }
            ErrorText(err)
        }
        val x = a
        if (x == null) item { Loading() } else {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Visitas web" to x.daily.sumOf { it.web }, "Pantallas app" to x.daily.sumOf { it.app },
                        "Visitantes web" to (x.visitors["web"] ?: 0), "Usuarios app" to (x.visitors["app"] ?: 0)).forEach { (l, v) ->
                        Card(Modifier.weight(1f)) { Column(Modifier.padding(10.dp)) { Text("$v", style = MaterialTheme.typography.headlineSmall); Text(l, style = MaterialTheme.typography.labelSmall) } }
                    }
                }
                val max = (x.daily.maxOfOrNull { it.web + it.app } ?: 1).coerceAtLeast(1)
                Text("Por día (verde: web · mostaza: app)", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth().height(120.dp).padding(top = 6.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    x.daily.takeLast(45).forEach { d ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                            Box(Modifier.fillMaxWidth().height((110f * d.app / max).dp).background(MaterialTheme.colorScheme.secondary))
                            Box(Modifier.fillMaxWidth().height((110f * d.web / max).dp).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
                SectionTitle("Lo más visto")
            }
            items(x.top) { t -> ListItem(headlineContent = { Text(t.name) }, supportingContent = { Text(t.source) }, trailingContent = { Text("${t.n}") }) }
            item { SectionTitle("Errores recientes (${x.errors.size})") }
            items(x.errors) { e -> ListItem(headlineContent = { Text("${e.source} · ${e.name}") },
                supportingContent = { Text("${prettyDate(e.ts)} · ${e.version ?: ""}\n${e.detail?.toString()?.take(300) ?: ""}", style = MaterialTheme.typography.bodySmall) },
                leadingContent = { Icon(Icons.Filled.BugReport, null, tint = MaterialTheme.colorScheme.error) }) }
        }
    }
}

// ---------------- Reembolsos ----------------
@Composable
private fun AdminRefunds() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<RefundRequest>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(reload) { while (true) { try { list = Locator.repo.refundRequests() } catch (e: ApiException) { err = e.message; list = list ?: emptyList() }; kotlinx.coroutines.delay(30_000) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { ErrorText(err); Text("La tarifa de servicio y gestión no se reembolsa. Si se pagó con tarjeta, la devolución se hace sola en Stripe al aprobar.", style = MaterialTheme.typography.bodySmall) }
        val l = list
        if (l == null) item { Loading() } else if (l.isEmpty()) item { Message(Icons.Filled.CurrencyExchange, "Sin solicitudes de reembolso") }
        else items(l, key = { it.id }) { r ->
            var amt by remember(r.id) { mutableStateOf(r.amount?.let { fmtNum(it) } ?: "") }
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Pedido nº ${r.orderId} · ${r.status}", style = MaterialTheme.typography.titleMedium)
                    Text(r.reason, style = MaterialTheme.typography.bodyMedium)
                    Text(prettyDate(r.createdAt), style = MaterialTheme.typography.bodySmall)
                    if (r.status == "pendiente") Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(amt, { amt = it }, Modifier.weight(1f), label = { Text("Importe") }, suffix = { Text("€") }, singleLine = true)
                        OutlinedButton({ scope.launch { runCatching { Locator.repo.resolveRefund(r.id, false, null, null) }; reload++ } }) { Text("Rechazar") }
                        Button({ scope.launch { try { Locator.repo.resolveRefund(r.id, true, parseNum(amt), null); reload++ } catch (e: ApiException) { err = e.message } } }) { Text("Aprobar") }
                    } else r.amount?.let { Text("Devuelto: ${eur(it)}", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

// ---------------- Reclamaciones ----------------
@Composable
private fun AdminComplaints() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Complaint>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { while (true) { list = runCatching { Locator.repo.complaints() }.getOrDefault(list ?: emptyList()); kotlinx.coroutines.delay(30_000) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { Text("Plazo de respuesta: 10 días hábiles. Guarda las respuestas: pueden pedirlas los servicios de consumo.", style = MaterialTheme.typography.bodySmall) }
        val l = list
        if (l == null) item { Loading() } else if (l.isEmpty()) item { Message(Icons.Filled.Feedback, "No hay reclamaciones") }
        else items(l, key = { it.id }) { c ->
            var ans by remember(c.id) { mutableStateOf(c.response.orEmpty()) }
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("${c.number} · ${c.status}", style = MaterialTheme.typography.titleMedium)
                    Text("${c.fullName} · ${c.contact}${c.orderId?.let { " · pedido nº $it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                    Text(c.subject, style = MaterialTheme.typography.titleSmall); Text(c.body, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(ans, { ans = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Respuesta") }, minLines = 2)
                    Button({ scope.launch { runCatching { Locator.repo.answerComplaint(c.id, ans) }; reload++ } }, enabled = ans.isNotBlank()) { Text("Guardar respuesta") }
                }
            }
        }
    }
}

// ---------------- Datos de facturación ----------------
@Composable
private fun AdminCompany() {
    val scope = rememberCoroutineScope()
    var c by remember { mutableStateOf<CompanyInfo?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c = runCatching { Locator.repo.company() }.getOrNull() }
    val x = c ?: return Loading()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Datos del emisor de las facturas (obligatorios por el Reglamento de facturación, RD 1619/2012).", style = MaterialTheme.typography.bodyMedium)
        listOf("Nombre o razón social" to x.name, "NIF" to x.nif, "Domicilio fiscal" to x.address, "Correo" to x.email, "Serie de facturas" to x.series).forEachIndexed { i, (l, v) ->
            OutlinedTextField(v, { nv -> c = when (i) { 0 -> x.copy(name = nv); 1 -> x.copy(nif = nv.uppercase()); 2 -> x.copy(address = nv); 3 -> x.copy(email = nv); else -> x.copy(series = nv.uppercase()) } },
                Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(l) }, singleLine = true, shape = MaterialTheme.shapes.medium)
        }
        Spacer(Modifier.height(12.dp))
        BigButton("Guardar", { scope.launch { msg = try { Locator.repo.saveCompany(x); "Guardado" } catch (e: ApiException) { e.message } } }, icon = Icons.Filled.Save)
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
