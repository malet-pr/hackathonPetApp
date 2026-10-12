package com.example.petcare.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.petCareSettings: DataStore<Preferences> by preferencesDataStore(name = "pet_care_settings")

class SettingsStore internal constructor(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.petCareSettings)
    val title: Flow<String> = dataStore.data
        .map { it[TITLE] ?: DEFAULT_TITLE }
        .catch { error ->
            if (error is IOException) emit(DEFAULT_TITLE) else throw error
        }
    val welcomeImagePath: Flow<String?> = dataStore.data.map { it[WELCOME_IMAGE] }
        .catch { error -> if (error is IOException) emit(null) else throw error }

    suspend fun saveTitle(title: String) {
        val validated = validateTitle(title)
        dataStore.edit { it[TITLE] = validated }
    }

    suspend fun saveAppearance(title: String, imagePath: String?) {
        val validated = validateTitle(title)
        dataStore.edit {
            it[TITLE] = validated
            if (imagePath == null) it.remove(WELCOME_IMAGE) else it[WELCOME_IMAGE] = imagePath
        }
    }

    companion object {
        const val DEFAULT_TITLE = "Pet Care"
        const val MAX_TITLE_LENGTH = 60
        private val TITLE = stringPreferencesKey("welcome_title")
        private val WELCOME_IMAGE = stringPreferencesKey("welcome_image_path")

        fun validateTitle(title: String): String {
            val trimmed = title.trim()
            require(trimmed.isNotEmpty()) { "title_empty" }
            require(trimmed.length <= MAX_TITLE_LENGTH) { "title_long" }
            return trimmed
        }
    }
}
