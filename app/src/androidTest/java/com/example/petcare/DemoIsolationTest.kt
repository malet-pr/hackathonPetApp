package com.example.petcare

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.petcare.data.PetCareDatabase
import com.example.petcare.data.TreatmentRepository
import com.example.petcare.demo.DemoSession
import com.example.petcare.reminders.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoIsolationTest {
    @Test fun sampleCrossingsAndNotificationsDoNotChangeRealRecords() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")).use { it.readBytes() }
        }
        val notifications = NotificationHelper(context)
        notifications.createChannels()
        val manager = context.getSystemService(NotificationManager::class.java)
        val realName = "demo-isolation-${UUID.randomUUID()}.db"
        val realDb = PetCareDatabase.open(context, realName)
        val realRepo = TreatmentRepository(realDb)
        val scheduler = AlarmScheduler(context, realRepo, notifications)
        var realPlanId: String? = null
        fun realPurchasePending(plan: com.example.petcare.data.TreatmentPlan): PendingIntent? =
            PendingIntent.getBroadcast(context, 0,
                Intent(context, ReminderReceiver::class.java).setAction(AlarmScheduler.ACTION_REMIND)
                    .setData(scheduler.identity(plan, plan.revision, ReminderKind.Purchase)),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        var demo: DemoSession? = null
        var overdueDemo: DemoSession? = null
        try {
            val realPet = realRepo.addPet("Real pet")
            val realPlan = checkNotNull(realRepo.saveFirstTreatment(realPet.id,
                LocalDate.now().minusDays(2), "Real medication", "Real dose", "", LocalDate.now().plusDays(10)))
            realPlanId = realPlan.id
            assertTrue(scheduler.reconcile())
            assertNotNull(realPurchasePending(realPlan))
            demo = DemoSession.create(context)
            val sample = demo.repository.dao.allPlans().single()
            val samplePet = checkNotNull(demo.repository.dao.pet(sample.petId))
            assertEquals("Demo Fido", samplePet.name)
            assertEquals(1, demo.repository.dao.history(samplePet.id).size)

            val events = ReminderRules.events(sample, demo.clock.zone)
            val purchase = demo.events.collect(events[0].at.plusSeconds(1))
            assertEquals(listOf(ReminderKind.Purchase), purchase.map { it.kind })
            assertTrue(demo.events.collect(events[0].at.plusSeconds(1)).isEmpty())
            val treatment = demo.events.collect(events[1].at.plusSeconds(1))
            assertEquals(listOf(ReminderKind.Treatment), treatment.map { it.kind })
            notifications.postDemo(demo.id, samplePet, sample, ReminderKind.Treatment, demo.clock)
            notifications.post(realPet, realPlan, ReminderKind.Purchase)
            withTimeout(5_000) {
                while (manager.activeNotifications.none { it.tag?.startsWith("demo/${demo.id}/") == true } ||
                    manager.activeNotifications.none { it.tag?.startsWith("real/${realPlan.id}/") == true }) delay(50)
            }
            assertTrue(manager.activeNotifications.any {
                it.tag?.startsWith("demo/${demo.id}/") == true &&
                    it.notification.extras.getString("android.title")?.startsWith("Demo —") == true
            })
            notifications.cancelAllDemo()
            withTimeout(5_000) {
                while (manager.activeNotifications.any { it.tag?.startsWith("demo/${demo.id}/") == true }) delay(50)
            }
            assertTrue(manager.activeNotifications.any { it.tag?.startsWith("real/${realPlan.id}/") == true })
            assertEquals("PENDING", realRepo.dao.plan(realPlan.id)?.status)
            assertEquals(1, realRepo.dao.history(realPet.id).size)
            assertNotNull(realPurchasePending(realPlan))
            assertEquals("PENDING", demo.repository.dao.plan(sample.id)?.status)

            demo.repository.confirmTreatment(samplePet.id, sample.id, sample.revision,
                LocalDate.now(demo.clock), "Medication B", "New dose", "")
            assertTrue(demo.events.collect(events[2].at.plusSeconds(1)).isEmpty())
            assertEquals("COMPLETED", demo.repository.dao.plan(sample.id)?.status)
            assertEquals(1, realRepo.dao.history(realPet.id).size)

            overdueDemo = DemoSession.create(context)
            val overduePlan = overdueDemo.repository.dao.allPlans().single()
            val overdueEvents = ReminderRules.events(overduePlan, overdueDemo.clock.zone)
            overdueDemo.events.collect(overdueEvents[0].at.plusSeconds(1))
            overdueDemo.events.collect(overdueEvents[1].at.plusSeconds(1))
            assertEquals(listOf(ReminderKind.Overdue),
                overdueDemo.events.collect(overdueEvents[2].at.plusSeconds(1)).map { it.kind })
            assertTrue(overdueDemo.events.collect(overdueEvents[2].at.plusSeconds(2)).isEmpty())
            assertEquals("PENDING", realRepo.dao.plan(realPlan.id)?.status)
            assertNotNull(realPurchasePending(realPlan))
        } finally {
            notifications.cancelAllDemo()
            realPlanId?.let(notifications::cancelPlan)
            demo?.database?.close()
            overdueDemo?.database?.close()
            realPlanId?.let { id -> realRepo.dao.plan(id)?.let { realRepo.dao.updatePlan(it.copy(status = "COMPLETED")) } }
            scheduler.reconcile()
            realDb.close()
            context.deleteDatabase(realName)
        }
    }
}
