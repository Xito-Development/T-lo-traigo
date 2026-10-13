package es.unocerobits.tlotraigo

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import es.unocerobits.tlotraigo.data.Order
import es.unocerobits.tlotraigo.ui.components.eur
import es.unocerobits.tlotraigo.ui.components.productos
import kotlinx.coroutines.*

/**
 * Servicio del repartidor mientras está «disponible»:
 *  - Comprueba cada 45 s si hay pedidos nuevos en sus pueblos y avisa con una notificación (sin Firebase:
 *    funciona en cualquier Android 5.0+ y sin servicios de Google).
 *  - Mientras lleva un pedido «en camino», comparte su ubicación GPS con el cliente cada ~15 s.
 */
class DriverService : Service() {
    companion object {
        private const val CH_SERVICE = "servicio"
        private const val CH_ORDERS = "pedidos"
        private const val ID_FG = 1
        fun start(ctx: Context) = runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, DriverService::class.java)) }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, DriverService::class.java)) }
        fun hasLocation(ctx: Context) = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private val seen = HashSet<Long>()
    private var towns: Map<Long, String> = emptyMap()
    private var tracking: Long? = null
    private var lastSent = 0L
    private val lm by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    private val listener = object : LocationListener {
        override fun onLocationChanged(l: Location) {
            val id = tracking ?: return
            if (System.currentTimeMillis() - lastSent < 12_000) return
            lastSent = System.currentTimeMillis()
            scope.launch { runCatching { Locator.repo.updateLocation(id, l.latitude, l.longitude, l.accuracy) } }
        }
        @Deprecated("") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
        override fun onProviderEnabled(p: String) {}
        override fun onProviderDisabled(p: String) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        channels()
        foreground(false)
        if (job == null) job = scope.launch { loop() }
        return START_STICKY
    }

    private fun channels() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_SERVICE, "Repartidor disponible", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_ORDERS, "Pedidos nuevos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso cuando hay un pedido nuevo en tus pueblos"; enableVibration(true)
        })
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))

    private fun foreground(location: Boolean) {
        val n = NotificationCompat.Builder(this, CH_SERVICE).setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(if (location) "Repartiendo un pedido" else "Estás disponible")
            .setContentText(if (location) "Tu ubicación se comparte con el cliente hasta la entrega" else "Te avisaremos cuando haya un pedido nuevo")
            .setOngoing(true).setContentIntent(openApp()).setPriority(NotificationCompat.PRIORITY_LOW).build()
        if (Build.VERSION.SDK_INT >= 29) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            if (location && hasLocation(this)) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            startForeground(ID_FG, n, type)
        } else startForeground(ID_FG, n)
    }

    private suspend fun loop() {
        var first = true
        runCatching { towns = Locator.repo.towns().associate { it.id to it.name } }
        while (currentCoroutineContext().isActive) {
            if (Locator.api.session == null) { stopSelf(); return }
            try {
                if (Locator.repo.driverSettings()?.available != true) { stopSelf(); return }
                val pend = Locator.repo.pendingOrders()
                val nuevos = pend.filter { it.id !in seen }
                if (nuevos.isNotEmpty()) notifyOrders(nuevos, first)
                seen.addAll(pend.map { it.id }); first = false
                val enCamino = Locator.repo.myDeliveries().firstOrNull { it.status == "en_camino" }?.id
                if (enCamino != tracking) setTracking(enCamino)
            } catch (_: Exception) { /* sin conexión: se reintenta */ }
            delay(45_000)
        }
    }

    private fun notifyOrders(list: List<Order>, initial: Boolean) {
        val nm = NotificationManagerCompat.from(this)
        if (!nm.areNotificationsEnabled()) return
        val o = list.first()
        val title = if (initial && list.size > 1) "Hay ${list.size} pedidos esperando" else "Nuevo pedido en ${towns[o.townId] ?: "tu zona"}"
        val text = if (list.size == 1) "${productos(o.items.sumOf { it.qty })} · ganas ${eur(o.driverEarnings)}" else "Abre la app para aceptarlos"
        @SuppressLint("MissingPermission")
        val ok = runCatching {
            nm.notify(1000 + (o.id % 1000).toInt(), NotificationCompat.Builder(this, CH_ORDERS).setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(openApp())
                .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL).build())
        }
    }

    @SuppressLint("MissingPermission")
    private fun setTracking(orderId: Long?) {
        runCatching { lm.removeUpdates(listener) }
        tracking = orderId
        if (orderId != null && hasLocation(this)) {
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER))
                if (runCatching { lm.isProviderEnabled(p) }.getOrDefault(false)) runCatching { lm.requestLocationUpdates(p, 15_000L, 20f, listener) }
            foreground(true)
        } else foreground(false)
    }

    /** Android 15: el servicio de sincronización tiene un límite diario; al agotarse se detiene avisando. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { lm.removeUpdates(listener) }
        scope.cancel()
        super.onDestroy()
    }
}
