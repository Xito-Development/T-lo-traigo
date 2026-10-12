package es.unocerobits.tlotraigo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CartLine(val product: Product, val qty: Int) { val total get() = product.price * qty }

/** Cesta persistente: no se pierde si se cierra la app (importante para personas mayores). */
class Cart(context: Context) {
    private val prefs = context.getSharedPreferences("cesta", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _lines = MutableStateFlow(load())
    val lines: StateFlow<Map<Long, CartLine>> = _lines

    private fun load(): Map<Long, CartLine> = runCatching {
        json.decodeFromString<List<CartLine>>(prefs.getString("l", "[]")!!).associateBy { it.product.id }
    }.getOrDefault(emptyMap())

    private fun put(m: Map<Long, CartLine>) {
        _lines.value = m
        prefs.edit().putString("l", json.encodeToString(m.values.toList())).apply()
    }

    fun qty(id: Long) = _lines.value[id]?.qty ?: 0
    fun add(p: Product) = set(p, qty(p.id) + 1)
    fun remove(p: Product) = set(p, qty(p.id) - 1)
    fun set(p: Product, q: Int) = put(if (q <= 0) _lines.value - p.id else _lines.value + (p.id to CartLine(p, q.coerceAtMost(99))))
    fun clear() = put(emptyMap())
    fun replaceWith(items: List<Pair<Product, Int>>) = put(items.associate { (p, q) -> p.id to CartLine(p, q) })
}
