package com.example.petcare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotificationNotice(onEnable: () -> Unit, onSettings: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Notifications are disabled", style = MaterialTheme.typography.titleSmall)
            Text("Records still save. Enable notifications for phone reminders.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEnable) { Text("Enable reminders") }
                TextButton(onClick = onSettings) { Text("Phone settings") }
            }
        }
    }
}
