package es.unocerobits.tlotraigo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class AppIcon(val key: String, val label: String, val group: String, val vector: ImageVector)

/** Catálogo de iconos para productos y categorías (clave estable guardada en la BD). */
object AppIcons {
    val all: List<AppIcon> = listOf(
        // Frescos
        AppIcon("verdura", "Verdura", "Frescos", Icons.Filled.Eco),
        AppIcon("fruta", "Fruta", "Frescos", Icons.Filled.Spa),
        AppIcon("hierbas", "Hierbas", "Frescos", Icons.Filled.Grass),
        AppIcon("campo", "Patatas / campo", "Frescos", Icons.Filled.Agriculture),
        AppIcon("setas", "Setas / monte", "Frescos", Icons.Filled.Park),
        AppIcon("flores", "Flores", "Frescos", Icons.Filled.LocalFlorist),
        AppIcon("huevos", "Huevos", "Frescos", Icons.Filled.Egg),
        AppIcon("pescado", "Pescado", "Frescos", Icons.Filled.SetMeal),
        AppIcon("carne", "Carne", "Frescos", Icons.Filled.KebabDining),
        AppIcon("pollo", "Pollo", "Frescos", Icons.Filled.DinnerDining),
        AppIcon("embutido", "Embutido", "Frescos", Icons.Filled.LunchDining),
        // Panadería y dulces
        AppIcon("pan", "Pan", "Panadería", Icons.Filled.BakeryDining),
        AppIcon("desayuno", "Desayuno", "Panadería", Icons.Filled.BreakfastDining),
        AppIcon("galletas", "Galletas", "Panadería", Icons.Filled.Cookie),
        AppIcon("tarta", "Tarta", "Panadería", Icons.Filled.Cake),
        AppIcon("helado", "Helado / postre", "Panadería", Icons.Filled.Icecream),
        AppIcon("brunch", "Bollería", "Panadería", Icons.Filled.BrunchDining),
        // Bebidas
        AppIcon("leche", "Leche", "Bebidas", Icons.Filled.LocalDrink),
        AppIcon("agua", "Agua", "Bebidas", Icons.Filled.WaterDrop),
        AppIcon("cafe", "Café", "Bebidas", Icons.Filled.Coffee),
        AppIcon("cafetera", "Café en cápsulas", "Bebidas", Icons.Filled.CoffeeMaker),
        AppIcon("infusion", "Té / infusión", "Bebidas", Icons.Filled.EmojiFoodBeverage),
        AppIcon("refresco", "Refresco", "Bebidas", Icons.Filled.LocalCafe),
        AppIcon("vino", "Vino", "Bebidas", Icons.Filled.WineBar),
        AppIcon("cerveza", "Cerveza", "Bebidas", Icons.Filled.SportsBar),
        AppIcon("licor", "Licores", "Bebidas", Icons.Filled.Liquor),
        AppIcon("copa", "Cócteles", "Bebidas", Icons.Filled.LocalBar),
        // Despensa
        AppIcon("aceite", "Aceite", "Despensa", Icons.Filled.Opacity),
        AppIcon("arroz", "Arroz", "Despensa", Icons.Filled.RiceBowl),
        AppIcon("legumbres", "Legumbres / sopa", "Despensa", Icons.Filled.RamenDining),
        AppIcon("pizza", "Pizza / pasta", "Despensa", Icons.Filled.LocalPizza),
        AppIcon("rapida", "Comida rápida", "Despensa", Icons.Filled.Fastfood),
        AppIcon("tapas", "Aperitivos", "Despensa", Icons.Filled.Tapas),
        AppIcon("preparado", "Platos preparados", "Despensa", Icons.Filled.TakeoutDining),
        AppIcon("conservas", "Conservas", "Despensa", Icons.Filled.Inventory),
        AppIcon("despensa", "Despensa", "Despensa", Icons.Filled.Inventory2),
        AppIcon("cocina", "Cocina", "Despensa", Icons.Filled.Kitchen),
        AppIcon("picante", "Especias / picante", "Despensa", Icons.Filled.Whatshot),
        AppIcon("congelado", "Congelados", "Despensa", Icons.Filled.AcUnit),
        AppIcon("cubiertos", "Menaje", "Despensa", Icons.Filled.Restaurant),
        AppIcon("barbacoa", "Barbacoa", "Despensa", Icons.Filled.OutdoorGrill),
        AppIcon("microondas", "Microondas", "Despensa", Icons.Filled.Microwave),
        AppIcon("batidora", "Electrodoméstico", "Despensa", Icons.Filled.Blender),
        // Limpieza
        AppIcon("limpieza", "Limpieza", "Limpieza", Icons.Filled.CleaningServices),
        AppIcon("lavadora", "Detergente", "Limpieza", Icons.Filled.LocalLaundryService),
        AppIcon("secadora", "Suavizante", "Limpieza", Icons.Filled.Dry),
        AppIcon("plancha", "Plancha", "Limpieza", Icons.Filled.Iron),
        AppIcon("lavar", "Lavavajillas", "Limpieza", Icons.Filled.Wash),
        AppIcon("desinfectante", "Desinfectante", "Limpieza", Icons.Filled.Sanitizer),
        AppIcon("bolsas", "Bolsas de basura", "Limpieza", Icons.Filled.DeleteOutline),
        // Higiene y salud
        AppIcon("jabon", "Jabón / gel", "Higiene", Icons.Filled.Soap),
        AppIcon("manos", "Higiene de manos", "Higiene", Icons.Filled.CleanHands),
        AppIcon("ducha", "Champú / ducha", "Higiene", Icons.Filled.Shower),
        AppIcon("bano", "Baño", "Higiene", Icons.Filled.Bathtub),
        AppIcon("aseo", "Papel higiénico", "Higiene", Icons.Filled.Wc),
        AppIcon("cepillo", "Cepillo / peine", "Higiene", Icons.Filled.Brush),
        AppIcon("cuchilla", "Afeitado", "Higiene", Icons.Filled.ContentCut),
        AppIcon("belleza", "Cosmética", "Higiene", Icons.Filled.Face),
        AppIcon("medicina", "Parafarmacia", "Higiene", Icons.Filled.Medication),
        AppIcon("botiquin", "Botiquín", "Higiene", Icons.Filled.Healing),
        AppIcon("farmacia", "Farmacia", "Higiene", Icons.Filled.LocalPharmacy),
        AppIcon("salud", "Salud", "Higiene", Icons.Filled.LocalHospital),
        AppIcon("mascarilla", "Mascarillas", "Higiene", Icons.Filled.Masks),
        AppIcon("vista", "Gafas / vista", "Higiene", Icons.Filled.Visibility),
        AppIcon("oido", "Audífonos / pilas", "Higiene", Icons.Filled.Hearing),
        AppIcon("movilidad", "Movilidad", "Higiene", Icons.Filled.Accessible),
        // Bebé y mascotas
        AppIcon("bebe", "Bebé", "Bebé y mascotas", Icons.Filled.ChildCare),
        AppIcon("panales", "Pañales", "Bebé y mascotas", Icons.Filled.BabyChangingStation),
        AppIcon("carrito_bebe", "Carrito de bebé", "Bebé y mascotas", Icons.Filled.ChildFriendly),
        AppIcon("juguetes", "Juguetes", "Bebé y mascotas", Icons.Filled.Toys),
        AppIcon("mascotas", "Mascotas", "Bebé y mascotas", Icons.Filled.Pets),
        // Hogar y otros
        AppIcon("bombilla", "Bombillas", "Hogar", Icons.Filled.Lightbulb),
        AppIcon("pilas", "Pilas", "Hogar", Icons.Filled.BatteryFull),
        AppIcon("enchufe", "Electricidad", "Hogar", Icons.Filled.Power),
        AppIcon("herramientas", "Herramientas", "Hogar", Icons.Filled.Handyman),
        AppIcon("ferreteria", "Ferretería", "Hogar", Icons.Filled.Hardware),
        AppIcon("fuego", "Cerillas / leña", "Hogar", Icons.Filled.LocalFireDepartment),
        AppIcon("jardin", "Jardín", "Hogar", Icons.Filled.Yard),
        AppIcon("ropa", "Ropa", "Hogar", Icons.Filled.Checkroom),
        AppIcon("regalo", "Regalos", "Hogar", Icons.Filled.CardGiftcard),
        AppIcon("prensa", "Prensa / revistas", "Hogar", Icons.Filled.Newspaper),
        AppIcon("papeleria", "Papelería", "Hogar", Icons.Filled.Edit),
        AppIcon("fiesta", "Fiesta", "Hogar", Icons.Filled.Celebration),
        AppIcon("cesta", "Cesta", "Hogar", Icons.Filled.ShoppingBasket),
        AppIcon("oferta", "Oferta", "Hogar", Icons.Filled.LocalOffer),
        AppIcon("favorito", "Favorito", "Hogar", Icons.Filled.Favorite),
        AppIcon("estrella", "Destacado", "Hogar", Icons.Filled.Star),
    )
    private val byKey = all.associateBy { it.key }

    // Claves antiguas (v0.1–0.2) → nuevas
    private val legacy = mapOf(
        "eco" to "verdura", "bakery_dining" to "pan", "egg" to "huevos", "set_meal" to "pescado", "kitchen" to "despensa",
        "local_drink" to "leche", "cleaning_services" to "limpieza", "medication" to "medicina", "ac_unit" to "congelado",
        "child_care" to "bebe", "pets" to "mascotas", "apple" to "fruta", "nutrition" to "fruta", "potato" to "campo",
        "bakery" to "pan", "baguette" to "pan", "milk" to "leche", "icecream" to "helado", "chicken" to "pollo",
        "meat" to "embutido", "oil" to "aceite", "rice" to "arroz", "soup" to "legumbres", "water" to "agua",
        "coffee" to "cafe", "cleaning" to "limpieza", "laundry" to "lavadora", "toilet" to "aseo", "soap" to "jabon",
    )

    fun find(key: String?): AppIcon? = key?.let { byKey[it] ?: byKey[legacy[it]] }
    fun vector(key: String?, fallback: ImageVector = Icons.Filled.ShoppingBasket) = find(key)?.vector ?: fallback
}

/** Selector de icono con buscador y rejilla agrupada. */
@Composable
fun IconPickerDialog(selected: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val list = remember(q) {
        val t = q.trim().lowercase()
        if (t.isEmpty()) AppIcons.all else AppIcons.all.filter { t in it.label.lowercase() || t in it.group.lowercase() || t in it.key }
    }
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.85f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text("Elige un icono (${AppIcons.all.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("Buscar: pan, leche, limpieza…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(GridCells.Adaptive(92.dp), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list, key = { it.key }) { ic ->
                        val sel = ic.key == selected
                        Surface(
                            Modifier.clip(MaterialTheme.shapes.medium).clickable { onPick(ic.key) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (sel) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        ) {
                            Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(ic.vector, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(ic.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton({ onPick(null) }) { Text("Sin icono") }
                    TextButton(onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}
