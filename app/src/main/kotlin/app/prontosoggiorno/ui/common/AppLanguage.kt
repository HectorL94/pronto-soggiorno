package app.prontosoggiorno.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.prontosoggiorno.core.network.PortalLanguage
import java.util.Locale

/**
 * Idiomas de la app, con su nombre escrito en ese mismo idioma (sin banderas:
 * un idioma no es un país). El idioma lo guarda Android, no la app.
 */
enum class AppLanguage(val tag: String, val nativeName: String) {
    ITALIAN("it", "Italiano"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    ARABIC("ar", "العربية"),
    RUSSIAN("ru", "Русский"),
    UKRAINIAN("uk", "Українська"),
    ;

    companion object {
        /** Idioma elegido dentro de la app; null = el del sistema. */
        fun current(): AppLanguage? {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) return null
            val language = locales[0]?.language ?: return null
            return entries.firstOrNull { it.tag == language }
        }

        /** Cambia el idioma de la app (Android vuelve a dibujar las pantallas). null = sistema. */
        fun apply(language: AppLanguage?) {
            AppCompatDelegate.setApplicationLocales(
                if (language == null) LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(language.tag),
            )
        }
    }
}

/** Idioma en que abrir la página del portal, según el idioma en que se ve la app. */
fun Locale.toPortalLanguage(): PortalLanguage = PortalLanguage.fromIsoCode(language)
