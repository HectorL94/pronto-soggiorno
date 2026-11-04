package app.prontosoggiorno

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.prontosoggiorno.core.CheckOutcome
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.classification.Classification
import app.prontosoggiorno.core.parser.PortalLink
import app.prontosoggiorno.data.AddPraticaResult
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.crypto.KeystoreFieldCipher
import app.prontosoggiorno.data.db.CheckTrigger
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Datos de DEMOSTRACIÓN para las capturas de la tienda (tools/screenshots.py).
 * Números ficticios (00ZZ…, provincia inexistente) y textos con la forma real del portal.
 * Escribe en la base de datos real de la app instalada, así que solo se ejecuta si se pide:
 *   adb shell am instrument -w -e seedScreenshots true -e class app.prontosoggiorno.ScreenshotSeeder ...
 * En la suite normal de tests se omite. No forma parte de la app publicada.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotSeeder {

    @Test
    fun seed() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seedScreenshots") == "true")
        val container = ApplicationProvider.getApplicationContext<Context>().appContainer
        container.praticaRepository.deleteAll()

        val now = System.currentTimeMillis()
        var clock = now
        val repo = PraticaRepository(container.database, KeystoreFieldCipher(), clock = { clock })
        fun at(daysAgo: Long, hoursAgo: Long = 0) {
            clock = now - TimeUnit.DAYS.toMillis(daysAgo) - TimeUnit.HOURS.toMillis(hoursAgo)
        }

        suspend fun add(number: String, alias: String) = (repo.add(number, alias) as AddPraticaResult.Added).id
        suspend fun record(id: Long, status: AdministrativeStatus, text: String, office: String? = null, link: Boolean = false, manual: Boolean = false) {
            val links = if (link) listOf(PortalLink("Orari", "https://questure.poliziadistato.it/stranieri/".toHttpUrl())) else emptyList()
            repo.recordOutcome(id, CheckOutcome.Checked(Classification(status, text, office, links)), if (manual) CheckTrigger.MANUAL else CheckTrigger.SCHEDULED)
        }

        // Lista: en trámite durante semanas y, hace 2 horas, lista para retirar.
        at(21); val amina = add("00ZZ000001", "Amina")
        record(amina, AdministrativeStatus.IN_PROGRESS, IN_PROGRESS, manual = true)
        at(14); record(amina, AdministrativeStatus.IN_PROGRESS, IN_PROGRESS)
        at(7); record(amina, AdministrativeStatus.IN_PROGRESS, IN_PROGRESS)
        at(0, 2); record(amina, AdministrativeStatus.READY_FOR_COLLECTION, READY, office = "QUESTURA DI ESEMPIO - UFFICIO IMMIGRAZIONE", link = true)

        // En trámite.
        at(10); val marco = add("00ZZ000002", "Marco")
        record(marco, AdministrativeStatus.IN_PROGRESS, IN_PROGRESS, manual = true)
        at(5); record(marco, AdministrativeStatus.IN_PROGRESS, IN_PROGRESS)
        at(0, 3); record(marco, AdministrativeStatus.IN_PROGRESS, IN_PROGRESS)

        // Recién añadida y todavía no aparece en el portal.
        at(1); val lucia = add("000000000003", "Lucía")
        record(lucia, AdministrativeStatus.NOT_FOUND, NOT_FOUND, manual = true)

        container.settingsRepository.setOnboardingCompleted(true)
    }

    private companion object {
        // Textos reales del portal (verificados), sin datos personales.
        const val IN_PROGRESS = "Stato del suo permesso di soggiorno: documento di soggiorno in trattazione."
        const val READY = "Il documento di soggiorno è pronto per la consegna.In consegna, con sms le verrà indicato quando e dove ritirare il permesso di soggiorno"
        const val NOT_FOUND = "Il documento di soggiorno non è presente in archivio."
    }
}
