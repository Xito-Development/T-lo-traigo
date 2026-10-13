package es.unocerobits.tlotraigo

import android.app.Application
import es.unocerobits.tlotraigo.data.*

object Locator {
    lateinit var api: SupabaseApi
    lateinit var repo: Repository
    lateinit var settings: SettingsStore
    lateinit var cart: Cart
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Locator.api = SupabaseApi(this, BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
        Locator.repo = Repository(Locator.api)
        Locator.settings = SettingsStore(this)
        Locator.cart = Cart(this)
        Analytics.init(this)
    }
}
