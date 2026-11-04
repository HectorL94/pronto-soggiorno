package app.prontosoggiorno.core.network

import app.prontosoggiorno.core.model.PraticaNumber
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/** Fuente del feed. Interfaz para poder sustituirla en tests sin red. */
fun interface FeedSource {
    /** Bloqueante: llamar desde un hilo de fondo (en Android, el worker o Dispatchers.IO). */
    fun fetchFeed(number: PraticaNumber): FetchResult
}

sealed interface FetchResult {
    /** Respuesta 200 con su cuerpo. `toString` no muestra el cuerpo: contiene el número. */
    class Body(val text: String) : FetchResult {
        override fun toString(): String = "Body(${text.length} chars)"
    }

    data class HttpError(val code: Int) : FetchResult

    data class NetworkError(val kind: NetworkErrorKind) : FetchResult

    data object TooLarge : FetchResult
}

enum class NetworkErrorKind { TIMEOUT, NO_CONNECTION, TLS, BLOCKED, OTHER }

/**
 * Cliente HTTP del portal. Solo se puede crear desde fuera del módulo con [create],
 * que siempre incluye la lista blanca ([PortalOnlyInterceptor]) y desactiva redirecciones.
 */
class PortalClient internal constructor(
    private val http: OkHttpClient,
    private val baseUrl: HttpUrl,
    private val userAgent: String,
) : FeedSource {

    override fun fetchFeed(number: PraticaNumber): FetchResult {
        val request = Request.Builder()
            .url(PortalEndpoint.feedUrl(number, baseUrl))
            .header("User-Agent", userAgent)
            .header("Accept", "application/rss+xml, application/xml;q=0.9, */*;q=0.1")
            .get()
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                if (response.code != 200) return FetchResult.HttpError(response.code)
                val source = response.body.source()
                // request(n) == true  ->  hay al menos n bytes: la respuesta es demasiado grande.
                if (source.request(MAX_BODY_BYTES + 1)) return FetchResult.TooLarge
                // El feed declara UTF-8 en su cabecera XML; se decodifica siempre como UTF-8.
                FetchResult.Body(source.buffer.readUtf8())
            }
        } catch (e: IOException) {
            // Nunca se propaga el mensaje de la excepción: podría incluir partes de la URL.
            FetchResult.NetworkError(classify(e))
        }
    }

    private fun classify(e: IOException): NetworkErrorKind = when (e) {
        is BlockedRequestException -> NetworkErrorKind.BLOCKED
        is InterruptedIOException -> NetworkErrorKind.TIMEOUT // incluye SocketTimeoutException
        is UnknownHostException, is ConnectException, is NoRouteToHostException ->
            NetworkErrorKind.NO_CONNECTION
        is SSLException -> NetworkErrorKind.TLS
        else -> NetworkErrorKind.OTHER
    }

    companion object {
        /** Límite de seguridad: una respuesta normal ocupa ~1 KB. */
        internal const val MAX_BODY_BYTES = 256L * 1024

        /** @param appVersion versión de la app, para un User-Agent honesto e identificable. */
        fun create(appVersion: String): PortalClient =
            PortalClient(secureHttpClient(), PortalEndpoint.BASE_URL, "ProntoSoggiorno/$appVersion")

        /** Configuración de producción. `internal` para poder verificarla en tests. */
        internal fun secureHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(PortalOnlyInterceptor())
            // Sin redirecciones: una respuesta 3xx se trata como error y nunca se sigue a otro sitio.
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .build()
    }
}
