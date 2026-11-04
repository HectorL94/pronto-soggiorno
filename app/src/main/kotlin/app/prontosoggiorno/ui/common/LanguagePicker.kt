package app.prontosoggiorno.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.prontosoggiorno.R

/** Lista de idiomas con su nombre nativo. `null` = idioma del sistema. */
@Composable
fun LanguageOptions(selected: AppLanguage?, onSelect: (AppLanguage?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.selectableGroup()) {
        RadioRow(stringResource(R.string.settings_language_system), selected == null) { onSelect(null) }
        AppLanguage.entries.forEach { language ->
            RadioRow(language.nativeName, selected == language) { onSelect(language) }
        }
    }
}

@Composable
fun RadioRow(label: String, selected: Boolean, supporting: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
