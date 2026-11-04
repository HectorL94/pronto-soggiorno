package app.prontosoggiorno.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.prontosoggiorno.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidStatusNotifierTest {

    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = AndroidStatusNotifier(context)

    @After
    fun cleanUp() = manager.cancelAll()

    private fun posted(id: Int) =
        manager.activeNotifications.firstOrNull { it.id == id && it.tag == AndroidStatusNotifier.TAG }

    @Test
    fun aviso_de_listo_con_alias_y_privado_en_pantalla_de_bloqueo() {
        notifier.notify(NotificationKind.READY, praticaId = 4242, alias = "Mamma")

        val sbn = posted(4242)
        assertNotNull("La notificación debe estar publicada", sbn)
        val n = sbn!!.notification
        assertEquals(AndroidStatusNotifier.CHANNEL_READY, n.channelId)
        assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility)
        assertEquals(
            context.getString(R.string.notification_ready_title_alias, "Mamma"),
            n.extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
        )
        // La versión de pantalla de bloqueo no revela ni el alias.
        val publicText = n.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        assertEquals(context.getString(R.string.notification_public_text), publicText)
        assertTrue(!publicText.contains("Mamma"))
    }

    @Test
    fun sin_alias_usa_un_titulo_generico() {
        notifier.notify(NotificationKind.STATUS_CHANGED, praticaId = 4343, alias = null)

        val n = posted(4343)!!.notification
        assertEquals(AndroidStatusNotifier.CHANNEL_UPDATES, n.channelId)
        assertEquals(
            context.getString(R.string.notification_changed_title),
            n.extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
        )
    }

    @Test
    fun los_canales_existen() {
        notifier.ensureChannels()
        assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel(AndroidStatusNotifier.CHANNEL_READY).importance)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, manager.getNotificationChannel(AndroidStatusNotifier.CHANNEL_UPDATES).importance)
    }
}
