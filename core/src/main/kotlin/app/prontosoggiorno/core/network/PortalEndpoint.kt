package app.prontosoggiorno.core.network

import app.prontosoggiorno.core.model.PraticaNumber
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Único destino externo de la aplicación.
 * Cualquier URL que no cumpla [isPortalUrl] se rechaza, tanto en las peticiones de red
 * como en los enlaces que se ofrecen al usuario.
 */
object PortalEndpoint {
    const val HOST = "questure.poliziadistato.it"

    val BASE_URL: HttpUrl = "https://$HOST/stranieri/".toHttpUrl()

    /** Solo HTTPS y solo el host exacto del portal (sin subdominios ni otros dominios). */
    fun isPortalUrl(url: HttpUrl): Boolean = url.isHttps && url.host == HOST

    /**
     * URL del feed RSS (`mime=4`). La clasificación siempre se hace sobre el texto en italiano,
     * que es el único con patrones verificados.
     */
    fun feedUrl(number: PraticaNumber, base: HttpUrl = BASE_URL): HttpUrl =
        base.newBuilder()
            .addQueryParameter("lang", PortalLanguage.ITALIAN.param)
            .addQueryParameter("mime", "4")
            .addQueryParameter("pratica", number.reveal())
            .build()

    /** URL de la página HTML del portal (`mime=1`), para abrirla en el navegador del usuario. */
    fun pageUrl(number: PraticaNumber, language: PortalLanguage): HttpUrl =
        BASE_URL.newBuilder()
            .addQueryParameter("mime", "1")
            .addQueryParameter("lang", language.param)
            .addQueryParameter("pratica", number.reveal())
            .build()
}

/** Idiomas que admite el portal en el parámetro `lang`, con su código ISO 639-1. */
enum class PortalLanguage(val isoCode: String, val param: String) {
    ITALIAN("it", "italian"),
    ENGLISH("en", "english"),
    SPANISH("es", "espanol"),
    FRENCH("fr", "french"),
    ARABIC("ar", "arabic"),
    RUSSIAN("ru", "russian"),
    UKRAINIAN("uk", "ukrainian"),
    ;

    companion object {
        /** Idioma del portal para un código ISO; italiano si el portal no lo ofrece. */
        fun fromIsoCode(code: String): PortalLanguage =
            entries.firstOrNull { it.isoCode.equals(code, ignoreCase = true) } ?: ITALIAN
    }
}
