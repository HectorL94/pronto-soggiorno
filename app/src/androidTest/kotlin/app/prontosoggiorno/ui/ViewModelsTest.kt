package app.prontosoggiorno.ui

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.prontosoggiorno.TestFeeds
import app.prontosoggiorno.core.PraticaChecker
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.model.IdentifierType
import app.prontosoggiorno.core.network.FeedSource
import app.prontosoggiorno.core.network.FetchResult
import app.prontosoggiorno.core.network.PortalEndpoint
import app.prontosoggiorno.core.network.PortalLanguage
import app.prontosoggiorno.data.AddPraticaResult
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.crypto.KeystoreFieldCipher
import app.prontosoggiorno.data.db.AppDatabase
import app.prontosoggiorno.data.settings.SettingsRepository
import app.prontosoggiorno.ui.add.AddError
import app.prontosoggiorno.ui.add.AddPraticaViewModel
import app.prontosoggiorno.ui.detail.DetailMessage
import app.prontosoggiorno.ui.detail.PraticaDetailViewModel
import app.prontosoggiorno.work.CheckRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** ViewModels con base de datos real (en memoria) y portal simulado: nunca hay red. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ViewModelsTest {

    private lateinit var db: AppDatabase
    private lateinit var cipher: KeystoreFieldCipher
    private lateinit var repo: PraticaRepository
    private lateinit var settings: SettingsRepository
    private lateinit var runner: CheckRunner
    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = mutableListOf<String>()
    private val cancelled = mutableListOf<Long>()
    private val viewModels = mutableListOf<ViewModel>()

    private fun <VM : ViewModel> track(vm: VM): VM = vm.also { viewModels += it }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        cipher = KeystoreFieldCipher(keyAlias = "test_ui_key")
        repo = PraticaRepository(db, cipher)
        val file = File(context.cacheDir, "ui_settings_${System.nanoTime()}.preferences_pb")
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = dataStoreScope) { file })
        val source = FeedSource { number ->
            synchronized(requests) { requests += number.reveal() }
            FetchResult.Body(TestFeeds.feed(number.reveal(), TestFeeds.IN_PROGRESS))
        }
        runner = CheckRunner(repo, PraticaChecker(source), settings, notifier = { _, _, _ -> }, pauseBetweenChecks = {})
    }

    @After
    fun tearDown() {
        // Primero se detienen los ViewModels; si no, seguirían usando la base de datos ya cerrada.
        runBlocking { viewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() } }
        db.close()
        cipher.deleteKey()
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    private fun <T> StateFlow<T>.awaitState(predicate: (T) -> Boolean): T =
        runBlocking { withTimeout(5_000) { first(predicate) } }

    @Test
    fun anadir_valida_y_detecta_duplicados() {
        val vm = track(AddPraticaViewModel(repo))

        vm.onNumberChange("00 zz 000001")
        assertEquals(IdentifierType.PRATICA, vm.state.value.detectedType)

        vm.onNumberChange("12345")
        vm.save()
        assertEquals(AddError.WRONG_LENGTH, vm.state.awaitState { it.error != null }.error)

        vm.onNumberChange("00ZZ000001")
        vm.onAliasChange("Yo")
        vm.save()
        val id = vm.state.awaitState { it.addedId != null }.addedId!!

        val again = track(AddPraticaViewModel(repo))
        again.onNumberChange("00zz000001")
        again.save()
        assertEquals(AddError.DUPLICATE, again.state.awaitState { it.error != null }.error)
        assertTrue(id > 0)
    }

    @Test
    fun detalle_verifica_al_abrir_por_primera_vez_y_limita_las_manuales() {
        val id = runBlocking { (repo.add("00ZZ000002", "Mamma") as AddPraticaResult.Added).id }
        val vm = track(PraticaDetailViewModel(id, repo, runner, settings, cancelNotification = { cancelled += it }))

        // Primera apertura: verificación automática (sin pulsar nada).
        val checked = vm.state.awaitState { it.pratica?.lastKnownState != null && !it.checking }
        assertEquals(AdministrativeStatus.IN_PROGRESS, checked.pratica!!.lastKnownState!!.status)
        assertEquals(listOf("00ZZ000002"), synchronized(requests) { requests.toList() })
        assertEquals(listOf(id), cancelled) // el aviso de esta pratica se retira al abrirla

        // Una manual justo después: no se consulta el portal otra vez.
        vm.checkNow()
        assertEquals(DetailMessage.TOO_SOON, vm.state.awaitState { it.message != null }.message)
        assertEquals(1, synchronized(requests) { requests.size })
    }

    @Test
    fun detalle_mostrar_numero_y_enlace_al_portal() {
        val id = runBlocking { (repo.add("00ZZ000003", null) as AddPraticaResult.Added).id }
        val vm = track(PraticaDetailViewModel(id, repo, runner, settings, cancelNotification = {}))
        assertEquals("00ZZ••••03", vm.state.awaitState { it.pratica != null }.pratica!!.maskedNumber)

        vm.toggleReveal()
        assertEquals("00ZZ000003", vm.state.awaitState { it.revealedNumber != null }.revealedNumber)
        vm.toggleReveal()
        assertNull(vm.state.awaitState { it.revealedNumber == null }.revealedNumber)

        vm.openPortal(PortalLanguage.SPANISH)
        val url = vm.state.awaitState { it.pendingUrl != null }.pendingUrl!!
        assertTrue(PortalEndpoint.isPortalUrl(url))
        assertEquals("espanol", url.queryParameter("lang"))
    }

    @Test
    fun detalle_eliminar() {
        val id = runBlocking { (repo.add("00ZZ000004", null) as AddPraticaResult.Added).id }
        val vm = track(PraticaDetailViewModel(id, repo, runner, settings, cancelNotification = { cancelled += it }))
        vm.state.awaitState { it.pratica != null }

        vm.delete()
        vm.state.awaitState { it.deleted }
        assertTrue(runBlocking { repo.observeSummaries().first() }.isEmpty())
    }
}
