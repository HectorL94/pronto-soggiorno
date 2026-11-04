package app.prontosoggiorno.core.parser

import app.prontosoggiorno.core.network.PortalEndpoint
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Enlace publicado en la respuesta del portal (p. ej. horarios de la Questura). */
data class PortalLink(val label: String, val url: HttpUrl)

/**
 * Contenido útil de la única `<item>` del feed.
 * No es `data class` a propósito: [title] contiene el número y no debe aparecer en `toString`.
 */
class FeedItem(
    /** Título del item; incluye el número consultado. Solo para comprobaciones, nunca se guarda. */
    val title: String,
    /** Líneas de texto de la descripción, ya sin HTML y sin el texto de los enlaces. */
    val lines: List<String>,
    /** Solo enlaces HTTPS al host del portal; cualquier otro se descarta. */
    val links: List<PortalLink>,
) {
    override fun toString(): String = "FeedItem(lines=${lines.size}, links=${links.size})"
}

sealed interface FeedParseResult {
    class Success(val item: FeedItem) : FeedParseResult {
        override fun toString(): String = "Success($item)"
    }

    data class Invalid(val reason: InvalidReason) : FeedParseResult
}

enum class InvalidReason { NOT_RSS, NO_ITEM, MULTIPLE_ITEMS, NO_DESCRIPTION, EMPTY_DESCRIPTION }

/**
 * Parser tolerante del feed RSS del portal.
 *
 * Por qué no se usa un parser XML estándar: el feed real NO es XML bien formado
 * (el `<guid>` contiene `&pratica=` sin escapar), así que un parser estricto fallaría
 * siempre. Se extraen solo las piezas necesarias y se valida la estructura; ante cualquier
 * forma inesperada se devuelve [FeedParseResult.Invalid], nunca un resultado adivinado.
 */
object PortalFeedParser {

    private val OPTIONS = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    private val RSS_ROOT = Regex("<rss\\b", RegexOption.IGNORE_CASE)
    private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val ITEM = Regex("<item\\b[^>]*>(.*?)</item>", OPTIONS)
    private val TITLE = Regex("<title\\b[^>]*>(.*?)</title>", OPTIONS)
    private val DESCRIPTION = Regex("<description\\b[^>]*>(.*?)</description>", OPTIONS)
    private val CDATA = Regex("<!\\[CDATA\\[(.*?)]]>", RegexOption.DOT_MATCHES_ALL)
    private val ANCHOR = Regex("<a\\b([^>]*)>(.*?)</a>", OPTIONS)
    private val HREF = Regex("href\\s*=\\s*([\"'])(.*?)\\1", OPTIONS)
    private val LINE_BREAK = Regex("<br\\s*/?>|</p>|</div>", RegexOption.IGNORE_CASE)

    fun parse(body: String): FeedParseResult {
        if (!RSS_ROOT.containsMatchIn(body)) return FeedParseResult.Invalid(InvalidReason.NOT_RSS)

        val withoutComments = COMMENT.replace(body, "")
        val items = ITEM.findAll(withoutComments).toList()
        when {
            items.isEmpty() -> return FeedParseResult.Invalid(InvalidReason.NO_ITEM)
            items.size > 1 -> return FeedParseResult.Invalid(InvalidReason.MULTIPLE_ITEMS)
        }
        val itemXml = items.single().groupValues[1]

        val title = TITLE.find(itemXml)?.groupValues?.get(1)
            ?.let { HtmlText.collapseSpaces(HtmlText.decodeEntities(unwrapCdata(it))) }
            .orEmpty()

        val rawDescription = DESCRIPTION.find(itemXml)?.groupValues?.get(1)
            ?: return FeedParseResult.Invalid(InvalidReason.NO_DESCRIPTION)
        val html = unwrapCdata(rawDescription)

        val links = mutableListOf<PortalLink>()
        val htmlWithoutAnchors = ANCHOR.replace(html) { anchor ->
            val href = HREF.find(anchor.groupValues[1])?.groupValues?.get(2)
            val url = href?.let { HtmlText.decodeEntities(it).trim().toHttpUrlOrNull() }
            val label = HtmlText.collapseSpaces(HtmlText.decodeEntities(HtmlText.stripTags(anchor.groupValues[2])))
            if (url != null && PortalEndpoint.isPortalUrl(url) && label.isNotEmpty()) {
                links += PortalLink(label, url)
            }
            "\n"
        }

        val lines = LINE_BREAK.replace(htmlWithoutAnchors, "\n")
            .let(HtmlText::stripTags)
            .let(HtmlText::decodeEntities)
            .lines()
            .map(HtmlText::collapseSpaces)
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) return FeedParseResult.Invalid(InvalidReason.EMPTY_DESCRIPTION)
        return FeedParseResult.Success(FeedItem(title, lines, links))
    }

    /** Si el contenido viene en CDATA se toma tal cual; si no, viene escapado como XML. */
    private fun unwrapCdata(content: String): String {
        val cdata = CDATA.findAll(content).map { it.groupValues[1] }.toList()
        return if (cdata.isNotEmpty()) cdata.joinToString("\n") else HtmlText.decodeEntities(content)
    }
}
