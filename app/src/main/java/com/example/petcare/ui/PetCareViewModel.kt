package com.example.petcare.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.petcare.PetCareApp
import com.example.petcare.data.SettingsStore
import com.example.petcare.data.Administration
import com.example.petcare.data.PetSummary
import com.example.petcare.data.TreatmentPlan
import com.example.petcare.data.TreatmentRepository
import com.example.petcare.demo.DemoSession
import com.example.petcare.reminders.NotificationHelper
import java.time.LocalDate
import java.time.Instant
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ExperimentalCoroutinesApi

enum class Screen { Welcome, Home, Pets, AddPet, EditPet, FirstTreatment, ConfirmTreatment, NextDate, Detail }
enum class TitleError { Empty, TooLong, SaveFailed, ImportFailed }

@OptIn(ExperimentalCoroutinesApi::class)
class PetCareViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PetCareApp
    private val settings = app.settingsStore
    private val repositoryFlow = MutableStateFlow(app.repository)
    val activeRepository: TreatmentRepository get() = repositoryFlow.value
    private var demoSession: DemoSession? = null
    private var demoTick: Job? = null
    private var appVisible = false
    var demoMode by mutableStateOf(false)
        private set
    var demoBusy by mutableStateOf(false)
        private set
    var demoNow by mutableStateOf<Instant?>(null)
        private set
    fun nowDate(): LocalDate = LocalDate.now(demoSession?.clock ?: Clock.systemDefaultZone())
    fun nowZone(): ZoneId = demoSession?.clock?.zone ?: ZoneId.systemDefault()
    val pets = repositoryFlow.flatMapLatest { owner -> owner.summaries.map { owner to it } }.catch {
        dataError = "Could not load saved pets. Reopen the app to retry."
        emit(activeRepository to emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val records = repositoryFlow.flatMapLatest { owner -> owner.administrations.map { owner to it } }.catch {
        dataError = "Could not load saved treatment records. Reopen the app to retry."
        emit(activeRepository to emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), app.repository to emptyList())
    private fun visiblePets(): List<PetSummary>? = pets.value?.takeIf { it.first === activeRepository }?.second
    private fun visibleRecords(): List<Administration> = records.value.takeIf { it.first === activeRepository }?.second.orEmpty()
    var selectedPetId by mutableStateOf<String?>(null)
        private set
    var petName by mutableStateOf("")
    var petBirthDate by mutableStateOf<LocalDate?>(null)
    var petOwner by mutableStateOf("")
    var petBreed by mutableStateOf("")
    var petPhoto by mutableStateOf<String?>(null)
        private set
    private var originalPetPhoto: String? = null
    var treatmentPhoto by mutableStateOf<String?>(null)
        private set
    var photoImporting by mutableStateOf(false)
        private set
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
    fun enter() { navigate(if (visiblePets()?.isEmpty() == true && dataError == null) Screen.AddPet else Screen.Home) }
    fun openPet(id: String) { selectedPetId = id; navigate(Screen.Detail) }
    fun startPet() {
        petName = ""; petBirthDate = null; petOwner = ""; petBreed = ""
        petPhoto = null; originalPetPhoto = null
        navigate(Screen.AddPet)
    }
    fun startEditPet() {
        val pet = visiblePets()?.firstOrNull { it.pet.id == selectedPetId }?.pet ?: return
        petName = pet.name; petBirthDate = pet.birthDate?.let(LocalDate::parse)
        petOwner = pet.owner.orEmpty(); petBreed = pet.breed.orEmpty()
        petPhoto = pet.photoPath; originalPetPhoto = pet.photoPath
        navigate(Screen.EditPet)
    }
    fun startTreatment() {
        medication = ""; dose = ""; source = ""
        treatmentPhoto = null
        actualDate = nowDate(); nextDate = null
        newPetPrompt = false
        navigate(Screen.FirstTreatment)
    }
    fun back() {
        navigate(when (screen) {
            Screen.AddPet -> Screen.Pets
            Screen.EditPet -> Screen.Detail
            Screen.FirstTreatment, Screen.ConfirmTreatment, Screen.NextDate -> Screen.Detail
            Screen.Detail -> Screen.Pets
            Screen.Pets -> Screen.Home
            else -> Screen.Welcome
        })
    }
    fun dismissScheduleWarning() { schedulingFailed = false }
    fun refreshNotificationsAndAlarms() {
        notificationsEnabled = app.notifications.enabled(if (demoMode) NotificationHelper.DEMO_CHANNEL else NotificationHelper.REAL_CHANNEL)
        viewModelScope.launch {
            try { schedulingFailed = !app.scheduler.reconcile() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { schedulingFailed = true }
        }
    }
    fun savePet() {
        if (careSaving || photoImporting) return
        if (petName.trim().isEmpty()) { formError = "Enter your pet’s name."; return }
        if (petBirthDate?.let { it > nowDate() } == true) {
            formError = "Birth date cannot be in the future."; return
        }
        careSaving = true
        viewModelScope.launch {
            try {
                val old = originalPetPhoto
                val editing = screen == Screen.EditPet
                val pet = if (editing) {
                    activeRepository.updatePet(requireNotNull(selectedPetId), petName, petBirthDate,
                        petOwner, petBreed, petPhoto)
                } else activeRepository.addPet(petName, petBirthDate, petOwner, petBreed, petPhoto)
                if (old != null && old != pet.photoPath) app.discardPhotoIfUnreferenced(old, activeRepository)
                selectedPetId = pet.id
                screen = Screen.Detail
                formError = null
                newPetPrompt = !editing
                petPhoto = null; originalPetPhoto = null
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
            actualDate > nowDate() -> "Administration date cannot be in the future."
            nextDate?.let { it <= actualDate } == true -> "Next treatment must be after the administration date."
            else -> null
        }
        if (formError != null) return
        val petId = selectedPetId ?: return
        val due = nextDate
        careSaving = true
        viewModelScope.launch {
            try {
                activeRepository.saveFirstTreatment(petId, actualDate, medication, dose, source, due, treatmentPhoto)
            } catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (error: Exception) {
                formError = if (error is IllegalArgumentException) error.message else "Could not save the treatment. Your entries are still here; try again."
                careSaving = false
                return@launch
            }
            // The DB transaction is committed. Scheduling failure is reported separately.
            notificationsEnabled = app.notifications.enabled(if (demoMode) NotificationHelper.DEMO_CHANNEL else NotificationHelper.REAL_CHANNEL)
            try { schedulingFailed = if (demoMode) false else !app.scheduler.reconcile() }
            catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (_: Exception) { schedulingFailed = true }
            notice = if (due == null) "Treatment saved. No next treatment selected; no reminders scheduled."
                else if (!notificationsEnabled) "Treatment saved. Notifications are disabled; check the app for upcoming dates."
                else if (schedulingFailed) "Treatment saved. Reminder scheduling needs a retry."
                else "Treatment saved. Reminder delivery times may vary."
            screen = Screen.Detail
            treatmentPhoto = null
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
        treatmentPhoto = null
        actualDate = nowDate()
        navigate(Screen.ConfirmTreatment)
    }
    fun copyLastTreatment() {
        if (careSaving) return
        visibleRecords().firstOrNull { it.petId == selectedPetId }?.let {
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
                activeRepository.confirmTreatment(petId, planId, revision, actualDate, medication, dose, source, treatmentPhoto)
            } catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (error: Exception) {
                formError = if (error is IllegalArgumentException) error.message else "Could not save the administration. Your entries are still here; try again."
                careSaving = false
                return@launch
            }
            reconcileAfterChange()
            demoSession?.let { app.notifications.cancelDemoPlan(it.id, planId) }
            screen = Screen.Detail
            treatmentPhoto = null
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
                activeRepository.postpone(plan.id, plan.revision)
            } catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (error: Exception) {
                notice = if (error is IllegalArgumentException) error.message else "Could not postpone the reminder. Try again."
                careSaving = false
                return@launch
            }
            reconcileAfterChange()
            demoSession?.let { app.notifications.cancelDemoPlan(it.id, plan.id) }
            notice = if (schedulingFailed) "Postponement saved. Reminder scheduling needs a retry."
                else if (!notificationsEnabled) "Postponed until 4 PM. Notifications are disabled; check the app."
                else "Postponed until 4 PM today; delivery time may vary."
            careSaving = false
        }
    }
    private suspend fun reconcileAfterChange() {
        notificationsEnabled = app.notifications.enabled(if (demoMode) NotificationHelper.DEMO_CHANNEL else NotificationHelper.REAL_CHANNEL)
        try { schedulingFailed = if (demoMode) false else !app.scheduler.reconcile() }
        catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
        catch (_: Exception) { schedulingFailed = true }
    }
    fun openNotification(petId: String, planId: String, revision: Int?, action: String?) {
        if (careSaving) return
        viewModelScope.launch {
            if (demoMode) endDemo()
            nextTreatmentPrompt = false
            newPetPrompt = false
            openPet(petId)
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
    fun openDemoNotification(sessionId: String, petId: String, planId: String, revision: Int?, action: String?) {
        val session = demoSession
        if (session == null || session.id != sessionId) {
            notice = "That Demo session has ended. Start a fresh one in Settings."
            return
        }
        if (careSaving) return
        nextTreatmentPrompt = false; newPetPrompt = false
        openPet(petId)
        viewModelScope.launch {
            val plan = session.repository.dao.plan(planId)
            if (demoSession !== session || plan == null || plan.petId != petId ||
                plan.status != "PENDING" || plan.revision != revision) {
                notice = "This Demo reminder is no longer current."
                return@launch
            }
            when (action) {
                "com.example.petcare.RECORD" -> startConfirmation(plan)
                "com.example.petcare.POSTPONE" -> postpone(plan)
            }
        }
    }

    fun onForeground() {
        appVisible = true
        demoSession?.clock?.resume()
        startDemoTicker()
    }
    fun onBackground() {
        appVisible = false
        demoSession?.clock?.pause()
        demoTick?.cancel(); demoTick = null
    }
    fun startDemo() {
        if (demoBusy || careSaving || photoImporting) return
        dismissSettings()
        navigate(Screen.Home)
        demoBusy = true
        viewModelScope.launch {
            try {
                endDemo()
                val session = DemoSession.create(app)
                demoSession = session
                repositoryFlow.value = session.repository
                demoMode = true
                demoNow = session.clock.instant()
                selectedPetId = null; nextTreatmentPrompt = false; newPetPrompt = false
                screen = Screen.Home; dataError = null; formError = null
                notificationsEnabled = app.notifications.enabled(NotificationHelper.DEMO_CHANNEL)
                if (appVisible) { session.clock.resume(); startDemoTicker() }
                notice = "Demo started with fresh sample records. One day passes per minute while the app is visible."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { notice = "Could not start Demo. Your real records are unchanged." }
            finally { demoBusy = false }
        }
    }
    fun exitDemo() {
        if (demoBusy || careSaving || photoImporting) return
        dismissSettings()
        navigate(Screen.Home)
        demoBusy = true
        viewModelScope.launch {
            try {
                endDemo()
                notificationsEnabled = app.notifications.enabled()
                notice = "Demo ended. Your real records and reminders are unchanged."
            } finally { demoBusy = false }
        }
    }
    private suspend fun endDemo() {
        val session = demoSession ?: return
        demoTick?.cancel(); demoTick = null
        session.clock.pause()
        demoSession = null
        demoMode = false
        demoNow = null
        repositoryFlow.value = app.repository
        app.notifications.cancelAllDemo()
        selectedPetId = null; nextTreatmentPrompt = false; newPetPrompt = false
        screen = Screen.Home; dataError = null; formError = null
        delay(50) // Let the visible Room Flow detach before closing its in-memory database.
        session.database.close()
        app.photos.clearDemo()
    }
    private fun startDemoTicker() {
        val session = demoSession ?: return
        if (!appVisible) return
        demoTick?.cancel()
        demoTick = viewModelScope.launch {
            while (isActive && appVisible && demoSession === session) {
                val now = session.clock.instant()
                demoNow = now
                try { session.events.collect(now).forEach {
                    app.notifications.postDemo(session.id, it.pet, it.plan, it.kind, session.clock)
                } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { notice = "A Demo reminder could not be shown. Reset Demo to try again." }
                delay(500)
            }
        }
    }
    override fun onCleared() {
        demoTick?.cancel()
        demoSession?.clock?.pause()
        demoSession?.database?.close()
        app.notifications.cancelAllDemo()
        app.applicationScope.launch { app.photos.clearDemo() }
        super.onCleared()
    }
    private var editingPlanId: String? = null
    private var editingRevision: Int? = null
    fun startNextDate() {
        val pet = visiblePets()?.firstOrNull { it.pet.id == selectedPetId } ?: return
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
                activeRepository.saveNextDate(petId, due, editingPlanId, editingRevision)
            } catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (error: Exception) {
                formError = if (error is IllegalArgumentException) error.message else "Could not save the next date. Try again."
                careSaving = false
                return@launch
            }
            notificationsEnabled = app.notifications.enabled(if (demoMode) NotificationHelper.DEMO_CHANNEL else NotificationHelper.REAL_CHANNEL)
            try { schedulingFailed = if (demoMode) false else !app.scheduler.reconcile() }
            catch (cancelled: CancellationException) { careSaving = false; throw cancelled }
            catch (_: Exception) { schedulingFailed = true }
            if (demoMode) editingPlanId?.let { id -> demoSession?.let { app.notifications.cancelDemoPlan(it.id, id) } }
            notice = if (!notificationsEnabled) "Next date saved. Notifications are disabled; check the app for upcoming dates."
                else if (schedulingFailed) "Next date saved. Reminder scheduling needs a retry."
                else "Next date saved. Reminders updated; delivery times may vary."
            screen = Screen.Detail
            formError = null
            careSaving = false
        }
    }
    val title = settings.title.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_TITLE)
    val welcomeImage = settings.welcomeImagePath.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    var screen by mutableStateOf(Screen.Welcome)
        private set
    var settingsOpen by mutableStateOf(false)
        private set
    var titleDraft by mutableStateOf("")
        private set
    var welcomeImageDraft by mutableStateOf<String?>(null)
        private set
    private var originalWelcomeImage: String? = null
    private var welcomeImageEdited = false
    var titleError by mutableStateOf<TitleError?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    fun navigate(destination: Screen) {
        if (careSaving || photoImporting) return
        if (screen != destination) {
            if (screen == Screen.AddPet || screen == Screen.EditPet) discardStaged(petPhoto, originalPetPhoto)
            if (screen == Screen.FirstTreatment || screen == Screen.ConfirmTreatment) discardStaged(treatmentPhoto, null)
        }
        screen = destination; formError = null
    }
    private fun discardStaged(path: String?, saved: String?) {
        if (path != null && path != saved) {
            val owner = activeRepository
            viewModelScope.launch { app.discardPhotoIfUnreferenced(path, owner) }
        }
    }
    fun choosePetPhoto(uri: Uri?) {
        if (uri == null || photoImporting) return
        photoImporting = true
        viewModelScope.launch {
            try {
                val path = app.photos.import(uri, demoMode)
                discardStaged(petPhoto, originalPetPhoto)
                petPhoto = path; formError = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                formError = (error as? IOException)?.message ?: "Could not import this photo. Your previous photo is unchanged."
            } finally { photoImporting = false }
        }
    }
    fun clearPetPhoto() { if (photoImporting) return; discardStaged(petPhoto, originalPetPhoto); petPhoto = null }
    fun chooseTreatmentPhoto(uri: Uri?) {
        if (uri == null || photoImporting) return
        photoImporting = true
        viewModelScope.launch {
            try {
                val path = app.photos.import(uri, demoMode)
                discardStaged(treatmentPhoto, null)
                treatmentPhoto = path; formError = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                formError = (error as? IOException)?.message ?: "Could not import this photo. Try another one."
            } finally { photoImporting = false }
        }
    }
    fun clearTreatmentPhoto() { if (photoImporting) return; discardStaged(treatmentPhoto, null); treatmentPhoto = null }
    fun showSettings() {
        titleDraft = title.value
        originalWelcomeImage = welcomeImage.value
        welcomeImageDraft = originalWelcomeImage
        welcomeImageEdited = false
        titleError = null
        settingsOpen = true
    }
    fun refreshSettingsImage(path: String?) {
        if (settingsOpen && !welcomeImageEdited && !saving) {
            originalWelcomeImage = path
            welcomeImageDraft = path
        }
    }
    fun editTitle(value: String) { titleDraft = value; titleError = null }
    fun chooseWelcomeImage(uri: Uri?) {
        if (uri == null || photoImporting) return
        photoImporting = true
        viewModelScope.launch {
            try {
                val path = app.photos.import(uri)
                discardStaged(welcomeImageDraft, originalWelcomeImage)
                welcomeImageDraft = path; titleError = null
                welcomeImageEdited = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { titleError = TitleError.ImportFailed }
            finally { photoImporting = false }
        }
    }
    fun clearWelcomeImage() {
        if (photoImporting) return
        discardStaged(welcomeImageDraft, originalWelcomeImage)
        welcomeImageDraft = null; welcomeImageEdited = true
    }
    fun dismissSettings() {
        if (saving || photoImporting) return
        discardStaged(welcomeImageDraft, originalWelcomeImage)
        settingsOpen = false
    }
    fun saveSettings() {
        if (saving || photoImporting) return
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
                if (welcomeImageEdited) {
                    val previous = settings.welcomeImagePath.first()
                    settings.saveAppearance(value, welcomeImageDraft)
                    if (previous != null && previous != welcomeImageDraft)
                        app.discardPhotoIfUnreferenced(previous)
                } else settings.saveTitle(value)
                settingsOpen = false
                originalWelcomeImage = welcomeImageDraft
            } catch (_: IOException) {
                titleError = TitleError.SaveFailed
            } finally {
                saving = false
            }
        }
    }
}
