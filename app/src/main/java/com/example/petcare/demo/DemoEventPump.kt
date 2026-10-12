package com.example.petcare.demo

import com.example.petcare.data.Pet
import com.example.petcare.data.TreatmentPlan
import com.example.petcare.reminders.ReminderKind
import com.example.petcare.reminders.ReminderRules
import java.time.Instant
import kotlinx.coroutines.sync.withLock

data class DemoDueEvent(val pet: Pet, val plan: TreatmentPlan, val kind: ReminderKind)

/** Records a crossing before asking Android to display it, so a recomposition cannot replay it. */
class DemoEventPump(private val session: DemoSession) {
    suspend fun collect(now: Instant): List<DemoDueEvent> = session.repository.reminderLock.withLock {
        val before = session.lastChecked
        if (now <= before) return@withLock emptyList()
        val due = mutableListOf<DemoDueEvent>()
        for (saved in session.repository.dao.allPlans()) {
            if (saved.status != "PENDING") continue
            val pet = session.repository.dao.pet(saved.petId) ?: continue
            var plan = saved
            for (event in ReminderRules.events(saved, session.clock.zone)) {
                if (event.at > before && event.at <= now &&
                    ReminderRules.eligible(plan, plan.revision, event.kind, now, session.clock.zone)) {
                    plan = plan.copy(handledBits = plan.handledBits or event.kind.bit,
                        needsReconciliation = false)
                    session.repository.dao.updatePlan(plan)
                    due += DemoDueEvent(pet, plan, event.kind)
                }
            }
        }
        session.lastChecked = now
        due
    }
}
