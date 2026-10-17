package es.unocerobits.tlotraigo.ui.components

import androidx.compose.ui.semantics.contentDescription

import androidx.compose.ui.semantics.semantics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.data.AddrSuggestion
import es.unocerobits.tlotraigo.data.Geo
import kotlinx.coroutines.delay

/** Buscador que se despliega desde una lupa con animación y se recoge al vaciarlo. */
@Composable
fun ExpandableSearch(query: String, onQuery: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, title: @Composable () -> Unit = {}) {
    var open by remember { mutableStateOf(query.isNotEmpty()) }
    val focus = remember { FocusRequester() }
    Row(modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(!open, Modifier.weight(1f), enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) { Box(Modifier.fillMaxWidth()) { title() } }
        AnimatedVisibility(open, Modifier.weight(1f), enter = fadeIn(tween(250)) + expandHorizontally(Motion.enter(380), expandFrom = Alignment.End),
            exit = fadeOut(tween(150)) + shrinkHorizontally(Motion.enter(300), shrinkTowards = Alignment.End)) {
            val kb = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            OutlinedTextField(query, onQuery, Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Buscar. $placeholder" }, label = { Text("Buscar") },
                placeholder = { Text(placeholder) }, singleLine = true, shape = CircleShape, leadingIcon = { Icon(Icons.Filled.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { kb?.hide() }),
                trailingIcon = { IconButton({ onQuery(""); open = false }, Modifier.size(48.dp)) { Icon(Icons.Filled.Close, if (query.isEmpty()) "Cerrar búsqueda" else "Borrar búsqueda") } })
            LaunchedEffect(Unit) { delay(120); runCatching { focus.requestFocus() } }
        }
        if (!open) FilledTonalIconButton({ open = true }, Modifier.size(52.dp).semantics { contentDescription = "Abrir el buscador" }) { Icon(Icons.Filled.Search, null, Modifier.size(26.dp)) }
    }
}

/** Campo con sugerencias de dirección o pueblo mientras se escribe (OpenStreetMap). */
@Composable
fun AddressSuggestField(value: String, onValue: (String) -> Unit, label: String, onPick: (AddrSuggestion) -> Unit, modifier: Modifier = Modifier,
                        context: String = "", onlyPlaces: Boolean = false, placeholder: String? = null) {
    var list by remember { mutableStateOf<List<AddrSuggestion>>(emptyList()) }
    var picked by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(value, context) {
        if (picked) { picked = false; list = emptyList(); return@LaunchedEffect }
        if (value.trim().length < 3) { list = emptyList(); return@LaunchedEffect }
        delay(350); loading = true
        list = Geo.search(listOf(value, context).filter { it.isNotBlank() }.joinToString(" "), onlyPlaces).take(6)
        loading = false
    }
    Column(modifier) {
        OutlinedTextField(value, { onValue(it) }, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text(label) }, singleLine = true,
            placeholder = placeholder?.let { { Text(it) } }, shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            trailingIcon = { if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.TravelExplore, null) })
        AnimatedVisibility(list.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Surface(Modifier.fillMaxWidth().padding(top = 4.dp), shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp, shadowElevation = 4.dp) {
                Column {
                    list.forEach { s ->
                        Row(Modifier.fillMaxWidth().clickable { picked = true; onPick(s); list = emptyList() }.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (s.street == null) Icons.Filled.LocationCity else Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column { Text(s.title, style = MaterialTheme.typography.bodyLarge); if (s.subtitle.isNotBlank()) Text(s.subtitle, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    Text("Sugerencias de © OpenStreetMap", Modifier.padding(start = 14.dp, bottom = 6.dp), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
