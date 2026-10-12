package com.example.petcare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.petcare.data.Administration
import com.example.petcare.data.PetSummary
import com.example.petcare.reminders.CalendarDates
import java.time.LocalDate
import java.time.Instant
import kotlinx.coroutines.delay
import com.example.petcare.reminders.ReminderRules

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarButton(label: String, date: LocalDate?, enabled: Boolean,
    defaultDate: LocalDate, onDate: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${date?.let { displayDate(it.toString()) } ?: "Choose date"}")
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = CalendarDates.toPicker(date ?: defaultDate))
        DatePickerDialog(onDismissRequest = { open = false }, confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onDate(CalendarDates.fromPicker(it)) }; open = false },
                enabled = state.selectedDateMillis != null) { Text("Choose") }
        }, dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }) {
            DatePicker(state = state, title = { Text(label, Modifier.padding(24.dp)) })
        }
    }
}

@Composable
fun TreatmentForm(model: PetCareViewModel, pet: PetSummary?, modifier: Modifier, notice: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val confirming = model.screen == Screen.ConfirmTreatment
        Text(if (confirming) "Record treatment" else "First flea treatment", style = MaterialTheme.typography.headlineMedium)
        Text(pet?.pet?.name ?: "Loading pet…", style = MaterialTheme.typography.titleLarge)
        Text(if (confirming) "Record the medication actually given. Previous treatment records will stay unchanged."
            else "Record what was actually given. Choose a next date only if another treatment is planned.")
        if (confirming) OutlinedButton(onClick = model::copyLastTreatment, enabled = !model.careSaving && pet?.last != null) {
            Text("Copy from last treatment")
        }
        notice()
        CalendarButton("Actual administration", model.actualDate, !model.careSaving,
            defaultDate = model.nowDate()) { model.actualDate = it }
        OutlinedTextField(model.medication, { model.medication = it }, label = { Text("Medication name") },
            enabled = !model.careSaving, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model.dose, { model.dose = it }, label = { Text("Dose as given") },
            enabled = !model.careSaving, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model.source, { model.source = it }, label = { Text("Where obtained (optional)") },
            enabled = !model.careSaving, singleLine = true, modifier = Modifier.fillMaxWidth())
        PhotoInput("Treatment photo (optional)", model.treatmentPhoto, !model.careSaving && !model.photoImporting,
            model::chooseTreatmentPhoto, model::clearTreatmentPhoto)
        if (model.photoImporting) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!confirming) {
        CalendarButton("Next chosen treatment (optional)", model.nextDate, !model.careSaving,
            defaultDate = model.nowDate()) { model.nextDate = it }
        if (model.nextDate != null) TextButton(onClick = { model.nextDate = null }, enabled = !model.careSaving) {
            Text("Clear next date")
        }
        if (model.nextDate == null) Text("No next treatment selected; no reminders will be scheduled.")
        Text("Reminders: buy 4 days before at noon; treatment at 8 AM; one overdue follow-up the next day at noon. Times may vary.",
            style = MaterialTheme.typography.bodySmall)
        }
        model.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { if (confirming) model.saveConfirmation() else model.saveFirstTreatment() },
            enabled = !model.careSaving && !model.photoImporting && pet != null,
            modifier = Modifier.fillMaxWidth()) { Text(if (model.careSaving) "Saving…" else "Save treatment") }
        OutlinedButton(onClick = model::back, enabled = !model.careSaving && !model.photoImporting) { Text("Cancel") }
    }
}

@Composable
fun TreatmentDetail(model: PetCareViewModel, pet: PetSummary?, history: List<Administration>, modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(pet?.pet?.name ?: "Loading pet…", style = MaterialTheme.typography.headlineMedium)
        Text("Flea treatment", style = MaterialTheme.typography.titleLarge)
        if (pet == null) CircularProgressIndicator()
        else {
            pet.pet.photoPath?.let { PhotoPreview(it, "Photo of ${pet.pet.name}", Modifier.fillMaxWidth().height(180.dp)) }
            val extras = listOfNotNull(pet.pet.birthDate?.let { "Born: ${displayDate(it)}" },
                pet.pet.owner?.let { "Owner: $it" }, pet.pet.breed?.let { "Breed: $it" })
            extras.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            OutlinedButton(onClick = model::startEditPet, enabled = !model.careSaving) { Text("Edit pet details") }
            CareCard {
                Text("Next chosen date", style = MaterialTheme.typography.titleMedium)
                Text(pet.pending?.let { displayDate(it.dueDate) } ?: "No treatment scheduled")
                if (pet.pending?.dueDate?.let { LocalDate.parse(it) < model.nowDate() } == true)
                    Text("Overdue — not recorded as given", color = MaterialTheme.colorScheme.error)
                pet.pending?.let { plan ->
                    var realNow by remember(plan.id) { mutableStateOf(Instant.now()) }
                    LaunchedEffect(plan.id, model.demoMode) {
                        while (!model.demoMode) { realNow = Instant.now(); delay(30_000) }
                    }
                    Button(onClick = { model.startConfirmation(plan) }, enabled = !model.careSaving) { Text("Record treatment") }
                    if (ReminderRules.canPostpone(plan, model.demoNow ?: realNow, model.nowZone())) {
                        OutlinedButton(onClick = { model.postpone(plan) }, enabled = !model.careSaving) { Text("Postpone until 4 PM") }
                    }
                    if (plan.postponedOnce) Text("Reminder postponed once until 4 PM on the chosen date.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (history.isEmpty()) {
                Text("No treatment recorded")
                Button(onClick = model::startTreatment) { Text("Add first treatment") }
            }
            if (history.isNotEmpty()) Button(onClick = model::startNextDate, enabled = !model.careSaving) {
                Text(if (pet.pending == null) "Schedule next treatment" else "Edit next date")
            }
            history.forEachIndexed { index, record ->
                var expanded by remember(record.id) { mutableStateOf(index == 0) }
                Card(onClick = { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (index == 0) "Last administered treatment" else "Previous treatment", style = MaterialTheme.typography.titleMedium)
                        Text("${displayDate(record.actualDate)} — ${record.medicationName}")
                        if (expanded) {
                            Text("Dose: ${record.doseText}")
                            Text("Where obtained: ${record.purchaseSource ?: "Not recorded"}")
                            Text("Administered on: ${displayDate(record.actualDate)}")
                            record.photoPath?.let {
                                var photoOpen by remember(record.id) { mutableStateOf(false) }
                                PhotoPreview(it, "Photo for ${record.medicationName}", Modifier.fillMaxWidth().height(180.dp))
                                TextButton(onClick = { photoOpen = true }) { Text("View larger photo") }
                                if (photoOpen) AlertDialog(onDismissRequest = { photoOpen = false },
                                    title = { Text("Treatment photo") },
                                    text = { PhotoPreview(it, "Photo for ${record.medicationName}", Modifier.fillMaxWidth().height(340.dp), large = true) },
                                    confirmButton = { TextButton(onClick = { photoOpen = false }) { Text("Close") } })
                            }
                        }
                        Text(if (expanded) "Hide details" else "Show details", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        TextButton(onClick = model::back) { Text("Back to pets") }
    }
}

@Composable
fun NextDateForm(model: PetCareViewModel, pet: PetSummary?, modifier: Modifier, notice: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (pet?.pending == null) "Schedule next treatment" else "Edit next date",
            style = MaterialTheme.typography.headlineMedium)
        Text(pet?.pet?.name ?: "Loading pet…", style = MaterialTheme.typography.titleLarge)
        Text("Changing the next date updates reminders. Saved administration details stay unchanged.")
        notice()
        CalendarButton("Next chosen treatment", model.nextDate, !model.careSaving,
            defaultDate = model.nowDate()) { model.nextDate = it }
        Text("Purchase: 4 days before at noon. Treatment: 8 AM. Overdue: next day at noon. Elapsed alerts are skipped; delivery times may vary.",
            style = MaterialTheme.typography.bodySmall)
        model.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = model::saveNextDate, enabled = !model.careSaving && pet != null) {
            Text(if (model.careSaving) "Saving…" else "Save next date")
        }
        OutlinedButton(onClick = model::back, enabled = !model.careSaving) { Text("Cancel") }
    }
}
