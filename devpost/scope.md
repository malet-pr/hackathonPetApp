---
doc: scope
status: approved
---

# Pet Care — Flea Treatment Tracker

A native Android app in Kotlin to record each dog's flea treatment and remember when to buy and give the next dose.

## The Unique Kernel

A treatment record connected to a follow-through loop: remember to buy the medication, remember to give it, and confirm it was given, with a follow-up if it remains unconfirmed. The owner enters the next due date for each treatment.

## Who It's For

Initially, the learner caring for their two dogs. They currently put a flea-treatment note on the fridge, but it does not always survive until it is needed.

## The Core Loop

Choose a dog, record the treatment date, dose, specific medication, and where it was obtained, and enter the next due date. Return to check the record, receive phone reminders to buy and administer the next treatment, and confirm when it has been given. Confirmation asks for the actual administration date, with a date picker defaulting to today and allowing an earlier date. Record administration on the selected date rather than automatically using the confirmation date or scheduled due date, and cancel any pending overdue reminders. An unconfirmed treatment gets an overdue reminder.

First entry includes a next due date. Confirming the pending treatment creates a new administration record with the medication actually given, which may differ from the previous product; prior records remain unchanged. Medication details start blank, with an explicit optional copy action. After confirmation, offer to schedule another treatment or choose "Not now" with no next treatment scheduled.

## Inspiration & Identity

Inspired by the learner's two dogs and a possible future tool for their own use. Earthy brown and sand base colors with green accents; the welcome title and image can be customized.

## Why This Matters to the Learner

Replace an unreliable fridge note with a useful record and reminders while learning structured AI-assisted development, Kotlin, and Android. Understanding the implementation matters more than polish or prizes.

## What "Working" Looks Like

On an Android phone, demonstrate a treatment record for a dog, its manually entered next due date, phone reminders, and confirmation that the treatment was given. A clearly labeled demo mode uses sample treatment data and accelerates time so one day becomes one minute. It lets the learner practice and demonstrate the purchase reminder, treatment reminder, and overdue follow-up without waiting for real treatment intervals. Demo data and reminders must be distinguishable from real treatments. Show an overdue follow-up when confirmation is withheld.

The final walkthrough should fit a short demo video; a public GitHub repository is also required for submission. User-facing reminder behavior will be defined in the PRD. During the technical specification stage, examine Android background notification scheduling constraints and choose a reliable implementation. Preserve the proposed one-day-per-minute demo mode where feasible; if necessary, adjust the demo mechanism without compromising real reminder behavior.

## The POC Boundary

- Support multiple pets with a required name and optional birth date, owner, breed, and photo; the demo may use one pet.
- Save and view flea-treatment details: date, dose, medication, where it was obtained, and one optional photo selected from existing phone images.
- Include a customizable welcome title/image, settings accessible everywhere, drawer navigation, and reminder cards. Deferred feature entries are visibly disabled.
- Let the owner enter the next due date manually.
- Provide a purchase reminder at noon four days ahead, a treatment reminder at 8 AM on the due date with one postponement to 4 PM that day, and one overdue reminder at noon the next day if unconfirmed.
- Let the owner confirm a treatment was given by selecting its actual administration date; default the date picker to today and allow earlier dates. Save the selected administration date and cancel any pending overdue reminders on confirmation. Provide an overdue reminder if the treatment remains unconfirmed.
- Provide a clearly labeled demo mode with sample treatment data, where one day becomes one minute, for learning, demonstration, and testing.
- Keep demo data separate from real records; the settings switch resets sample data on entry and cancels only demo reminders on exit. Real reminders continue normally.
- Warn when notifications are disabled while still permitting record saving. Retain records, settings, and photos between sessions.

The welcome customization, richer pet records, optional photos, and copy action were added and approved during the PRD interview. Detailed behavior and acceptance criteria are in `prd.md`.

Phone reminders and their verification are the main technical uncertainty within the 2–4-hour target. The technical plan must account for that work; any scope revision needs the learner's agreement.

## Later

- Vaccination records and reminders, including the learner's interest in tracking rabies vaccination dates.
- Grooming and veterinary appointments and a calendar.
- A provider directory with contact information.
- Broader medication tracking beyond flea treatments.
- A guided onboarding tutorial; the accelerated demo flow provides the initial way to try the app.
- Per-medication reminder timing, light/dark switching, camera capture, multiple attachments, and image editing.

These remain part of the larger idea but are deferred to keep the first proof of concept focused on flea treatment.

## Explicitly Cut

Automatic calculation of the next treatment date from a product or repeat interval: the learner explicitly prefers entering the date themselves because schedules may vary. The app records the owner's chosen date.
