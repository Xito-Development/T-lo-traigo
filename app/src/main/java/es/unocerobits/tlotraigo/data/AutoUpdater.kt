package es.unocerobits.tlotraigo.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import es.unocerobits.tlotraigo.MainActivity
import es.unocerobits.tlotraigo.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Actualización automática en segundo plano (versión GitHub): cuando la app está sin usar (1 minuto sin tocar
 * la pantalla) o pasa a segundo plano, busca y descarga la versión nueva sin molestar y avisa con una notificación
 * y un aviso en la app. Android exige que el usuario confirme la instalación: es un solo toque.
 */
object AutoUpdater {
    data class Ready(val version: String, val file: File)
    val ready = MutableStateFlow<Ready?>(null)
    private var job: Job? = null
    private var lastCheck = 0L
    private const val CH = "actualizaciones"

    fun onIdle(ctx: Context) {
        if (!Updater.enabled || job?.isActive == true || ready.value != null) return
        if (System.currentTimeMillis() - lastCheck < 6 * 3600_000L) return   // como mucho cada 6 horas
        lastCheck = System.currentTimeMillis()
        val app = ctx.applicationContext
        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val info = Updater.check() ?: return@launch
            val f = runCatching { Updater.download(app, info) {} }.getOrNull() ?: return@launch
            ready.value = Ready(info.version, f)
            notifyReady(app, info.version, fromStore = false)
        }
    }

    fun install(ctx: Context) { ready.value?.let { Updater.install(ctx, it.file) } }

    /** Notificación «actualización lista» (la usan las dos versiones). */
    fun notifyReady(ctx: Context, version: String?, fromStore: Boolean) {
        if (Build.VERSION.SDK_INT >= 26) ctx.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CH, "Actualizaciones de la app", NotificationManager.IMPORTANCE_DEFAULT))
        val nm = NotificationManagerCompat.from(ctx); if (!nm.areNotificationsEnabled()) return
        val pi = PendingIntent.getActivity(ctx, 7001, Intent(ctx, MainActivity::class.java).putExtra("install_update", true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        val text = if (fromStore) "Se ha descargado desde Google Play. Se instalará al salir de la app." else "Descargada en segundo plano. Toca para instalarla."
        runCatching {
            @Suppress("MissingPermission")
            nm.notify(7001, NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("Nueva versión${version?.let { " $it" } ?: ""} de T lo traigo lista").setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(pi).build())
        }
    }
}
