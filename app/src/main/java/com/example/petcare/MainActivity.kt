package com.example.petcare

import android.os.Bundle
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.example.petcare.ui.PetCareViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.petcare.ui.AppShell
import com.example.petcare.ui.theme.PetCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        routeNotification(intent)
        setContent { PetCareTheme { AppShell() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeNotification(intent)
    }
    private fun routeNotification(intent: Intent) {
        if (intent.data?.scheme != "petcare" || intent.data?.authority != "detail") return
        intent.getStringExtra("pet_id")?.let { ViewModelProvider(this)[PetCareViewModel::class.java].openPet(it) }
    }
}
