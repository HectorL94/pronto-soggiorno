package app.prontosoggiorno.core.parser

/**
 * Utilidades mínimas para convertir el fragmento HTML de la descripción del feed en texto.
 * Kotlin puro (sin Android) para que funcione igual en tests y en el teléfono.
 */
internal object HtmlText {

    private val ENTITY = Regex("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,31});")
    private val TAG = Regex("<[^>]*>")
    private val HORIZONTAL_SPACE = Regex("[ \\t\\u00A0]+")

    /** Entidades con nombre que aparecen en textos en italiano y otras comunes. */
    private val NAMED = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "agrave" to "à", "aacute" to "á", "egrave" to "è", "eacute" to "é", "igrave" to "ì",
        "iacute" to "í", "ograve" to "ò", "oacute" to "ó", "ugrave" to "ù", "uacute" to "ú",
        "Agrave" to "À", "Aacute" to "Á", "Egrave" to "È", "Eacute" to "É", "Igrave" to "Ì",
        "Iacute" to "Í", "Ograve" to "Ò", "Oacute" to "Ó", "Ugrave" to "Ù", "Uacute" to "Ú",
        "acirc" to "â", "ecirc" to "ê", "icirc" to "î", "ocirc" to "ô", "ucirc" to "û",
        "auml" to "ä", "euml" to "ë", "iuml" to "ï", "ouml" to "ö", "uuml" to "ü",
        "ccedil" to "ç", "Ccedil" to "Ç", "ntilde" to "ñ", "Ntilde" to "Ñ",
        "laquo" to "«", "raquo" to "»", "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“",
        "rdquo" to "”", "ndash" to "–", "mdash" to "—", "hellip" to "…", "deg" to "°",
        "euro" to "€", "middot" to "·",
    )

    /** Decodifica entidades HTML. Las desconocidas se dejan tal cual (nunca se inventa texto). */
    fun decodeEntities(input: String): String = ENTITY.replace(input) { match ->
        val body = match.groupValues[1]
        when {
            body.startsWith("#x") || body.startsWith("#X") -> codePoint(body.substring(2).toIntOrNull(16))
            body.startsWith("#") -> codePoint(body.substring(1).toIntOrNull())
            else -> NAMED[body]
        } ?: match.value
    }

    private fun codePoint(cp: Int?): String? =
        if (cp != null && cp in 1..0x10FFFF && cp !in 0xD800..0xDFFF) String(Character.toChars(cp)) else null

    fun stripTags(input: String): String = TAG.replace(input, "")

    /** Colapsa espacios horizontales y recorta. */
    fun collapseSpaces(input: String): String = HORIZONTAL_SPACE.replace(input, " ").trim()
}
