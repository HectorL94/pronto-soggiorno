package app.prontosoggiorno.core.classification

import app.prontosoggiorno.core.Fixtures
import app.prontosoggiorno.core.parser.FeedParseResult
import app.prontosoggiorno.core.parser.PortalFeedParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StatusClassifierTest {

    private fun classify(body: String): Classification =
        StatusClassifier.classify((PortalFeedParser.parse(body) as FeedParseResult.Success).item)

    private fun classifyHtml(html: String) = classify(Fixtures.syntheticFeed(html))

    @Test
    fun `respuesta real lista - estado, texto y oficina`() {
        val c = classify(Fixtures.load("ready.xml"))
        assertEquals(AdministrativeStatus.READY_FOR_COLLECTION, c.status)
        assertEquals("UFFICIO IMMIGRAZIONE PRESSO UFFICIO STRANIERI POLIZIA - COSENZA", c.office)
        assert(c.statusText.startsWith("Il documento di soggiorno è pronto per la consegna."))
        assert(!c.statusText.contains("ritirare presso"))
    }

    @Test
    fun `respuesta real no encontrado`() {
        val c = classify(Fixtures.load("not_found.xml"))
        assertEquals(AdministrativeStatus.NOT_FOUND, c.status)
        assertNull(c.office)
    }

    @Test
    fun `respuesta real en tramite`() {
        val c = classify(Fixtures.load("in_progress.xml"))
        assertEquals(AdministrativeStatus.IN_PROGRESS, c.status)
        assertEquals("Stato del suo permesso di soggiorno: documento di soggiorno in trattazione.", c.statusText)
        assertNull(c.office)
    }

    @Test
    fun `una negacion no se confunde con en tramite`() {
        assertEquals(AdministrativeStatus.UNKNOWN, classifyHtml("Il documento non in trattazione.").status)
    }

    @Test
    fun `solo se clasifica en italiano - un texto en espanol queda UNKNOWN`() {
        // La app siempre consulta lang=italian. Esta fixture documenta que otro idioma
        // nunca se clasifica por accidente.
        val c = classify(Fixtures.load("in_progress_es.xml"))
        assertEquals(AdministrativeStatus.UNKNOWN, c.status)
        assertEquals("Situación de su permiso de residencia: EL PERMISO DE RESIDENCIA ESTÁ SIENDO TRAMITADO.", c.statusText)
    }

    @Test
    fun `texto desconocido es UNKNOWN y conserva el texto original`() {
        val c = classifyHtml("La pratica &egrave; in una fase nuova mai vista.")
        assertEquals(AdministrativeStatus.UNKNOWN, c.status)
        assertEquals("La pratica è in una fase nuova mai vista.", c.statusText)
    }

    @Test
    fun `una negacion no se confunde con listo`() {
        val c = classifyHtml("Il documento di soggiorno non &egrave; pronto per la consegna.")
        assertEquals(AdministrativeStatus.UNKNOWN, c.status)
    }

    @Test
    fun `textos contradictorios son UNKNOWN`() {
        val c = classifyHtml(
            "Il documento di soggiorno &egrave; pronto per la consegna.<br/>" +
                "Il documento di soggiorno non &egrave; presente in archivio.",
        )
        assertEquals(AdministrativeStatus.UNKNOWN, c.status)
    }

    @Test
    fun `la huella ignora mayusculas, acentos y espacios pero detecta cambios reales`() {
        val a = classifyHtml("Il documento &egrave;   PRONTO per la consegna.")
        val b = classifyHtml("il documento e pronto per la consegna.")
        val other = classifyHtml("Il documento di soggiorno non &egrave; presente in archivio.")
        assertEquals(a.fingerprint, b.fingerprint)
        assertNotEquals(a.fingerprint, other.fingerprint)
    }
}
