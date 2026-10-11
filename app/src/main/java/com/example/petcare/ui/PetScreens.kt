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
        Text("Add a pet", style = MaterialTheme.typography.headlineMedium)
        Text("Start with a name. Optional profile details and pictures arrive in a later step.")
        OutlinedTextField(model.petName, { model.petName = it }, label = { Text("Pet name") },
            singleLine = true, enabled = !model.careSaving, modifier = Modifier.fillMaxWidth())
        model.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = model::savePet, enabled = !model.careSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (model.careSaving) "Saving…" else "Save pet")
        }
        OutlinedButton(onClick = model::back, enabled = !model.careSaving) { Text("Cancel") }
    }
}
