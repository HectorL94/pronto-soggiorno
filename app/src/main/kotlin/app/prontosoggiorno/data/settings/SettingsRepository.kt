package app.prontosoggiorno.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Frecuencias permitidas. Sin opciones más agresivas: el estado cambia en días, no en horas. */
enum class CheckInterval(val hours: Int) {
    EVERY_6_HOURS(6),
    EVERY_12_HOURS(12),
    EVERY_24_HOURS(24),
    ;

    companion object {
        val DEFAULT = EVERY_12_HOURS
        fun fromHours(hours: Int?): CheckInterval = entries.firstOrNull { it.hours == hours } ?: DEFAULT
    }
}

data class AppSettings(
    val interval: CheckInterval = CheckInterval.DEFAULT,
    /** Avisar de cualquier cambio de texto, no solo cuando está listo. */
    val notifyAllChanges: Boolean = true,
    val onboardingCompleted: Boolean = false,
)

/**
 * Ajustes de la app (DataStore). No contiene datos personales.
 * El idioma NO se guarda aquí: lo gestiona Android con el selector de idioma por app.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            interval = CheckInterval.fromHours(prefs[Keys.INTERVAL_HOURS]),
            notifyAllChanges = prefs[Keys.NOTIFY_ALL_CHANGES] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    suspend fun setInterval(interval: CheckInterval) = edit { it[Keys.INTERVAL_HOURS] = interval.hours }
    suspend fun setNotifyAllChanges(enabled: Boolean) = edit { it[Keys.NOTIFY_ALL_CHANGES] = enabled }
    suspend fun setOnboardingCompleted(done: Boolean) = edit { it[Keys.ONBOARDING_COMPLETED] = done }

    /** Vuelve a los valores por defecto (parte de "borrar todos los datos"). */
    suspend fun clear() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private object Keys {
        val INTERVAL_HOURS = intPreferencesKey("interval_hours")
        val NOTIFY_ALL_CHANGES = booleanPreferencesKey("notify_all_changes")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    companion object {
        val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    }
}
