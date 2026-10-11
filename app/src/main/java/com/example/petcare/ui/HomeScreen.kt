package com.example.petcare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.petcare.R

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onMedication: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.home_heading), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(28.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(painterResource(R.drawable.ic_paw), null, Modifier.padding(16.dp).size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.no_reminders_title), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.no_reminders_body), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                FilledTonalButton(onClick = onMedication, shape = RoundedCornerShape(14.dp)) {
                    Text(stringResource(R.string.view_medication))
                }
            }
        }
    }
}

@Composable
fun PetsScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.pets_heading), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(36.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(painterResource(R.drawable.ic_paw), null, Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.pets_empty_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.pets_empty_body), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                SuggestionChip(onClick = {}, enabled = false,
                    label = { Text(stringResource(R.string.next_step)) })
            }
        }
    }
}
