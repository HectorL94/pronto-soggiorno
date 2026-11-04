package app.prontosoggiorno.core.classification

import app.prontosoggiorno.core.parser.FeedItem
import app.prontosoggiorno.core.parser.PortalLink
import java.text.Normalizer
import java.util.Locale

/** Estado administrativo deducido del texto publicado por el portal. */
enum class AdministrativeStatus {
    /** "Il documento di soggiorno è pronto per la consegna." */
    READY_FOR_COLLECTION,

    /** "Stato del suo permesso di soggiorno: documento di soggiorno in trattazione." */
    IN_PROGRESS,

    /** "Il documento di soggiorno non è presente in archivio." */
    NOT_FOUND,

    /** Texto no reconocido o ambiguo. La UI debe mostrar el texto original, nunca adivinar. */
    UNKNOWN,
}

data class Classification(
    val status: AdministrativeStatus,
    /** Texto del estado tal como lo publica el portal (sin la línea de la oficina). */
    val statusText: String,
    /** Oficina de retiro, si el portal la indica. */
    val office: String?,
    /** Enlaces del portal (solo `https://questure.poliziadistato.it`). */
    val links: List<PortalLink>,
) {
    /**
     * Huella para detectar cambios. Se notifica cuando cambia la huella, aunque el
     * [status] siga igual o sea [AdministrativeStatus.UNKNOWN]: cualquier cambio de texto
     * del portal puede importarle al usuario.
     */
    val fingerprint: String
        get() = TextNormalizer.normalize(statusText) + "|" + TextNormalizer.normalize(office.orEmpty())
}

/**
 * Patrones de texto del portal (siempre en italiano, `lang=italian`).
 *
 * REGLA: solo se añaden patrones comprobados contra una respuesta REAL del portal,
 * con su fixture en `core/src/test/resources/fixtures/` y un test. Nada de suposiciones.
 * Los patrones se aplican sobre texto normalizado (minúsculas, sin acentos, espacios simples).
 */
internal object StatusPatterns {
    val byStatus: Map<AdministrativeStatus, List<Regex>> = mapOf(
        // Verificado 2026-11-04 (fixture ready.xml). El lookbehind evita "non e pronto...".
        AdministrativeStatus.READY_FOR_COLLECTION to listOf(
            Regex("(?<!non )\\be pronto per la consegna"),
        ),
        // Verificado 2026-11-04 (fixture not_found.xml).
        AdministrativeStatus.NOT_FOUND to listOf(
            Regex("\\bnon e presente in archivio"),
        ),
        // Verificado 2026-11-04 (fixture in_progress.xml):
        // "Stato del suo permesso di soggiorno: documento di soggiorno in trattazione."
        AdministrativeStatus.IN_PROGRESS to listOf(
            Regex("(?<!non )\\bin trattazione\\b"),
        ),
    )

    /** Línea que introduce la oficina de retiro: "Lo può ritirare presso: <OFICINA>". */
    val officePrefix = Regex("^lo puo ritirare presso\\s*:?\\s*")
}

object StatusClassifier {

    fun classify(item: FeedItem): Classification {
        val officeLine = item.lines.firstOrNull {
            StatusPatterns.officePrefix.containsMatchIn(TextNormalizer.normalize(it))
        }
        val office = officeLine
            ?.substringAfter(':', missingDelimiterValue = "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val statusText = item.lines.filter { it !== officeLine }.joinToString("\n")
        val normalized = TextNormalizer.normalize(statusText)

        val matches = StatusPatterns.byStatus
            .filterValues { patterns -> patterns.any { it.containsMatchIn(normalized) } }
            .keys

        // Ninguna coincidencia o varias a la vez (contradicción) -> UNKNOWN: no se adivina.
        val status = matches.singleOrNull() ?: AdministrativeStatus.UNKNOWN

        return Classification(status, statusText, office, item.links)
    }
}

internal object TextNormalizer {
    private val DIACRITICS = Regex("\\p{Mn}+")
    private val WHITESPACE = Regex("\\s+")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE, " ")
            .trim()
}
