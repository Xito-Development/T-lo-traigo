package es.unocerobits.tlotraigo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.unocerobits.tlotraigo.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Boot {
    data object Loading : Boot
    data object LoggedOut : Boot
    data object NeedsProfile : Boot
    data object NeedsTerms : Boot
    data object Client : Boot
    data object Driver : Boot
    data class Error(val msg: String) : Boot
}

/** Versión de las condiciones/privacidad vigentes. Al cambiarla, todos deben volver a aceptarlas. */
const val TERMS_VERSION = "2026-10-2"

class AppViewModel : ViewModel() {
    private val repo = Locator.repo
    val api = Locator.api
    val cart = Locator.cart

    val settings: StateFlow<UiSettings> = Locator.settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())

    private val _boot = MutableStateFlow<Boot>(Boot.Loading)
    val boot: StateFlow<Boot> = _boot
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile
    private val _towns = MutableStateFlow<List<Town>>(emptyList())
    val towns: StateFlow<List<Town>> = _towns

    // --- Actualizaciones (solo versión GitHub)
    private val _update = MutableStateFlow<UpdateInfo?>(null)
    val update: StateFlow<UpdateInfo?> = _update
    private val _updateMsg = MutableStateFlow<String?>(null)
    val updateMsg: StateFlow<String?> = _updateMsg
    private var updateChecked = false

    /** Comprueba si hay una versión nueva. Automático una vez por arranque; manual desde «Acerca de». */
    fun checkUpdate(manual: Boolean = false) = viewModelScope.launch {
        if (!Updater.enabled || (updateChecked && !manual)) return@launch
        updateChecked = true
        val u = Updater.check()
        _update.value = u
        if (manual) _updateMsg.value = if (u == null) "Ya tienes la última versión (${es.unocerobits.tlotraigo.BuildConfig.VERSION_NAME})." else null
    }
    fun viewModelScopeLaunch(block: suspend () -> Unit) = viewModelScope.launch { runCatching { block() } }
    fun dismissUpdate() { _update.value = null }
    fun clearUpdateMsg() { _updateMsg.value = null }

    private val _driverView = MutableStateFlow(false)
    val driverView: StateFlow<Boolean> = _driverView

    init {
        // Si la sesión no se puede renovar, volver a la pantalla de entrada en lugar de mostrar errores.
        api.onSessionLost = { _profile.value = null; _boot.value = Boot.LoggedOut }
        start()
    }

    /** Solo admin/tester: alterna entre vista cliente y repartidor. */
    fun setDriverView(on: Boolean) {
        if (_profile.value?.isAdmin != true) return
        _driverView.value = on
        _boot.value = if (on) Boot.Driver else Boot.Client
    }

    fun start() = viewModelScope.launch {
        _boot.value = Boot.Loading
        runCatching { if (_towns.value.isEmpty()) _towns.value = repo.towns() }
        if (api.session == null) { _boot.value = Boot.LoggedOut; return@launch }
        _boot.value = try {
            val p = repo.myProfile()
            _profile.value = p
            when {
                p == null -> { api.signOut(); Boot.LoggedOut }
                p.termsVersion != TERMS_VERSION -> Boot.NeedsTerms
                p.townId == null || p.fullName.isBlank() -> Boot.NeedsProfile
                p.isDriver || (p.isAdmin && _driverView.value) -> Boot.Driver
                else -> Boot.Client
            }
        } catch (e: ApiException) {
            if (api.session == null) Boot.LoggedOut else Boot.Error(e.message ?: "Error")
        }
    }

    fun refreshProfile() = viewModelScope.launch { runCatching { _profile.value = repo.myProfile() } }

    fun updateSettings(transform: (UiSettings) -> UiSettings) = viewModelScope.launch {
        Locator.settings.save(transform(settings.value))
    }

    fun townName(id: Long?) = _towns.value.firstOrNull { it.id == id }?.name ?: ""

    fun logout() { api.signOut(); cart.clear(); _profile.value = null; _driverView.value = false; _boot.value = Boot.LoggedOut }
}
