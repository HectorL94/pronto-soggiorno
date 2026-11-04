package app.prontosoggiorno.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.data.RecordedCheck
import app.prontosoggiorno.data.db.CheckTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationPolicyTest {

    private fun rec(change: ChangeKind, status: AdministrativeStatus?) = RecordedCheck(1, change, status, false)
    private fun decide(change: ChangeKind, status: AdministrativeStatus?, all: Boolean = true,
                       trigger: CheckTrigger = CheckTrigger.SCHEDULED) =
        NotificationPolicy.decide(rec(change, status), trigger, all)

    @Test
    fun listo_se_avisa_siempre_en_automatico() {
        assertEquals(NotificationKind.READY, decide(ChangeKind.CHANGED, AdministrativeStatus.READY_FOR_COLLECTION))
        assertEquals(NotificationKind.READY, decide(ChangeKind.CHANGED, AdministrativeStatus.READY_FOR_COLLECTION, all = false))
        assertEquals(NotificationKind.READY, decide(ChangeKind.FIRST_RESULT, AdministrativeStatus.READY_FOR_COLLECTION))
    }

    @Test
    fun otros_cambios_respetan_la_preferencia() {
        assertEquals(NotificationKind.STATUS_CHANGED, decide(ChangeKind.CHANGED, AdministrativeStatus.UNKNOWN))
        assertEquals(NotificationKind.STATUS_CHANGED, decide(ChangeKind.CHANGED, AdministrativeStatus.IN_PROGRESS))
        assertNull(decide(ChangeKind.CHANGED, AdministrativeStatus.IN_PROGRESS, all = false))
    }

    @Test
    fun sin_cambios_fallos_y_manuales_no_avisan() {
        assertNull(decide(ChangeKind.UNCHANGED, AdministrativeStatus.READY_FOR_COLLECTION))
        assertNull(decide(ChangeKind.FAILED, null))
        assertNull(decide(ChangeKind.FIRST_RESULT, AdministrativeStatus.IN_PROGRESS))
        assertNull(decide(ChangeKind.CHANGED, AdministrativeStatus.READY_FOR_COLLECTION, trigger = CheckTrigger.MANUAL))
    }

    @Test
    fun pausa_por_no_encontrado_se_avisa_siempre_salvo_en_manual() {
        val paused = RecordedCheck(1, ChangeKind.UNCHANGED, AdministrativeStatus.NOT_FOUND, false, pausedNotFound = true)
        assertEquals(NotificationKind.NOT_FOUND_PAUSED, NotificationPolicy.decide(paused, CheckTrigger.SCHEDULED, notifyAllChanges = false))
        assertNull(NotificationPolicy.decide(paused, CheckTrigger.MANUAL, notifyAllChanges = true))
    }
}
