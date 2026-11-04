package app.prontosoggiorno.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.CheckOutcome
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.classification.Classification
import app.prontosoggiorno.core.model.PraticaNumber
import app.prontosoggiorno.core.network.NetworkErrorKind
import app.prontosoggiorno.data.crypto.KeystoreFieldCipher
import app.prontosoggiorno.data.db.AppDatabase
import app.prontosoggiorno.data.db.CheckResultKind
import app.prontosoggiorno.data.db.CheckTrigger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PraticaRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var cipher: KeystoreFieldCipher
    private lateinit var repo: PraticaRepository
    private var now = 1_000_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        cipher = KeystoreFieldCipher(keyAlias = "test_repository_key")
        repo = PraticaRepository(db, cipher, clock = { now })
    }

    @After
    fun tearDown() {
        db.close()
        cipher.deleteKey()
    }

    private fun checked(status: AdministrativeStatus, text: String, office: String? = null) =
        CheckOutcome.Checked(Classification(status, text, office, emptyList()))

    private suspend fun addOk(raw: String, alias: String? = null): Long =
        (repo.add(raw, alias) as AddPraticaResult.Added).id

    @Test
    fun anadir_validar_y_evitar_duplicados() = runTest {
        val id = addOk("00ZZ000000", "  Yo  ")
        assertEquals(AddPraticaResult.Duplicate, repo.add("00-zz 000000", null))
        assertEquals(
            AddPraticaResult.Invalid(PraticaNumber.InvalidReason.WRONG_LENGTH),
            repo.add("123", null),
        )

        val summary = repo.observeSummaries().first().single()
        assertEquals(id, summary.id)
        assertEquals("Yo", summary.alias)
        assertEquals("00ZZ••••00", summary.maskedNumber)
        assertNull(summary.lastCheck)
    }

    @Test
    fun el_numero_y_el_alias_nunca_se_guardan_en_claro() = runTest {
        addOk("00ZZ000000", "Mamma")
        db.openHelper.readableDatabase.query("SELECT * FROM pratiche").use { c ->
            assertTrue(c.moveToFirst())
            for (i in 0 until c.columnCount) {
                val raw = when (c.getType(i)) {
                    android.database.Cursor.FIELD_TYPE_BLOB -> String(c.getBlob(i), Charsets.ISO_8859_1)
                    android.database.Cursor.FIELD_TYPE_STRING -> c.getString(i)
                    else -> ""
                }
                assertFalse("Columna ${c.getColumnName(i)} en claro", raw.contains("00ZZ000000"))
                assertFalse("Columna ${c.getColumnName(i)} en claro", raw.contains("Mamma"))
            }
        }
    }

    @Test
    fun historial_detecta_cambios_y_ignora_fallos() = runTest {
        val id = addOk("00ZZ000000")
        val inProgress = checked(AdministrativeStatus.IN_PROGRESS, "documento di soggiorno in trattazione.")
        val ready = checked(AdministrativeStatus.READY_FOR_COLLECTION, "è pronto per la consegna.", "QUESTURA")

        assertEquals(ChangeKind.FIRST_RESULT, repo.recordOutcome(id, inProgress, CheckTrigger.MANUAL)!!.changeKind)
        assertEquals(ChangeKind.UNCHANGED, repo.recordOutcome(id, inProgress, CheckTrigger.SCHEDULED)!!.changeKind)
        val failed = repo.recordOutcome(id, CheckOutcome.NetworkFailure(NetworkErrorKind.TIMEOUT), CheckTrigger.SCHEDULED)!!
        assertEquals(ChangeKind.FAILED, failed.changeKind)
        assertEquals(ChangeKind.UNCHANGED, repo.recordOutcome(id, inProgress, CheckTrigger.SCHEDULED)!!.changeKind)

        val becameReady = repo.recordOutcome(id, ready, CheckTrigger.SCHEDULED)!!
        assertEquals(ChangeKind.CHANGED, becameReady.changeKind)
        assertTrue(becameReady.monitoringJustCompleted)

        val summary = repo.observeSummaries().first().single()
        assertTrue(summary.monitoringCompleted)
        val lastKnown = summary.lastKnownState!!
        assertEquals(AdministrativeStatus.READY_FOR_COLLECTION, lastKnown.status)
        assertEquals("QUESTURA", lastKnown.office)
        assertEquals(5, repo.observeHistory(id).first().size)
    }

    @Test
    fun un_fallo_no_borra_el_ultimo_estado_conocido() = runTest {
        val id = addOk("00ZZ000000")
        repo.recordOutcome(id, checked(AdministrativeStatus.IN_PROGRESS, "in trattazione"), CheckTrigger.MANUAL)
        repo.recordOutcome(id, CheckOutcome.HttpFailure(503), CheckTrigger.SCHEDULED)

        val summary = repo.observeSummaries().first().single()
        val lastCheck = summary.lastCheck!!
        assertEquals(CheckResultKind.HTTP_FAILURE, lastCheck.result)
        assertEquals("503", lastCheck.failureDetail)
        assertEquals(AdministrativeStatus.IN_PROGRESS, summary.lastKnownState!!.status)
    }

    @Test
    fun solo_se_monitorean_las_activas() = runTest {
        val active = addOk("00ZZ000001")
        val paused = addOk("00ZZ000002")
        val ready = addOk("00ZZ000003")
        repo.setPaused(paused, true)
        repo.recordOutcome(ready, checked(AdministrativeStatus.READY_FOR_COLLECTION, "pronto"), CheckTrigger.MANUAL)

        assertEquals(listOf(active), repo.monitoredPratiche().map { it.id })
        assertEquals("00ZZ000001", repo.monitoredPratiche().single().number.reveal())

        repo.resumeMonitoring(ready)
        assertEquals(listOf(active, ready), repo.monitoredPratiche().map { it.id })
    }

    @Test
    fun borrar_elimina_tambien_el_historial() = runTest {
        val id = addOk("00ZZ000000")
        repo.recordOutcome(id, checked(AdministrativeStatus.IN_PROGRESS, "in trattazione"), CheckTrigger.MANUAL)
        repo.delete(id)

        assertTrue(repo.observeSummaries().first().isEmpty())
        assertTrue(repo.observeHistory(id).first().isEmpty())
        assertNull(repo.recordOutcome(id, CheckOutcome.ResponseTooLarge, CheckTrigger.SCHEDULED))
    }

    @Test
    fun limpieza_conserva_cambios_y_la_ultima_verificacion() = runTest {
        val id = addOk("00ZZ000000")
        val text = checked(AdministrativeStatus.IN_PROGRESS, "in trattazione")
        repo.recordOutcome(id, text, CheckTrigger.MANUAL) // FIRST_RESULT: se conserva siempre
        repeat(3) { repo.recordOutcome(id, text, CheckTrigger.SCHEDULED) } // UNCHANGED rutinarias

        now += TimeUnit.DAYS.toMillis(61) // pasa el periodo de retención
        repo.recordOutcome(id, text, CheckTrigger.SCHEDULED)

        val kinds = repo.observeHistory(id).first().map { it.changeKind }
        assertEquals(listOf(ChangeKind.UNCHANGED, ChangeKind.FIRST_RESULT), kinds)
    }

    @Test
    fun alias_se_normaliza_y_se_puede_cambiar_o_quitar() = runTest {
        val id = addOk("00ZZ000000", "   ")
        assertNull(repo.observeSummaries().first().single().alias)

        repo.rename(id, "x".repeat(100))
        assertEquals(PraticaRepository.MAX_ALIAS_LENGTH, repo.observeSummaries().first().single().alias!!.length)

        repo.rename(id, null)
        assertNull(repo.observeSummaries().first().single().alias)
        assertNotNull(repo.numberFor(id))
    }

    // --- "No encontrado" durante 30 días: pausa automática ---

    private val notFound get() = checked(AdministrativeStatus.NOT_FOUND, "non è presente in archivio")
    private fun days(n: Long) = TimeUnit.DAYS.toMillis(n)

    @Test
    fun no_encontrado_menos_de_30_dias_sigue_verificandose() = runTest {
        val id = addOk("00ZZ000000")
        repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)
        now += days(29)
        val recorded = repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)!!

        assertFalse(recorded.pausedNotFound)
        assertEquals(listOf(id), repo.monitoredPratiche().map { it.id })
    }

    @Test
    fun no_encontrado_30_dias_seguidos_pausa_una_sola_vez() = runTest {
        val id = addOk("00ZZ000000")
        repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)
        now += days(15)
        repo.recordOutcome(id, CheckOutcome.NetworkFailure(NetworkErrorKind.TIMEOUT), CheckTrigger.SCHEDULED)
        now += days(15)
        val recorded = repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)!!

        assertTrue(recorded.pausedNotFound) // los fallos de red intermedios no reinician el plazo
        assertTrue(repo.monitoredPratiche().isEmpty())
        assertTrue(repo.observeSummaries().first().single().notFoundPaused)
        // Una verificación manual posterior, todavía no encontrada: no se "vuelve a pausar".
        assertFalse(repo.recordOutcome(id, notFound, CheckTrigger.MANUAL)!!.pausedNotFound)
    }

    @Test
    fun si_aparece_se_reactiva_sola() = runTest {
        val id = addOk("00ZZ000000")
        repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)
        now += days(31)
        repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)
        assertTrue(repo.monitoredPratiche().isEmpty())

        // El usuario verifica a mano y el portal ya la encuentra.
        repo.recordOutcome(id, checked(AdministrativeStatus.IN_PROGRESS, "in trattazione"), CheckTrigger.MANUAL)

        assertFalse(repo.observeSummaries().first().single().notFoundPaused)
        assertEquals(listOf(id), repo.monitoredPratiche().map { it.id })
    }

    @Test
    fun el_plazo_cuenta_desde_el_ultimo_estado_distinto() = runTest {
        val id = addOk("00ZZ000000")
        repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)
        now += days(20)
        repo.recordOutcome(id, checked(AdministrativeStatus.IN_PROGRESS, "in trattazione"), CheckTrigger.SCHEDULED)
        now += days(1)
        repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED) // vuelve a "no encontrado": nuevo plazo
        now += days(20)

        assertFalse(repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)!!.pausedNotFound)
    }

    @Test
    fun reanudar_a_mano_da_otros_30_dias() = runTest {
        val id = addOk("00ZZ000000")
        repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)
        now += days(30)
        assertTrue(repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)!!.pausedNotFound)

        repo.resumeAfterNotFound(id)
        assertEquals(listOf(id), repo.monitoredPratiche().map { it.id })
        now += days(10)
        assertFalse(repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)!!.pausedNotFound)
        now += days(20)
        assertTrue(repo.recordOutcome(id, notFound, CheckTrigger.SCHEDULED)!!.pausedNotFound)
    }
}
