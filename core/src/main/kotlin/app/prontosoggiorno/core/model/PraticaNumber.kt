package app.prontosoggiorno.core.model

import java.util.Locale

/** Tipo de identificador que acepta el portal en el parámetro `pratica`. */
enum class IdentifierType {
    /** Número de pratica: 2 cifras (año) + 2 letras (provincia) + 6 cifras. Ej. formato `26RM123456`. */
    PRATICA,

    /** Código de la "assicurata" de Poste Italiane: 12 cifras. */
    ASSICURATA,
}

/**
 * Identificador validado de una solicitud.
 *
 * Privacidad: [toString] está enmascarado a propósito, de modo que un log, un mensaje de
 * error o una interpolación accidental nunca expongan el número completo.
 * El valor real solo se obtiene llamando explícitamente a [reveal].
 */
class PraticaNumber private constructor(
    val type: IdentifierType,
    private val value: String,
) {
    /** Valor completo. Usar solo para construir la petición al portal o para cifrarlo. */
    fun reveal(): String = value

    /** Versión para mostrar en pantalla, p. ej. `26RM••••56`. */
    fun masked(): String = when (type) {
        IdentifierType.PRATICA -> value.take(4) + MASK.repeat(4) + value.takeLast(2)
        IdentifierType.ASSICURATA -> MASK.repeat(8) + value.takeLast(4)
    }

    override fun toString(): String = "PraticaNumber($type, ${masked()})"

    override fun equals(other: Any?): Boolean =
        other is PraticaNumber && other.type == type && other.value == value

    override fun hashCode(): Int = 31 * type.hashCode() + value.hashCode()

    sealed interface ParseResult {
        class Valid(val number: PraticaNumber) : ParseResult {
            override fun toString(): String = "Valid($number)"
        }

        data class Invalid(val reason: InvalidReason) : ParseResult
    }

    enum class InvalidReason { EMPTY, WRONG_LENGTH, WRONG_FORMAT }

    companion object {
        private const val MASK = "•"
        private val PRATICA_REGEX = Regex("^[0-9]{2}[A-Z]{2}[0-9]{6}$")
        private val ASSICURATA_REGEX = Regex("^[0-9]{12}$")

        /**
         * Normaliza lo que escribe el usuario: quita espacios, guiones y otros separadores,
         * pasa a mayúsculas y convierte cifras de otros sistemas (p. ej. arábigo-índicas
         * `٠١٢٣` de un teclado árabe) a cifras ASCII.
         */
        fun normalize(raw: String): String = buildString {
            for (c in raw) {
                when {
                    Character.isDigit(c) -> append('0' + Character.digit(c, 10))
                    c in 'a'..'z' || c in 'A'..'Z' -> append(c.uppercaseChar())
                    Character.isLetter(c) -> append(c.toString().uppercase(Locale.ROOT))
                    // Separadores (espacio, guion, punto, barra...) se ignoran.
                }
            }
        }

        /** Valida y detecta el tipo automáticamente según el formato. */
        fun parse(raw: String): ParseResult {
            val normalized = normalize(raw)
            return when {
                normalized.isEmpty() -> ParseResult.Invalid(InvalidReason.EMPTY)
                PRATICA_REGEX.matches(normalized) ->
                    ParseResult.Valid(PraticaNumber(IdentifierType.PRATICA, normalized))
                ASSICURATA_REGEX.matches(normalized) ->
                    ParseResult.Valid(PraticaNumber(IdentifierType.ASSICURATA, normalized))
                normalized.length != 10 && normalized.length != 12 ->
                    ParseResult.Invalid(InvalidReason.WRONG_LENGTH)
                else -> ParseResult.Invalid(InvalidReason.WRONG_FORMAT)
            }
        }
    }
}
