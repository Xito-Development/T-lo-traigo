package es.unocerobits.tlotraigo.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import es.unocerobits.tlotraigo.ui.components.Motion

/**
 * Dictado propio con animación: ondas y barras que siguen tu voz y el texto aparece mientras hablas.
 * Escucha frases largas con pausas; «Seguir hablando» añade más. Si el móvil no tiene reconocimiento de voz, usa el de Google.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictationDialog(prompt: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }          // lo ya confirmado
    var partial by remember { mutableStateOf("") }       // lo que va saliendo ahora
    var listening by remember { mutableStateOf(false) }
    var level by remember { mutableFloatStateOf(0f) }
    var err by remember { mutableStateOf<String?>(null) }
    val available = remember { SpeechRecognizer.isRecognitionAvailable(ctx) }
    val recognizer = remember { if (available) SpeechRecognizer.createSpeechRecognizer(ctx) else null }
    val fallback = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { onDone(it) } ?: onDismiss() }
    fun start() {
        err = null; partial = ""
        recognizer?.startListening(dictationIntent(prompt).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L))
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) start() else err = "Sin permiso del micrófono no puedo escucharte." }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) { level = ((rms + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false; level = 0f }
            override fun onError(e: Int) { listening = false; level = 0f
                err = when (e) { SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (text.isBlank()) "No te he oído. Pulsa el micrófono y habla." else null
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sin conexión para reconocer la voz."; else -> "No he podido escucharte. Inténtalo de nuevo." } }
            override fun onResults(r: Bundle?) { listening = false; level = 0f
                val t = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (t.isNotBlank()) text = (if (text.isBlank()) t else "$text, $t"); partial = "" }
            override fun onPartialResults(r: Bundle?) { partial = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty() }
            override fun onEvent(t: Int, b: Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }
    LaunchedEffect(Unit) {
        if (!available) { runCatching { fallback.launch(dictationIntent(prompt)) }.onFailure { onDismiss() }; return@LaunchedEffect }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start() else perm.launch(Manifest.permission.RECORD_AUDIO)
    }
    if (!available) return
    val smooth by animateFloatAsState(level, spring(dampingRatio = 0.6f, stiffness = 300f), label = "nivel")
    val inf = rememberInfiniteTransition(label = "dictado")
    val wave by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "onda")
    val phase by inf.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(900, easing = LinearEasing)), label = "fase")
    val primary = MaterialTheme.colorScheme.primary; val accent = androidx.compose.ui.graphics.Color(0xFFF2B544)
    Dialog(onDismissRequest = { recognizer?.cancel(); onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (listening) "Te escucho…" else if (text.isBlank()) "Pulsa y habla" else "¿Algo más?", style = MaterialTheme.typography.headlineSmall)
                Text("Habla con naturalidad: «dos barras de pan, leche y unas manzanas».", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Box(Modifier.size(200.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    // Ondas que crecen con la voz
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2
                        if (listening) for (i in 0..2) { val p = (wave + i / 3f) % 1f
                            drawCircle(primary.copy(alpha = (1f - p) * (0.18f + 0.35f * smooth)), r * (0.42f + 0.55f * p * (0.6f + smooth)), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())) }
                        drawCircle(primary.copy(alpha = 0.12f), r * (0.5f + 0.25f * smooth))
                    }
                    FilledIconButton({ if (listening) recognizer?.stopListening() else start() }, Modifier.size(92.dp).graphicsLayer { scaleX = 1f + 0.12f * smooth; scaleY = 1f + 0.12f * smooth },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (listening) primary else accent,
                            contentColor = if (listening) MaterialTheme.colorScheme.onPrimary else androidx.compose.ui.graphics.Color(0xFF1B2428))) {
                        Icon(if (listening) Icons.Filled.GraphicEq else Icons.Filled.Mic, if (listening) "Parar" else "Hablar", Modifier.size(40.dp))
                    }
                }
                // Barras de nivel
                Canvas(Modifier.fillMaxWidth().height(36.dp)) {
                    val n = 21; val w = size.width / n
                    for (i in 0 until n) {
                        val env = 1f - kotlin.math.abs(i - n / 2f) / (n / 2f)
                        val h = size.height * (0.12f + (if (listening) smooth else 0f) * env * (0.55f + 0.45f * kotlin.math.sin(phase + i * 0.7f).let { (it + 1f) / 2f }))
                        drawRoundRect(if (i % 4 == 0) accent else primary, Offset(i * w + w * 0.25f, (size.height - h) / 2), Size(w * 0.5f, h), CornerRadius(w * 0.25f))
                    }
                }
                // Texto en directo
                Surface(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(top = 12.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    AnimatedContent(listOf(text, partial).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "…" }, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                        label = "txt") { t -> Text(t, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge) }
                }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton({ recognizer?.cancel(); onDismiss() }, Modifier.weight(1f)) { Text("Cancelar") }
                    Button({ recognizer?.cancel(); onDone(listOf(text, partial).filter { it.isNotBlank() }.joinToString(", ")) }, Modifier.weight(1f),
                        enabled = text.isNotBlank() || partial.isNotBlank()) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp)); Text("Listo") }
                }
            }
        }
    }
}
