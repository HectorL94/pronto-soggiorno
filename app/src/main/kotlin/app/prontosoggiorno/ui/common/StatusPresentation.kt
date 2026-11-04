package app.prontosoggiorno.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.core.text.BidiFormatter
import app.prontosoggiorno.R
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.network.NetworkErrorKind
import app.prontosoggiorno.data.CheckRecord
import app.prontosoggiorno.data.db.CheckResultKind
import app.prontosoggiorno.ui.theme.AppTheme
import app.prontosoggiorno.ui.theme.StatusTone
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.util.Locale

/** Estado tal como se muestra en pantalla (incluye "aún sin verificar"). */
enum class StatusUi(
    @StringRes val label: Int,
    @StringRes val explanation: Int,
    @DrawableRes val icon: Int,
) {
    READY(R.string.status_ready, R.string.status_ready_explain, R.drawable.ic_check_circle),
    IN_PROGRESS(R.string.status_in_progress, R.string.status_in_progress_explain, R.drawable.ic_hourglass),
    NOT_FOUND(R.string.status_not_found, R.string.status_not_found_explain, R.drawable.ic_search_off),
    UNKNOWN(R.string.status_unknown, R.string.status_unknown_explain, R.drawable.ic_help),
    NOT_CHECKED(R.string.status_not_checked, R.string.status_not_checked_explain, R.drawable.ic_schedule),
    ;

    val tone: StatusTone
        @Composable @ReadOnlyComposable get() = when (this) {
            READY -> AppTheme.statusColors.ready
            IN_PROGRESS -> AppTheme.statusColors.inProgress
            NOT_FOUND -> AppTheme.statusColors.problem
            UNKNOWN, NOT_CHECKED -> AppTheme.statusColors.neutral
        }

    companion object {
        fun of(status: AdministrativeStatus?): StatusUi = when (status) {
            AdministrativeStatus.READY_FOR_COLLECTION -> READY
            AdministrativeStatus.IN_PROGRESS -> IN_PROGRESS
            AdministrativeStatus.NOT_FOUND -> NOT_FOUND
            AdministrativeStatus.UNKNOWN -> UNKNOWN
            null -> NOT_CHECKED
        }
    }
}

val CheckRecord.failed: Boolean get() = result != CheckResultKind.CHECKED

/** Motivo de un fallo, en palabras sencillas. Null si la verificación salió bien. */
@Composable
fun failureReason(record: CheckRecord): String? = when (record.result) {
    CheckResultKind.CHECKED -> null
    CheckResultKind.NETWORK_FAILURE -> stringResource(
        when (record.failureDetail?.let { runCatching { NetworkErrorKind.valueOf(it) }.getOrNull() }) {
            NetworkErrorKind.TIMEOUT -> R.string.failure_timeout
            NetworkErrorKind.NO_CONNECTION -> R.string.failure_no_connection
            NetworkErrorKind.TLS -> R.string.failure_tls
            NetworkErrorKind.BLOCKED -> R.string.failure_blocked
            NetworkErrorKind.OTHER, null -> R.string.failure_network
        },
    )
    CheckResultKind.HTTP_FAILURE -> stringResource(R.string.failure_http, record.failureDetail.orEmpty())
    CheckResultKind.RESPONSE_TOO_LARGE, CheckResultKind.UNEXPECTED_RESPONSE -> stringResource(R.string.failure_unexpected)
}

/** Idioma en que se está mostrando la app ahora mismo. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalLocale.current.platformLocale

/** Formatea fecha y hora en el idioma de la app, p. ej. "4 nov 2026, 14:15". */
@Composable
fun rememberDateTimeFormatter(): (Long) -> String {
    val locale = currentLocale()
    return remember(locale) {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            // Mismas cifras que el resto del texto (p. ej. cifras arábigo-índicas en árabe).
            .withDecimalStyle(DecimalStyle.of(locale))
        val zone = ZoneId.systemDefault()
        // Aislada (Unicode bidi) para que no se mezcle con el texto que la rodea en árabe.
        val bidi = BidiFormatter.getInstance(locale)
        val format: (Long) -> String = { millis -> bidi.unicodeWrap(formatter.format(Instant.ofEpochMilli(millis).atZone(zone))) }
        format
    }
}
