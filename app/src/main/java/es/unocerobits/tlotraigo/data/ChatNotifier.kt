package es.unocerobits.tlotraigo.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import es.unocerobits.tlotraigo.Locator
import es.unocerobits.tlotraigo.MainActivity
import es.unocerobits.tlotraigo.R
import kotlinx.coroutines.*

/** Comprueba los mensajes nuevos del chat cada 12 s mientras la app (o el servicio del repartidor) está viva y avisa con una notificación. */
object ChatNotifier {
    private var job: Job? = null
    private val notified = HashSet<Long>()
    @Volatile var openChatOrder: Long? = null   // si el chat de ese pedido está abierto, no se notifica
    private const val CH = "mensajes"

    fun start(ctx: Context) {
        if (job?.isActive == true) return
        val app = ctx.applicationContext
        if (Build.VERSION.SDK_INT >= 26) app.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CH, "Mensajes del pedido", NotificationManager.IMPORTANCE_HIGH))
        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            var first = true
            val prefs = app.getSharedPreferences("avisos", Context.MODE_PRIVATE)
            var orgId: Long? = null; var tick = 0
            while (isActive) {
                if (Locator.api.session != null) runCatching {
                    val list = Locator.repo.unread()
                    list.filter { it.id !in notified && it.orderId != openChatOrder }.forEach { m -> if (!first) notify(app, m) }
                    notified.addAll(list.map { it.id }); first = false
                    // Avisos de la entidad (días sin reparto, verificación…), cada minuto
                    if (tick++ % 5 == 0) {
                        if (orgId == null) orgId = Locator.repo.myProfile()?.orgId
                        orgId?.let { oid ->
                            val last = prefs.getLong("ultimo_$oid", -1L)
                            val news = Locator.repo.orgNotices(oid, maxOf(last, 0L))
                            if (last >= 0) news.sortedBy { it.id }.forEach { notifyNotice(app, it) }
                            news.maxOfOrNull { it.id }?.let { prefs.edit().putLong("ultimo_$oid", it).apply() } ?: if (last < 0) prefs.edit().putLong("ultimo_$oid", 0L).apply() else Unit
                        }
                    }
                }
                delay(12_000)
            }
        }
    }

    private fun notifyNotice(ctx: Context, n: OrgNotice) {
        val nm = NotificationManagerCompat.from(ctx); if (!nm.areNotificationsEnabled()) return
        val pi = PendingIntent.getActivity(ctx, 6000, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        runCatching {
            @Suppress("MissingPermission")
            nm.notify(6000 + (n.id % 1000).toInt(), NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("Aviso de tu servicio").setContentText(n.body).setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
                .setAutoCancel(true).setContentIntent(pi).build())
        }
    }

    private fun notify(ctx: Context, m: Unread) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val who = when (m.role) { "repartidor" -> "Tu repartidor"; "cliente" -> "El cliente"; "sistema" -> "T lo traigo"; else -> "Soporte" }
        val pi = PendingIntent.getActivity(ctx, m.orderId.toInt(), Intent(ctx, MainActivity::class.java).putExtra("chat", m.orderId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        runCatching {
            @Suppress("MissingPermission")
            nm.notify(5000 + (m.orderId % 1000).toInt(), NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("$who · pedido nº ${m.orderId}").setContentText(m.body).setStyle(NotificationCompat.BigTextStyle().bigText(m.body))
                .setAutoCancel(true).setContentIntent(pi).setPriority(NotificationCompat.PRIORITY_HIGH).setDefaults(NotificationCompat.DEFAULT_ALL).build())
        }
    }
}
