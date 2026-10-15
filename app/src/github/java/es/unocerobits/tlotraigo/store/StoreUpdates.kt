package es.unocerobits.tlotraigo.store

import android.app.Activity

/** Versión de GitHub: las actualizaciones las gestiona data/AutoUpdater.kt (descarga en segundo plano + aviso). */
object StoreUpdates {
    const val FROM_STORE = false
    fun check(activity: Activity) {}
    fun onBackground(activity: Activity) {}
    fun checkManual(activity: Activity, onResult: (String) -> Unit) {}
}
