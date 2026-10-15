package es.unocerobits.tlotraigo.ui.components

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.interaction.collectIsPressedAsState

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import es.unocerobits.tlotraigo.data.Town
import es.unocerobits.tlotraigo.R
import es.unocerobits.tlotraigo.ui.theme.LocalHighContrast
import es.unocerobits.tlotraigo.ui.theme.LocalSenior
import java.text.NumberFormat
import java.util.Locale

private val es = Locale("es", "ES")
private val money = NumberFormat.getCurrencyInstance(es)
fun eur(v: Double): String = money.format(v)
fun productos(n: Int) = if (n == 1) "1 producto" else "$n productos"

fun categoryIcon(name: String): ImageVector = AppIcons.vector(name)
fun productIcon(p: es.unocerobits.tlotraigo.data.Product, categoryKey: String? = null): ImageVector =
    AppIcons.find(p.icon)?.vector ?: AppIcons.vector(categoryKey)

/** Lectura en voz alta (TTS del sistema, disponible desde Android 5.0). */
class Speaker(ctx: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(ctx.applicationContext, this)
    private var ready = false
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) { tts.language = es; tts.setSpeechRate(0.9f); ready = true }
    }
    fun say(text: String) { if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tlt") }
    fun shutdown() = tts.shutdown()
}
val LocalSpeaker = staticCompositionLocalOf<Speaker?> { null }

fun Context.dial(phone: String) = safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
fun Context.openMap(address: String) = safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address))))
/** Navegación paso a paso hasta la dirección (Google Maps u otra app de mapas). */
fun Context.navigateTo(address: String) {
    val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(address)))
    if (nav.resolveActivity(packageManager) != null) safeStart(nav) else openMap(address)
}
fun Context.share(text: String) = safeStart(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
private fun Context.safeStart(i: Intent) = try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: ActivityNotFoundException) {}

@Composable
fun bigH(): Dp = if (LocalSenior.current) 64.dp else 54.dp

@Composable
fun BigButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    icon: ImageVector? = null, enabled: Boolean = true, loading: Boolean = false, tonal: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val click = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }
    val content: @Composable RowScope.() -> Unit = {
        AnimatedContent(loading, label = "btn") { l ->
            if (l) CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp, color = LocalContentColor.current)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let { Icon(it, null, Modifier.size(26.dp)); Spacer(Modifier.width(10.dp)) }
                Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.96f else 1f, androidx.compose.animation.core.spring(0.5f, 600f), label = "press")
    val m = modifier.fillMaxWidth().heightIn(min = bigH()).graphicsLayer { scaleX = sc; scaleY = sc }
    val shape = MaterialTheme.shapes.medium
    if (tonal) FilledTonalButton(click, m, enabled && !loading, shape, interactionSource = src, content = content)
    else Button(click, m, enabled && !loading, shape,
        border = if (LocalHighContrast.current) BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary) else null, interactionSource = src, content = content)
}

@Composable
fun QtyStepper(qty: Int, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val size = if (LocalSenior.current) 52.dp else 44.dp
    Row(
        modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton({ haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onMinus() }, Modifier.size(size)) {
            Icon(if (qty <= 1) Icons.Filled.DeleteOutline else Icons.Filled.Remove, "Quitar uno", tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        AnimatedContent(qty, transitionSpec = {
            if (targetState > initialState) (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            else (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
        }, label = "qty") {
            Text("$it", Modifier.widthIn(min = 28.dp), style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        IconButton({ haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlus() }, Modifier.size(size)) {
            Icon(Icons.Filled.Add, "Añadir uno", tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun SpeakButton(text: String) {
    val sp = LocalSpeaker.current ?: return
    IconButton({ sp.say(text) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Leer en voz alta", tint = MaterialTheme.colorScheme.primary) }
}

@Composable
fun Loading(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
    CircularProgressIndicator()
}

@Composable
fun Message(icon: ImageVector, title: String, body: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), Alignment.Center) {
            Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        body?.let { Spacer(Modifier.height(6.dp)); Text(it, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (action != null && onAction != null) { Spacer(Modifier.height(20.dp)); BigButton(action, onAction, tonal = true) }
    }
}

@Composable
fun ErrorText(msg: String?) {
    AnimatedVisibility(msg != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Text(msg.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Selector de opción grande, accesible (toda la fila es pulsable). */
@Composable
fun ChoiceRow(title: String, subtitle: String?, selected: Boolean, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val hc = LocalHighContrast.current
    val border by animateDpAsState(if (selected) 3.dp else if (hc) 2.dp else 1.dp, label = "b")
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(MaterialTheme.shapes.medium).clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium, border = BorderStroke(border, color),
        color = if (selected && !hc) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f) else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(16.dp).alpha(if (enabled) 1f else .45f), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            RadioButton(selected, null, enabled = enabled)
        }
    }
}


@Composable
fun TownPicker(towns: List<Town>, selected: Long?, onPick: (Town) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = towns.firstOrNull { it.id == selected }
    OutlinedCard(Modifier.fillMaxWidth().clickable { open = true }, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Pueblo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(current?.name ?: "Toca para elegir", style = MaterialTheme.typography.titleMedium)
            }
            Icon(Icons.Filled.ExpandMore, null)
        }
    }
    if (open) Dialog({ open = false }) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(Modifier.padding(vertical = 16.dp)) {
                Text("¿En qué pueblo vives?", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(towns, key = { it.id }) { t ->
                        ListItem(
                            headlineContent = { Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = if (t.id == selected) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = { Text("Envío ${eur(t.deliveryFee)}") },
                            trailingContent = { if (t.id == selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            modifier = Modifier.clickable { onPick(t); open = false }.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Pequeño rebote cuando cambia un valor (p. ej. contador de la cesta). */
@Composable
fun Modifier.bump(key: Any): Modifier {
    val s = remember { Animatable(1f) }
    LaunchedEffect(key) { s.snapTo(1.25f); s.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
    return this.scale(s.value)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier.padding(top = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.titleLarge)

/** Logotipo de texto «T lo traigo» (Baloo 2) con la sonrisa de reparto; se tiñe con los colores del tema. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, width: Dp = 220.dp, text: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
             smile: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF2B544)) {
    Box(modifier.width(width).aspectRatio(4600f / 1240f)) {
        Icon(androidx.compose.ui.res.painterResource(R.drawable.wordmark_text), "T lo traigo", Modifier.fillMaxSize(), tint = text)
        Icon(androidx.compose.ui.res.painterResource(R.drawable.wordmark_swoosh), null, Modifier.fillMaxSize(), tint = smile)
    }
}

fun Context.openUrl(url: String) = safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

fun Context.safeStartPublic(i: Intent) = safeStart(i)

/** Distintivo de la forma de pago con la estética de la app. */
@Composable
fun PayBadge(method: String, modifier: Modifier = Modifier) {
    val (label, icon, bg, fg) = when (method) {
        "bizum" -> listOf("Bizum", Icons.Filled.Smartphone, androidx.compose.ui.graphics.Color(0xFF0E8A8A), androidx.compose.ui.graphics.Color.White)
        "tarjeta" -> listOf("Tarjeta", Icons.Filled.CreditCard, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else -> listOf("Efectivo", Icons.Filled.Payments, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }
    Surface(modifier, shape = CircleShape, color = bg as androidx.compose.ui.graphics.Color) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null, Modifier.size(18.dp), tint = fg as androidx.compose.ui.graphics.Color)
            Spacer(Modifier.width(6.dp)); Text(label as String, style = MaterialTheme.typography.labelLarge, color = fg)
        }
    }
}
