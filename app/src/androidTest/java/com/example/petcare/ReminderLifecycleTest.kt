package com.example.petcare

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.petcare.data.*
import com.example.petcare.reminders.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderLifecycleTest {
    @Test fun postponeReplacesAlarmsAndConfirmationClearsAlarmsAndDisplayedNotifications() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")).use { it.readBytes() }
        }
        val name = "delivery-${UUID.randomUUID()}.db"
        val zone = ZoneId.systemDefault()
        // Future relative to the actual device, so no real alarm fires while this test inspects it.
        val due = LocalDate.now().plusDays(2)
        val clock = Clock.fixed(due.atTime(9, 0).atZone(zone).toInstant(), zone)
        val db = PetCareDatabase.open(context, name)
        val repo = TreatmentRepository(db, clock)
        val notifications = NotificationHelper(context, clock)
        val manager = context.getSystemService(NotificationManager::class.java)
        val scheduler = AlarmScheduler(context, repo, notifications, clock)
        fun pending(plan: TreatmentPlan, kind: ReminderKind): PendingIntent? = PendingIntent.getBroadcast(context, 0,
            Intent(context, ReminderReceiver::class.java).setAction(AlarmScheduler.ACTION_REMIND)
                .setData(scheduler.identity(plan, plan.revision, kind)), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        suspend fun awaitNotificationCount(planId: String, count: Int) = withTimeout(5_000) {
            while (manager.activeNotifications.count { it.tag.contains(planId) } != count) delay(50)
        }
        var latest: TreatmentPlan? = null
        try {
            notifications.createChannels()
            assertTrue(notifications.enabled())
            val pet = repo.addPet("Notification Fido")
            val plan = checkNotNull(repo.saveFirstTreatment(pet.id, due.minusDays(30), "A", "Dose", "", due))
            latest = plan
            assertTrue(scheduler.reconcile())
            notifications.post(pet, plan, ReminderKind.Treatment)
            awaitNotificationCount(plan.id, 1)
            val shown = manager.activeNotifications.single { it.tag.contains(plan.id) }
            assertEquals(listOf("Record treatment", "Postpone until 4 PM"), shown.notification.actions.map { it.title.toString() })
            val postponed = repo.postpone(plan.id, plan.revision)
            latest = postponed
            assertTrue(scheduler.reconcile())
            awaitNotificationCount(plan.id, 0)
            assertTrue(manager.activeNotifications.none { it.tag.contains(plan.id) })
            assertNull(pending(plan, ReminderKind.Treatment))
            assertNull(pending(plan, ReminderKind.Overdue))
            assertNotNull(pending(postponed, ReminderKind.Postponed))
            assertNotNull(pending(postponed, ReminderKind.Overdue))
            // Recovery retains only the new requests and never replenishes postponement.
            assertTrue(scheduler.reconcile())
            assertNotNull(pending(postponed, ReminderKind.Postponed))
            notifications.post(pet, postponed, ReminderKind.Postponed)
            awaitNotificationCount(plan.id, 1)
            assertEquals(1, manager.activeNotifications.single { it.tag.contains(plan.id) }.notification.actions.size)
            repo.confirmTreatment(pet.id, postponed.id, postponed.revision, due, "B", "Dose B", "")
            assertTrue(scheduler.reconcile())
            assertTrue(scheduler.reconcile())
            awaitNotificationCount(plan.id, 0)
            assertTrue(manager.activeNotifications.none { it.tag.contains(plan.id) })
            for (kind in ReminderKind.entries) {
                assertNull(pending(plan, kind))
                assertNull(pending(postponed, kind))
            }
            assertFalse(ReminderRules.eligible(repo.dao.plan(plan.id)!!, postponed.revision,
                ReminderKind.Overdue, clock.instant().plusSeconds(86400), zone))
        } finally {
            latest?.let { repo.dao.plan(it.id)?.let { saved -> repo.dao.updatePlan(saved.copy(status = "COMPLETED")) } }
            scheduler.reconcile()
            db.close(); context.deleteDatabase(name)
        }
    }
}
