package app.prontosoggiorno.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import app.prontosoggiorno.data.settings.CheckInterval
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CheckSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: CheckScheduler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
        scheduler = CheckScheduler(workManager)
    }

    private fun infos() = workManager.getWorkInfosForUniqueWork(CheckScheduler.UNIQUE_WORK_NAME).get()

    @Test
    fun programa_un_unico_trabajo_con_cualquier_red() {
        scheduler.schedule(CheckInterval.EVERY_12_HOURS)

        val info = infos().single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(TimeUnit.HOURS.toMillis(12), info.periodicityInfo!!.repeatIntervalMillis)
        // CONNECTED = Wi-Fi o datos móviles (no UNMETERED, que sería solo Wi-Fi).
        assertEquals(NetworkType.CONNECTED, info.constraints.requiredNetworkType)
    }

    @Test
    fun cambiar_la_frecuencia_actualiza_sin_duplicar() {
        scheduler.schedule(CheckInterval.EVERY_12_HOURS)
        scheduler.schedule(CheckInterval.EVERY_24_HOURS)

        val info = infos().single()
        assertEquals(TimeUnit.HOURS.toMillis(24), info.periodicityInfo!!.repeatIntervalMillis)
    }

    @Test
    fun cancelar() {
        scheduler.schedule(CheckInterval.EVERY_6_HOURS)
        scheduler.cancel()
        assertEquals(WorkInfo.State.CANCELLED, infos().single().state)
    }
}
