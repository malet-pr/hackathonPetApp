package com.example.petcare

import android.app.Application
import com.example.petcare.data.*
import com.example.petcare.reminders.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class PetCareApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settingsStore by lazy { SettingsStore(applicationContext) }
    val photos by lazy { PhotoStore(applicationContext) }
    val database by lazy { PetCareDatabase.open(this) }
    val repository by lazy { TreatmentRepository(database) }
    val notifications by lazy { NotificationHelper(this) }
    val scheduler by lazy { AlarmScheduler(this, repository, notifications) }
    suspend fun discardPhotoIfUnreferenced(path: String?) {
        if (path == null) return
        try {
            if (repository.dao.photoReferenceCount(path) != 0) return
            if (settingsStore.welcomeImagePath.first() == path) return
            photos.discard(path)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Uncertain ownership: leave the file rather than remove it. */ }
    }
    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
    }
}
