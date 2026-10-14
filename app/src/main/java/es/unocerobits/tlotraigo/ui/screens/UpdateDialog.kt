package es.unocerobits.tlotraigo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import es.unocerobits.tlotraigo.BuildConfig
import es.unocerobits.tlotraigo.data.ApiException
import es.unocerobits.tlotraigo.data.UpdateInfo
import es.unocerobits.tlotraigo.data.Updater
import es.unocerobits.tlotraigo.ui.components.BigButton
import es.unocerobits.tlotraigo.ui.components.ErrorText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

private sealed interface Paso {
    data object Info : Paso
    data class Bajando(val p: Float) : Paso
    data class Listo(val file: File) : Paso
}

/** Diálogo de actualización: novedades → descarga animada → instalar. */
@Composable
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var paso by remember { mutableStateOf<Paso>(Paso.Info) }
    var err by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var askPermission by remember { mutableStateOf(false) }

    // Al volver de los ajustes de "instalar apps desconocidas", instalar directamente si ya hay permiso.
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(paso, askPermission) {
        val p = paso
        if (p is Paso.Listo && askPermission) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (Updater.canInstall(ctx)) { askPermission = false; Updater.install(ctx, p.file) }
        }
    }

    fun startDownload() {
        err = null; paso = Paso.Bajando(0f)
        job = scope.launch {
            try { val f = Updater.download(ctx, info) { paso = Paso.Bajando(it) }; paso = Paso.Listo(f) }
            catch (e: ApiException) { err = e.message; paso = Paso.Info }
        }
    }
    fun install(f: File) {
        if (Updater.canInstall(ctx)) Updater.install(ctx, f) else { askPermission = true; Updater.openInstallPermission(ctx) }
    }

    Dialog(onDismissRequest = { if (paso !is Paso.Bajando) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.92f).widthIn(max = 520.dp), shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val progress = when (val p = paso) { is Paso.Bajando -> p.p; is Paso.Listo -> 1f; else -> 0f }
                DownloadAnimation(progress, downloading = paso is Paso.Bajando, done = paso is Paso.Listo)
                Spacer(Modifier.height(12.dp))
                AnimatedContent(paso::class, transitionSpec = { (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut() }, label = "paso") { k ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        when (k) {
                            Paso.Info::class -> {
                                Text("¡Nueva versión ${info.version}!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                                Text("Tienes la ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (info.notes.isNotBlank()) Surface(Modifier.fillMaxWidth().padding(top = 12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
                                    Text(info.notes, Modifier.padding(14.dp).heightIn(max = 220.dp), style = MaterialTheme.typography.bodyMedium)
                                }
                                ErrorText(err)
                                Spacer(Modifier.height(16.dp))
                                BigButton("Actualizar ahora" + if (info.size > 0) " (${"%.1f".format(info.size / 1048576f)} MB)" else "", { startDownload() }, icon = Icons.Filled.Download)
                                TextButton(onDismiss) { Text("Más tarde") }
                                if (BuildConfig.PLAY_PUBLISHED) TextButton({ runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(BuildConfig.PLAY_URL))
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }) { Text("Prefiero Google Play (antes desinstala esta versión)") }
                            }
                            Paso.Bajando::class -> {
                                Text("Descargando…", style = MaterialTheme.typography.headlineSmall)
                                Text("No cierres la app, solo tarda un momento.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton({ job?.cancel(); paso = Paso.Info }) { Text("Cancelar") }
                            }
                            else -> {
                                Text("¡Lista para instalar!", style = MaterialTheme.typography.headlineSmall)
                                if (!Updater.canInstall(ctx)) Text("La primera vez Android te pedirá permitir que T lo traigo instale actualizaciones. Actívalo y vuelve aquí.",
                                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                BigButton("Instalar ahora", { (paso as? Paso.Listo)?.let { install(it.file) } }, icon = Icons.Filled.InstallMobile)
                                TextButton(onDismiss) { Text("Instalar más tarde") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Anillo de progreso + bolsa de la compra que se llena como un líquido + estela de reparto. */
@Composable
fun DownloadAnimation(progress: Float, downloading: Boolean, done: Boolean, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(progress, spring(dampingRatio = .9f, stiffness = 60f), label = "p")
    val inf = rememberInfiniteTransition(label = "inf")
    val wave by inf.animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "wave")
    val trail by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing)), label = "trail")
    val breathe by inf.animateFloat(.96f, 1.04f, infiniteRepeatable(tween(1100, easing = EaseInOutSine), RepeatMode.Reverse), label = "b")
    val pop by animateFloatAsState(if (done) 1f else 0f, spring(dampingRatio = .45f, stiffness = 220f), label = "pop")

    val ring = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val bagLine = MaterialTheme.colorScheme.onSurface.copy(alpha = .25f)
    val fill = Color(0xFFF2B544); val fillDark = Color(0xFFDE9A2A); val leaf = Color(0xFF5BB08A); val leaf2 = Color(0xFF7BC9A4)
    val trailColor = MaterialTheme.colorScheme.primary

    Box(modifier.size(220.dp).scale(if (downloading) breathe else 1f + pop * .04f), Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2 + 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val sweep = 360f * p
            drawArc(Brush.sweepGradient(listOf(ring.copy(alpha = .55f), ring, ring)), -90f, sweep, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
            if (p in 0.01f..0.995f) { // punto brillante en la cabeza del arco
                val a = Math.toRadians((sweep - 90f).toDouble()); val r = arcSize.width / 2
                val c = Offset(center.x + (r * kotlin.math.cos(a)).toFloat(), center.y + (r * sin(a)).toFloat())
                drawCircle(ring.copy(alpha = .25f), stroke * 1.3f, c); drawCircle(Color.White, stroke * .32f, c)
            }

            // Bolsa (mismas proporciones que el icono de la app, viewport 108)
            val s = size.minDimension / 108f * .62f
            val ox = center.x - 56f * s; val oy = center.y - 62f * s
            fun pt(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
            val bag = Path().apply {
                moveTo(pt(31f, 50f).x, pt(31f, 50f).y); lineTo(pt(81f, 50f).x, pt(81f, 50f).y); lineTo(pt(81f, 78f).x, pt(81f, 78f).y)
                quadraticBezierTo(pt(81f, 84f).x, pt(81f, 84f).y, pt(75f, 84f).x, pt(75f, 84f).y); lineTo(pt(37f, 84f).x, pt(37f, 84f).y)
                quadraticBezierTo(pt(31f, 84f).x, pt(31f, 84f).y, pt(31f, 78f).x, pt(31f, 78f).y); close()
            }
            // Hojas que asoman al terminar
            if (pop > 0f) {
                val lift = (1f - pop) * 14f
                val l1 = Path().apply { moveTo(pt(66f, 50f + lift).x, pt(66f, 50f + lift).y); cubicTo(pt(64f, 42f + lift).x, pt(64f, 42f + lift).y, pt(66f, 37f + lift).x, pt(66f, 37f + lift).y, pt(71f, 34f + lift).x, pt(71f, 34f + lift).y)
                    cubicTo(pt(73f, 39f + lift).x, pt(73f, 39f + lift).y, pt(72f, 45f + lift).x, pt(72f, 45f + lift).y, pt(66f, 50f + lift).x, pt(66f, 50f + lift).y) }
                val l2 = Path().apply { moveTo(pt(68f, 50f + lift).x, pt(68f, 50f + lift).y); cubicTo(pt(70f, 43f + lift).x, pt(70f, 43f + lift).y, pt(75f, 40f + lift).x, pt(75f, 40f + lift).y, pt(81f, 40f + lift).x, pt(81f, 40f + lift).y)
                    cubicTo(pt(80f, 46f + lift).x, pt(80f, 46f + lift).y, pt(75f, 50f + lift).x, pt(75f, 50f + lift).y, pt(68f, 50f + lift).x, pt(68f, 50f + lift).y) }
                drawPath(l1, leaf2.copy(alpha = pop.coerceIn(0f, 1f))); drawPath(l2, leaf.copy(alpha = pop.coerceIn(0f, 1f)))
            }
            // Asa
            val handle = Path().apply { moveTo(pt(40f, 51f).x, pt(40f, 51f).y); cubicTo(pt(40f, 39f).x, pt(40f, 39f).y, pt(56f, 39f).x, pt(56f, 39f).y, pt(56f, 51f).x, pt(56f, 51f).y) }
            drawPath(handle, bagLine, style = Stroke(3f * s, cap = StrokeCap.Round))
            // Contenido que sube con ola
            val top = pt(0f, 84f).y - (34f * s) * p
            val liquid = Path().apply {
                val x0 = pt(31f, 0f).x; val x1 = pt(81f, 0f).x; val amp = if (done) 0f else 2.2f * s
                moveTo(x0, size.height); lineTo(x0, top)
                var x = x0; while (x <= x1) { lineTo(x, top + amp * sin((x - x0) / (12f * s) + wave)); x += 2f }
                lineTo(x1, size.height); close()
            }
            clipPath(bag) {
                drawRect(Color.White.copy(alpha = .06f))
                drawPath(liquid, fill)
                drawRect(fillDark, topLeft = pt(31f, 50f), size = Size(50f * s, 5f * s), alpha = (p * 3f).coerceIn(0f, 1f))
            }
            drawPath(bag, bagLine, style = Stroke(2.2f * s))
            // Casa o tic al terminar
            if (done) {
                val c = pt(56f, 69f)
                drawCircle(Color.White, 9f * s * pop, c)
                val ck = Path().apply { moveTo(c.x - 4f * s, c.y); lineTo(c.x - 1f * s, c.y + 3.2f * s); lineTo(c.x + 4.5f * s, c.y - 3.5f * s) }
                drawPath(ck, ring, style = Stroke(2.2f * s * pop, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                val house = Path().apply { moveTo(pt(56f, 60f).x, pt(56f, 60f).y); lineTo(pt(66f, 68f).x, pt(66f, 68f).y); lineTo(pt(63f, 68f).x, pt(63f, 68f).y)
                    lineTo(pt(63f, 78f).x, pt(63f, 78f).y); lineTo(pt(49f, 78f).x, pt(49f, 78f).y); lineTo(pt(49f, 68f).x, pt(49f, 68f).y); lineTo(pt(46f, 68f).x, pt(46f, 68f).y); close() }
                drawPath(house, Color(0xFFFFF8EC).copy(alpha = .4f + .6f * p))
            }
            // Estela de reparto mientras descarga
            if (downloading) for (i in 0..2) {
                val y = pt(0f, 60f + i * 7f).y
                val len = (8f - i * 1.5f) * s
                val shift = ((trail + i * .33f) % 1f) * 10f * s
                val x = pt(28f, 0f).x - shift
                drawLine(trailColor.copy(alpha = (1f - (trail + i * .33f) % 1f) * .8f), Offset(x - len, y), Offset(x, y), 3f * s, StrokeCap.Round)
            }
        }
        // Porcentaje
        if (!done && progress > 0f) Text("${(p * 100).toInt()} %", Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
