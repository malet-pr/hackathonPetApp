package com.example.petcare.data

import androidx.room.withTransaction
import java.time.Clock
import java.time.LocalDate
import com.example.petcare.reminders.ReminderRules
import com.example.petcare.reminders.ReminderKind
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TreatmentRepository(val database: PetCareDatabase, private val clock: Clock? = null) {
    val dao = database.careDao()
    // Alarm receivers and future confirmation/edit operations share this lock.
    val reminderLock = Mutex()
    val summaries = combine(dao.observePets(), dao.observeAdministrations(), dao.observePlans()) { pets, records, plans ->
        pets.map { pet -> PetSummary(pet, records.firstOrNull { it.petId == pet.id },
            plans.firstOrNull { it.petId == pet.id && it.status == "PENDING" }) }
            .sortedWith(compareBy<PetSummary> { it.pending?.dueDate ?: "9999" }.thenBy { it.pet.name.lowercase() })
    }
    val administrations = dao.observeAdministrations()

    suspend fun addPet(name: String, birthDate: LocalDate? = null, owner: String = "", breed: String = "",
        photoPath: String? = null): Pet {
        require(name.trim().isNotEmpty()) { "Enter your pet’s name." }
        require(birthDate == null || birthDate <= LocalDate.now(clock ?: Clock.systemDefaultZone())) {
            "Birth date cannot be in the future."
        }
        return Pet(name = name.trim(), birthDate = birthDate?.toString(), owner = owner.trim().ifEmpty { null },
            breed = breed.trim().ifEmpty { null }, photoPath = photoPath).also { dao.insertPet(it) }
    }

    suspend fun updatePet(id: String, name: String, birthDate: LocalDate?, owner: String, breed: String,
        photoPath: String?): Pet = database.withTransaction {
        require(name.trim().isNotEmpty()) { "Enter your pet’s name." }
        require(birthDate == null || birthDate <= LocalDate.now(clock ?: Clock.systemDefaultZone())) {
            "Birth date cannot be in the future."
        }
        val pet = requireNotNull(dao.pet(id)) { "This pet could not be found." }
        pet.copy(name = name.trim(), birthDate = birthDate?.toString(), owner = owner.trim().ifEmpty { null },
            breed = breed.trim().ifEmpty { null }, photoPath = photoPath).also { dao.updatePet(it) }
    }

    suspend fun saveFirstTreatment(petId: String, actual: LocalDate, medication: String, dose: String,
        source: String, next: LocalDate?, photoPath: String? = null): TreatmentPlan? = reminderLock.withLock {
        require(medication.trim().isNotEmpty()) { "Enter the medication name." }
        require(dose.trim().isNotEmpty()) { "Enter the dose as given." }
        require(actual <= LocalDate.now(clock ?: Clock.systemDefaultZone())) { "Administration date cannot be in the future." }
        require(next == null || next > actual) { "Next treatment must be after the administration date." }
        database.withTransaction {
            requireNotNull(dao.pet(petId)) { "This pet could not be found." }
            require(dao.history(petId).isEmpty() && dao.pending(petId) == null) { "This pet already has a treatment record." }
            val administration = Administration(petId = petId, actualDate = actual.toString(),
                medicationName = medication.trim(), doseText = dose.trim(),
                purchaseSource = source.trim().ifEmpty { null }, photoPath = photoPath,
                recordedAt = clock?.millis() ?: System.currentTimeMillis())
            dao.insertAdministration(administration)
            next?.let {
                TreatmentPlan(petId = petId, sourceAdministrationId = administration.id, dueDate = it.toString())
                    .also { plan -> dao.insertPlan(plan) }
            }
        }
    }

    suspend fun saveNextDate(petId: String, next: LocalDate, expectedPlanId: String?,
        expectedRevision: Int?): TreatmentPlan = reminderLock.withLock {
        database.withTransaction {
            val pending = dao.pending(petId)
            require(pending?.id == expectedPlanId && pending?.revision == expectedRevision) {
                "This schedule changed. Reopen it and try again."
            }
            val history = dao.history(petId)
            val administration = requireNotNull(if (pending == null) history.firstOrNull()
                else history.firstOrNull { it.id == pending.sourceAdministrationId }) {
                "Record an administered treatment first."
            }
            require(next > LocalDate.parse(administration.actualDate)) {
                "Next treatment must be after the administration date."
            }
            if (pending == null) {
                TreatmentPlan(petId = petId, sourceAdministrationId = administration.id, dueDate = next.toString())
                    .also { dao.insertPlan(it) }
            } else if (pending.dueDate == next.toString()) pending
            else pending.copy(dueDate = next.toString(), revision = pending.revision + 1,
                handledBits = 0, needsReconciliation = true).also { dao.updatePlan(it) }
        }
    }

    suspend fun confirmTreatment(petId: String, planId: String, revision: Int, actual: LocalDate,
        medication: String, dose: String, source: String, photoPath: String? = null): Administration = reminderLock.withLock {
        require(medication.trim().isNotEmpty()) { "Enter the medication name." }
        require(dose.trim().isNotEmpty()) { "Enter the dose as given." }
        require(actual <= LocalDate.now(clock ?: Clock.systemDefaultZone())) { "Administration date cannot be in the future." }
        database.withTransaction {
            val plan = requireNotNull(dao.plan(planId)) { "This schedule could not be found." }
            require(plan.petId == petId && plan.status == "PENDING" && plan.revision == revision) {
                "This schedule changed or was already completed. Reopen the record."
            }
            val previous = requireNotNull(dao.history(petId).firstOrNull { it.id == plan.sourceAdministrationId })
            require(actual >= LocalDate.parse(previous.actualDate)) {
                "Administration date cannot be before the previous treatment."
            }
            val administration = Administration(petId = petId, actualDate = actual.toString(),
                medicationName = medication.trim(), doseText = dose.trim(),
                purchaseSource = source.trim().ifEmpty { null }, photoPath = photoPath,
                recordedAt = clock?.millis() ?: System.currentTimeMillis(),
                completedPlanId = plan.id)
            dao.insertAdministration(administration)
            dao.updatePlan(plan.copy(status = "COMPLETED", needsReconciliation = true))
            administration
        }
    }

    suspend fun postpone(planId: String, revision: Int): TreatmentPlan = reminderLock.withLock {
        database.withTransaction {
            val plan = requireNotNull(dao.plan(planId)) { "This schedule could not be found." }
            val time = clock ?: Clock.systemDefaultZone()
            require(plan.revision == revision && ReminderRules.canPostpone(plan, time.instant(), time.zone)) {
                "Postponement is available once, before 4 PM on the due day. Reopen the record."
            }
            plan.copy(revision = plan.revision + 1, postponedOnce = true,
                handledBits = plan.handledBits or ReminderKind.Treatment.bit,
                needsReconciliation = true).also { dao.updatePlan(it) }
        }
    }

}
