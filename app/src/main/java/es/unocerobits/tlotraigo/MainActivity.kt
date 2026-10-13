package es.unocerobits.tlotraigo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import es.unocerobits.tlotraigo.ui.AppNav
import es.unocerobits.tlotraigo.ui.components.LocalSpeaker
import es.unocerobits.tlotraigo.ui.components.Speaker
import es.unocerobits.tlotraigo.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        es.unocerobits.tlotraigo.data.ChatNotifier.start(this)
        intent?.getLongExtra("chat", 0L)?.takeIf { it > 0 }?.let { vm.openChat.value = it }
        setContent {
            val settings by vm.settings.collectAsState()
            val dark = settings.mode.isDark(isSystemInDarkTheme())
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
            }
            val ctx = LocalContext.current
            val speaker = remember { Speaker(ctx) }
            DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
            AppTheme(settings) {
                CompositionLocalProvider(LocalSpeaker provides if (settings.speak) speaker else null) {
                    // Surface fija el color de texto por defecto (onBackground): sin él, el texto sale negro en temas oscuros.
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        // En pantallas anchas el contenido se centra con un ancho cómodo de lectura
                        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                            val w = if (maxWidth > 900.dp) 860.dp else maxWidth
                            androidx.compose.foundation.layout.Box(Modifier.width(w).fillMaxSize()) { AppNav(vm) }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getLongExtra("chat", 0L).takeIf { it > 0 }?.let { vm.openChat.value = it }
    }
}
