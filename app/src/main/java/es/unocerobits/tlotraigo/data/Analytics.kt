package es.unocerobits.tlotraigo.data

import android.content.Context
import es.unocerobits.tlotraigo.BuildConfig
import es.unocerobits.tlotraigo.Locator
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Estadísticas de uso y registro de errores propios (se guardan en Supabase, en la UE).
 * Solo se envía algo si el usuario lo ha aceptado (Ajustes / bienvenida). Sin terceros ni publicidad.
 */
object Analytics {
    private lateinit var prefs: android.content.SharedPreferences
    private val queue = mutableListOf<JsonObject>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val visitor by lazy { prefs.getString("vid", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("vid", it).apply() } }

    var consent: Boolean
        get() = prefs.getBoolean("consent", false)
        set(v) { prefs.edit().putBoolean("consent", v).putBoolean("asked", true).apply(); if (!v) synchronized(queue) { queue.clear() } }
    val asked get() = prefs.getBoolean("asked", false)

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("estadisticas", Context.MODE_PRIVATE)
        // Errores no controlados: se guardan y se envían en el siguiente arranque
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { prefs.edit().putString("crash", (e::class.java.simpleName + ": " + e.message + "\n" + e.stackTrace.take(12).joinToString("\n")).take(3500)).commit() }
            prev?.uncaughtException(t, e)
        }
        prefs.getString("crash", null)?.let { error("crash", it); prefs.edit().remove("crash").apply() }
    }

    private fun add(kind: String, name: String, detail: JsonObject? = null) {
        if (!consent) return
        val e = buildJsonObject {
            put("source", "app"); put("kind", kind); put("name", name); put("visitor", visitor)
            put("app_version", BuildConfig.VERSION_NAME + " (" + android.os.Build.VERSION.SDK_INT + ")"); detail?.let { put("detail", it) }
        }
        synchronized(queue) { queue += e; if (queue.size >= 10 || kind == "error") flush() }
    }
    fun screen(name: String) = add("view", name)
    fun event(name: String) = add("event", name)
    fun error(where: String, text: String) = add("error", where, buildJsonObject { put("msg", text.take(3500)) })

    fun flush() {
        val batch = synchronized(queue) { if (queue.isEmpty()) return; queue.toList().also { queue.clear() } }
        scope.launch { runCatching { Locator.repo.track(JsonArray(batch)) } }
    }
}
