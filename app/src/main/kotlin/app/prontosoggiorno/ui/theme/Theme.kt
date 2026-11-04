package app.prontosoggiorno.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp

/**
 * Colores tomados de los tokens Material 3 de `stitch_permesso_alert_android_app/permesso_alert/DESIGN.md`.
 * Sin "dynamic color": los colores de estado deben significar siempre lo mismo.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF002043),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF17365D),
    onPrimaryContainer = Color(0xFFD5E3FF),
    inversePrimary = Color(0xFFACC8F7),
    secondary = Color(0xFF006B58),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF82F7D8),
    onSecondaryContainer = Color(0xFF00382D),
    tertiary = Color(0xFF005137),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF85F8C4),
    onTertiaryContainer = Color(0xFF002114),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF111C2D),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF111C2D),
    surfaceVariant = Color(0xFFD8E3FB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF435F89),
    inverseSurface = Color(0xFF263143),
    inverseOnSurface = Color(0xFFECF1FF),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6CF),
    surfaceBright = Color(0xFFF9F9FF),
    surfaceDim = Color(0xFFCFDAF2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F3FF),
    surfaceContainer = Color(0xFFE7EEFF),
    surfaceContainerHigh = Color(0xFFDEE8FF),
    surfaceContainerHighest = Color(0xFFD8E3FB),
)

/** Variante oscura construida con los tonos "fixed" de los mismos tokens. */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFACC8F7),
    onPrimary = Color(0xFF113157),
    primaryContainer = Color(0xFF2B486F),
    onPrimaryContainer = Color(0xFFD5E3FF),
    inversePrimary = Color(0xFF435F89),
    secondary = Color(0xFF65DABC),
    onSecondary = Color(0xFF00382D),
    secondaryContainer = Color(0xFF005142),
    onSecondaryContainer = Color(0xFF82F7D8),
    tertiary = Color(0xFF68DBA9),
    onTertiary = Color(0xFF003825),
    tertiaryContainer = Color(0xFF005137),
    onTertiaryContainer = Color(0xFF85F8C4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10131B),
    onBackground = Color(0xFFDFE2EE),
    surface = Color(0xFF10131B),
    onSurface = Color(0xFFDFE2EE),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6CF),
    surfaceTint = Color(0xFFACC8F7),
    inverseSurface = Color(0xFFDFE2EE),
    inverseOnSurface = Color(0xFF2D3039),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF43474E),
    surfaceBright = Color(0xFF363943),
    surfaceDim = Color(0xFF10131B),
    surfaceContainerLowest = Color(0xFF0B0E15),
    surfaceContainerLow = Color(0xFF181B24),
    surfaceContainer = Color(0xFF1C1F28),
    surfaceContainerHigh = Color(0xFF272A33),
    surfaceContainerHighest = Color(0xFF32353E),
)

/** Colores con significado fijo para los estados de una pratica (DESIGN.md, "Status Pill Badges"). */
@Immutable
data class StatusTone(val container: Color, val content: Color, val dot: Color)

@Immutable
data class StatusColors(
    val ready: StatusTone,
    val inProgress: StatusTone,
    val problem: StatusTone,
    val neutral: StatusTone,
)

private val LightStatusColors = StatusColors(
    ready = StatusTone(Color(0xFFECFDF5), Color(0xFF046C4E), Color(0xFF10B981)),
    inProgress = StatusTone(Color(0xFFFFFBEB), Color(0xFF92400E), Color(0xFFF59E0B)),
    problem = StatusTone(Color(0xFFFEF2F2), Color(0xFFB91C1C), Color(0xFFEF4444)),
    neutral = StatusTone(Color(0xFFF1F5F9), Color(0xFF475569), Color(0xFF94A3B8)),
)

private val DarkStatusColors = StatusColors(
    ready = StatusTone(Color(0xFF0B3B2C), Color(0xFF6EE7B7), Color(0xFF34D399)),
    inProgress = StatusTone(Color(0xFF41300B), Color(0xFFFCD34D), Color(0xFFFBBF24)),
    problem = StatusTone(Color(0xFF4A1616), Color(0xFFFCA5A5), Color(0xFFF87171)),
    neutral = StatusTone(Color(0xFF1E293B), Color(0xFFCBD5E1), Color(0xFF94A3B8)),
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), // campos de texto
    large = RoundedCornerShape(16.dp), // tarjetas
    extraLarge = RoundedCornerShape(24.dp),
)

object AppTheme {
    val statusColors: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
}

@Composable
fun ProntoSoggiornoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = typographyFor(LocalLocale.current.language),
            shapes = AppShapes,
            content = content,
        )
    }
}
