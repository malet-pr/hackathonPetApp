package com.example.petcare

import android.app.PendingIntent
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.petcare.data.*
import com.example.petcare.reminders.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmReconciliationTest {
    @Test fun reconcileIsIdempotentAndPetAlarmsHaveDistinctIdentities() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "alarms-${UUID.randomUUID()}.db"
        val db = PetCareDatabase.open(context, name)
        val repo = TreatmentRepository(db)
        val scheduler = AlarmScheduler(context, repo, NotificationHelper(context))
        fun pending(plan: TreatmentPlan, kind: ReminderKind): PendingIntent? =
            PendingIntent.getBroadcast(context, 0,
                Intent(context, ReminderReceiver::class.java).setAction(AlarmScheduler.ACTION_REMIND)
                    .setData(scheduler.identity(plan, plan.revision, kind)),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        var plans = emptyList<TreatmentPlan>()
        try {
            val today = LocalDate.now()
            plans = listOf(repo.addPet("Test Fido"), repo.addPet("Test Luna")).map {
                checkNotNull(repo.saveFirstTreatment(it.id, today, "A", "Dose", "", today.plusDays(8)))
            }
            assertTrue(scheduler.reconcile())
            val first = pending(plans[0], ReminderKind.Treatment)
            assertNotNull(first)
            assertNotEquals(first, pending(plans[1], ReminderKind.Treatment))
            assertNotEquals(first, pending(plans[0], ReminderKind.Purchase))
            assertTrue(scheduler.reconcile())
            assertNotNull(pending(plans[0], ReminderKind.Treatment))
            // Reconciliation cancels/recreates the OS token; its URI, not Binder identity, is stable.
            assertTrue(repo.dao.allPlans().none { it.needsReconciliation })
            // A handled event is not reintroduced by recovery.
            repo.dao.updatePlan(repo.dao.plan(plans[0].id)!!.copy(handledBits = ReminderKind.Purchase.bit))
            assertTrue(scheduler.reconcile())
            assertNull(pending(plans[0], ReminderKind.Purchase))
            assertNotNull(pending(plans[0], ReminderKind.Treatment))
            val old = plans[0]
            val revised = repo.saveNextDate(old.petId, today.plusDays(10), old.id, old.revision)
            plans = listOf(revised, plans[1])
            assertTrue(scheduler.reconcile())
            for (kind in listOf(ReminderKind.Purchase, ReminderKind.Treatment, ReminderKind.Overdue)) {
                assertNull(pending(old, kind))
                assertNotNull(pending(revised, kind))
                assertNotNull(pending(plans[1], kind))
            }
            assertFalse(ReminderRules.eligible(revised, old.revision, ReminderKind.Treatment,
                java.time.Instant.now(), java.time.ZoneId.systemDefault()))
        } finally {
            plans.forEach { repo.dao.plan(it.id)?.let { saved -> repo.dao.updatePlan(saved.copy(status = "COMPLETED")) } }
            scheduler.reconcile()
            plans.forEach { assertNull(pending(it, ReminderKind.Treatment)) }
            db.close()
            context.deleteDatabase(name)
        }
    }
}
