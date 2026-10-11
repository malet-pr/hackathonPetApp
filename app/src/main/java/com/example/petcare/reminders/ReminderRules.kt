package com.example.petcare.reminders

import com.example.petcare.data.TreatmentPlan
import java.time.*

enum class ReminderKind(val bit: Int) { Purchase(1), Treatment(2), Overdue(4), Postponed(8) }
data class ReminderEvent(val kind: ReminderKind, val at: Instant)

object ReminderRules {
    fun events(plan: TreatmentPlan, zone: ZoneId): List<ReminderEvent> {
        if (plan.status != "PENDING") return emptyList()
        val due = LocalDate.parse(plan.dueDate)
        fun event(kind: ReminderKind, date: LocalDate, hour: Int) =
            ReminderEvent(kind, date.atTime(hour, 0).atZone(zone).toInstant())
        return listOf(
            event(ReminderKind.Purchase, due.minusDays(4), 12),
            if (plan.postponedOnce) event(ReminderKind.Postponed, due, 16)
            else event(ReminderKind.Treatment, due, 8),
            event(ReminderKind.Overdue, due.plusDays(1), 12),
        )
    }

    fun futureEvents(plan: TreatmentPlan, now: Instant, zone: ZoneId): List<ReminderEvent> =
        events(plan, zone).filter { it.at > now && plan.handledBits and it.kind.bit == 0 }

    fun eligible(plan: TreatmentPlan, revision: Int, kind: ReminderKind, now: Instant, zone: ZoneId): Boolean {
        if (plan.status != "PENDING" || plan.revision != revision || plan.handledBits and kind.bit != 0) return false
        val event = events(plan, zone).firstOrNull { it.kind == kind } ?: return false
        if (now < event.at) return false
        val day = now.atZone(zone).toLocalDate()
        val due = LocalDate.parse(plan.dueDate)
        return when (kind) {
            ReminderKind.Purchase -> day < due
            ReminderKind.Treatment, ReminderKind.Postponed -> day == due
            ReminderKind.Overdue -> day > due
        }
    }
}

object CalendarDates {
    // Material date pickers encode calendar dates at UTC midnight, independent of the phone zone.
    fun fromPicker(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    fun toPicker(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
