package com.example.petcare.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.PetCareApp
import com.example.petcare.data.SettingsStore
import com.example.petcare.data.TreatmentPlan
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { Welcome, Home, Pets, AddPet, FirstTreatment, ConfirmTreatment, NextDate, Detail }
enum class TitleError { Empty, TooLong, SaveFailed }

class PetCareViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PetCareApp
    private val settings = app.settingsStore
    val pets = app.repository.summaries.catch {
        dataError = "Could not load saved pets. Reopen the app to retry."
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val records = app.repository.administrations.catch {
        dataError = "Could not load saved treatment records. Reopen the app to retry."
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var selectedPetId by mutableStateOf<String?>(null)
        private set
    var petName by mutableStateOf("")
    var medication by mutableStateOf("")
    var dose by mutableStateOf("")
    var source by mutableStateOf("")
    var actualDate by mutableStateOf(LocalDate.now())
    var nextDate by mutableStateOf<LocalDate?>(null)
    var formError by mutableStateOf<String?>(null)
        private set
    var dataError by mutableStateOf<String?>(null)
        private set
    var careSaving by mutableStateOf(false)
        private set
    var nextTreatmentPrompt by mutableStateOf(false)
    var newPetPrompt by mutableStateOf(false)
    var notificationsEnabled by mutableStateOf(false)
        private set
    var schedulingFailed by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)
    fun enter() { navigate(if (pets.value?.isEmpty() == true && dataError == null) Screen.AddPet else Screen.Home) }
    fun openPet(id: String) { selectedPetId = id; navigate(Screen.Detail) }
    fun startPet() { petName = ""; navigate(Screen.AddPet) }
    fun startTreatment() {
        medication = ""; dose = ""; source = ""
        actualDate = LocalDate.now(); nextDate = null
        newPetPrompt = false
        navigate(Screen.FirstTreatment)
    }
    fun back() {
        navigate(when (screen) {
            Screen.AddPet -> Screen.Pets
            Screen.FirstTreatment, Screen.ConfirmTreatment, Screen.NextDate -> Screen.Detail
            Screen.Detail -> Screen.Pets
            Screen.Pets -> Screen.Home
            else -> Screen.Welcome
        })
    }
    fun dismissScheduleWarning() { schedulingFailed = false }
    fun refreshNotificationsAndAlarms() {
        notificationsEnabled = app.notifications.enabled()
        viewModelScope.launch {
            try { schedulingFailed = !app.scheduler.reconcile() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { schedulingFailed = true }
        }
    }
    fun savePet() {
        if (careSaving) return
        if (petName.trim().isEmpty()) { formError = "Enter your pet’s name."; return }
        careSaving = true
        viewModelScope.launch {
            try {
                val pet = app.repository.addPet(petName)
                selectedPetId = pet.id
                screen = Screen.Detail
                formError = null
                newPetPrompt = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { formError = "Could not save your pet. Your entries are still here; try again." }
            finally { careSaving = false }
        }
    }
    fun saveFirstTreatment() {
        if (careSaving) return
        formError = when {
            medication.trim().isEmpty() -> "Enter the medication name."
            dose.trim().isEmpty() -> "Enter the dose as given."
            actualDate > LocalDate.now() -> "Administration date cannot be in the future."
            nextDate?.let { it <= actualDate } == true -> "Next treatment must be after the administration date."
            else -> null
        }
        if (formError != null) return
        val petId = selectedPetId ?: return
        val due = nextDate
        careSaving = true
        viewModelScope.launch {
            try {
                app.repository.saveFirstTreatment(petId, actualDate, medication, dose, source, due)
            } catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (error: Exception) {
                formError = if (error is IllegalArgumentException) error.message else "Could not save the treatment. Your entries are still here; try again."
                careSaving = false
                return@launch
            }
            // The DB transaction is committed. Scheduling failure is reported separately.
            notificationsEnabled = app.notifications.enabled()
            try { schedulingFailed = !app.scheduler.reconcile() }
            catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (_: Exception) { schedulingFailed = true }
            notice = if (due == null) "Treatment saved. No next treatment selected; no reminders scheduled."
                else if (!notificationsEnabled) "Treatment saved. Notifications are disabled; check the app for upcoming dates."
                else if (schedulingFailed) "Treatment saved. Reminder scheduling needs a retry."
                else "Treatment saved. Reminder delivery times may vary."
            screen = Screen.Detail
            formError = null
            careSaving = false
        }
    }
    private var confirmingPlanId: String? = null
    private var confirmingRevision: Int? = null
    fun startConfirmation(plan: TreatmentPlan) {
        if (careSaving) return
        confirmingPlanId = plan.id
        confirmingRevision = plan.revision
        medication = ""; dose = ""; source = ""
        actualDate = LocalDate.now()
        navigate(Screen.ConfirmTreatment)
    }
    fun copyLastTreatment() {
        if (careSaving) return
        records.value.firstOrNull { it.petId == selectedPetId }?.let {
            medication = it.medicationName; dose = it.doseText; source = it.purchaseSource.orEmpty()
        }
    }
    fun saveConfirmation() {
        if (careSaving) return
        val petId = selectedPetId ?: return
        val planId = confirmingPlanId ?: return
        val revision = confirmingRevision ?: return
        careSaving = true
        viewModelScope.launch {
            try {
                app.repository.confirmTreatment(petId, planId, revision, actualDate, medication, dose, source)
            } catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (error: Exception) {
                formError = if (error is IllegalArgumentException) error.message else "Could not save the administration. Your entries are still here; try again."
                careSaving = false
                return@launch
            }
            reconcileAfterChange()
            screen = Screen.Detail
            formError = null
            careSaving = false
            notice = if (schedulingFailed) "Administration saved. Reminder cleanup needs a retry."
                else "Administration saved. Remaining reminders for that treatment cancelled."
            nextTreatmentPrompt = true
        }
    }
    fun scheduleAfterConfirmation() {
        nextTreatmentPrompt = false
        // The confirmation transaction has completed, even if the summary Flow has not emitted yet.
        editingPlanId = null; editingRevision = null; nextDate = null
        navigate(Screen.NextDate)
    }
    fun postpone(plan: TreatmentPlan) {
        if (careSaving) return
        careSaving = true
        viewModelScope.launch {
            try {
                app.repository.postpone(plan.id, plan.revision)
            } catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (error: Exception) {
                notice = if (error is IllegalArgumentException) error.message else "Could not postpone the reminder. Try again."
                careSaving = false
                return@launch
            }
            reconcileAfterChange()
            notice = if (schedulingFailed) "Postponement saved. Reminder scheduling needs a retry."
                else if (!notificationsEnabled) "Postponed until 4 PM. Notifications are disabled; check the app."
                else "Postponed until 4 PM today; delivery time may vary."
            careSaving = false
        }
    }
    private suspend fun reconcileAfterChange() {
        notificationsEnabled = app.notifications.enabled()
        try { schedulingFailed = !app.scheduler.reconcile() }
        catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
        catch (_: Exception) { schedulingFailed = true }
    }
    fun openNotification(petId: String, planId: String, revision: Int?, action: String?) {
        if (careSaving) return
        nextTreatmentPrompt = false
        newPetPrompt = false
        openPet(petId)
        viewModelScope.launch {
            val plan = try { app.repository.dao.plan(planId) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { notice = "Could not open the reminder. Reopen the app to retry."; return@launch }
            if (plan == null || plan.petId != petId || plan.status != "PENDING" || plan.revision != revision) {
                if (action != null) notice = "This reminder is no longer current. Review the saved record."
                return@launch
            }
            when (action) {
                "com.example.petcare.RECORD" -> startConfirmation(plan)
                "com.example.petcare.POSTPONE" -> postpone(plan)
            }
        }
    }
    private var editingPlanId: String? = null
    private var editingRevision: Int? = null
    fun startNextDate() {
        val pet = pets.value?.firstOrNull { it.pet.id == selectedPetId } ?: return
        editingPlanId = pet.pending?.id
        editingRevision = pet.pending?.revision
        nextDate = pet.pending?.dueDate?.let(LocalDate::parse)
        navigate(Screen.NextDate)
    }
    fun saveNextDate() {
        if (careSaving) return
        val due = nextDate
        if (due == null) { formError = "Choose the next treatment date."; return }
        val petId = selectedPetId ?: return
        careSaving = true
        viewModelScope.launch {
            try {
                app.repository.saveNextDate(petId, due, editingPlanId, editingRevision)
            } catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (error: Exception) {
                formError = if (error is IllegalArgumentException) error.message else "Could not save the next date. Try again."
                careSaving = false
                return@launch
            }
            notificationsEnabled = app.notifications.enabled()
            try { schedulingFailed = !app.scheduler.reconcile() }
            catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (_: Exception) { schedulingFailed = true }
            notice = if (!notificationsEnabled) "Next date saved. Notifications are disabled; check the app for upcoming dates."
                else if (schedulingFailed) "Next date saved. Reminder scheduling needs a retry."
                else "Next date saved. Reminders updated; delivery times may vary."
            screen = Screen.Detail
            formError = null
            careSaving = false
        }
    }
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

    fun navigate(destination: Screen) { if (!careSaving) { screen = destination; formError = null } }
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
