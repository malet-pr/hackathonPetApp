package com.example.petcare.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.petcare.R

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier, notificationNotice: @Composable () -> Unit = {}, onEnter: () -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        notificationNotice()
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.welcome_dog), stringResource(R.string.welcome_art),
                Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(0.9f))
        }
        Text(stringResource(R.string.welcome_caption), style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onEnter, Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.enter_app), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
    }
}
