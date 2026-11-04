package app.prontosoggiorno.core.model

import app.prontosoggiorno.core.model.PraticaNumber.InvalidReason
import app.prontosoggiorno.core.model.PraticaNumber.ParseResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class PraticaNumberTest {

    private fun valid(raw: String) = (PraticaNumber.parse(raw) as ParseResult.Valid).number

    @Test
    fun `acepta pratica con minusculas, espacios y guiones`() {
        val n = valid(" 26-zz 004 321 ")
        assertEquals(IdentifierType.PRATICA, n.type)
        assertEquals("26ZZ004321", n.reveal())
    }

    @Test
    fun `convierte cifras arabigo-indicas de un teclado arabe`() {
        assertEquals("26ZZ004321", valid("٢٦ZZ٠٠٤٣٢١").reveal())
    }

    @Test
    fun `acepta assicurata de 12 cifras`() {
        val n = valid("0504 1234 5678")
        assertEquals(IdentifierType.ASSICURATA, n.type)
        assertEquals("050412345678", n.reveal())
    }

    @Test
    fun `rechaza entradas invalidas con el motivo correcto`() {
        assertEquals(ParseResult.Invalid(InvalidReason.EMPTY), PraticaNumber.parse("  - "))
        assertEquals(ParseResult.Invalid(InvalidReason.WRONG_LENGTH), PraticaNumber.parse("26ZZ0043"))
        assertEquals(ParseResult.Invalid(InvalidReason.WRONG_FORMAT), PraticaNumber.parse("2XZZ004321"))
        assertEquals(ParseResult.Invalid(InvalidReason.WRONG_FORMAT), PraticaNumber.parse("05041234567A"))
    }

    @Test
    fun `toString y masked nunca exponen el numero completo`() {
        val n = valid("26ZZ004321")
        assertEquals("26ZZ••••21", n.masked())
        assertFalse(n.toString().contains("26ZZ004321"))
        assertFalse(PraticaNumber.parse("26ZZ004321").toString().contains("004321"))
        assertEquals("••••••••5678", valid("050412345678").masked())
    }
}
