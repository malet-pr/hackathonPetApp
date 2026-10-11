package com.example.petcare.reminders

import com.example.petcare.data.TreatmentPlan
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReminderRulesTest {
    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")
    private val plan = TreatmentPlan(id = "plan", petId = "fido", sourceAdministrationId = "a", dueDate = "2027-01-02")
    @Test fun scheduleCrossesYearBoundaryInLocalTime() {
        val dates = ReminderRules.events(plan, zone).map { it.at.atZone(zone).toLocalDateTime().toString() }
        assertEquals(listOf("2026-12-29T12:00", "2027-01-02T08:00", "2027-01-03T12:00"), dates)
    }
    @Test fun elapsedTimesAndHandledEventsAreNotRescheduled() {
        val now = LocalDate.parse(plan.dueDate).atTime(9, 0).atZone(zone).toInstant()
        assertEquals(listOf(ReminderKind.Overdue), ReminderRules.futureEvents(plan, now, zone).map { it.kind })
        assertTrue(ReminderRules.futureEvents(plan.copy(handledBits = ReminderKind.Overdue.bit), now, zone).isEmpty())
    }
    @Test fun lateDeliveryMustStillBeEligibleAndCurrent() {
        val dueMorning = LocalDate.parse(plan.dueDate).atTime(10, 0).atZone(zone).toInstant()
        assertFalse(ReminderRules.eligible(plan, 0, ReminderKind.Purchase, dueMorning, zone))
        assertTrue(ReminderRules.eligible(plan, 0, ReminderKind.Treatment, dueMorning, zone))
        assertFalse(ReminderRules.eligible(plan, 1, ReminderKind.Treatment, dueMorning, zone))
        assertFalse(ReminderRules.eligible(plan.copy(status = "COMPLETED"), 0, ReminderKind.Treatment, dueMorning, zone))
        val tomorrow = dueMorning.plusSeconds(86_400)
        assertFalse(ReminderRules.eligible(plan, 0, ReminderKind.Treatment, tomorrow, zone))
        assertTrue(ReminderRules.eligible(plan, 0, ReminderKind.Overdue, tomorrow.plusSeconds(7_200), zone))
        assertFalse(ReminderRules.eligible(plan.copy(handledBits = 4), 0, ReminderKind.Overdue, tomorrow.plusSeconds(7_200), zone))
    }
    @Test fun postponementIsOnceBeforeFourAndReplacesTheMorningEvent() {
        val morning = LocalDate.parse(plan.dueDate).atTime(9, 0).atZone(zone).toInstant()
        assertTrue(ReminderRules.canPostpone(plan, morning, zone))
        assertFalse(ReminderRules.canPostpone(plan, morning.plusSeconds(7 * 3600), zone))
        assertFalse(ReminderRules.canPostpone(plan, morning.minusSeconds(86400), zone))
        assertFalse(ReminderRules.canPostpone(plan.copy(status = "COMPLETED"), morning, zone))
        val revised = plan.copy(postponedOnce = true, revision = 1, handledBits = ReminderKind.Treatment.bit)
        assertFalse(ReminderRules.canPostpone(revised, morning, zone))
        assertEquals(listOf(ReminderKind.Postponed, ReminderKind.Overdue),
            ReminderRules.futureEvents(revised, morning, zone).map { it.kind })
        assertEquals(16, ReminderRules.events(revised, zone)[1].at.atZone(zone).hour)
        assertFalse(ReminderRules.eligible(revised, 0, ReminderKind.Treatment, morning, zone))
    }
    @Test fun datePickerUsesUtcCalendarDateEvenWestOfUtc() {
        val day = LocalDate.of(2026, 10, 11)
        val value = CalendarDates.toPicker(day)
        assertEquals(LocalDate.of(2026, 10, 10), Instant.ofEpochMilli(value).atZone(zone).toLocalDate())
        assertEquals(day, CalendarDates.fromPicker(value))
        assertEquals(day, CalendarDates.fromPicker(day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()))
    }
    @Test fun daylightSavingUsesWallClockRatherThanFixed24Hours() {
        val newYork = ZoneId.of("America/New_York")
        val spring = plan.copy(dueDate = "2027-03-14")
        assertEquals(8, ReminderRules.events(spring, newYork)[1].at.atZone(newYork).hour)
        assertEquals(12, ReminderRules.events(spring, newYork)[2].at.atZone(newYork).hour)
    }
}
