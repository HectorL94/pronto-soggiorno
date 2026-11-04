package app.prontosoggiorno.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.settings.AppSettings
import app.prontosoggiorno.data.settings.CheckInterval
import app.prontosoggiorno.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val repository: PraticaRepository,
    private val cancelAllNotifications: () -> Unit,
) : ViewModel() {

    val state: StateFlow<AppSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setInterval(interval: CheckInterval) = viewModelScope.launch { settings.setInterval(interval) }

    fun setNotifyAllChanges(enabled: Boolean) = viewModelScope.launch { settings.setNotifyAllChanges(enabled) }

    /** Borra pratiche, historial y ajustes. La app vuelve a la bienvenida. */
    fun deleteAll(onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteAll()
        cancelAllNotifications()
        settings.clear()
        onDone()
    }
}
