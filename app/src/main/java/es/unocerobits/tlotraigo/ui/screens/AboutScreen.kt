package es.unocerobits.tlotraigo.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import es.unocerobits.tlotraigo.BuildConfig
import es.unocerobits.tlotraigo.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import es.unocerobits.tlotraigo.ui.components.SectionTitle

private val thirdParty = listOf(
    "Jetpack Compose, AndroidX, Material 3" to "Apache License 2.0 · Google",
    "Kotlin, kotlinx.coroutines, kotlinx.serialization" to "Apache License 2.0 · JetBrains",
    "OkHttp" to "Apache License 2.0 · Square",
    "Coil" to "Apache License 2.0 · Coil Contributors",
    "Material Icons" to "Apache License 2.0 · Google",
    "Atkinson Hyperlegible" to "SIL Open Font License 1.1 · Braille Institute of America",
    "Andika" to "SIL Open Font License 1.1 · SIL International",
    "Lato" to "SIL Open Font License 1.1 · Łukasz Dziedzic",
    "Supabase (servicio)" to "Backend: Postgres, Auth y Storage",
)

/** Créditos, contacto, licencia y avisos legales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    fun open(uri: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(topBar = {
                TopAppBar({ Text("Acerca de") }, navigationIcon = { IconButton(onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } })
            }) { pad ->
                Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandMark(96)
                    Spacer(Modifier.height(12.dp))
                    Text("T lo traigo", style = MaterialTheme.typography.headlineLarge)
                    Text("Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("La compra del súper en la puerta de casa, para los pueblos de León.", textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge)

                    SectionTitle("Créditos", Modifier.fillMaxWidth())
                    Text("Desarrollado por", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        CreditLogo("Xito Development", "https://github.com/Xito-Development") {
                            // Logo en blanco sobre círculo oscuro, a juego con el de UNOCEROBITS
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF263238)), Alignment.Center) {
                                Image(painterResource(R.drawable.logo_xito), null, Modifier.fillMaxSize(.78f))
                            }
                        }
                        CreditLogo("UNOCEROBITS", "https://unocerobits.com") {
                            Image(painterResource(R.drawable.logo_unocerobits), null, Modifier.fillMaxSize())
                        }
                    }
                    SectionTitle("Contacto", Modifier.fillMaxWidth())
                    ListItem(headlineContent = { Text(BuildConfig.CONTACT_EMAIL) }, supportingContent = { Text("Dudas, sugerencias o incidencias") },
                        leadingContent = { Icon(Icons.Filled.Email, null) },
                        modifier = Modifier.fillMaxWidth().padding(0.dp).let { it },
                        trailingContent = { TextButton({ open("mailto:${BuildConfig.CONTACT_EMAIL}?subject=T%20lo%20traigo") }) { Text("Escribir") } })
                    ListItem(headlineContent = { Text("unocerobits.com") }, supportingContent = { Text("Web del proyecto") },
                        leadingContent = { Icon(Icons.Filled.Language, null) },
                        trailingContent = { TextButton({ open(BuildConfig.WEB_URL) }) { Text("Abrir") } })
                    ListItem(headlineContent = { Text("Política de privacidad") }, leadingContent = { Icon(Icons.Filled.PrivacyTip, null) },
                        trailingContent = { TextButton({ open(BuildConfig.PRIVACY_URL) }) { Text("Leer") } })

                    SectionTitle("Licencia", Modifier.fillMaxWidth())
                    Text("© 2026 UNOCEROBITS y Xito Development. Todos los derechos reservados. Software propietario: no se permite copiar, " +
                        "modificar ni distribuir sin autorización escrita.", style = MaterialTheme.typography.bodyMedium)

                    SectionTitle("Software de terceros", Modifier.fillMaxWidth())
                    thirdParty.forEach { (n, l) ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(n, style = MaterialTheme.typography.titleSmall)
                            Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CreditLogo(name: String, url: String, logo: @Composable () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.width(140.dp).clip(MaterialTheme.shapes.medium)
            .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(96.dp)) { logo() }
        Spacer(Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
    }
}
