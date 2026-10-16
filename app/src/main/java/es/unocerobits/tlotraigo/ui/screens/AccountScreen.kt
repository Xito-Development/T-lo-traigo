package es.unocerobits.tlotraigo.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.unocerobits.tlotraigo.AppViewModel
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.data.ApiException
import es.unocerobits.tlotraigo.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

/** Ajustes → Cuenta: correo, contraseña, avisos por correo, mis datos, sesiones y eliminar cuenta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(vm: AppViewModel, onBack: () -> Unit) {
    val p by vm.profile.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val celebrate = LocalCelebrate.current
    val email = Locator.api.email.orEmpty()
    val internal = email.endsWith("@tlotraigo.app")
    var msg by remember { mutableStateOf<String?>(null) }
    var askDelete by remember { mutableStateOf(false) }
    var askAll by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar({ Text("Cuenta") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Cabecera
            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(Color.Transparent)) {
                Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF2E6B57), Color(0xFF1F4D3F)))).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    val initials = p?.fullName.orEmpty().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFF2B544)), Alignment.Center) { Text(initials, fontSize = 24.sp, color = Color(0xFF1B2428), style = MaterialTheme.typography.titleLarge) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(p?.fullName.orEmpty(), style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(if (internal) "Usuario: ${email.substringBefore('@')}" else email, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE4F1EA))
                        Text(when { p?.isSuper == true -> "Superadministrador"; p?.isGestor == true -> "Administrador de entidad"; p?.isDriver == true -> "Repartidor"; else -> "Usuario" } +
                            (p?.createdAt?.let { " · desde ${prettyDay(it.take(10))}" } ?: ""), style = MaterialTheme.typography.labelMedium, color = Color(0xFFF2B544))
                    }
                }
            }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }

            // Correos
            Section("Correos", Icons.Filled.Email) {
                var mails by remember(p?.emailNotifications) { mutableStateOf(p?.emailNotifications ?: true) }
                var notify by remember(p?.notifyEmail) { mutableStateOf(p?.notifyEmail.orEmpty()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Correos de mis compras", style = MaterialTheme.typography.titleSmall)
                        Text("Resumen del pedido y factura o justificante al entregarlo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    AppSwitch(mails, { v -> mails = v; scope.launch { runCatching { Locator.repo.setEmailNotifications(v) }; vm.refreshProfile() } })
                }
                OutlinedTextField(notify, { notify = it.trim() }, Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text(if (internal) "Correo para recibir avisos" else "Otro correo para avisos (opcional)") },
                    placeholder = { Text(if (internal) "tucorreo@ejemplo.com" else email) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = { Text(if (internal) "Tu cuenta se creó con nombre de usuario: añade un correo para recibir las compras y facturas." else "Si lo dejas vacío, usamos $email.") })
                Button({ scope.launch { msg = try { require(notify.isBlank() || Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(notify)) { "Ese correo no parece válido" }
                    Locator.repo.setNotifyEmail(notify); vm.refreshProfile(); "Correo para avisos guardado" } catch (e: Exception) { e.message } } }) { Text("Guardar correo") }
            }

            // Contraseña
            Section("Contraseña", Icons.Filled.Lock) {
                var cur by remember { mutableStateOf("") }; var nw by remember { mutableStateOf("") }; var rep by remember { mutableStateOf("") }
                var show by remember { mutableStateOf(false) }; var busy by remember { mutableStateOf(false) }; var err by remember { mutableStateOf<String?>(null) }
                val vt = if (show) VisualTransformation.None else PasswordVisualTransformation()
                val eye = @Composable { IconButton({ show = !show }) { Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (show) "Ocultar" else "Mostrar") } }
                OutlinedTextField(cur, { cur = it }, Modifier.fillMaxWidth(), label = { Text("Contraseña actual") }, singleLine = true, visualTransformation = vt, trailingIcon = eye)
                OutlinedTextField(nw, { nw = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Nueva contraseña") }, singleLine = true, visualTransformation = vt)
                val strength = listOf(nw.length >= 8, nw.any { it.isDigit() }, nw.any { it.isUpperCase() }, nw.any { !it.isLetterOrDigit() }).count { it }
                val bar by animateFloatAsState(strength / 4f, Motion.bouncy(), label = "fuerza")
                LinearProgressIndicator({ bar }, Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).clip(CircleShape),
                    color = when (strength) { 0, 1 -> MaterialTheme.colorScheme.error; 2 -> Color(0xFFDE9A2A); else -> MaterialTheme.colorScheme.primary })
                Text(when (strength) { 0, 1 -> "Débil: usa al menos 8 caracteres"; 2 -> "Aceptable"; 3 -> "Buena"; else -> "Muy buena" }, style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(rep, { rep = it }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Repite la nueva contraseña") }, singleLine = true, visualTransformation = vt,
                    isError = rep.isNotEmpty() && rep != nw)
                ErrorText(err)
                Button({ scope.launch { busy = true; err = null
                    try { Locator.api.changePassword(cur, nw); cur = ""; nw = ""; rep = ""; celebrate(Celebration("Contraseña cambiada", "Úsala la próxima vez que entres", "🔒")) }
                    catch (e: ApiException) { err = e.message }; busy = false } }, enabled = !busy && cur.isNotEmpty() && nw.length >= 8 && nw == rep && nw != cur) { Text("Cambiar contraseña") }
            }

            // Correo de acceso (solo cuentas con correo real)
            if (!internal && email.isNotBlank()) Section("Correo de acceso", Icons.Filled.AlternateEmail) {
                var ne by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
                Text("Ahora entras con $email.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(ne, { ne = it.trim() }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Nuevo correo") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                ErrorText(err)
                Button({ scope.launch { try { Locator.api.changeEmail(ne); msg = "Te hemos enviado un correo a $ne para confirmar el cambio."; ne = "" } catch (e: ApiException) { err = e.message } } },
                    enabled = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(ne) && ne != email) { Text("Cambiar correo") }
            }

            // Correo de la plataforma y avisos del vigilante (solo superadministrador)
            if (p?.isSuper == true) { MailConfig(); AdminAlerts(vm) }

            // Privacidad y sesiones
            Section("Privacidad y sesiones", Icons.Filled.Shield) {
                OutlinedButton({ scope.launch { msg = try {
                    val f = java.io.File(java.io.File(ctx.cacheDir, "exports").apply { mkdirs() }, "mis-datos-tlotraigo.json"); f.writeText(Locator.repo.exportMyData())
                    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Guardar mis datos")); null
                } catch (e: Exception) { e.message } } }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Download, null); Spacer(Modifier.width(8.dp)); Text("Descargar mis datos") }
                Text("Perfil, pedidos, listas, facturas y reclamaciones en un archivo (derecho de portabilidad, RGPD).", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedButton({ askAll = true }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Devices, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión en todos los dispositivos") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton({ vm.logout() }, Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión") }
            }

            if (p?.isSuper != true) Section("Zona peligrosa", Icons.Filled.Warning, danger = true) {
                Text("Se borran tu cuenta y tus datos personales. Las facturas se conservan el tiempo que exige la ley.", style = MaterialTheme.typography.bodySmall)
                Button({ askDelete = true }, colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error), modifier = Modifier.padding(top = 6.dp)) { Text("Eliminar mi cuenta") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (askAll) AlertDialog(onDismissRequest = { askAll = false }, title = { Text("¿Cerrar todas las sesiones?") },
        text = { Text("Tendrás que volver a entrar en todos los móviles y ordenadores, también en este.") },
        confirmButton = { Button({ askAll = false; scope.launch { Locator.api.signOutEverywhere(); vm.logout() } }) { Text("Cerrar todas") } },
        dismissButton = { TextButton({ askAll = false }) { Text("Cancelar") } })
    if (askDelete) DeleteAccountDialog(vm) { askDelete = false }
}

@Composable
private fun Section(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, danger: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().animateContentSize(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(if (danger) MaterialTheme.colorScheme.errorContainer.copy(alpha = .4f) else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

/** Configuración del correo de compras (Brevo) y diagnóstico de los últimos envíos. */
@Composable
private fun MailConfig() {
    val scope = rememberCoroutineScope()
    var st by remember { mutableStateOf<JsonObject?>(null) }
    var key by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { st = runCatching { Locator.repo.mailStatus() }.getOrNull() }
    Section("Correo de compras (Brevo)", Icons.Filled.MarkEmailRead) {
        val hasKey = st?.get("db_key")?.jsonPrimitive?.booleanOrNull == true
        val last = st?.get("last")?.jsonArray.orEmpty()
        val lastMsg = last.firstOrNull()?.jsonObject?.get("msg")?.jsonPrimitive?.contentOrNull.orEmpty()
        val missing = !hasKey && lastMsg.contains("falta", true)
        Text(when { missing -> "⚠️ Falta la clave de Brevo: por eso no llegan los correos."; hasKey -> "✓ Clave guardada."; else -> "Comprobando…" },
            color = if (missing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        Text("Brevo → ⚙ → SMTP y API → Claves API → «Generar una nueva clave API». Empieza por xkeysib-.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(key, { key = it.trim() }, Modifier.fillMaxWidth().padding(top = 6.dp), label = { Text("Clave API de Brevo") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation())
        ErrorText(err)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ scope.launch { err = try { Locator.repo.setBrevoKey(key); key = ""; reload++; null } catch (e: ApiException) { e.message } } }, enabled = key.startsWith("xkeysib-")) { Text("Guardar clave") }
            OutlinedButton({ scope.launch { err = try { Locator.repo.sendTestEmail(); kotlinx.coroutines.delay(3500); reload++; null } catch (e: ApiException) { e.message } } }) { Text("Enviar prueba") }
        }
        if (last.isNotEmpty()) {
            Text("Últimos envíos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
            last.take(5).forEach { r -> val o = r.jsonObject
                val ok = o["status"]?.jsonPrimitive?.intOrNull == 200 && o["msg"]?.jsonPrimitive?.contentOrNull?.startsWith("enviado") == true
                Text("${if (ok) "✓" else "•"} ${o["msg"]?.jsonPrimitive?.contentOrNull.orEmpty()}", style = MaterialTheme.typography.bodySmall,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Avisos al correo del superadministrador: caídas, errores, pedidos atascados, reclamaciones, entidades… */
@Composable
private fun AdminAlerts(vm: AppViewModel) {
    val p by vm.profile.collectAsState()
    val scope = rememberCoroutineScope()
    var on by remember(p?.adminAlertEmails) { mutableStateOf(p?.adminAlertEmails ?: true) }
    var alerts by remember { mutableStateOf<JsonArray?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { alerts = runCatching { Locator.repo.activeAlerts() }.getOrNull() }
    Section("Avisos del vigilante", Icons.Filled.NotificationsActive) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Recibir avisos por correo", style = MaterialTheme.typography.titleSmall)
                Text("Caídas de la web o de la compilación, errores de la app, pedidos sin repartidor o reclamados, reclamaciones, reembolsos, entidades por verificar o vencidas y fallos de correo. Lo urgente llega al momento; cada mañana, un resumen.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            AppSwitch(on, { v -> on = v; scope.launch { runCatching { Locator.repo.setAdminAlerts(v) }; vm.refreshProfile() } })
        }
        if (p?.notifyEmail.isNullOrBlank() && Locator.api.email.orEmpty().endsWith("@tlotraigo.app"))
            Text("⚠️ Añade arriba tu «correo para avisos»: tu cuenta no tiene buzón.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        OutlinedButton({ scope.launch { msg = try { Locator.repo.sendAdminDigest(); "Resumen en camino: revisa tu correo en un minuto." } catch (e: ApiException) { e.message } } },
            Modifier.padding(top = 8.dp)) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(8.dp)); Text("Enviarme el resumen ahora") }
        msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        val list = alerts
        if (list != null) {
            Text(if (list.isEmpty()) "✓ Ningún aviso activo" else "Avisos activos (${list.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
            list.take(8).forEach { a -> val o = a.jsonObject; val sev = o["severity"]?.jsonPrimitive?.contentOrNull
                Row(Modifier.padding(vertical = 3.dp)) {
                    Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(when (sev) { "alta" -> MaterialTheme.colorScheme.error; "media" -> Color(0xFFDE9A2A); else -> MaterialTheme.colorScheme.primary }))
                    Spacer(Modifier.width(8.dp))
                    Column { Text(o["title"]?.jsonPrimitive?.contentOrNull.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        Text(o["body"]?.jsonPrimitive?.contentOrNull.orEmpty().replace(Regex("<[^>]+>"), ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
