package app.prontosoggiorno.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.prontosoggiorno.R
import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.model.IdentifierType
import app.prontosoggiorno.data.CheckRecord
import app.prontosoggiorno.data.PraticaSummary
import app.prontosoggiorno.data.db.CheckResultKind
import app.prontosoggiorno.data.db.CheckTrigger
import app.prontosoggiorno.data.settings.CheckInterval
import app.prontosoggiorno.ui.add.AddError
import app.prontosoggiorno.ui.add.AddPraticaScreen
import app.prontosoggiorno.ui.add.AddUiState
import app.prontosoggiorno.ui.add.TAG_SAVE
import app.prontosoggiorno.ui.detail.DetailUiState
import app.prontosoggiorno.ui.detail.PraticaDetailScreen
import app.prontosoggiorno.ui.detail.TAG_LIST
import app.prontosoggiorno.ui.detail.TAG_NUMBER
import app.prontosoggiorno.ui.detail.TAG_STATUS
import app.prontosoggiorno.ui.list.ListUiState
import app.prontosoggiorno.ui.list.PraticaListScreen
import app.prontosoggiorno.ui.theme.ProntoSoggiornoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pantallas con estados fijos: comprueban qué ve el usuario (y qué NO ve). */
@RunWith(AndroidJUnit4::class)
class ScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun record(status: AdministrativeStatus?, result: CheckResultKind = CheckResultKind.CHECKED, detail: String? = null) =
        CheckRecord(
            id = 1, checkedAt = 1_762_000_000_000, trigger = CheckTrigger.SCHEDULED, result = result,
            changeKind = if (result == CheckResultKind.CHECKED) ChangeKind.FIRST_RESULT else ChangeKind.FAILED,
            status = status, statusText = "Il documento di soggiorno è pronto per la consegna.",
            office = "QUESTURA DI PROVA", linkLabel = null, linkUrl = null, failureDetail = detail,
        )

    private fun summary(lastCheck: CheckRecord?, known: CheckRecord?, alias: String? = "Mamma") =
        PraticaSummary(7, alias, "00ZZ••••07", IdentifierType.PRATICA, paused = false,
            monitoringCompleted = known?.status == AdministrativeStatus.READY_FOR_COLLECTION, lastCheck = lastCheck, lastKnownState = known)

    @Test
    fun lista_vacia_ofrece_anadir_y_aviso_de_notificaciones() {
        var added = false
        compose.setContent {
            ProntoSoggiornoTheme {
                PraticaListScreen(ListUiState.Loaded(emptyList()), notificationsEnabled = false, {}, { added = true }, {}, {})
            }
        }
        compose.onNodeWithText(str(R.string.list_empty_title)).assertExists()
        compose.onNodeWithText(str(R.string.banner_notifications_off_title)).assertExists()
        compose.onNodeWithText(str(R.string.list_add)).performClick()
        assertEquals(true, added)
    }

    @Test
    fun tarjeta_muestra_alias_estado_y_numero_enmascarado() {
        val ready = record(AdministrativeStatus.READY_FOR_COLLECTION)
        compose.setContent {
            ProntoSoggiornoTheme {
                PraticaListScreen(ListUiState.Loaded(listOf(summary(ready, ready))), true, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Mamma").assertExists()
        compose.onNodeWithText("00ZZ••••07").assertExists()
        compose.onNodeWithText(str(R.string.status_ready)).assertExists()
    }

    @Test
    fun anadir_muestra_el_error() {
        compose.setContent {
            ProntoSoggiornoTheme { AddPraticaScreen(AddUiState(number = "123", error = AddError.WRONG_LENGTH), {}, {}, {}, {}) }
        }
        compose.onNodeWithText(str(R.string.add_error_length)).assertExists()
    }

    @Test
    fun anadir_sin_numero_no_deja_guardar() {
        compose.setContent { ProntoSoggiornoTheme { AddPraticaScreen(AddUiState(), {}, {}, {}, {}) } }
        compose.onNodeWithTag(TAG_SAVE).assertIsNotEnabled()
    }

    @Test
    fun detalle_listo_con_sms_oficina_y_fallo_posterior() {
        val ready = record(AdministrativeStatus.READY_FOR_COLLECTION)
        val failed = record(null, CheckResultKind.NETWORK_FAILURE, "NO_CONNECTION")
        compose.setContent {
            ProntoSoggiornoTheme {
                PraticaDetailScreen(
                    state = DetailUiState(
                        loading = false,
                        pratica = summary(lastCheck = failed, known = ready),
                        history = listOf(failed, ready),
                        interval = CheckInterval.EVERY_12_HOURS,
                    ),
                    snackbar = SnackbarHostState(),
                    onBack = {}, onCheckNow = {}, onTogglePaused = {}, onToggleReveal = {}, onOpenPortal = {},
                    onOpenLink = {}, onRename = {}, onDelete = {},
                )
            }
        }
        compose.onNodeWithTag(TAG_STATUS).assertExists()
        compose.onNodeWithText(str(R.string.detail_ready_title)).assertExists()
        compose.onNodeWithText(str(R.string.detail_ready_sms)).assertExists()
        compose.onNodeWithTag(TAG_LIST).performScrollToNode(hasText("QUESTURA DI PROVA"))
        compose.onNodeWithText("QUESTURA DI PROVA").assertExists()
        // El fallo de red posterior no borra el último estado conocido, solo se avisa.
        compose.onNodeWithText(str(R.string.detail_failed_banner, str(R.string.failure_no_connection))).assertExists()
        // Por defecto, el número está enmascarado.
        compose.onNodeWithTag(TAG_LIST).performScrollToNode(hasTestTag(TAG_NUMBER))
        compose.onNodeWithTag(TAG_NUMBER).assertExists()
        compose.onNodeWithText("00ZZ••••07").assertExists()
    }

    @Test
    fun detalle_pausado_por_no_encontrado_explica_y_ofrece_reanudar() {
        val notFound = record(AdministrativeStatus.NOT_FOUND)
        var resumed = false
        compose.setContent {
            ProntoSoggiornoTheme {
                PraticaDetailScreen(
                    state = DetailUiState(
                        loading = false,
                        pratica = summary(notFound, notFound).copy(notFoundPaused = true),
                        history = listOf(notFound),
                    ),
                    snackbar = SnackbarHostState(),
                    onBack = {}, onCheckNow = {}, onTogglePaused = { resumed = true }, onToggleReveal = {},
                    onOpenPortal = {}, onOpenLink = {}, onRename = {}, onDelete = {},
                )
            }
        }
        compose.onNodeWithText(str(R.string.detail_not_found_paused_title)).assertExists()
        compose.onNodeWithTag(TAG_LIST).performScrollToNode(hasText(str(R.string.detail_auto_not_found)))
        compose.onNodeWithTag(TAG_LIST).performScrollToNode(hasText(str(R.string.detail_resume)))
        compose.onNodeWithText(str(R.string.detail_resume)).performClick()
        assertEquals(true, resumed)
    }
}
