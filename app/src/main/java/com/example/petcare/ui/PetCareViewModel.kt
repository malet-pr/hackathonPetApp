package com.example.petcare.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.PetCareApp
import com.example.petcare.data.SettingsStore
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { Welcome, Home, Pets }
enum class TitleError { Empty, TooLong, SaveFailed }

class PetCareViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = (application as PetCareApp).settingsStore
    val title = settings.title.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_TITLE)
    var screen by mutableStateOf(Screen.Welcome)
        private set
    var settingsOpen by mutableStateOf(false)
        private set
    var titleDraft by mutableStateOf("")
        private set
    var titleError by mutableStateOf<TitleError?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    fun navigate(destination: Screen) { screen = destination }
    fun showSettings() {
        titleDraft = title.value
        titleError = null
        settingsOpen = true
    }
    fun editTitle(value: String) { titleDraft = value; titleError = null }
    fun dismissSettings() { if (!saving) settingsOpen = false }
    fun saveSettings() {
        if (saving) return
        val value = titleDraft.trim()
        titleError = when {
            value.isEmpty() -> TitleError.Empty
            value.length > SettingsStore.MAX_TITLE_LENGTH -> TitleError.TooLong
            else -> null
        }
        if (titleError != null) return
        saving = true
        viewModelScope.launch {
            try {
                settings.saveTitle(value)
                settingsOpen = false
            } catch (_: IOException) {
                titleError = TitleError.SaveFailed
            } finally {
                saving = false
            }
        }
    }
}
