package app.prontosoggiorno.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Lista blanca de red: bloquea cualquier petición que no vaya por HTTPS al host del portal.
 * Se registra como interceptor de aplicación, así que actúa ANTES de abrir cualquier conexión
 * (ni siquiera se resuelve el DNS de un host no permitido).
 */
class PortalOnlyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        if (!PortalEndpoint.isPortalUrl(url)) {
            // Solo esquema y host en el mensaje: la URL completa contiene el número de pratica.
            throw BlockedRequestException("${url.scheme}://${url.host}")
        }
        return chain.proceed(chain.request())
    }
}

class BlockedRequestException(target: String) :
    IOException("Request blocked: only https://${PortalEndpoint.HOST} is allowed (was $target)")
