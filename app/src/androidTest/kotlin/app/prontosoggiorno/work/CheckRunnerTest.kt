package app.prontosoggiorno.work

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.prontosoggiorno.TestFeeds
import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.PraticaChecker
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.network.FeedSource
import app.prontosoggiorno.core.network.FetchResult
import app.prontosoggiorno.core.network.NetworkErrorKind
import app.prontosoggiorno.data.AddPraticaResult
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.crypto.KeystoreFieldCipher
import app.prontosoggiorno.data.db.AppDatabase
import app.prontosoggiorno.data.db.CheckTrigger
import app.prontosoggiorno.data.settings.SettingsRepository
import app.prontosoggiorno.notifications.NotificationKind
import app.prontosoggiorno.notifications.StatusNotifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CheckRunnerTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var db: AppDatabase
    private lateinit var cipher: KeystoreFieldCipher
    private lateinit var repo: PraticaRepository
    private lateinit var settings: SettingsRepository

    /** Respuesta simulada del portal por número; nunca hay red real en estos tests. */
    private val responses = mutableMapOf<String, FetchResult>()
    private val requested = mutableListOf<String>()
    private val notifications = mutableListOf<Triple<NotificationKind, Long, String?>>()
    private var pauses = 0

    private lateinit var runner: CheckRunner

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        cipher = KeystoreFieldCipher(keyAlias = "test_runner_key")
        repo = PraticaRepository(db, cipher)
        val file = File(context.cacheDir, "runner_settings_${System.nanoTime()}.preferences_pb")
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) { file })

        val source = FeedSource { number ->
            requested += number.reveal()
            responses[number.reveal()] ?: FetchResult.HttpError(404)
        }
        runner = CheckRunner(
            repository = repo,
            checker = PraticaChecker(source),
            settings = settings,
            notifier = StatusNotifier { kind, id, alias -> notifications += Triple(kind, id, alias) },
            pauseBetweenChecks = { pauses++ },
        )
    }

    @After
    fun tearDown() {
        db.close()
        cipher.deleteKey()
    }

    private suspend fun add(number: String, alias: String?) = (repo.add(number, alias) as AddPraticaResult.Added).id

    private fun respond(number: String, html: String) {
        responses[number] = FetchResult.Body(TestFeeds.feed(number, html))
    }

    @Test
    fun ciclo_completo_en_tramite_y_luego_listo() = testScope.runTest {
        val id = add("00ZZ000001", "Mamma")
        respond("00ZZ000001", TestFeeds.IN_PROGRESS)

        runner.runScheduled() // primera: en trámite, sin aviso
        assertTrue(notifications.isEmpty())

        respond("00ZZ000001", TestFeeds.READY)
        runner.runScheduled() // cambia a listo: aviso con alias

        assertEquals(listOf(Triple(NotificationKind.READY, id, "Mamma")), notifications)
        // Ya está lista: la siguiente ronda ni siquiera consulta el portal.
        requested.clear()
        runner.runScheduled()
        assertTrue(requested.isEmpty())
        assertTrue(repo.observeSummaries().first().single().monitoringCompleted)
    }

    @Test
    fun varias_pratiche_una_tras_otra_con_pausa() = testScope.runTest {
        add("00ZZ000001", null)
        add("00ZZ000002", null)
        val paused = add("00ZZ000003", null)
        repo.setPaused(paused, true)
        respond("00ZZ000001", TestFeeds.IN_PROGRESS)
        respond("00ZZ000002", TestFeeds.IN_PROGRESS)

        val summary = runner.runScheduled()

        assertEquals(listOf("00ZZ000001", "00ZZ000002"), requested) // la pausada no se consulta
        assertEquals(1, pauses) // pausa ENTRE consultas, no antes de la primera
        assertEquals(RunSummary(checked = 2, networkFailures = 0), summary)
        assertFalse(summary.shouldRetry)
    }

    @Test
    fun sin_red_en_todas_se_pide_reintento() = testScope.runTest {
        add("00ZZ000001", null)
        responses["00ZZ000001"] = FetchResult.NetworkError(NetworkErrorKind.NO_CONNECTION)

        assertTrue(runner.runScheduled().shouldRetry)
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun sin_pratiche_no_hace_nada() = testScope.runTest {
        val summary = runner.runScheduled()
        assertEquals(RunSummary(0, 0), summary)
        assertFalse(summary.shouldRetry)
        assertTrue(requested.isEmpty())
    }

    @Test
    fun respetar_preferencia_de_avisar_solo_cuando_esta_listo() = testScope.runTest {
        settings.setNotifyAllChanges(false)
        add("00ZZ000001", null)
        respond("00ZZ000001", TestFeeds.IN_PROGRESS)
        runner.runScheduled()
        respond("00ZZ000001", "Un testo nuovo mai visto.")
        runner.runScheduled() // cambio a texto desconocido: no se avisa con esta preferencia

        assertTrue(notifications.isEmpty())
    }

    @Test
    fun verificacion_manual_guarda_pero_no_notifica() = testScope.runTest {
        val id = add("00ZZ000001", null)
        respond("00ZZ000001", TestFeeds.READY)

        val recorded = runner.checkNow(id)!!

        assertEquals(ChangeKind.FIRST_RESULT, recorded.changeKind)
        assertEquals(AdministrativeStatus.READY_FOR_COLLECTION, recorded.status)
        assertTrue(notifications.isEmpty())
        assertEquals(CheckTrigger.MANUAL, repo.observeHistory(id).first().single().trigger)
    }
}
