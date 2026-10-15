package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import es.unocerobits.tlotraigo.ui.components.Motion
import es.unocerobits.tlotraigo.ui.components.Wordmark
import es.unocerobits.tlotraigo.ui.components.reducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Pine = Color(0xFF2E6B57); private val PineDeep = Color(0xFF1F4D3F); private val Mustard = Color(0xFFF2B544)
private val MustardDeep = Color(0xFFDE9A2A); private val Cream = Color(0xFFFFF8EC); private val Leaf = Color(0xFF5BB08A); private val Leaf2 = Color(0xFF7BC9A4)

/**
 * Animación de arranque (≈1,8 s): ondas que se expanden, la bolsa cae con rebote, brotan las hojas, aparece la casa,
 * estela de reparto, el nombre se revela de izquierda a derecha y la sonrisa se dibuja. Al terminar llama a [onDone].
 */
@Composable
fun AnimatedSplash(onDone: () -> Unit) {
    val reduced = reducedMotion()
    val drop = remember { Animatable(if (reduced) 0f else -1f) }
    val leaves = remember { Animatable(if (reduced) 1f else 0f) }
    val house = remember { Animatable(if (reduced) 1f else 0f) }
    val trail = remember { Animatable(if (reduced) 1f else 0f) }
    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    val tagline = remember { Animatable(if (reduced) 1f else 0f) }
    val inf = rememberInfiniteTransition(label = "splash")
    val wave by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "wave")
    LaunchedEffect(Unit) {
        if (reduced) { delay(300); onDone(); return@LaunchedEffect }
        launch { drop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 260f)) }
        launch { delay(380); leaves.animateTo(1f, spring(0.4f, 300f)) }
        launch { delay(480); house.animateTo(1f, spring(0.5f, 400f)) }
        launch { delay(560); trail.animateTo(1f, tween(700, easing = Motion.emphasized)) }
        launch { delay(650); reveal.animateTo(1f, tween(650, easing = Motion.emphasized)) }
        launch { delay(1050); tagline.animateTo(1f, tween(450, easing = Motion.emphasized)) }
        delay(1850); onDone()
    }
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF3A8A70), Pine, PineDeep), radius = 1400f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(220.dp)) {
                val c = center; val u = size.minDimension / 108f
                // Ondas que se expanden desde la bolsa
                for (i in 0..2) {
                    val p = (wave + i / 3f) % 1f
                    drawCircle(Cream.copy(alpha = (1f - p) * 0.18f * (1f - drop.value * -1f).coerceIn(0f, 1f)), radius = size.minDimension * (0.25f + 0.3f * p), center = c, style = Stroke(2.dp.toPx()))
                }
                drawCircle(Color.White.copy(alpha = 0.08f), size.minDimension * 0.42f, c)
                val dy = drop.value * size.height * 0.9f
                val squash = 1f + (if (drop.value > -0.05f) (0.05f - kotlin.math.abs(drop.value)).coerceAtLeast(0f) * 2f else 0f)
                fun pt(x: Float, y: Float) = Offset(c.x + (x - 56f) * u, c.y + (y - 62f) * u + dy)
                // Estela de reparto
                for (i in 0..2) {
                    val y = pt(0f, 62f + i * 7f).y; val len = (14f - i * 3f) * u * trail.value
                    val x = pt(26f, 0f).x - (1f - trail.value) * 30f * u
                    if (trail.value > 0f) drawLine(Cream.copy(alpha = 0.85f - i * 0.2f), Offset(x - len, y), Offset(x, y), 3.2f * u, StrokeCap.Round)
                }
                scale(1f / squash, squash, pivot = pt(56f, 84f)) {
                    // Hojas que brotan
                    if (leaves.value > 0f) {
                        rotate(-25f * (1f - leaves.value), pt(66f, 50f)) { scale(leaves.value, pivot = pt(66f, 50f)) {
                            drawPath(Path().apply { moveTo(pt(66f, 50f).x, pt(66f, 50f).y); cubicTo(pt(64f, 42f).x, pt(64f, 42f).y, pt(66f, 37f).x, pt(66f, 37f).y, pt(71f, 34f).x, pt(71f, 34f).y)
                                cubicTo(pt(73f, 39f).x, pt(73f, 39f).y, pt(72f, 45f).x, pt(72f, 45f).y, pt(66f, 50f).x, pt(66f, 50f).y) }, Leaf2) } }
                        rotate(30f * (1f - leaves.value), pt(68f, 50f)) { scale(leaves.value, pivot = pt(68f, 50f)) {
                            drawPath(Path().apply { moveTo(pt(68f, 50f).x, pt(68f, 50f).y); cubicTo(pt(70f, 43f).x, pt(70f, 43f).y, pt(75f, 40f).x, pt(75f, 40f).y, pt(81f, 40f).x, pt(81f, 40f).y)
                                cubicTo(pt(80f, 46f).x, pt(80f, 46f).y, pt(75f, 50f).x, pt(75f, 50f).y, pt(68f, 50f).x, pt(68f, 50f).y) }, Leaf) } }
                    }
                    // Asa y bolsa
                    drawPath(Path().apply { moveTo(pt(40f, 51f).x, pt(40f, 51f).y); cubicTo(pt(40f, 39f).x, pt(40f, 39f).y, pt(56f, 39f).x, pt(56f, 39f).y, pt(56f, 51f).x, pt(56f, 51f).y) },
                        Cream, style = Stroke(3.2f * u, cap = StrokeCap.Round))
                    val bag = Path().apply { moveTo(pt(31f, 50f).x, pt(31f, 50f).y); lineTo(pt(81f, 50f).x, pt(81f, 50f).y); lineTo(pt(81f, 78f).x, pt(81f, 78f).y)
                        quadraticTo(pt(81f, 84f).x, pt(81f, 84f).y, pt(75f, 84f).x, pt(75f, 84f).y); lineTo(pt(37f, 84f).x, pt(37f, 84f).y)
                        quadraticTo(pt(31f, 84f).x, pt(31f, 84f).y, pt(31f, 78f).x, pt(31f, 78f).y); close() }
                    drawPath(bag, Mustard)
                    drawRect(MustardDeep, topLeft = pt(31f, 50f), size = Size(50f * u, 5f * u))
                    // Casa
                    if (house.value > 0f) scale(house.value, pivot = pt(56f, 80f)) {
                        drawPath(Path().apply { moveTo(pt(56f, 58f).x, pt(56f, 58f).y); lineTo(pt(69f, 68f).x, pt(69f, 68f).y); lineTo(pt(65.5f, 68f).x, pt(65.5f, 68f).y)
                            lineTo(pt(65.5f, 80f).x, pt(65.5f, 80f).y); lineTo(pt(46.5f, 80f).x, pt(46.5f, 80f).y); lineTo(pt(46.5f, 68f).x, pt(46.5f, 68f).y); lineTo(pt(43f, 68f).x, pt(43f, 68f).y); close() }, Cream)
                        drawRect(Pine, topLeft = pt(53.5f, 72f), size = Size(5f * u, 8f * u))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Nombre que se revela de izquierda a derecha
            Box(Modifier.drawWithContent { clipRect(right = size.width * reveal.value) { this@drawWithContent.drawContent() } }) {
                Wordmark(width = 250.dp, text = Cream, smile = Mustard)
            }
            Spacer(Modifier.height(14.dp))
            Text("La compra del súper, en la puerta de casa", style = MaterialTheme.typography.bodyLarge, color = Cream.copy(alpha = 0.9f * tagline.value),
                modifier = Modifier.offset(y = (12f * (1f - tagline.value)).dp))
        }
    }
}
