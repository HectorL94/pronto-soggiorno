package app.prontosoggiorno.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.prontosoggiorno.MainActivity
import app.prontosoggiorno.R

fun interface StatusNotifier {
    /** @param alias nombre que el usuario dio a la pratica. NUNCA se pasa el número. */
    fun notify(kind: NotificationKind, praticaId: Long, alias: String?)
}

/**
 * Notificaciones del sistema.
 * Privacidad: el texto nunca incluye el número de pratica, solo el alias; y en la pantalla de
 * bloqueo se muestra una versión pública genérica (VISIBILITY_PRIVATE + publicVersion).
 */
class AndroidStatusNotifier(private val context: Context) : StatusNotifier {

    fun ensureChannels() {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_READY, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.channel_ready_name))
                .setDescription(context.getString(R.string.channel_ready_description))
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_UPDATES, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.channel_updates_name))
                .setDescription(context.getString(R.string.channel_updates_description))
                .build(),
        )
    }

    fun canNotify(): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // comprobado en canNotify()
    override fun notify(kind: NotificationKind, praticaId: Long, alias: String?) {
        if (!canNotify()) return // sin permiso: la app mostrará el estado al abrirse
        ensureChannels()

        val (channel, title, text) = when (kind) {
            NotificationKind.READY -> Triple(
                CHANNEL_READY,
                alias?.let { context.getString(R.string.notification_ready_title_alias, it) }
                    ?: context.getString(R.string.notification_ready_title),
                context.getString(R.string.notification_ready_text),
            )
            NotificationKind.STATUS_CHANGED -> Triple(
                CHANNEL_UPDATES,
                alias?.let { context.getString(R.string.notification_changed_title_alias, it) }
                    ?: context.getString(R.string.notification_changed_title),
                context.getString(R.string.notification_changed_text),
            )
            NotificationKind.NOT_FOUND_PAUSED -> Triple(
                CHANNEL_UPDATES,
                alias?.let { context.getString(R.string.notification_notfound_title_alias, it) }
                    ?: context.getString(R.string.notification_notfound_title),
                context.getString(R.string.notification_notfound_text),
            )
        }

        val publicVersion = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_public_text))
            .build()

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(
                if (kind == NotificationKind.READY) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT,
            )
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(openAppIntent(praticaId))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(TAG, praticaId.toInt(), notification)
    }

    /** Quita el aviso de una pratica (al abrirla en la app o al eliminarla). */
    fun cancel(praticaId: Long) = NotificationManagerCompat.from(context).cancel(TAG, praticaId.toInt())

    fun cancelAll() = NotificationManagerCompat.from(context).cancelAll()

    private fun openAppIntent(praticaId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_PRATICA_ID, praticaId)
        return PendingIntent.getActivity(
            context,
            praticaId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_READY = "status_ready"
        const val CHANNEL_UPDATES = "status_updates"
        const val TAG = "pratica_status"
        const val EXTRA_PRATICA_ID = "pratica_id"
    }
}
