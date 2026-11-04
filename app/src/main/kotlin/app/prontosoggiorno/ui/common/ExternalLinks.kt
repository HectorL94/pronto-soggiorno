package app.prontosoggiorno.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import app.prontosoggiorno.core.network.PortalEndpoint
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Enlaces que el USUARIO decide abrir. Se abren en su navegador: la app no se conecta a ellos.
 * Solo se permiten direcciones del portal de la Polizia (y el código fuente, si está configurado).
 */
object ExternalLinks {

    /**
     * Repositorio público del código. Se rellenará al publicar el proyecto; mientras sea null,
     * la opción "Código fuente" no aparece.
     */
    val SOURCE_CODE_URL: String? = null

    /** Abre una dirección del portal. Devuelve false si no es del portal o no hay navegador. */
    fun openPortal(context: Context, url: HttpUrl): Boolean {
        if (!PortalEndpoint.isPortalUrl(url)) return false
        return view(context, url.toString().toUri())
    }

    fun openPortal(context: Context, url: String?): Boolean =
        url?.toHttpUrlOrNull()?.let { openPortal(context, it) } ?: false

    fun openSourceCode(context: Context): Boolean {
        val url = SOURCE_CODE_URL?.toHttpUrlOrNull()?.takeIf { it.isHttps } ?: return false
        return view(context, url.toString().toUri())
    }

    /** Ajustes de notificaciones de esta app en el sistema. */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun view(context: Context, uri: Uri): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
