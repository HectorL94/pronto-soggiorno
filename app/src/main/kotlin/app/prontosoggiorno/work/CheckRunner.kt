package app.prontosoggiorno.work

import app.prontosoggiorno.core.CheckOutcome
import app.prontosoggiorno.core.PraticaChecker
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.RecordedCheck
import app.prontosoggiorno.data.db.CheckTrigger
import app.prontosoggiorno.data.settings.SettingsRepository
import app.prontosoggiorno.notifications.NotificationPolicy
import app.prontosoggiorno.notifications.StatusNotifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class RunSummary(val checked: Int, val networkFailures: Int) {
    /** Todas fallaron por red: probablemente no había conexión real; conviene reintentar pronto. */
    val shouldRetry: Boolean get() = checked > 0 && networkFailures == checked
}

/**
 * Ejecuta verificaciones: lo usan el trabajo periódico (WorkManager) y el botón "Verificar ahora".
 * Las pratiche se consultan UNA TRAS OTRA con una pausa entre ellas, nunca en paralelo,
 * para no cargar el portal.
 */
class CheckRunner(
    private val repository: PraticaRepository,
    private val checker: PraticaChecker,
    private val settings: SettingsRepository,
    private val notifier: StatusNotifier,
    private val pauseBetweenChecks: suspend () -> Unit = { delay(Random.nextLong(4_000, 9_000)) },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun runScheduled(): RunSummary {
        val pratiche = repository.monitoredPratiche()
        val notifyAllChanges = settings.settings.first().notifyAllChanges
        var networkFailures = 0

        pratiche.forEachIndexed { index, pratica ->
            if (index > 0) pauseBetweenChecks()
            val outcome = withContext(ioDispatcher) { checker.check(pratica.number) }
            if (outcome is CheckOutcome.NetworkFailure) networkFailures++

            val recorded = repository.recordOutcome(pratica.id, outcome, CheckTrigger.SCHEDULED)
                ?: return@forEachIndexed // eliminada mientras se verificaba
            NotificationPolicy.decide(recorded, CheckTrigger.SCHEDULED, notifyAllChanges)
                ?.let { kind -> notifier.notify(kind, pratica.id, pratica.alias) }
        }
        return RunSummary(checked = pratiche.size, networkFailures = networkFailures)
    }

    /** Verificación manual de una pratica (sin notificación: el usuario ya está en la app). */
    suspend fun checkNow(praticaId: Long): RecordedCheck? {
        val number = repository.numberFor(praticaId) ?: return null
        val outcome = withContext(ioDispatcher) { checker.check(number) }
        return repository.recordOutcome(praticaId, outcome, CheckTrigger.MANUAL)
    }
}
