package app.prontosoggiorno

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import app.prontosoggiorno.core.PraticaChecker
import app.prontosoggiorno.core.network.PortalClient
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.crypto.KeystoreFieldCipher
import app.prontosoggiorno.data.db.AppDatabase
import app.prontosoggiorno.data.settings.SettingsRepository
import app.prontosoggiorno.data.settings.SettingsRepository.Companion.settingsDataStore
import app.prontosoggiorno.notifications.AndroidStatusNotifier
import app.prontosoggiorno.work.CheckRunner
import app.prontosoggiorno.work.CheckScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Construye y comparte los objetos de la app (inyección de dependencias manual, sin librerías).
 * Cada pieza se crea la primera vez que se usa.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    val praticaRepository: PraticaRepository by lazy { PraticaRepository(database, KeystoreFieldCipher()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.settingsDataStore) }

    /** Única conexión externa de la app (lista blanca dentro de PortalClient). */
    val checker: PraticaChecker by lazy { PraticaChecker(PortalClient.create(BuildConfig.VERSION_NAME)) }

    val notifier: AndroidStatusNotifier by lazy { AndroidStatusNotifier(appContext) }
    val scheduler: CheckScheduler by lazy { CheckScheduler(WorkManager.getInstance(appContext)) }
    val checkRunner: CheckRunner by lazy {
        CheckRunner(praticaRepository, checker, settingsRepository, notifier)
    }
}

class ProntoSoggiornoApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.ensureChannels()

        // Mantiene la verificación periódica alineada con la frecuencia elegida en Ajustes.
        appScope.launch {
            container.settingsRepository.settings
                .map { it.interval }
                .distinctUntilChanged()
                .collect { container.scheduler.schedule(it) }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as ProntoSoggiornoApp).container
