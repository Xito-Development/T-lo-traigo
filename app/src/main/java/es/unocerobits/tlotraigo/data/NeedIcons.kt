package es.unocerobits.tlotraigo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** Iconos personalizados de la Lista fácil (los elige cada usuario manteniendo pulsado un artículo; se guardan en el móvil). */
object NeedIcons {
    val map = MutableStateFlow<Map<Long, String>>(emptyMap())
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("iconos_lista", Context.MODE_PRIVATE)
    fun load(ctx: Context) { map.value = prefs(ctx).all.mapNotNull { (k, v) -> k.toLongOrNull()?.let { id -> (v as? String)?.let { id to it } } }.toMap() }
    fun set(ctx: Context, needId: Long, icon: String?) {
        prefs(ctx).edit().apply { if (icon == null) remove(needId.toString()) else putString(needId.toString(), icon) }.apply()
        map.value = if (icon == null) map.value - needId else map.value + (needId to icon)
    }
}
