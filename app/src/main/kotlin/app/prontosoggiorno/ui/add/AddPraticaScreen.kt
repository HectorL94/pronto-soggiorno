package app.prontosoggiorno.ui.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.prontosoggiorno.R
import app.prontosoggiorno.appContainer
import app.prontosoggiorno.core.model.IdentifierType
import app.prontosoggiorno.ui.common.InfoBanner
import app.prontosoggiorno.ui.theme.NumberTextStyle
import app.prontosoggiorno.ui.theme.ProntoSoggiornoTheme

@Composable
fun AddPraticaRoute(onBack: () -> Unit, onAdded: (Long) -> Unit) {
    val container = LocalContext.current.appContainer
    val viewModel: AddPraticaViewModel = viewModel { AddPraticaViewModel(container.praticaRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.addedId) { state.addedId?.let(onAdded) }
    AddPraticaScreen(
        state = state,
        onNumberChange = viewModel::onNumberChange,
        onAliasChange = viewModel::onAliasChange,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPraticaScreen(
    state: AddUiState,
    onNumberChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.add_intro), style = MaterialTheme.typography.bodyLarge)

            OutlinedTextField(
                value = state.number,
                onValueChange = onNumberChange,
                modifier = Modifier.fillMaxWidth().testTag(TAG_NUMBER),
                label = { Text(stringResource(R.string.add_number_label)) },
                textStyle = NumberTextStyle,
                singleLine = true,
                isError = state.error != null,
                supportingText = { Text(supportingText(state)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = state.alias,
                onValueChange = onAliasChange,
                modifier = Modifier.fillMaxWidth().testTag(TAG_ALIAS),
                label = { Text(stringResource(R.string.add_alias_label)) },
                singleLine = true,
                supportingText = { Text(stringResource(R.string.add_alias_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
            )

            WhereToFind()

            InfoBanner(icon = R.drawable.ic_lock, text = stringResource(R.string.add_privacy_note))
            Text(
                stringResource(R.string.add_consent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = onSave,
                enabled = !state.saving && state.number.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag(TAG_SAVE),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.add_save))
                }
            }
        }
    }
}

@Composable
private fun supportingText(state: AddUiState): String = when (state.error) {
    AddError.EMPTY -> stringResource(R.string.add_error_empty)
    AddError.WRONG_LENGTH -> stringResource(R.string.add_error_length)
    AddError.WRONG_FORMAT -> stringResource(R.string.add_error_format)
    AddError.DUPLICATE -> stringResource(R.string.add_error_duplicate)
    null -> when (state.detectedType) {
        IdentifierType.PRATICA -> stringResource(R.string.add_detected_pratica)
        IdentifierType.ASSICURATA -> stringResource(R.string.add_detected_assicurata)
        null -> stringResource(R.string.add_number_hint)
    }
}

@Composable
private fun WhereToFind() {
    var open by rememberSaveable { mutableStateOf(false) }
    Column {
        TextButton(onClick = { open = !open }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_help), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.add_where_title))
                Spacer(Modifier.size(4.dp))
                Icon(
                    painterResource(if (open) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        AnimatedVisibility(open) {
            Text(
                stringResource(R.string.add_where_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

const val TAG_NUMBER = "add_number"
const val TAG_ALIAS = "add_alias"
const val TAG_SAVE = "add_save"

@Preview(showBackground = true)
@Composable
private fun AddPreview() {
    ProntoSoggiornoTheme {
        AddPraticaScreen(AddUiState(number = "00ZZ000000", detectedType = IdentifierType.PRATICA), {}, {}, {}, {})
    }
}
