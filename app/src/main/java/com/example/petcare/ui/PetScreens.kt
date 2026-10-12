package com.example.petcare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PetForm(model: PetCareViewModel, modifier: Modifier) {
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(if (model.screen == Screen.EditPet) "Edit pet" else "Add a pet", style = MaterialTheme.typography.headlineMedium)
        Text("Only the name is required. Add or change the other details whenever you like.")
        OutlinedTextField(model.petName, { model.petName = it }, label = { Text("Pet name") },
            singleLine = true, enabled = !model.careSaving && !model.photoImporting, modifier = Modifier.fillMaxWidth())
        CalendarButton("Birth date (optional)", model.petBirthDate, !model.careSaving && !model.photoImporting,
            defaultDate = model.nowDate()) {
            model.petBirthDate = it
        }
        if (model.petBirthDate != null) TextButton(onClick = { model.petBirthDate = null },
            enabled = !model.careSaving && !model.photoImporting) { Text("Clear birth date") }
        OutlinedTextField(model.petOwner, { model.petOwner = it }, label = { Text("Owner (optional)") },
            singleLine = true, enabled = !model.careSaving && !model.photoImporting, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model.petBreed, { model.petBreed = it }, label = { Text("Breed (optional)") },
            singleLine = true, enabled = !model.careSaving && !model.photoImporting, modifier = Modifier.fillMaxWidth())
        PhotoInput("Pet photo (optional)", model.petPhoto, !model.careSaving && !model.photoImporting,
            model::choosePetPhoto, model::clearPetPhoto)
        if (model.photoImporting) LinearProgressIndicator(Modifier.fillMaxWidth())
        model.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = model::savePet, enabled = !model.careSaving && !model.photoImporting, modifier = Modifier.fillMaxWidth()) {
            Text(if (model.careSaving) "Saving…" else "Save pet")
        }
        OutlinedButton(onClick = model::back, enabled = !model.careSaving && !model.photoImporting) { Text("Cancel") }
    }
}
