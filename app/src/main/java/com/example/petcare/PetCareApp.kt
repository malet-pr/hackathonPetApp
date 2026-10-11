package com.example.petcare

import android.app.Application
import com.example.petcare.data.SettingsStore

class PetCareApp : Application() {
    val settingsStore by lazy { SettingsStore(applicationContext) }
}
