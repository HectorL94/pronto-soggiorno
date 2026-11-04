package app.prontosoggiorno.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private fun newRepository(): SettingsRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "settings_test_${System.nanoTime()}.preferences_pb")
        return SettingsRepository(PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) { file })
    }

    @Test
    fun valores_por_defecto() = testScope.runTest {
        assertEquals(AppSettings(), newRepository().settings.first())
        assertEquals(CheckInterval.EVERY_12_HOURS, AppSettings().interval)
    }

    @Test
    fun guarda_cambios_y_los_borra() = testScope.runTest {
        val repo = newRepository()
        repo.setInterval(CheckInterval.EVERY_24_HOURS)
        repo.setNotifyAllChanges(false)
        repo.setOnboardingCompleted(true)
        assertEquals(
            AppSettings(CheckInterval.EVERY_24_HOURS, notifyAllChanges = false, onboardingCompleted = true),
            repo.settings.first(),
        )

        repo.clear()
        assertEquals(AppSettings(), repo.settings.first())
    }

    @Test
    fun un_intervalo_desconocido_vuelve_al_valor_por_defecto() {
        assertEquals(CheckInterval.DEFAULT, CheckInterval.fromHours(2))
        assertEquals(CheckInterval.DEFAULT, CheckInterval.fromHours(null))
    }
}
