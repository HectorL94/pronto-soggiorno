package app.prontosoggiorno.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.prontosoggiorno.R

/**
 * Inter va EMPAQUETADA en la app (res/font, licencia SIL OFL 1.1): no se descarga nada.
 * Es una fuente variable; se declara un peso por cada grosor que usamos.
 * Inter no tiene letras árabes: con la app en árabe se usa la fuente del sistema (ver [typographyFor]).
 */
private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0, family: FontFamily = Inter) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    // Sin espaciado extra fuera de Inter: separar letras árabes rompe su unión.
    letterSpacing = if (family == Inter) tracking.em else 0.em,
)

/** Escala tipográfica de DESIGN.md adaptada a los nombres de Material 3. */
private fun typography(family: FontFamily) = Typography(
    displaySmall = style(32, 40, FontWeight.Bold, -0.02, family),
    headlineLarge = style(28, 36, FontWeight.Bold, -0.01, family),
    headlineMedium = style(26, 32, FontWeight.Bold, -0.01, family),
    headlineSmall = style(22, 28, FontWeight.SemiBold, 0.0, family),
    titleLarge = style(22, 28, FontWeight.SemiBold, 0.0, family),
    titleMedium = style(18, 24, FontWeight.SemiBold, 0.0, family),
    titleSmall = style(16, 22, FontWeight.SemiBold, 0.0, family),
    bodyLarge = style(16, 24, FontWeight.Normal, 0.0, family),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.0, family),
    bodySmall = style(12, 16, FontWeight.Normal, 0.0, family),
    labelLarge = style(14, 20, FontWeight.SemiBold, 0.01, family),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.02, family),
    labelSmall = style(11, 14, FontWeight.Medium, 0.03, family),
)

private val InterTypography = typography(Inter)
private val SystemTypography = typography(FontFamily.Default)

/**
 * Escritura árabe: la fuente del sistema trae letras árabes en todos los grosores (Inter no).
 * Si se mezclara Inter con la fuente de reserva, los títulos árabes perderían la negrita.
 */
fun typographyFor(language: String): Typography =
    if (language in SYSTEM_FONT_LANGUAGES) SystemTypography else InterTypography

private val SYSTEM_FONT_LANGUAGES = setOf("ar")

/**
 * Texto publicado por el portal: siempre está en italiano, así que se marca como italiano y de
 * izquierda a derecha, también cuando la app está en árabe.
 */
fun TextStyle.asPortalText(): TextStyle = copy(textDirection = TextDirection.Ltr, localeList = LocaleList("it"))

/**
 * Números de pratica: espaciado amplio, cifras tabulares y SIEMPRE de izquierda a derecha,
 * también en árabe (un número no se invierte).
 */
val NumberTextStyle = style(18, 24, FontWeight.SemiBold, 0.08).copy(
    fontFeatureSettings = "tnum",
    textDirection = TextDirection.Ltr,
)
