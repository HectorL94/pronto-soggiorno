package app.prontosoggiorno.core

import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.network.FeedSource
import app.prontosoggiorno.core.network.FetchResult
import app.prontosoggiorno.core.network.NetworkErrorKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PraticaCheckerTest {

    private fun checkerReturning(result: FetchResult) = PraticaChecker(FeedSource { result })

    @Test
    fun `flujo completo con la respuesta real lista`() {
        val outcome = checkerReturning(FetchResult.Body(Fixtures.load("ready.xml"))).check(Fixtures.number())
        outcome as CheckOutcome.Checked
        assertEquals(AdministrativeStatus.READY_FOR_COLLECTION, outcome.classification.status)
    }

    @Test
    fun `si el feed habla de otro numero, no se acepta`() {
        val other = Fixtures.number("99ZZ999999")
        val outcome = checkerReturning(FetchResult.Body(Fixtures.load("ready.xml"))).check(other)
        assertEquals(CheckOutcome.UnexpectedResponse(UnexpectedReason.NUMBER_MISMATCH), outcome)
    }

    @Test
    fun `errores de red, HTTP y respuestas raras se propagan como fallos`() {
        val n = Fixtures.number()
        assertEquals(
            CheckOutcome.NetworkFailure(NetworkErrorKind.NO_CONNECTION),
            checkerReturning(FetchResult.NetworkError(NetworkErrorKind.NO_CONNECTION)).check(n),
        )
        assertEquals(CheckOutcome.HttpFailure(500), checkerReturning(FetchResult.HttpError(500)).check(n))
        assertEquals(CheckOutcome.ResponseTooLarge, checkerReturning(FetchResult.TooLarge).check(n))
        assertEquals(
            CheckOutcome.UnexpectedResponse(UnexpectedReason.NOT_RSS),
            checkerReturning(FetchResult.Body("<html>Manutenzione</html>")).check(n),
        )
    }
}
