package app.prontosoggiorno.core

import app.prontosoggiorno.core.classification.Classification
import app.prontosoggiorno.core.classification.StatusClassifier
import app.prontosoggiorno.core.model.PraticaNumber
import app.prontosoggiorno.core.network.FeedSource
import app.prontosoggiorno.core.network.FetchResult
import app.prontosoggiorno.core.network.NetworkErrorKind
import app.prontosoggiorno.core.parser.FeedParseResult
import app.prontosoggiorno.core.parser.InvalidReason
import app.prontosoggiorno.core.parser.PortalFeedParser

/** Resultado de una verificación completa (red -> parser -> clasificación). */
sealed interface CheckOutcome {
    data class Checked(val classification: Classification) : CheckOutcome

    /** No se pudo verificar. El estado de la pratica es DESCONOCIDO, no "sin cambios". */
    sealed interface Failed : CheckOutcome

    data class NetworkFailure(val kind: NetworkErrorKind) : Failed
    data class HttpFailure(val code: Int) : Failed
    data object ResponseTooLarge : Failed
    data class UnexpectedResponse(val reason: UnexpectedReason) : Failed
}

enum class UnexpectedReason {
    NOT_RSS, NO_ITEM, MULTIPLE_ITEMS, NO_DESCRIPTION, EMPTY_DESCRIPTION,

    /** El feed no menciona el número consultado: posible respuesta cruzada o de caché. */
    NUMBER_MISMATCH,
}

/** Punto de entrada único del "cerebro" para la app Android. */
class PraticaChecker(private val source: FeedSource) {

    /** Bloqueante: llamar desde un hilo de fondo. */
    fun check(number: PraticaNumber): CheckOutcome =
        when (val fetched = source.fetchFeed(number)) {
            is FetchResult.NetworkError -> CheckOutcome.NetworkFailure(fetched.kind)
            is FetchResult.HttpError -> CheckOutcome.HttpFailure(fetched.code)
            FetchResult.TooLarge -> CheckOutcome.ResponseTooLarge
            is FetchResult.Body -> interpret(fetched.text, number)
        }

    private fun interpret(body: String, number: PraticaNumber): CheckOutcome =
        when (val parsed = PortalFeedParser.parse(body)) {
            is FeedParseResult.Invalid -> CheckOutcome.UnexpectedResponse(parsed.reason.toUnexpected())
            is FeedParseResult.Success -> {
                // Defensa: la respuesta debe corresponder al número pedido, nunca a otro.
                if (!parsed.item.title.contains(number.reveal(), ignoreCase = true)) {
                    CheckOutcome.UnexpectedResponse(UnexpectedReason.NUMBER_MISMATCH)
                } else {
                    CheckOutcome.Checked(StatusClassifier.classify(parsed.item))
                }
            }
        }

    private fun InvalidReason.toUnexpected(): UnexpectedReason = when (this) {
        InvalidReason.NOT_RSS -> UnexpectedReason.NOT_RSS
        InvalidReason.NO_ITEM -> UnexpectedReason.NO_ITEM
        InvalidReason.MULTIPLE_ITEMS -> UnexpectedReason.MULTIPLE_ITEMS
        InvalidReason.NO_DESCRIPTION -> UnexpectedReason.NO_DESCRIPTION
        InvalidReason.EMPTY_DESCRIPTION -> UnexpectedReason.EMPTY_DESCRIPTION
    }
}
