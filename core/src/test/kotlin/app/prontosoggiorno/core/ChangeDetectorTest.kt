package app.prontosoggiorno.core

import app.prontosoggiorno.core.network.FeedSource
import app.prontosoggiorno.core.network.FetchResult
import app.prontosoggiorno.core.network.NetworkErrorKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangeDetectorTest {

    private fun checked(fixture: String) =
        PraticaChecker(FeedSource { FetchResult.Body(Fixtures.load(fixture)) }).check(Fixtures.number())
            as CheckOutcome.Checked

    private val inProgress = checked("in_progress.xml")
    private val ready = checked("ready.xml")
    private val failure = CheckOutcome.NetworkFailure(NetworkErrorKind.TIMEOUT)

    @Test
    fun `secuencia realista - primera, igual, fallo, cambio`() {
        assertEquals(ChangeKind.FIRST_RESULT, ChangeDetector.evaluate(null, inProgress))
        val fp = inProgress.classification.fingerprint
        assertEquals(ChangeKind.UNCHANGED, ChangeDetector.evaluate(fp, inProgress))
        assertEquals(ChangeKind.FAILED, ChangeDetector.evaluate(fp, failure))
        assertEquals(ChangeKind.CHANGED, ChangeDetector.evaluate(fp, ready))
    }

    @Test
    fun `un fallo sin historial previo sigue siendo un fallo`() {
        assertEquals(ChangeKind.FAILED, ChangeDetector.evaluate(null, failure))
    }

    @Test
    fun `solo se deja de monitorear cuando esta listo`() {
        assertTrue(ChangeDetector.shouldStopMonitoring(ready))
        assertFalse(ChangeDetector.shouldStopMonitoring(inProgress))
        assertFalse(ChangeDetector.shouldStopMonitoring(failure))
    }
}
