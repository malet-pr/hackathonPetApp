package com.example.petcare

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.petcare.data.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreatmentLifecycleTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val clock = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), zone)

    @Test fun actualMedicationAndBackdatePersistAndConfirmationCannotRepeat() = runBlocking {
        val name = "lifecycle-${UUID.randomUUID()}.db"
        var db = PetCareDatabase.open(context, name)
        try {
            var repo = TreatmentRepository(db, clock)
            val pet = repo.addPet("Fido")
            val plan = checkNotNull(repo.saveFirstTreatment(pet.id, LocalDate.of(2026, 10, 10), "Medication A", "Dose A", "Clinic A", LocalDate.of(2026, 11, 10)))
            val original = repo.dao.history(pet.id).single()
            val given = repo.confirmTreatment(pet.id, plan.id, plan.revision, LocalDate.of(2026, 11, 10), " Medication B ", "Dose B", "Clinic B")
            db.close()
            db = PetCareDatabase.open(context, name)
            repo = TreatmentRepository(db, clock)
            assertEquals(original, repo.dao.history(pet.id).last())
            assertEquals(given, repo.dao.history(pet.id).first())
            assertEquals("Medication B", given.medicationName)
            assertEquals("2026-11-10", given.actualDate)
            assertEquals(plan.id, given.completedPlanId)
            assertEquals("COMPLETED", repo.dao.plan(plan.id)!!.status)
            assertTrue(repo.dao.plan(plan.id)!!.needsReconciliation)
            assertNull(repo.dao.pending(pet.id)) // Declining the offer needs no additional write.
            try {
                repo.confirmTreatment(pet.id, plan.id, 0, LocalDate.of(2026, 11, 10), "Duplicate", "Dose", "")
                fail("Duplicate confirmation accepted")
            } catch (_: IllegalArgumentException) { }
            assertEquals(2, repo.dao.history(pet.id).size)
            val next = repo.saveNextDate(pet.id, LocalDate.of(2026, 12, 10), null, null)
            assertEquals(given.id, next.sourceAdministrationId)
            assertFalse(next.postponedOnce)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun invalidOrStaleConfirmationAndFailedCompletionLeaveNoHalfRecord() = runBlocking {
        val name = "atomic-${UUID.randomUUID()}.db"
        val db = PetCareDatabase.open(context, name)
        try {
            val repo = TreatmentRepository(db, clock)
            val pet = repo.addPet("Fido")
            val plan = checkNotNull(repo.saveFirstTreatment(pet.id, LocalDate.of(2026, 10, 10), "A", "Dose", "", LocalDate.of(2026, 11, 10)))
            val other = repo.addPet("Luna")
            for ((petId, revision, actual, medication, dose) in listOf(
                listOf(other.id, "0", "2026-11-10", "B", "Dose"),
                listOf(pet.id, "1", "2026-11-10", "B", "Dose"),
                listOf(pet.id, "0", "2026-10-09", "B", "Dose"),
                listOf(pet.id, "0", "2026-11-13", "B", "Dose"),
                listOf(pet.id, "0", "2026-11-10", " ", "Dose"),
                listOf(pet.id, "0", "2026-11-10", "B", " "),
            )) {
                try {
                    repo.confirmTreatment(petId, plan.id, revision.toInt(), LocalDate.parse(actual), medication, dose, "")
                    fail("Invalid confirmation accepted")
                } catch (_: IllegalArgumentException) { }
            }
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_completion BEFORE UPDATE ON treatment_plans WHEN NEW.status = 'COMPLETED' BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try {
                repo.confirmTreatment(pet.id, plan.id, 0, LocalDate.of(2026, 11, 10), "B", "Dose", "")
                fail("Completion should fail")
            } catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(1, repo.dao.history(pet.id).size)
            assertEquals(plan, repo.dao.pending(pet.id))
            assertTrue(repo.dao.history(other.id).isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun postponePersistsOneUseAndRejectsFourPmOrWrongDay() = runBlocking {
        val name = "postpone-${UUID.randomUUID()}.db"
        var db = PetCareDatabase.open(context, name)
        val morning = Clock.fixed(LocalDate.of(2026, 11, 10).atTime(9, 0).atZone(zone).toInstant(), zone)
        try {
            var repo = TreatmentRepository(db, morning)
            val pet = repo.addPet("Fido")
            val plan = checkNotNull(repo.saveFirstTreatment(pet.id, LocalDate.of(2026, 10, 10), "A", "Dose", "", LocalDate.of(2026, 11, 10)))
            for (invalid in listOf(morning.instant().minusSeconds(86400), morning.instant().plusSeconds(7 * 3600))) {
                try { TreatmentRepository(db, Clock.fixed(invalid, zone)).postpone(plan.id, 0); fail("Invalid postponement accepted") }
                catch (_: IllegalArgumentException) { }
            }
            val postponed = repo.postpone(plan.id, 0)
            assertEquals(1, postponed.revision)
            assertTrue(postponed.postponedOnce)
            db.close(); db = PetCareDatabase.open(context, name)
            repo = TreatmentRepository(db, morning)
            assertEquals(postponed, repo.dao.pending(pet.id))
            try { repo.postpone(plan.id, postponed.revision); fail("Second postponement accepted") }
            catch (_: IllegalArgumentException) { }
            val edited = repo.saveNextDate(pet.id, LocalDate.of(2026, 11, 11), postponed.id, postponed.revision)
            assertTrue(edited.postponedOnce)
            assertEquals(1, repo.dao.history(pet.id).size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
