package com.example.petcare.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.petcare.PetCareApp
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_REMIND) return
        val uri = intent.data ?: return
        if (uri.scheme != "petcare" || uri.authority != "real" || uri.pathSegments.size != 3) return
        val (id, revisionText, kindText) = uri.pathSegments
        val revision = revisionText.toIntOrNull() ?: return
        val kind = ReminderKind.entries.firstOrNull { it.name == kindText } ?: return
        val pending = goAsync()
        val app = context.applicationContext as PetCareApp
        app.applicationScope.launch {
            try {
                withTimeout(8_000) {
                    app.repository.reminderLock.withLock {
                        val plan = app.repository.dao.plan(id) ?: return@withLock
                        if (!ReminderRules.eligible(plan, revision, kind, Instant.now(), ZoneId.systemDefault())) return@withLock
                        val pet = app.repository.dao.pet(plan.petId) ?: return@withLock
                        // Check permission immediately before posting. Disabled events are handled too,
                        // so enabling notifications later cannot replay old reminders.
                        if (app.notifications.enabled()) app.notifications.post(pet, plan, kind)
                        app.repository.dao.updatePlan(plan.copy(handledBits = plan.handledBits or kind.bit))
                    }
                }
            } catch (error: Exception) { Log.e("PetCareReminder", "Reminder could not be handled", error) }
            finally { pending.finish() }
        }
    }
}
