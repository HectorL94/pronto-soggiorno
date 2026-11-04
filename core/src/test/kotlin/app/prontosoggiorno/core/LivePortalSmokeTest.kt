package app.prontosoggiorno.core

import app.prontosoggiorno.core.model.PraticaNumber
import app.prontosoggiorno.core.network.PortalClient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * Prueba manual contra el portal REAL con el cliente de producción (lista blanca incluida).
 * Solo se ejecuta si se define la variable de entorno con TU PROPIO número:
 *
 *   PowerShell:  $env:PORTAL_SMOKE_NUMBER = "<tu numero>"; .\gradlew.bat :core:test --tests "*LivePortalSmokeTest*"
 *
 * Nunca imprime el número; solo el estado clasificado y el texto del portal.
 */
@EnabledIfEnvironmentVariable(named = "PORTAL_SMOKE_NUMBER", matches = ".+")
class LivePortalSmokeTest {

    @Test
    fun `consulta real al portal`() {
        val raw = System.getenv("PORTAL_SMOKE_NUMBER")
        val number = (PraticaNumber.parse(raw) as PraticaNumber.ParseResult.Valid).number

        val outcome = PraticaChecker(PortalClient.create("smoke-test")).check(number)

        println("Numero: ${number.masked()}")
        println("Resultado: $outcome")
        assertTrue(outcome is CheckOutcome.Checked, "Se esperaba una respuesta clasificable: $outcome")
    }
}
