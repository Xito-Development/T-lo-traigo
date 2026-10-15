package es.unocerobits.tlotraigo.store

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import es.unocerobits.tlotraigo.data.AutoUpdater

/**
 * Versión de Google Play: la actualización se descarga desde la tienda en segundo plano (flexible) y se instala sola
 * cuando el usuario sale de la app, avisando con una notificación. Si es urgente (prioridad 4–5 en Play Console), es inmediata.
 */
object StoreUpdates {
    const val FROM_STORE = true
    private var mgr: AppUpdateManager? = null
    private var listener: InstallStateUpdatedListener? = null
    @Volatile private var downloaded = false

    private fun manager(a: Activity) = mgr ?: AppUpdateManagerFactory.create(a.applicationContext).also { mgr = it }

    fun check(activity: Activity) {
        val m = manager(activity)
        m.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> downloaded = true   // se instalará al salir
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    runCatching { m.startUpdateFlow(info, activity, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()) }
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                    val immediate = info.updatePriority() >= 4 && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    if (!immediate && listener == null) {
                        listener = InstallStateUpdatedListener { st ->
                            if (st.installStatus() == InstallStatus.DOWNLOADED) { downloaded = true; AutoUpdater.notifyReady(activity.applicationContext, null, fromStore = true) }
                        }.also { m.registerListener(it) }
                    }
                    val type = if (immediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                    if (info.isUpdateTypeAllowed(type)) runCatching { m.startUpdateFlow(info, activity, AppUpdateOptions.newBuilder(type).build()) }
                }
            }
        }
    }

    /** Al salir de la app (onStop): si la descarga terminó, Google Play la instala sin interrumpir al usuario. */
    fun onBackground(activity: Activity) { if (downloaded) runCatching { manager(activity).completeUpdate() } }

    fun checkManual(activity: Activity, onResult: (String) -> Unit) {
        manager(activity).appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) { check(activity); onResult("Descargando la actualización desde Google Play…") }
            else if (info.installStatus() == InstallStatus.DOWNLOADED) { onResult("Actualización lista: se instalará al salir de la app.") }
            else onResult("Ya tienes la última versión.")
        }.addOnFailureListener {
            runCatching { activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${activity.packageName}"))) }
            onResult("Abre Google Play para comprobar si hay una versión nueva.")
        }
    }
}
