package app.prontosoggiorno.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.prontosoggiorno.BuildConfig
import app.prontosoggiorno.R
import app.prontosoggiorno.appContainer
import app.prontosoggiorno.data.settings.AppSettings
import app.prontosoggiorno.data.settings.CheckInterval
import app.prontosoggiorno.ui.common.AppLanguage
import app.prontosoggiorno.ui.common.ConfirmDialog
import app.prontosoggiorno.ui.common.ExternalLinks
import app.prontosoggiorno.ui.common.LanguageOptions
import app.prontosoggiorno.ui.common.RadioRow
import app.prontosoggiorno.ui.common.SectionCard
import app.prontosoggiorno.ui.common.rememberNotificationPermissionState

@Composable
fun SettingsRoute(onBack: () -> Unit, onAllDataDeleted: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(container.settingsRepository, container.praticaRepository, container.notifier::cancelAll)
    }
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val notifications = rememberNotificationPermissionState()
    val current = settings ?: return

    SettingsScreen(
        settings = current,
        language = AppLanguage.current(),
        notificationsEnabled = notifications.enabled,
        onBack = onBack,
        onLanguage = AppLanguage::apply,
        onInterval = { viewModel.setInterval(it) },
        onNotifyAllChanges = { viewModel.setNotifyAllChanges(it) },
        onNotificationSettings = notifications::request,
        onSourceCode = if (ExternalLinks.SOURCE_CODE_URL != null) ({ ExternalLinks.openSourceCode(context) }) else null,
        onDeleteAll = { viewModel.deleteAll(onAllDataDeleted) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    language: AppLanguage?,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onLanguage: (AppLanguage?) -> Unit,
    onInterval: (CheckInterval) -> Unit,
    onNotifyAllChanges: (Boolean) -> Unit,
    onNotificationSettings: () -> Unit,
    onSourceCode: (() -> Unit)?,
    onDeleteAll: () -> Unit,
) {
    var languageDialog by rememberSaveable { mutableStateOf(false) }
    var licensesDialog by rememberSaveable { mutableStateOf(false) }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionCard {
                    SectionTitle(R.string.settings_language)
                    SettingRow(
                        icon = R.drawable.ic_language,
                        title = language?.nativeName ?: stringResource(R.string.settings_language_system),
                        onClick = { languageDialog = true },
                    )
                }
            }
            item {
                SectionCard {
                    SectionTitle(R.string.settings_section_checks)
                    Column(Modifier.selectableGroup()) {
                        CheckInterval.entries.forEach { interval ->
                            RadioRow(
                                label = pluralStringResource(R.plurals.settings_every_hours, interval.hours, interval.hours),
                                selected = settings.interval == interval,
                                supporting = if (interval == CheckInterval.DEFAULT) stringResource(R.string.settings_recommended) else null,
                                onClick = { onInterval(interval) },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.settings_frequency_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SectionCard {
                    SectionTitle(R.string.settings_section_notifications)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_notify_all)) },
                        supportingContent = { Text(stringResource(R.string.settings_notify_all_desc)) },
                        trailingContent = { Switch(checked = settings.notifyAllChanges, onCheckedChange = onNotifyAllChanges) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onNotifyAllChanges(!settings.notifyAllChanges) },
                    )
                    SettingRow(
                        icon = if (notificationsEnabled) R.drawable.ic_notifications else R.drawable.ic_notifications_off,
                        title = stringResource(R.string.settings_notifications_system),
                        subtitle = stringResource(
                            if (notificationsEnabled) R.string.settings_notifications_on else R.string.settings_notifications_off,
                        ),
                        onClick = onNotificationSettings,
                    )
                }
            }
            item {
                SectionCard {
                    SectionTitle(R.string.settings_section_privacy)
                    Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                SectionCard {
                    SectionTitle(R.string.settings_section_about)
                    Text(stringResource(R.string.independence_long), style = MaterialTheme.typography.bodyMedium)
                    SettingRow(
                        icon = R.drawable.ic_info,
                        title = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        subtitle = stringResource(R.string.settings_license),
                    )
                    SettingRow(
                        icon = R.drawable.ic_article,
                        title = stringResource(R.string.settings_licenses),
                        onClick = { licensesDialog = true },
                    )
                    if (onSourceCode != null) {
                        SettingRow(icon = R.drawable.ic_code, title = stringResource(R.string.settings_source), onClick = onSourceCode)
                    }
                }
            }
            item {
                SectionCard {
                    SectionTitle(R.string.settings_section_data)
                    SettingRow(
                        icon = R.drawable.ic_delete,
                        title = stringResource(R.string.settings_delete_all),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { deleteDialog = true },
                    )
                }
            }
        }
    }

    if (languageDialog) {
        AlertDialog(
            onDismissRequest = { languageDialog = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LanguageOptions(selected = language, onSelect = { languageDialog = false; onLanguage(it) })
                }
            },
            confirmButton = { TextButton(onClick = { languageDialog = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (licensesDialog) {
        AlertDialog(
            onDismissRequest = { licensesDialog = false },
            title = { Text(stringResource(R.string.settings_licenses)) },
            text = {
                Text(
                    stringResource(R.string.licenses_body),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { licensesDialog = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (deleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_all_title),
            text = stringResource(R.string.settings_delete_all_body),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { deleteDialog = false; onDeleteAll() },
            onDismiss = { deleteDialog = false },
        )
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SettingRow(
    icon: Int,
    title: String,
    subtitle: String? = null,
    tint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title, color = tint) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(painterResource(icon), contentDescription = null, tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}
