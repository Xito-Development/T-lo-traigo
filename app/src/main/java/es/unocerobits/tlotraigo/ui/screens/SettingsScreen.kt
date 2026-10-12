package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.BuildConfig
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.ApiException
import es.unocerobits.tlotraigo.data.Profile
import es.unocerobits.tlotraigo.data.*
import es.unocerobits.tlotraigo.ui.theme.fontFamilyFor
import es.unocerobits.tlotraigo.ui.theme.schemeFor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel, showFamily: Boolean = true, onDebug: (() -> Unit)? = null) {
    val s by vm.settings.collectAsState()
    val p by vm.profile.collectAsState()
    val ctx = LocalContext.current
    var editProfile by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (showFamily) Text("Ajustes", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 12.dp))

        AppearanceSection(s) { f -> vm.updateSettings(f) }
        ToggleRow("Modo fácil", "Botones y letras más grandes, lectura en voz alta", s.senior, Icons.Filled.Elderly) { v -> vm.updateSettings { it.copy(senior = v) } }
        ToggleRow("Leer en voz alta", "Botón de altavoz junto a los productos", s.speak, Icons.Filled.RecordVoiceOver) { v -> vm.updateSettings { it.copy(speak = v) } }
        Text("Tamaño de letra", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        val steps = listOf(1f to "Normal", 1.15f to "Grande", 1.3f to "Muy grande", 1.5f to "Enorme")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            steps.forEach { (v, l) ->
                FilterChip(kotlin.math.abs(s.textScale - v) < .01f, { vm.updateSettings { it.copy(textScale = v) } }, { Text(l) })
            }
        }

        SectionTitle("Mis datos")
        p?.let { pr ->
            if (editProfile) ProfileForm(vm, "", "Guardar cambios") { editProfile = false }
            else {
                Text(pr.fullName, style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(pr.address, vm.townName(pr.townId).ifBlank { null }, pr.phone).joinToString("\n"), style = MaterialTheme.typography.bodyMedium)
                TextButton({ editProfile = true }) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(6.dp)); Text("Cambiar mis datos") }
            }
        }

        if (showFamily && p?.isDriver == false) p?.let { FamilySection(it, vm) }
        if (p?.isDriver == true) DriverApplicationSection(vm)

        if (p?.isAdmin == true) {
            val driverView by vm.driverView.collectAsState()
            SectionTitle("Pruebas (administrador)")
            ToggleRow("Vista de repartidor", "Cambia entre la app del cliente y la del repartidor", driverView, Icons.Filled.LocalShipping) { vm.setDriverView(it) }
            onDebug?.let { BigButton("Abrir panel de pruebas", it, tonal = true, icon = Icons.Filled.BugReport) }
        }

        SectionTitle("Ayuda")
        var about by remember { mutableStateOf(false) }
        var askDelete by remember { mutableStateOf(false) }
        if (about) AboutDialog({ about = false }, vm)
        if (askDelete) DeleteAccountDialog(vm) { askDelete = false }
        BigButton("Llamar al teléfono de ayuda", { ctx.dial(BuildConfig.SUPPORT_PHONE) }, tonal = true, icon = Icons.Filled.Call)
        Spacer(Modifier.height(10.dp))
        BigButton("Acerca de, créditos y privacidad", { about = true }, tonal = true, icon = Icons.Filled.Info)
        Spacer(Modifier.height(10.dp))
        BigButton("Cerrar sesión", { vm.logout() }, tonal = true, icon = Icons.AutoMirrored.Filled.Logout)
        if (p?.isSuper != true) TextButton({ askDelete = true }, Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
            Text("Eliminar mi cuenta", color = MaterialTheme.colorScheme.error)
        }
        Text("Versión ${BuildConfig.VERSION_NAME} · Xito Development & UNOCEROBITS", Modifier.padding(vertical = 20.dp).align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ToggleRow(title: String, sub: String, value: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(sub, style = MaterialTheme.typography.bodySmall) }
        Switch(value, onChange)
    }
}

@Composable
private fun FamilySection(me: Profile, vm: AppViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var seniors by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { runCatching { seniors = Locator.repo.linkedSeniors() } }

    SectionTitle("Familia")
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(18.dp)) {
            Text("Mi código para familiares", style = MaterialTheme.typography.titleMedium)
            Text(me.linkCode ?: "—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, letterSpacing = MaterialTheme.typography.displaySmall.letterSpacing)
            Text("Dáselo a tu hijo o hija para que pueda hacerte pedidos. Solo sirve una vez.", style = MaterialTheme.typography.bodyMedium)
            TextButton({ ctx.share("Mi código en T lo traigo es ${me.linkCode}") },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(6.dp)); Text("Enviar código") }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Hacer pedidos para un familiar", style = MaterialTheme.typography.titleMedium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(code, { code = it.uppercase().take(6) }, Modifier.weight(1f), label = { Text("Su código") }, singleLine = true, shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.width(8.dp))
        Button({
            scope.launch {
                busy = true
                try { Locator.repo.linkSenior(code); code = ""; err = null; reload++ } catch (e: ApiException) { err = e.message }
                busy = false
            }
        }, enabled = code.length == 6 && !busy, modifier = Modifier.height(56.dp)) { Text("Vincular") }
    }
    ErrorText(err)
    seniors.forEach { s ->
        ListItem(headlineContent = { Text(s.fullName) }, supportingContent = { Text(vm.townName(s.townId)) },
            leadingContent = { Icon(Icons.Filled.Elderly, null) })
    }
}

/** Apariencia: paleta de color, modo claro/oscuro, alto contraste y tipografía. */
@Composable
fun AppearanceSection(s: UiSettings, update: ((UiSettings) -> UiSettings) -> Unit) {
    SectionTitle("Colores")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(Palette.entries) { pal ->
            val sel = pal == s.palette && !s.highContrast
            val sc = schemeFor(pal, false)
            Column(Modifier.width(76.dp).clip(MaterialTheme.shapes.medium).clickable { update { it.copy(palette = pal, highContrast = false) } }
                .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(sc.primaryContainer)
                    .border(if (sel) 4.dp else 1.dp, if (sel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    Alignment.Center) {
                    Row { listOf(sc.primary, sc.secondary, sc.tertiary).forEach { c -> Box(Modifier.padding(1.dp).size(12.dp).clip(CircleShape).background(c)) } }
                    if (sel) Icon(Icons.Filled.Check, null, Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(16.dp), tint = sc.onPrimaryContainer)
                }
                Text(pal.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
    Text("Modo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        ThemeMode.entries.forEach { m ->
            val icon = when (m) { ThemeMode.SISTEMA -> Icons.Filled.BrightnessAuto; ThemeMode.CLARO -> Icons.Filled.LightMode; ThemeMode.OSCURO -> Icons.Filled.DarkMode }
            FilterChip(s.mode == m, { update { it.copy(mode = m) } }, { Text(m.label) }, leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) })
        }
    }
    ToggleRow("Alto contraste", "Máxima legibilidad: blanco y negro (o negro y amarillo en modo oscuro)", s.highContrast, Icons.Filled.Contrast) { v ->
        update { it.copy(highContrast = v) }
    }
    SectionTitle("Tipo de letra")
    AppFont.entries.forEach { f ->
        val sel = f == s.font
        Surface(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(MaterialTheme.shapes.medium).clickable { update { it.copy(font = f) } },
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Aa · ${f.label}", style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamilyFor(f)))
                    Text(f.hint, style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamilyFor(f)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RadioButton(sel, null)
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var txt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Eliminar mi cuenta") },
        text = {
            Column {
                Text("Se borrarán tus datos personales, tu cesta y tus vínculos familiares. Los pedidos ya realizados se conservan " +
                    "de forma anónima por obligación legal. Esta acción no se puede deshacer.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(txt, { txt = it }, label = { Text("Escribe ELIMINAR para confirmar") }, singleLine = true)
                ErrorText(err)
            }
        },
        confirmButton = {
            TextButton({
                scope.launch {
                    busy = true; err = null
                    try { Locator.repo.deleteMyAccount(); vm.logout() } catch (e: ApiException) { err = e.message }
                    busy = false
                }
            }, enabled = txt.trim().equals("ELIMINAR", true) && !busy) { Text("Eliminar definitivamente", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}
