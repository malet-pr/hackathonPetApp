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
    private val careModel get() = ViewModelProvider(this)[PetCareViewModel::class.java]
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) routeNotification(intent)
        setContent { PetCareTheme { AppShell() } }
    }
    override fun onResume() {
        super.onResume()
        careModel.onForeground()
        careModel.refreshNotificationsAndAlarms()
    }
    override fun onPause() {
        careModel.onBackground()
        super.onPause()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeNotification(intent)
    }
    private fun routeNotification(intent: Intent) {
        if (intent.data?.scheme != "petcare") return
        val petId = intent.getStringExtra("pet_id") ?: return
        val uri = intent.data ?: return
        val revision = if (intent.hasExtra("revision")) intent.getIntExtra("revision", -1) else null
        val model = careModel
        when (uri.authority) {
            "detail" -> uri.pathSegments.singleOrNull()?.let { model.openNotification(petId, it, revision, intent.action) }
            "demo" -> if (uri.pathSegments.size == 2) model.openDemoNotification(
                uri.pathSegments[0], petId, uri.pathSegments[1], revision, intent.action)
        }
    }
}
