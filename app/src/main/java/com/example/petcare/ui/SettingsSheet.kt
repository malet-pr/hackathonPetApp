package com.example.petcare.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.ComponentName
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.petcare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(model: PetCareViewModel) {
    val context = LocalContext.current
    val hasDiagnostic = remember(context) {
        try {
            context.packageManager.getReceiverInfo(ComponentName(context.packageName,
                "com.example.petcare.reminders.DiagnosticReceiver"), 0)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }
    }
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
                        TitleError.ImportFailed -> R.string.photo_import_failed
                        null -> R.string.title_hint
                    }))
                })
            PhotoInput("Welcome image", model.welcomeImageDraft, !model.saving && !model.photoImporting,
                model::chooseWelcomeImage, model::clearWelcomeImage)
            if (model.photoImporting) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = model::dismissSettings, enabled = !model.saving && !model.photoImporting,
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = model::saveSettings, enabled = !model.saving && !model.photoImporting,
                    modifier = Modifier.weight(1f).testTag("save_settings"), shape = RoundedCornerShape(14.dp)) {
                    Text(stringResource(if (model.saving) R.string.saving else R.string.save_changes))
                }
            }
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text("Phone notification settings") }
            if (hasDiagnostic) {
                Text("Development check", style = MaterialTheme.typography.titleSmall)
                Text("This schedules a short background alarm without changing any treatment dates. Enable notifications first, then leave the app open in the background. Inexact delivery may take longer than 20 seconds.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    context.sendBroadcast(Intent("com.example.petcare.DEBUG_SCHEDULE").setComponent(
                        ComponentName(context.packageName, "com.example.petcare.reminders.DiagnosticReceiver")))
                    model.dismissSettings()
                    model.notice = "Diagnostic requested. Press Home and watch for a phone notification."
                }) { Text("Test background reminder (20 seconds)") }
            }
            Text(stringResource(R.string.coming_settings), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
