package app.prontosoggiorno.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import app.prontosoggiorno.data.PraticaRepository
import app.prontosoggiorno.data.PraticaSummary
import app.prontosoggiorno.data.db.CheckResultKind
import app.prontosoggiorno.data.db.CheckTrigger
import app.prontosoggiorno.data.settings.CheckInterval
import app.prontosoggiorno.ui.common.ConfirmDialog
import app.prontosoggiorno.ui.common.ExternalLinks
import app.prontosoggiorno.ui.common.IndependenceFootnote
import app.prontosoggiorno.ui.common.InfoBanner
import app.prontosoggiorno.ui.common.NumberText
import app.prontosoggiorno.ui.common.SectionCard
import app.prontosoggiorno.ui.common.StatusUi
import app.prontosoggiorno.ui.common.currentLocale
import app.prontosoggiorno.ui.common.failed
import app.prontosoggiorno.ui.common.failureReason
import app.prontosoggiorno.ui.common.rememberDateTimeFormatter
import app.prontosoggiorno.ui.common.toPortalLanguage
import app.prontosoggiorno.ui.theme.ProntoSoggiornoTheme
import app.prontosoggiorno.ui.theme.asPortalText
import kotlinx.coroutines.launch

@Composable
fun PraticaDetailRoute(praticaId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel: PraticaDetailViewModel = viewModel {
        PraticaDetailViewModel(
            praticaId = praticaId,
            repository = container.praticaRepository,
            runner = container.checkRunner,
            settings = container.settingsRepository,
            cancelNotification = container.notifier::cancel,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // Ámbito propio: si el snackbar se lanzara dentro del LaunchedEffect, se cancelaría
    // en cuanto el mensaje se marca como mostrado.
    val snackbarScope = rememberCoroutineScope()
    val locale = currentLocale()

    if (state.deleted || (!state.loading && state.pratica == null)) {
        LaunchedEffect(Unit) { onBack() }
    }

    val noBrowser = stringResource(R.string.error_no_browser)
    LaunchedEffect(state.pendingUrl) {
        val url = state.pendingUrl ?: return@LaunchedEffect
        viewModel.urlOpened()
        if (!ExternalLinks.openPortal(context, url)) snackbarScope.launch { snackbar.showSnackbar(noBrowser) }
    }

    val messageText = state.message?.let {
        when (it) {
            DetailMessage.TOO_SOON -> stringResource(R.string.detail_too_soon)
            DetailMessage.UNCHANGED -> stringResource(R.string.detail_check_unchanged)
            DetailMessage.CHANGED -> stringResource(R.string.detail_check_changed)
            DetailMessage.FAILED -> stringResource(R.string.detail_check_failed)
        }
    }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            viewModel.messageShown()
            snackbarScope.launch { snackbar.showSnackbar(messageText) }
        }
    }

    PraticaDetailScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onCheckNow = { viewModel.checkNow() },
        onTogglePaused = { viewModel.togglePaused() },
        onToggleReveal = { viewModel.toggleReveal() },
        onOpenPortal = { viewModel.openPortal(locale.toPortalLanguage()) },
        onOpenLink = { url -> ExternalLinks.openPortal(context, url) },
        onRename = { viewModel.rename(it) },
        onDelete = { viewModel.delete() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PraticaDetailScreen(
    state: DetailUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onCheckNow: () -> Unit,
    onTogglePaused: () -> Unit,
    onToggleReveal: () -> Unit,
    onOpenPortal: () -> Unit,
    onOpenLink: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val pratica = state.pratica
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(pratica?.alias ?: stringResource(R.string.list_unnamed), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (pratica != null && !pratica.unreadable) {
                        IconButton(onClick = { renaming = true }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.detail_rename))
                        }
                    }
                    if (pratica != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (pratica == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag(TAG_LIST),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (pratica.unreadable) {
                item {
                    InfoBanner(
                        icon = R.drawable.ic_warning,
                        title = stringResource(R.string.detail_unreadable_title),
                        text = stringResource(R.string.detail_unreadable_body),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                return@LazyColumn
            }
            val status = StatusUi.of(pratica.lastKnownState?.status)
            item(key = "status") { StatusHero(status, state.checking) }

            val last = pratica.lastCheck
            if (last != null && last.failed && !state.checking) {
                item(key = "failed") {
                    InfoBanner(
                        icon = R.drawable.ic_cloud_off,
                        text = stringResource(R.string.detail_failed_banner, failureReason(last).orEmpty()),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            if (pratica.notFoundPaused) {
                item(key = "not_found_paused") {
                    InfoBanner(
                        icon = R.drawable.ic_pause,
                        title = stringResource(R.string.detail_not_found_paused_title),
                        text = stringResource(R.string.detail_not_found_paused_body),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            pratica.lastKnownState?.let { known ->
                item(key = "portal") { PortalTextCard(known, onOpenLink) }
            }

            item(key = "info") { InfoCard(pratica, state, onToggleReveal) }

            item(key = "actions") {
                Actions(
                    pratica = pratica,
                    checking = state.checking,
                    onCheckNow = onCheckNow,
                    onTogglePaused = onTogglePaused,
                    onOpenPortal = onOpenPortal,
                )
            }

            item(key = "history") { History(state.history) }
            item(key = "footnote") { IndependenceFootnote() }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.detail_delete_title),
            text = stringResource(R.string.detail_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
    if (renaming && pratica != null) {
        RenameDialog(
            initial = pratica.alias.orEmpty(),
            onConfirm = { renaming = false; onRename(it) },
            onDismiss = { renaming = false },
        )
    }
}

@Composable
private fun StatusHero(status: StatusUi, checking: Boolean) {
    val tone = status.tone
    Surface(color = tone.container, contentColor = tone.content, shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(72.dp).background(tone.dot.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(status.icon), contentDescription = null, modifier = Modifier.size(40.dp))
            }
            Text(
                stringResource(if (status == StatusUi.READY) R.string.detail_ready_title else status.label),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(TAG_STATUS),
            )
            Text(
                stringResource(status.explanation),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (status == StatusUi.READY) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(R.drawable.ic_sms), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.detail_ready_sms), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (checking) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.detail_checking), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PortalTextCard(record: CheckRecord, onOpenLink: (String) -> Unit) {
    SectionCard {
        Text(
            stringResource(R.string.detail_portal_text_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Texto exactamente como lo publica el portal, en italiano: no se traduce ni se interpreta.
        Text(
            "«${record.statusText.orEmpty()}»",
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic).asPortalText(),
        )
        Text(
            stringResource(R.string.detail_portal_text_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        record.office?.let { office ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_place), contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column {
                    Text(
                        stringResource(R.string.detail_office_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(office, style = MaterialTheme.typography.bodyLarge.asPortalText())
                }
            }
        }
        record.linkUrl?.let { url ->
            TextButton(onClick = { onOpenLink(url) }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(painterResource(R.drawable.ic_schedule), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_office_hours))
                Spacer(Modifier.width(4.dp))
                Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(pratica: PraticaSummary, state: DetailUiState, onToggleReveal: () -> Unit) {
    val format = rememberDateTimeFormatter()
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (pratica.type == IdentifierType.PRATICA) R.string.add_detected_pratica
                        else R.string.add_detected_assicurata,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumberText(state.revealedNumber ?: pratica.maskedNumber.orEmpty(), modifier = Modifier.testTag(TAG_NUMBER))
            }
            TextButton(onClick = onToggleReveal) {
                val revealed = state.revealedNumber != null
                Icon(
                    painterResource(if (revealed) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (revealed) R.string.detail_hide_number else R.string.detail_show_number))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        InfoRow(
            icon = R.drawable.ic_refresh,
            label = stringResource(R.string.detail_last_check),
            value = pratica.lastCheck?.let { format(it.checkedAt) } ?: stringResource(R.string.list_never_checked),
        )
        InfoRow(
            icon = R.drawable.ic_schedule,
            label = stringResource(R.string.detail_auto_checks),
            value = when {
                pratica.monitoringCompleted -> stringResource(R.string.detail_auto_done)
                pratica.paused -> stringResource(R.string.detail_auto_paused)
                pratica.notFoundPaused -> stringResource(R.string.detail_auto_not_found)
                else -> androidx.compose.ui.res.pluralStringResource(
                    R.plurals.detail_auto_every, state.interval.hours, state.interval.hours,
                )
            },
        )
    }
}

@Composable
private fun InfoRow(icon: Int, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Actions(
    pratica: PraticaSummary,
    checking: Boolean,
    onCheckNow: () -> Unit,
    onTogglePaused: () -> Unit,
    onOpenPortal: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onCheckNow,
            enabled = !checking,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag(TAG_CHECK_NOW),
        ) {
            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (checking) R.string.detail_checking else R.string.detail_check_now))
        }
        // Misma altura para los dos botones; si un idioma tiene textos largos, ocupan dos líneas.
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!pratica.monitoringCompleted) {
                // Pausada por el usuario o por llevar 30 días sin aparecer: el botón reanuda.
                val stopped = pratica.paused || pratica.notFoundPaused
                FilledTonalButton(onClick = onTogglePaused, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)) {
                    Icon(
                        painterResource(if (stopped) R.drawable.ic_play else R.drawable.ic_pause),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(if (stopped) R.string.detail_resume else R.string.detail_pause),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
            OutlinedButton(onClick = onOpenPortal, modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)) {
                Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.detail_open_portal), textAlign = TextAlign.Center, maxLines = 2)
            }
        }
        Text(
            stringResource(R.string.detail_open_portal_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun History(history: List<CheckRecord>) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    val changes = history.filter { it.changeKind == ChangeKind.FIRST_RESULT || it.changeKind == ChangeKind.CHANGED }
    val visible = if (showAll) history else changes
    SectionCard {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium)
        when {
            history.isEmpty() -> Text(
                stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            visible.isEmpty() -> Text(
                stringResource(R.string.history_no_changes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visible.forEach { HistoryItem(it) }
            }
        }
        if (history.size > changes.size) {
            TextButton(onClick = { showAll = !showAll }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(
                    if (showAll) stringResource(R.string.history_show_changes)
                    else stringResource(R.string.history_show_all, history.size),
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(record: CheckRecord) {
    val format = rememberDateTimeFormatter()
    val status = StatusUi.of(record.status)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .background(if (record.failed) MaterialTheme.colorScheme.error else status.tone.dot, CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val title = when {
                record.failed -> stringResource(R.string.history_failed, failureReason(record).orEmpty())
                record.changeKind == ChangeKind.UNCHANGED -> stringResource(R.string.history_unchanged)
                else -> stringResource(status.label)
            }
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (!record.failed && record.changeKind != ChangeKind.UNCHANGED && record.status == AdministrativeStatus.UNKNOWN) {
                record.statusText?.let {
                    Text("«$it»", style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic).asPortalText())
                }
            }
            val meta = buildList {
                add(format(record.checkedAt))
                if (record.trigger == CheckTrigger.MANUAL) add(stringResource(R.string.history_manual))
                // Detalle técnico, discreto, para quien quiera saber qué pasó.
                if (record.failed) record.failureDetail?.let { add(it) }
            }.joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_rename)) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(PraticaRepository.MAX_ALIAS_LENGTH) },
                singleLine = true,
                label = { Text(stringResource(R.string.add_alias_label)) },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

const val TAG_LIST = "detail_list"
const val TAG_STATUS = "detail_status"
const val TAG_NUMBER = "detail_number"
const val TAG_CHECK_NOW = "detail_check_now"

// --- Vista previa con datos inventados (solo Android Studio) ---

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ReadyPreview() {
    val ready = CheckRecord(
        id = 2, checkedAt = 1_762_000_000_000, trigger = CheckTrigger.SCHEDULED, result = CheckResultKind.CHECKED,
        changeKind = ChangeKind.CHANGED, status = AdministrativeStatus.READY_FOR_COLLECTION,
        statusText = "Il documento di soggiorno è pronto per la consegna.", office = "Questura di Esempio",
        linkLabel = "Orari", linkUrl = "https://questure.poliziadistato.it/", failureDetail = null,
    )
    ProntoSoggiornoTheme {
        PraticaDetailScreen(
            state = DetailUiState(
                loading = false,
                pratica = PraticaSummary(1, "Yo", "00ZZ••••00", IdentifierType.PRATICA, false, true, ready, ready),
                history = listOf(ready),
                interval = CheckInterval.EVERY_12_HOURS,
            ),
            snackbar = remember { SnackbarHostState() },
            onBack = {}, onCheckNow = {}, onTogglePaused = {}, onToggleReveal = {}, onOpenPortal = {},
            onOpenLink = {}, onRename = {}, onDelete = {},
        )
    }
}
