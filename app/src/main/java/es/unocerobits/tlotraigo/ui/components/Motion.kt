package es.unocerobits.tlotraigo.ui.components

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** ¿El usuario ha quitado las animaciones en Android (Opciones de accesibilidad)? Entonces no animamos. */
@Composable
fun reducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember { runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false) }
}

/** Curvas y duraciones comunes: suaves y con un pequeño rebote, estilo Material 3 «expressive». */
object Motion {
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    fun <T> enter(ms: Int = 380) = tween<T>(ms, easing = emphasized)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
}

data class Celebration(val title: String, val subtitle: String? = null, val emoji: String = "✓")

/** Muestra una celebración breve (pedido hecho, aceptado, entregado…) desde cualquier pantalla. */
val LocalCelebrate = staticCompositionLocalOf<(Celebration) -> Unit> { {} }

/** Capa de celebración: círculo que crece, tic que se dibuja, confeti suave y cierre automático. */
@Composable
fun CelebrationOverlay(c: Celebration?, onDone: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    AnimatedVisibility(c != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(250))) {
        val cel = c ?: return@AnimatedVisibility
        val circle = remember(cel) { Animatable(0f) }
        val check = remember(cel) { Animatable(0f) }
        val burst = remember(cel) { Animatable(0f) }
        val particles = remember(cel) { List(28) { Triple(Random.nextFloat() * 360f, 0.55f + Random.nextFloat() * 0.6f, Random.nextInt(4)) } }
        LaunchedEffect(cel) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            circle.animateTo(1f, Motion.bouncy())
        }
        LaunchedEffect(cel) { delay(180); check.animateTo(1f, tween(420, easing = Motion.emphasized)) }
        LaunchedEffect(cel) { delay(250); burst.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
        LaunchedEffect(cel) { delay(1900); onDone() }
        val primary = MaterialTheme.colorScheme.primary
        val colors = listOf(Color(0xFFF2B544), Color(0xFF7BC9A4), primary, Color(0xFFDE9A2A))
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
            Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 8.dp, shadowElevation = 12.dp, modifier = Modifier.padding(32.dp).scale(0.85f + 0.15f * circle.value)) {
                Column(Modifier.padding(horizontal = 32.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2
                            // Confeti
                            particles.forEach { (ang, dist, ci) ->
                                val a = Math.toRadians(ang.toDouble()); val d = r * (0.6f + dist * burst.value)
                                val p = Offset(center.x + (cos(a) * d).toFloat(), center.y + (sin(a) * d).toFloat() + 30f * burst.value * burst.value)
                                rotate(ang + 200f * burst.value, p) {
                                    drawRoundRect(colors[ci].copy(alpha = (1f - burst.value).coerceIn(0f, 1f)), topLeft = Offset(p.x - 5f, p.y - 2.5f),
                                        size = androidx.compose.ui.geometry.Size(10f, 5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f))
                                }
                            }
                            drawCircle(primary.copy(alpha = .15f), r * 0.78f * circle.value)
                            drawCircle(primary, r * 0.6f * circle.value)
                            // Tic que se dibuja
                            val path = Path().apply { moveTo(center.x - r * .26f, center.y + r * .02f); lineTo(center.x - r * .06f, center.y + r * .22f); lineTo(center.x + r * .3f, center.y - r * .2f) }
                            val pm = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
                            val seg = Path(); pm.getSegment(0f, pm.length * check.value, seg, true)
                            drawPath(seg, Color.White, style = Stroke(r * .1f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(cel.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    cel.subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

/** Interruptor animado: icono ✓/✕ que gira y aparece, pequeño rebote y vibración al cambiar. */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val haptic = LocalHapticFeedback.current
    var bump by remember { mutableIntStateOf(0) }
    val sc = remember { Animatable(1f) }
    LaunchedEffect(bump) { if (bump > 0) { sc.animateTo(1.12f, tween(90)); sc.animateTo(1f, Motion.bouncy()) } }
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { cb -> { v: Boolean -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); bump++; cb(v) } },
        modifier = modifier.scale(sc.value), enabled = enabled,
        thumbContent = {
            AnimatedContent(checked, transitionSpec = {
                (scaleIn(Motion.bouncy()) + fadeIn() togetherWith scaleOut(tween(120)) + fadeOut(tween(120)))
            }, label = "thumb") { on ->
                val rot by animateFloatAsState(if (on) 0f else -90f, Motion.bouncy(), label = "rot")
                androidx.compose.material3.Icon(if (on) androidx.compose.material.icons.Icons.Filled.CheckCircle.let { androidx.compose.material.icons.Icons.Default.Check } else androidx.compose.material.icons.Icons.Default.Close, null,
                    Modifier.size(SwitchDefaults.IconSize).graphicsLayerRotate(rot))
            }
        },
        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedIconColor = MaterialTheme.colorScheme.primary, uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}
private fun Modifier.graphicsLayerRotate(deg: Float) = this.then(Modifier.rotateCompat(deg))
private fun Modifier.rotateCompat(deg: Float): Modifier = this.then(Modifier.rotate(deg))
