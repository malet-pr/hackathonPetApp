package com.example.petcare.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.petcare.data.TreatmentRepository
import com.example.petcare.data.TreatmentPlan
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

class AlarmScheduler(private val context: Context, private val repository: TreatmentRepository,
    private val notifications: NotificationHelper, private val clock: Clock = Clock.systemUTC()) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun identity(plan: TreatmentPlan, revision: Int, kind: ReminderKind): Uri =
        Uri.Builder().scheme("petcare").authority("real").appendPath(plan.id)
            .appendPath(revision.toString()).appendPath(kind.name).build()

    private fun intent(plan: TreatmentPlan, revision: Int, kind: ReminderKind) =
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND).setData(identity(plan, revision, kind))

    // Also used by the debug-only short diagnostic: same alarm type and scheduling API.
    fun setAlarm(atMillis: Long, intent: Intent) {
        val pending = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
    }

    suspend fun reconcile(): Boolean = repository.reminderLock.withLock {
        var success = true
        for (saved in repository.dao.allPlans()) {
            // Set marker BEFORE touching the nontransactional OS alarm service.
            val plan = saved.copy(needsReconciliation = true)
            repository.dao.updatePlan(plan)
            try {
                for (revision in 0..plan.revision) for (kind in ReminderKind.entries) {
                    PendingIntent.getBroadcast(context, 0, intent(plan, revision, kind),
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                        alarms.cancel(it)
                        it.cancel()
                    }
                }
                if (saved.needsReconciliation || plan.status != "PENDING") notifications.cancelPlan(plan.id)
                for (event in ReminderRules.futureEvents(plan, clock.instant(), ZoneId.systemDefault())) {
                    setAlarm(event.at.toEpochMilli(), intent(plan, plan.revision, event.kind))
                }
                repository.dao.updatePlan(plan.copy(needsReconciliation = false))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                success = false // Saved record stays valid; retry on launch/recovery.
            }
        }
        success
    }

    companion object { const val ACTION_REMIND = "com.example.petcare.REMIND" }
}
