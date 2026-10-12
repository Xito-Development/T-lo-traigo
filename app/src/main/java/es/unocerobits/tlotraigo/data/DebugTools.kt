package es.unocerobits.tlotraigo.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class HttpLog(val time: Long, val method: String, val path: String, val code: Int, val ms: Long, val error: String? = null)

/** Herramientas de depuración en tiempo de ejecución (solo visibles para administradores). */
object DebugTools {
    @Volatile var extraLatencyMs: Long = 0
    @Volatile var failNext: Boolean = false
    private val _log = MutableStateFlow<List<HttpLog>>(emptyList())
    val log: StateFlow<List<HttpLog>> = _log

    fun record(e: HttpLog) = _log.update { (listOf(e) + it).take(60) }
    fun clearLog() { _log.value = emptyList() }
}
