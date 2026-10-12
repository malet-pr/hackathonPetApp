package com.example.petcare

import android.app.Application
import com.example.petcare.data.*
import com.example.petcare.reminders.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

class PetCareApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settingsStore by lazy { SettingsStore(applicationContext) }
    val photos by lazy { PhotoStore(applicationContext) }
    val database by lazy { PetCareDatabase.open(this) }
    val repository by lazy { TreatmentRepository(database) }
    val notifications by lazy { NotificationHelper(this) }
    val scheduler by lazy { AlarmScheduler(this, repository, notifications) }
    suspend fun discardPhotoIfUnreferenced(path: String?, owner: TreatmentRepository = repository) {
        if (path == null) return
        try {
            if (owner.dao.photoReferenceCount(path) != 0) return
            if (!photos.isDemoPath(path) && settingsStore.welcomeImagePath.first() == path) return
            photos.discard(path)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Uncertain ownership: leave the file rather than remove it. */ }
    }
    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
        notifications.cancelAllDemo()
        File(filesDir, "demo-photos").deleteRecursively()
    }
}
