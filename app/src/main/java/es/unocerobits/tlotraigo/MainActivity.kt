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
        es.unocerobits.tlotraigo.store.StoreUpdates.check(this)   // Google Play: actualizar desde la tienda
        intent?.getLongExtra("chat", 0L)?.takeIf { it > 0 }?.let { vm.openChat.value = it }
        intent?.data?.takeIf { it.host == "unirse" }?.getQueryParameter("c")?.let { vm.inviteCode.value = it }
        if (intent?.getBooleanExtra("install_update", false) == true) es.unocerobits.tlotraigo.data.AutoUpdater.install(this)
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
        intent.data?.takeIf { it.host == "unirse" }?.getQueryParameter("c")?.let { vm.inviteCode.value = it }
        if (intent.getBooleanExtra("install_update", false)) es.unocerobits.tlotraigo.data.AutoUpdater.install(this)
    }

    // --- Actualización automática cuando no se está usando la app
    private var lastTouch = System.currentTimeMillis()
    private val idleHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val idleCheck = object : Runnable {
        override fun run() {
            if (System.currentTimeMillis() - lastTouch > 60_000) es.unocerobits.tlotraigo.data.AutoUpdater.onIdle(this@MainActivity)
            idleHandler.postDelayed(this, 20_000)
        }
    }
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean { lastTouch = System.currentTimeMillis(); return super.dispatchTouchEvent(ev) }
    override fun onResume() {
        super.onResume()
        es.unocerobits.tlotraigo.store.StoreUpdates.check(this)
        idleHandler.postDelayed(idleCheck, 20_000)
    }
    override fun onPause() { idleHandler.removeCallbacks(idleCheck); super.onPause() }
    override fun onStop() {
        super.onStop()
        es.unocerobits.tlotraigo.data.AutoUpdater.onIdle(this)          // GitHub: descarga mientras no se usa
        es.unocerobits.tlotraigo.store.StoreUpdates.onBackground(this)  // Play: instala lo ya descargado
    }
}
