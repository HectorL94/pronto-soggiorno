package app.prontosoggiorno.core.parser

import app.prontosoggiorno.core.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PortalFeedParserTest {

    private fun success(body: String) = (PortalFeedParser.parse(body) as FeedParseResult.Success).item

    @Test
    fun `parsea la respuesta real de permiso listo pese a no ser XML bien formado`() {
        val item = success(Fixtures.load("ready.xml"))

        assertEquals(
            listOf(
                "Il documento di soggiorno è pronto per la consegna.In consegna, con sms le verrà " +
                    "indicato quando e dove ritirare il permesso di soggiorno",
                "Lo può ritirare presso: UFFICIO IMMIGRAZIONE PRESSO UFFICIO STRANIERI POLIZIA - COSENZA",
            ),
            item.lines,
        )
        assertEquals(1, item.links.size)
        assertEquals("Orari Questura - Cosenza", item.links[0].label)
        assertEquals(
            "https://questure.poliziadistato.it/servizio/orari/5730dc9d1659b749666027",
            item.links[0].url.toString(),
        )
        assertTrue(item.title.contains(Fixtures.FIXTURE_NUMBER))
    }

    @Test
    fun `parsea la respuesta real de no encontrado`() {
        val item = success(Fixtures.load("not_found.xml"))
        assertEquals(listOf("Il documento di soggiorno non è presente in archivio."), item.lines)
        assertTrue(item.links.isEmpty())
    }

    @Test
    fun `parsea la respuesta real en tramite en espanol`() {
        val item = success(Fixtures.load("in_progress_es.xml"))
        assertEquals(
            listOf("Situación de su permiso de residencia: EL PERMISO DE RESIDENCIA ESTÁ SIENDO TRAMITADO."),
            item.lines,
        )
        assertTrue(item.title.contains(Fixtures.FIXTURE_NUMBER))
    }

    @Test
    fun `una pagina HTML (mantenimiento, error) no es RSS`() {
        val html = "<!DOCTYPE html><html><body><h1>Servizio non disponibile</h1></body></html>"
        assertEquals(FeedParseResult.Invalid(InvalidReason.NOT_RSS), PortalFeedParser.parse(html))
        assertEquals(FeedParseResult.Invalid(InvalidReason.NOT_RSS), PortalFeedParser.parse(""))
    }

    @Test
    fun `estructuras inesperadas se rechazan en vez de adivinar`() {
        assertEquals(
            FeedParseResult.Invalid(InvalidReason.NO_ITEM),
            PortalFeedParser.parse("<rss><channel></channel></rss>"),
        )
        val two = "<rss><channel><item><description>a</description></item>" +
            "<item><description>b</description></item></channel></rss>"
        assertEquals(FeedParseResult.Invalid(InvalidReason.MULTIPLE_ITEMS), PortalFeedParser.parse(two))
        assertEquals(
            FeedParseResult.Invalid(InvalidReason.NO_DESCRIPTION),
            PortalFeedParser.parse("<rss><item><title>x</title></item></rss>"),
        )
        assertEquals(
            FeedParseResult.Invalid(InvalidReason.EMPTY_DESCRIPTION),
            PortalFeedParser.parse(Fixtures.syntheticFeed("  <br />  ")),
        )
    }

    @Test
    fun `descarta enlaces que no son https al portal`() {
        val html = "Testo<br/>" +
            "<a href=\"https://evil.example.com/x\">Fuera</a>" +
            "<a href=\"http://questure.poliziadistato.it/servizio/orari/1\">Sin https</a>" +
            "<a href='https://www.poliziadistato.it/'>Otro subdominio</a>" +
            "<a href=\"https://questure.poliziadistato.it/servizio/orari/1\">Orari</a>"
        val item = success(Fixtures.syntheticFeed(html))
        assertEquals(listOf("Orari"), item.links.map { it.label })
        assertEquals(listOf("Testo"), item.lines)
    }

    @Test
    fun `decodifica entidades y no inventa las desconocidas`() {
        val item = success(Fixtures.syntheticFeed("Citt&agrave; &#232; &#x00E8; &foo; &amp;"))
        assertEquals(listOf("Città è è &foo; &"), item.lines)
    }
}
