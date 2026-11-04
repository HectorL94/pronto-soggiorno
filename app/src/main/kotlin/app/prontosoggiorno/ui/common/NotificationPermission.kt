package app.prontosoggiorno.ui.common

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.prontosoggiorno.appContainer

@Stable
class NotificationPermissionState(
    enabledState: () -> Boolean,
    private val onRequest: () -> Unit,
) {
    private val enabledGetter = enabledState
    val enabled: Boolean get() = enabledGetter()
    fun request() = onRequest()
}

/**
 * Estado del permiso de notificaciones, actualizado cada vez que la pantalla vuelve a primer plano
 * (por si el usuario lo cambió en los ajustes del sistema).
 * La primera vez se muestra el diálogo del sistema; si el usuario ya lo rechazó, se abren los ajustes.
 */
@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    val notifier = remember(context) { context.appContainer.notifier }
    var enabled by remember { mutableStateOf(notifier.canNotify()) }
    var askedBefore by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        enabled = notifier.canNotify()
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedBefore = true
        enabled = notifier.canNotify()
    }

    return remember(launcher) {
        NotificationPermissionState(
            enabledState = { enabled },
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedBefore) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    ExternalLinks.openNotificationSettings(context)
                }
            },
        )
    }
}
