package app.prontosoggiorno.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.prontosoggiorno.R
import app.prontosoggiorno.appContainer
import app.prontosoggiorno.core.ChangeKind
import app.prontosoggiorno.core.classification.AdministrativeStatus
import app.prontosoggiorno.core.model.IdentifierType
import app.prontosoggiorno.data.CheckRecord
import app.prontosoggiorno.data.PraticaSummary
import app.prontosoggiorno.data.db.CheckResultKind
import app.prontosoggiorno.data.db.CheckTrigger
import app.prontosoggiorno.ui.common.IndependenceFootnote
import app.prontosoggiorno.ui.common.InfoBanner
import app.prontosoggiorno.ui.common.NumberText
import app.prontosoggiorno.ui.common.StatusPill
import app.prontosoggiorno.ui.common.StatusUi
import app.prontosoggiorno.ui.common.failed
import app.prontosoggiorno.ui.common.rememberDateTimeFormatter
import app.prontosoggiorno.ui.common.rememberNotificationPermissionState
import app.prontosoggiorno.ui.theme.NumberTextStyle
import app.prontosoggiorno.ui.theme.ProntoSoggiornoTheme

@Composable
fun PraticaListRoute(onAdd: () -> Unit, onOpen: (Long) -> Unit, onSettings: () -> Unit) {
    val container = LocalContext.current.appContainer
    val viewModel: PraticaListViewModel = viewModel { PraticaListViewModel(container.praticaRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notifications = rememberNotificationPermissionState()
    PraticaListScreen(
        state = state,
        notificationsEnabled = notifications.enabled,
        onEnableNotifications = notifications::request,
        onAdd = onAdd,
        onOpen = onOpen,
        onSettings = onSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PraticaListScreen(
    state: ListUiState,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        floatingActionButton = {
            if (state is ListUiState.Loaded && state.pratiche.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.list_add)) },
                )
            }
        },
    ) { padding ->
        when (state) {
            ListUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is ListUiState.Loaded -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!notificationsEnabled) {
                    item(key = "notifications") {
                        InfoBanner(
                            icon = R.drawable.ic_notifications_off,
                            title = stringResource(R.string.banner_notifications_off_title),
                            text = stringResource(R.string.banner_notifications_off_body),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            action = {
                                TextButton(onClick = onEnableNotifications, contentPadding = PaddingValues(0.dp)) {
                                    Text(stringResource(R.string.banner_notifications_enable))
                                }
                            },
                        )
                    }
                }
                if (state.pratiche.isEmpty()) {
                    item(key = "empty") { EmptyState(onAdd) }
                } else {
                    items(state.pratiche, key = { it.id }) { PraticaCard(it, onClick = { onOpen(it.id) }) }
                }
                item(key = "footnote") { IndependenceFootnote(Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_article),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Text(stringResource(R.string.list_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.list_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAdd, modifier = Modifier.height(48.dp)) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.list_add))
        }
    }
}

@Composable
private fun PraticaCard(pratica: PraticaSummary, onClick: () -> Unit) {
    val format = rememberDateTimeFormatter()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    pratica.alias ?: stringResource(R.string.list_unnamed),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (!pratica.unreadable) StatusPill(StatusUi.of(pratica.lastKnownState?.status))
            }
            if (pratica.unreadable) {
                Text(
                    stringResource(R.string.list_unreadable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                return@Column
            }
            NumberText(
                pratica.maskedNumber.orEmpty(),
                style = NumberTextStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val last = pratica.lastCheck
            val secondary = buildList {
                add(
                    if (last == null) stringResource(R.string.list_never_checked)
                    else stringResource(R.string.list_last_check, format(last.checkedAt)),
                )
                when {
                    pratica.paused -> add(stringResource(R.string.label_paused))
                    pratica.monitoringCompleted -> add(stringResource(R.string.label_monitoring_done))
                    pratica.notFoundPaused -> add(stringResource(R.string.label_not_found_paused))
                }
            }.joinToString(" · ")
            Text(secondary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (last != null && last.failed) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_cloud_off),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        stringResource(R.string.list_last_attempt_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

// --- Vistas previas (solo para Android Studio; datos inventados, nunca en la app real) ---

private fun previewRecord(status: AdministrativeStatus?, result: CheckResultKind = CheckResultKind.CHECKED) = CheckRecord(
    id = 1, checkedAt = 1_762_000_000_000, trigger = CheckTrigger.SCHEDULED, result = result,
    changeKind = ChangeKind.UNCHANGED, status = status, statusText = null, office = null,
    linkLabel = null, linkUrl = null, failureDetail = null,
)

@Preview(showBackground = true)
@Composable
private fun ListPreview() {
    ProntoSoggiornoTheme {
        PraticaListScreen(
            state = ListUiState.Loaded(
                listOf(
                    PraticaSummary(1, "Yo", "00ZZ••••00", IdentifierType.PRATICA, false, false,
                        previewRecord(AdministrativeStatus.IN_PROGRESS), previewRecord(AdministrativeStatus.IN_PROGRESS)),
                    PraticaSummary(2, "Mamá", "••••••••0000", IdentifierType.ASSICURATA, false, true,
                        previewRecord(AdministrativeStatus.READY_FOR_COLLECTION), previewRecord(AdministrativeStatus.READY_FOR_COLLECTION)),
                ),
            ),
            notificationsEnabled = false, onEnableNotifications = {}, onAdd = {}, onOpen = {}, onSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPreview() {
    ProntoSoggiornoTheme(darkTheme = true) {
        PraticaListScreen(ListUiState.Loaded(emptyList()), true, {}, {}, {}, {})
    }
}
