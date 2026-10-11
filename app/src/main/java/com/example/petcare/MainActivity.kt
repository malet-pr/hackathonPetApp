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
        if (savedInstanceState == null) routeNotification(intent)
        setContent { PetCareTheme { AppShell() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeNotification(intent)
    }
    private fun routeNotification(intent: Intent) {
        if (intent.data?.scheme != "petcare" || intent.data?.authority != "detail") return
        val petId = intent.getStringExtra("pet_id") ?: return
        val planId = intent.data?.pathSegments?.singleOrNull() ?: return
        ViewModelProvider(this)[PetCareViewModel::class.java].openNotification(petId, planId,
            if (intent.hasExtra("revision")) intent.getIntExtra("revision", -1) else null, intent.action)
    }
}
