package com.example.petcare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.petcare.data.PetSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun displayDate(iso: String): String = LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@Composable
fun HomeScreen(modifier: Modifier = Modifier, pets: List<PetSummary>?, error: String?,
    onAdd: () -> Unit, onPet: (String) -> Unit, medicationArea: Boolean = false) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (medicationArea) "Your companions" else "A little care, every day.",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(if (medicationArea) "Flea treatment records for each pet." else "Keep the important dates close.",
            style = MaterialTheme.typography.bodyLarge)
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        else if (pets == null) CircularProgressIndicator()
        else if (pets.isEmpty()) CareCard {
            Text("A fresh start", style = MaterialTheme.typography.titleLarge)
            Text("Add your first pet to start keeping treatment records.")
        }
        pets?.forEach { summary ->
            Card(onClick = { onPet(summary.pet.id) }, shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${summary.pet.name} — Flea treatment", style = MaterialTheme.typography.titleLarge)
                    Text(summary.last?.let { "Last given: ${displayDate(it.actualDate)}" } ?: "No treatment recorded")
                    Text(summary.pending?.let { "Next chosen date: ${displayDate(it.dueDate)}" } ?: "No treatment scheduled")
                    if (summary.pending?.dueDate?.let { LocalDate.parse(it) < LocalDate.now() } == true) {
                        Text("Overdue — not recorded as given", color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Text("View full record", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (error == null) Button(onClick = onAdd, enabled = pets != null) { Text("Add pet") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun CareCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}
