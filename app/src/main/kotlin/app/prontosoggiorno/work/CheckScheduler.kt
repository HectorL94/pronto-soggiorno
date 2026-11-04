package app.prontosoggiorno.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.prontosoggiorno.appContainer
import app.prontosoggiorno.data.settings.CheckInterval
import java.util.concurrent.TimeUnit

/** Programa la verificación periódica con WorkManager (sobrevive a reinicios del teléfono). */
class CheckScheduler(private val workManager: WorkManager) {

    fun schedule(interval: CheckInterval) {
        val constraints = Constraints.Builder()
            // CONNECTED = cualquier red: Wi-Fi o datos móviles. Cada consulta ocupa ~1 KB.
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<PeriodicCheckWorker>(interval.hours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        // UPDATE: si ya existe, se actualiza conservando su calendario (no se lanza una consulta extra).
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "periodic_pratica_check"
    }
}

/**
 * Trabajo periódico. No recibe datos de entrada: WorkManager guarda los `inputData` sin cifrar,
 * así que el número NUNCA pasa por aquí; se lee cifrado desde la base de datos.
 */
class PeriodicCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val summary = applicationContext.appContainer.checkRunner.runScheduled()
        return if (summary.shouldRetry && runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
