package com.example.petcare.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.petcare.PetCareApp

@Composable
fun PhotoPreview(path: String?, description: String, modifier: Modifier = Modifier, large: Boolean = false) {
    if (path == null) return
    val photos = (LocalContext.current.applicationContext as PetCareApp).photos
    val loaded by produceState<Pair<Boolean, android.graphics.Bitmap?>>(initialValue = false to null, path, large) {
        value = true to photos.thumbnail(path, if (large) 1200 else 480)
    }
    val bitmap = loaded.second
    if (bitmap == null) {
        Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                if (loaded.first) Text("Photo unavailable") else CircularProgressIndicator()
            }
        }
    } else Image(bitmap!!.asImageBitmap(), description, modifier.clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Crop)
}

@Composable
fun PhotoInput(label: String, path: String?, enabled: Boolean, onSelected: (Uri?) -> Unit,
    onRemove: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { onSelected(it) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        if (path != null) PhotoPreview(path, label, Modifier.fillMaxWidth().height(180.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, enabled = enabled) { Text(if (path == null) "Choose photo" else "Replace photo") }
            if (path != null) TextButton(onClick = onRemove, enabled = enabled) { Text("Remove") }
        }
        Text("Select an existing image from your phone.", style = MaterialTheme.typography.bodySmall)
    }
}
