package es.unocerobits.tlotraigo.store

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.InstallStateUpdatedListener

/**
 * Versión de Google Play: las actualizaciones llegan por la tienda.
 * Si hay una versión nueva se descarga en segundo plano (flexible) y, al terminar, se instala.
 * Si la versión instalada es muy antigua (prioridad alta en Play Console), la actualización es obligatoria (inmediata).
 */
object StoreUpdates {
    const val FROM_STORE = true
    private var listener: InstallStateUpdatedListener? = null

    fun check(activity: Activity) {
        val mgr = AppUpdateManagerFactory.create(activity)
        mgr.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> mgr.completeUpdate()
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    runCatching { mgr.startUpdateFlow(info, activity, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()) }
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                    val immediate = info.updatePriority() >= 4 && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    if (!immediate) {
                        listener?.let { mgr.unregisterListener(it) }
                        listener = InstallStateUpdatedListener { st -> if (st.installStatus() == InstallStatus.DOWNLOADED) mgr.completeUpdate() }.also { mgr.registerListener(it) }
                    }
                    val type = if (immediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                    if (info.isUpdateTypeAllowed(type)) runCatching { mgr.startUpdateFlow(info, activity, AppUpdateOptions.newBuilder(type).build()) }
                }
            }
        }
    }
}
