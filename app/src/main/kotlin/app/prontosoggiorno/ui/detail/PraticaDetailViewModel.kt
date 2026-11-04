package app.prontosoggiorno.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.network.PortalEndpoint
import app.prontosoggiorno.core.network.PortalLanguage
import app.prontosoggiorno.data.CheckRecord
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.PraticaSummary
import app.prontosoggiorno.data.settings.CheckInterval
import app.prontosoggiorno.data.settings.SettingsRepository
import app.prontosoggiorno.work.CheckRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl

/** Mensaje breve tras una acción (se muestra en un snackbar y se descarta). */
enum class DetailMessage { TOO_SOON, UNCHANGED, CHANGED, FAILED }

data class DetailUiState(
    val loading: Boolean = true,
    /** null cuando la pratica ya no existe (eliminada). */
    val pratica: PraticaSummary? = null,
    val history: List<CheckRecord> = emptyList(),
    val interval: CheckInterval = CheckInterval.DEFAULT,
    val checking: Boolean = false,
    /** Número completo, solo mientras el usuario pulsa "Mostrar". */
    val revealedNumber: String? = null,
    val message: DetailMessage? = null,
    /** Dirección del portal pendiente de abrir en el navegador. */
    val pendingUrl: HttpUrl? = null,
    val deleted: Boolean = false,
)

private data class LocalState(
    val checking: Boolean = false,
    val revealedNumber: String? = null,
    val message: DetailMessage? = null,
    val pendingUrl: HttpUrl? = null,
    val deleted: Boolean = false,
)

class PraticaDetailViewModel(
    private val praticaId: Long,
    private val repository: PraticaRepository,
    private val runner: CheckRunner,
    settings: SettingsRepository,
    private val cancelNotification: (Long) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val local = MutableStateFlow(LocalState())

    private val pratica = repository.observeSummaries().map { list -> list.firstOrNull { it.id == praticaId } }

    val state: StateFlow<DetailUiState> = combine(
        pratica,
        repository.observeHistory(praticaId),
        settings.settings,
        local,
    ) { p, history, appSettings, l ->
        DetailUiState(
            loading = false,
            pratica = p,
            history = history,
            interval = appSettings.interval,
            checking = l.checking,
            revealedNumber = l.revealedNumber,
            message = l.message,
            pendingUrl = l.pendingUrl,
            deleted = l.deleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        // Quien abre la pratica ya ve su estado: el aviso ya no hace falta.
        cancelNotification(praticaId)
        // Recién añadida (o nunca verificada): primera verificación automática.
        viewModelScope.launch {
            val current = pratica.first()
            if (current != null && !current.unreadable && current.lastCheck == null) check(manual = false)
        }
    }

    fun checkNow() = viewModelScope.launch { check(manual = true) }

    private suspend fun check(manual: Boolean) {
        // Se marca antes de cualquier suspensión para que dos pulsaciones no lancen dos consultas.
        if (local.value.checking) return
        local.update { it.copy(checking = true) }
        val recorded = try {
            val last = pratica.first()?.lastCheck
            if (manual && last != null && clock() - last.checkedAt < MIN_MS_BETWEEN_MANUAL_CHECKS) {
                local.update { it.copy(message = DetailMessage.TOO_SOON) }
                return
            }
            runner.checkNow(praticaId)
        } finally {
            local.update { it.copy(checking = false) }
        }
        if (!manual || recorded == null) return
        val message = when (recorded.changeKind) {
            ChangeKind.FAILED -> DetailMessage.FAILED
            ChangeKind.UNCHANGED -> DetailMessage.UNCHANGED
            ChangeKind.CHANGED, ChangeKind.FIRST_RESULT -> DetailMessage.CHANGED
        }
        local.update { it.copy(message = message) }
    }

    fun messageShown() = local.update { it.copy(message = null) }

    fun toggleReveal() = viewModelScope.launch {
        if (local.value.revealedNumber != null) {
            local.update { it.copy(revealedNumber = null) }
        } else {
            val number = repository.numberFor(praticaId)?.reveal()
            local.update { it.copy(revealedNumber = number) }
        }
    }

    fun togglePaused() = viewModelScope.launch {
        val current = pratica.first() ?: return@launch
        when {
            // Pausada por "no encontrado": reanudar da otros 30 días.
            current.notFoundPaused && !current.paused -> repository.resumeAfterNotFound(praticaId)
            else -> repository.setPaused(praticaId, !current.paused)
        }
    }

    fun rename(alias: String) = viewModelScope.launch { repository.rename(praticaId, alias) }

    fun delete() = viewModelScope.launch {
        repository.delete(praticaId)
        cancelNotification(praticaId)
        local.update { it.copy(deleted = true, revealedNumber = null) }
    }

    /** Prepara la dirección de la página del portal para esta pratica, en el idioma de la app. */
    fun openPortal(language: PortalLanguage) = viewModelScope.launch {
        val number = repository.numberFor(praticaId) ?: return@launch
        local.update { it.copy(pendingUrl = PortalEndpoint.pageUrl(number, language)) }
    }

    fun urlOpened() = local.update { it.copy(pendingUrl = null) }

    companion object {
        /** Evita pulsaciones repetidas: como mucho una verificación manual por minuto. */
        const val MIN_MS_BETWEEN_MANUAL_CHECKS = 60_000L
    }
}
