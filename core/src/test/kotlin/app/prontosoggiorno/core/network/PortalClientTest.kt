package app.prontosoggiorno.core.network

import app.prontosoggiorno.core.Fixtures
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit

class PortalClientTest {

    private val server = MockWebServer()

    @BeforeEach
    fun start() = server.start()

    @AfterEach
    fun stop() = server.close()

    /** Cliente apuntando al servidor local de pruebas (sin lista blanca, que lo bloquearía). */
    private fun testClient(readTimeoutMs: Long = 5_000) = PortalClient(
        OkHttpClient.Builder()
            .followRedirects(false)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build(),
        server.url("/stranieri/"),
        "ProntoSoggiorno/test",
    )

    @Test
    fun `pide el feed en italiano con mime 4 y un User-Agent identificable`() {
        server.enqueue(MockResponse.Builder().code(200).body(Fixtures.load("ready.xml")).build())

        val result = testClient().fetchFeed(Fixtures.number())

        assertTrue(result is FetchResult.Body)
        val recorded = server.takeRequest()
        assertEquals("italian", recorded.url.queryParameter("lang"))
        assertEquals("4", recorded.url.queryParameter("mime"))
        assertEquals(Fixtures.FIXTURE_NUMBER, recorded.url.queryParameter("pratica"))
        assertEquals("ProntoSoggiorno/test", recorded.headers["User-Agent"])
    }

    @Test
    fun `codigos distintos de 200 son errores, y las redirecciones no se siguen`() {
        server.enqueue(MockResponse.Builder().code(503).build())
        assertEquals(FetchResult.HttpError(503), testClient().fetchFeed(Fixtures.number()))

        server.enqueue(MockResponse.Builder().code(302).addHeader("Location", "https://evil.example.com/").build())
        assertEquals(FetchResult.HttpError(302), testClient().fetchFeed(Fixtures.number()))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `respuesta demasiado grande se rechaza`() {
        val huge = "x".repeat((PortalClient.MAX_BODY_BYTES + 10).toInt())
        server.enqueue(MockResponse.Builder().code(200).body(huge).build())
        assertEquals(FetchResult.TooLarge, testClient().fetchFeed(Fixtures.number()))
    }

    @Test
    fun `timeout se informa como TIMEOUT`() {
        server.enqueue(
            MockResponse.Builder().code(200).body("late").headersDelay(2, TimeUnit.SECONDS).build(),
        )
        assertEquals(
            FetchResult.NetworkError(NetworkErrorKind.TIMEOUT),
            testClient(readTimeoutMs = 200).fetchFeed(Fixtures.number()),
        )
    }

    @Test
    fun `el cliente de produccion bloquea cualquier host que no sea el portal`() {
        val production = PortalClient(PortalClient.secureHttpClient(), server.url("/stranieri/"), "x")

        assertEquals(
            FetchResult.NetworkError(NetworkErrorKind.BLOCKED),
            production.fetchFeed(Fixtures.number()),
        )
        assertEquals(0, server.requestCount, "La petición no debe llegar a salir")
    }

    @Test
    fun `la lista blanca exige https y el host exacto`() {
        val http = PortalClient.secureHttpClient()
        for (url in listOf(
            "http://questure.poliziadistato.it/stranieri/",
            "https://www.poliziadistato.it/",
            "https://questure.poliziadistato.it.evil.example.com/",
            "https://example.com/",
        )) {
            val e = assertThrows<BlockedRequestException> {
                http.newCall(Request.Builder().url(url).build()).execute()
            }
            assertFalse(e.message!!.contains("stranieri"), "El mensaje no debe incluir la ruta/consulta")
        }
    }
}
