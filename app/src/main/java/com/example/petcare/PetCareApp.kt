package com.example.petcare

import android.app.Application
import com.example.petcare.data.*
import com.example.petcare.reminders.*
import kotlinx.coroutines.*

class PetCareApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settingsStore by lazy { SettingsStore(applicationContext) }
    val database by lazy { PetCareDatabase.open(this) }
    val repository by lazy { TreatmentRepository(database) }
    val notifications by lazy { NotificationHelper(this) }
    val scheduler by lazy { AlarmScheduler(this, repository, notifications) }
    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
    }
}
