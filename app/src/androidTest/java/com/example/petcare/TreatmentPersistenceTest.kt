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
class TreatmentPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
    @Test fun firstRecordsAndSeparatePetsSurviveReopening() = runBlocking {
        val name = "care-${UUID.randomUUID()}.db"
        var db = PetCareDatabase.open(context, name)
        try {
            val repo = TreatmentRepository(db, clock)
            val fido = repo.addPet(" Fido ")
            val luna = repo.addPet("Luna")
            val plan = checkNotNull(repo.saveFirstTreatment(fido.id, LocalDate.of(2026, 10, 10), "Medication A", "One tablet", "Clinic", LocalDate.of(2026, 11, 10)))
            repo.saveFirstTreatment(luna.id, LocalDate.of(2026, 10, 9), "Medication B", "As recorded", "", LocalDate.of(2026, 11, 9))
            db.close()
            db = PetCareDatabase.open(context, name)
            assertEquals("Fido", db.careDao().pet(fido.id)!!.name)
            assertEquals("2026-10-10", db.careDao().history(fido.id).single().actualDate)
            assertEquals("Medication A", db.careDao().history(fido.id).single().medicationName)
            assertEquals("Clinic", db.careDao().history(fido.id).single().purchaseSource)
            assertEquals("Medication B", db.careDao().history(luna.id).single().medicationName)
            assertNull(db.careDao().history(luna.id).single().purchaseSource)
            assertEquals("2026-11-10", db.careDao().pending(fido.id)!!.dueDate)
            assertEquals(plan.sourceAdministrationId, db.careDao().history(fido.id).single().id)
            assertTrue(db.careDao().plan(plan.id)!!.needsReconciliation)
            try {
                TreatmentRepository(db, clock).saveFirstTreatment(fido.id, LocalDate.of(2026, 10, 10), "Other", "Other", "", LocalDate.of(2026, 11, 10))
                fail("Duplicate first treatment should be rejected")
            } catch (_: IllegalArgumentException) { }
            assertEquals(1, db.careDao().history(fido.id).size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun optionalScheduleCanBeAddedThenEditedWithoutChangingHistory() = runBlocking {
        val name = "optional-${UUID.randomUUID()}.db"
        var db = PetCareDatabase.open(context, name)
        try {
            var repo = TreatmentRepository(db, clock)
            val pet = repo.addPet("Fido")
            assertNull(repo.saveFirstTreatment(pet.id, LocalDate.of(2026, 10, 10), "A", "Dose", "Clinic", null))
            db.close()
            db = PetCareDatabase.open(context, name)
            repo = TreatmentRepository(db, clock)
            val original = repo.dao.history(pet.id).single()
            assertNull(repo.dao.pending(pet.id))
            assertTrue(repo.dao.allPlans().isEmpty())
            val first = repo.saveNextDate(pet.id, LocalDate.of(2026, 11, 10), null, null)
            repo.dao.updatePlan(first.copy(handledBits = 1, postponedOnce = true))
            val revised = repo.saveNextDate(pet.id, LocalDate.of(2026, 11, 12), first.id, first.revision)
            assertEquals(first.id, revised.id)
            assertEquals(1, revised.revision)
            assertEquals(0, revised.handledBits)
            assertTrue(revised.postponedOnce)
            assertTrue(revised.needsReconciliation)
            // A stale editor must not overwrite a more recent revision.
            try {
                repo.saveNextDate(pet.id, LocalDate.of(2026, 11, 15), first.id, first.revision)
                fail("Stale editor accepted")
            } catch (_: IllegalArgumentException) { }
            try {
                repo.saveNextDate(pet.id, LocalDate.of(2026, 10, 10), revised.id, revised.revision)
                fail("Invalid next date accepted")
            } catch (_: IllegalArgumentException) { }
            assertEquals(revised, repo.saveNextDate(pet.id, LocalDate.of(2026, 11, 12), revised.id, revised.revision))
            db.close()
            db = PetCareDatabase.open(context, name)
            assertEquals(original, db.careDao().history(pet.id).single())
            assertEquals(revised, db.careDao().pending(pet.id))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun failedPlanInsertRollsBackAdministration() = runBlocking {
        val name = "rollback-${UUID.randomUUID()}.db"
        val db = PetCareDatabase.open(context, name)
        try {
            val repo = TreatmentRepository(db, clock)
            val pet = repo.addPet("Fido")
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_plan BEFORE INSERT ON treatment_plans BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try {
                repo.saveFirstTreatment(pet.id, LocalDate.of(2026, 10, 10), "A", "Dose", "", LocalDate.of(2026, 11, 10))
                fail("Plan insert should fail")
            } catch (_: android.database.sqlite.SQLiteException) { }
            assertTrue(db.careDao().history(pet.id).isEmpty())
            assertNull(db.careDao().pending(pet.id))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun invalidDatesDoNotWriteRecords() = runBlocking {
        val name = "validation-${UUID.randomUUID()}.db"
        val db = PetCareDatabase.open(context, name)
        try {
            val repo = TreatmentRepository(db, clock)
            val pet = repo.addPet("Fido")
            for ((actual, due) in listOf("2026-10-12" to "2026-11-12", "2026-10-10" to "2026-10-10")) {
                try {
                    repo.saveFirstTreatment(pet.id, LocalDate.parse(actual), "A", "Dose", "", LocalDate.parse(due))
                    fail("Invalid date accepted")
                } catch (_: IllegalArgumentException) { }
            }
            assertTrue(db.careDao().history(pet.id).isEmpty())
            assertTrue(db.careDao().allPlans().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
