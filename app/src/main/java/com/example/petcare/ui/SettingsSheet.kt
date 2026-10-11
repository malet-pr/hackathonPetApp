package com.example.petcare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.petcare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(model: PetCareViewModel) {
    ModalBottomSheet(onDismissRequest = model::dismissSettings,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .imePadding().padding(horizontal = 28.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.settings_caption), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = model.titleDraft, onValueChange = model::editTitle,
                label = { Text(stringResource(R.string.welcome_title_label)) },
                modifier = Modifier.fillMaxWidth().testTag("welcome_title_input"),
                shape = RoundedCornerShape(16.dp), enabled = !model.saving,
                singleLine = true, isError = model.titleError != null,
                supportingText = {
                    Text(stringResource(when (model.titleError) {
                        TitleError.Empty -> R.string.title_empty
                        TitleError.TooLong -> R.string.title_long
                        TitleError.SaveFailed -> R.string.save_failed
                        null -> R.string.title_hint
                    }))
                })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = model::dismissSettings, enabled = !model.saving,
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = model::saveSettings, enabled = !model.saving,
                    modifier = Modifier.weight(1f).testTag("save_settings"), shape = RoundedCornerShape(14.dp)) {
                    Text(stringResource(if (model.saving) R.string.saving else R.string.save_changes))
                }
            }
            Text(stringResource(R.string.coming_settings), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
