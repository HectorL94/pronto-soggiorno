package app.prontosoggiorno.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.prontosoggiorno.R
import app.prontosoggiorno.appContainer
import app.prontosoggiorno.ui.common.AppLanguage
import app.prontosoggiorno.ui.common.InfoBanner
import app.prontosoggiorno.ui.common.LanguageOptions
import app.prontosoggiorno.ui.common.rememberNotificationPermissionState
import app.prontosoggiorno.ui.theme.ProntoSoggiornoTheme
import kotlinx.coroutines.launch

/**
 * Primera apertura: aviso de independencia y privacidad → idioma → notificaciones → añadir pratica.
 * @param onFinished `addFirst = true` si el usuario quiere añadir su pratica enseguida.
 */
@Composable
fun OnboardingRoute(onFinished: (addFirst: Boolean) -> Unit) {
    val settings = LocalContext.current.appContainer.settingsRepository
    val scope = rememberCoroutineScope()
    val notifications = rememberNotificationPermissionState()
    OnboardingScreen(
        language = AppLanguage.current(),
        onLanguage = AppLanguage::apply,
        notificationsEnabled = notifications.enabled,
        onRequestNotifications = notifications::request,
        onFinished = { addFirst ->
            scope.launch {
                settings.setOnboardingCompleted(true)
                onFinished(addFirst)
            }
        },
    )
}

@Composable
fun OnboardingScreen(
    language: AppLanguage?,
    onLanguage: (AppLanguage?) -> Unit,
    notificationsEnabled: Boolean,
    onRequestNotifications: () -> Unit,
    onFinished: (addFirst: Boolean) -> Unit,
) {
    // rememberSaveable: el paso se conserva aunque la pantalla se redibuje al cambiar de idioma.
    var step by rememberSaveable { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (step) {
                    0 -> WelcomeStep()
                    1 -> LanguageStep(language, onLanguage)
                    else -> NotificationsStep(notificationsEnabled, onRequestNotifications)
                }
            }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepDots(current = step, total = STEPS)
                if (step < STEPS - 1) {
                    Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag(TAG_NEXT)) {
                        Text(stringResource(R.string.action_continue))
                    }
                    if (step > 0) {
                        TextButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_back))
                        }
                    }
                } else {
                    Button(onClick = { onFinished(true) }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag(TAG_FINISH)) {
                        Text(stringResource(R.string.onb_start))
                    }
                    OutlinedButton(onClick = { onFinished(false) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text(stringResource(R.string.onb_later))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.WelcomeStep() {
    Spacer(Modifier.height(16.dp))
    Box(
        Modifier
            .align(Alignment.CenterHorizontally)
            .size(112.dp)
            .background(colorResource(R.color.ic_launcher_background), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(112.dp))
    }
    Text(stringResource(R.string.onb_welcome_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.onb_welcome_body), style = MaterialTheme.typography.bodyLarge)
    InfoBanner(
        icon = R.drawable.ic_info,
        title = stringResource(R.string.onb_independence_title),
        text = stringResource(R.string.independence_long),
    )
    InfoBanner(
        icon = R.drawable.ic_lock,
        title = stringResource(R.string.onb_privacy_title),
        text = stringResource(R.string.onb_privacy_body),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun LanguageStep(language: AppLanguage?, onLanguage: (AppLanguage?) -> Unit) {
    StepHeader(R.drawable.ic_language, R.string.onb_language_title)
    LanguageOptions(selected = language, onSelect = onLanguage)
}

@Composable
private fun NotificationsStep(enabled: Boolean, onRequest: () -> Unit) {
    StepHeader(if (enabled) R.drawable.ic_notifications else R.drawable.ic_notifications_off, R.string.onb_notifications_title)
    Text(stringResource(R.string.onb_notifications_body), style = MaterialTheme.typography.bodyLarge)
    if (enabled) {
        InfoBanner(
            icon = R.drawable.ic_check_circle,
            text = stringResource(R.string.onb_notifications_granted),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    } else {
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth().height(48.dp).testTag(TAG_ALLOW)) {
            Text(stringResource(R.string.onb_notifications_allow))
        }
        Text(
            stringResource(R.string.onb_notifications_denied),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepHeader(icon: Int, title: Int) {
    Spacer(Modifier.height(16.dp))
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
    Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(total) { index ->
            Box(
                Modifier
                    .padding(4.dp)
                    .size(if (index == current) 10.dp else 8.dp)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

private const val STEPS = 3
const val TAG_NEXT = "onboarding_next"
const val TAG_FINISH = "onboarding_finish"
const val TAG_ALLOW = "onboarding_allow"

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    ProntoSoggiornoTheme { OnboardingScreen(null, {}, false, {}, {}) }
}
