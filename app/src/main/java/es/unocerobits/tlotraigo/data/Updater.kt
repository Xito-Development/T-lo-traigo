package es.unocerobits.tlotraigo.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import es.unocerobits.tlotraigo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

@Serializable
private data class GhAsset(val name: String, @SerialName("browser_download_url") val url: String, val size: Long = 0)

@Serializable
private data class GhRelease(@SerialName("tag_name") val tag: String, val body: String? = null, val assets: List<GhAsset> = emptyList())

data class UpdateInfo(val version: String, val notes: String, val url: String, val size: Long)

/** Actualizador de la versión de GitHub: consulta la última Release, descarga el APK y lanza el instalador. */
object Updater {
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true).followSslRedirects(true).build()
    private val json = Json { ignoreUnknownKeys = true }

    val enabled get() = BuildConfig.SELF_UPDATE

    /** Devuelve la actualización si la Release publicada es más nueva que la instalada. */
    suspend fun check(current: String = BuildConfig.VERSION_NAME): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        val req = Request.Builder().url(BuildConfig.RELEASES_API)
            .header("Accept", "application/vnd.github+json").header("User-Agent", "TLoTraigo-Android").build()
        try {
            http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext null
                val rel = json.decodeFromString<GhRelease>(r.body?.string().orEmpty())
                val v = rel.tag.removePrefix("v")
                val apk = rel.assets.firstOrNull { it.name == "TLoTraigo.apk" } ?: rel.assets.firstOrNull { it.name.endsWith(".apk") }
                if (apk != null && isNewer(v, current)) UpdateInfo(v, cleanNotes(rel.body), apk.url, apk.size) else null
            }
        } catch (e: IOException) { null }
    }

    /** 0.4.1 > 0.4.0; una versión de desarrollo (0.4.1-dev.7) es anterior a su versión final (0.4.1). */
    fun isNewer(remote: String, local: String): Boolean {
        fun core(s: String) = s.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
        val a = core(remote); val b = core(local)
        for (i in 0..2) if (a[i] != b[i]) return a[i] > b[i]
        return local.contains('-') && !remote.contains('-')
    }

    private fun cleanNotes(md: String?): String = md.orEmpty().lines()
        .map { it.trim().removePrefix("###").removePrefix("##").removePrefix("#").trim().removePrefix("- ").let { l -> if (l.isEmpty()) l else l } }
        .filter { it.isNotBlank() }.take(14).joinToString("\n") { if (it in listOf("Añadido", "Cambiado", "Corregido")) "\n$it" else "• $it" }.trim()

    /** Descarga el APK informando del progreso (0..1) y comprueba que es de esta app. */
    suspend fun download(ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.getExternalFilesDir(null) ?: ctx.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, "TLoTraigo-${info.version}.apk")
        val req = Request.Builder().url(info.url).header("User-Agent", "TLoTraigo-Android").build()
        try {
            http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw ApiException("No se pudo descargar la actualización (${r.code}).")
                val body = r.body ?: throw ApiException("Descarga vacía.")
                val total = body.contentLength().takeIf { it > 0 } ?: info.size
                body.byteStream().use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024); var done = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf); if (n < 0) break
                            out.write(buf, 0, n); done += n
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        } catch (e: IOException) { file.delete(); throw ApiException("Se ha cortado la descarga. Comprueba la conexión e inténtalo otra vez.") }
        val pkg = ctx.packageManager.getPackageArchiveInfo(file.path, 0)
        if (pkg?.packageName != ctx.packageName) { file.delete(); throw ApiException("El archivo descargado no es válido.") }
        onProgress(1f)
        file
    }

    fun canInstall(ctx: Context) = Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()

    /** Android 8+: el usuario debe permitir a la app instalar actualizaciones (una sola vez). */
    fun openInstallPermission(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) runCatching {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun install(ctx: Context, file: File) {
        val uri = if (Build.VERSION.SDK_INT >= 24) FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file) else Uri.fromFile(file)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
