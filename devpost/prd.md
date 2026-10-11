---
doc: prd
status: approved
---

# Pet Care — Flea Treatment Tracker: Product Requirements

Default in-app title: "Pet Care," customizable by the owner. A native Android app for an owner to record flea treatments for multiple pets and follow through on manually scheduled doses.

Source: `scope.md > The Core Loop`, `The POC Boundary`, and `What "Working" Looks Like`.

## The Core Journey

1. Open a welcome screen with a title at the top, a large centered picture, a settings gear, and a button to enter.
2. If notifications are disabled, see a prominent warning; records remain usable.
3. On first use, add a pet. After saving, receive an invitation to add a treatment and a brief explanation of the side drawer.
4. Inside the app, use a left navigation drawer and a list of reminder cards. Each card identifies the pet and flea treatment, and shows the last administration date and next chosen treatment date.
5. Tap a card to open the previous treatment's full record.
6. Record the first administered treatment with its actual administration date, medication, dose, purchase source, optional photo, and manually chosen next due date. Saving it schedules the reminders for that next treatment.
7. When a pending treatment is confirmed, use a date picker defaulting to today and allowing an earlier date. Save the selected administration date and cancel pending overdue reminders. Then ask, "Would you like to schedule another treatment?" Choosing "Not now" leaves the completed record saved with no next treatment scheduled. Choosing "Yes" lets the owner manually enter the next due date. Scheduled treatments receive a purchase reminder four days ahead, a treatment reminder, and an overdue reminder the following day if still unconfirmed.
8. Edit a next due date later and update its pending reminders.
9. Use clearly labeled sample data in demo mode to practice and demonstrate the same loop with one simulated day per real minute, subject to the approved technical investigation.

## Screens and Layout

### Welcome and Settings

Learner-requested addition to the original scope: title at the top, a large centered default image, a settings gear, and a button to enter. Settings allow changing the displayed title and image. This custom title is inside the app; changing the Android launcher name has not been requested.

The settings gear remains available on every screen and includes the demo-mode switch. Light/dark switching is deferred. Select existing phone images rather than adding camera capture.

### Home and Navigation Drawer

Source: `scope.md > The Core Loop`.

A recognizable navigation icon opens a drawer from the left. Show the broader feature list, with only medication enabled for this POC. Vaccinations, appointments, and providers are disabled and labeled "Later." Medication means flea-treatment tracking in this version, with different product names allowed.

The main area contains reminder cards with a title such as "FIDO — Flea treatment," the last administration date, and the next owner-chosen date. There is no separate coverage-expiration date. Show all next scheduled treatments, including overdue ones, ordered by due date without a month boundary. Pets without a next date show "No treatment scheduled." Real-mode cards use real records, while sample reminders belong to labeled demo mode.

### Pet Record

Learner-requested expansion from `scope.md > The POC Boundary` and `Later`.

Support adding multiple pets, even if the demo uses only one. Require name; date of birth, owner, breed, and picture are optional. After saving a new pet, offer to add its first treatment and explain the drawer briefly. This is a short first-use prompt, not a guided tutorial.

### Treatment Detail and Entry

Source: `scope.md > The Core Loop` and `The POC Boundary`.

Opening a card shows the full previous treatment record: pet, actual administration date, medication, dose, where it was obtained, chosen next due date, and optional photo.

New treatment entry starts with medication, dose, and purchase source blank. An optional "Copy from last treatment" action copies those three fields from the same pet's last treatment, leaving them editable. Dates and photos are not copied. If there is no previous treatment, the action cannot supply values.

Use a standard calendar date picker for actual administration date, defaulting to today and allowing earlier dates. First treatment entry includes a required, manually chosen next due date; saving schedules its reminders. This first-entry flow does not use the optional post-confirmation scheduling question. After confirming a pending treatment, offer to schedule another; the owner can decline. When scheduling another treatment, ask for its next due date and allow editing it later. Include a single optional photo per treatment, selected from existing phone images; camera capture is deferred.

## Look and Feel

Earthy brown and sand base colors with green accents. Welcome image centered and visually dominant, title above it. Standard recognizable gear and drawer icons. Font choice and other visual details were not specified by the learner.

## Features and Behavior

### Administration Confirmation

Source: `scope.md > The Core Loop`.

Always ask for actual administration date. Default to today but permit an earlier date so a timely treatment can be recorded late. Store the selected date, not the scheduled date or the date of confirmation. Cancel pending overdue reminders for the confirmed treatment. Preserve treatment records when moving to the next dose.

After confirmation, ask whether to schedule another treatment. "Not now" preserves the completed record with no next treatment scheduled; "Yes" asks for a manually selected next date. Do not require another treatment to save confirmation. This accommodates a future treatment category without a foreseeable next dose, while this POC remains limited to flea treatment.

The full treatment detail provides a "Record treatment" action for the pending dose. It opens a new administration form with medication, dose, and purchase source blank, the optional copy action, optional photo, and actual administration date. The medication actually administered may differ from the previous treatment's medication. Saving creates a new administration record linked to completion of the pending treatment; it must not overwrite the earlier administration or assume a product was administered merely because a date was scheduled. Dismissing a notification does not confirm administration.

Example: Fido receives medication A on October 10 and has another treatment scheduled for November 10. On November 12 the owner records medication B and chooses the actual administration date (November 12, or November 10 if recording it late). The new record contains B and the selected date; the October 10 record still contains A. Cancel pending overdue/postponed reminders for the completed scheduled treatment, then offer to schedule another.

### Reminder Schedule and Due-Date Changes

Source: `scope.md > The POC Boundary`.

- Purchase reminder: at noon, four days before the next chosen treatment date.
- Treatment reminder: on the due date at 8 AM, with an option to postpone it once until 4 PM that same day. Postponement does not confirm administration. Only one postponement is available per scheduled treatment.
- Overdue reminder: at noon the next day if unconfirmed. One follow-up only.
- Per-medication timing is deferred in favor of these shared defaults.
- Editing a next due date updates pending reminders to reflect the revised date.

Use local phone time. Skip elapsed reminder times when creating/editing schedules rather than sending old alerts in a burst; retain future alerts and show overdue status in the app. Offer postponement only before 4 PM on the due day. Confirmation cancels pending postponed alerts as well as overdue alerts. No medication interval or dose is calculated or recommended by the app.

### Notifications Disabled

Show a warning on the welcome screen and when saving a treatment if phone notifications are disabled. Saving succeeds despite the warning. Records and upcoming/overdue information remain accessible in the app. The learner chose this because recordkeeping is still useful without phone alerts.

### Accelerated Demo Mode

Source: `scope.md > What "Working" Looks Like`.

The settings gear is available on every screen and contains a demo-mode switch. Clearly label demo mode and its sample records/reminders. One simulated day equals one real minute. Demonstrate purchase, treatment, and overdue reminders, plus confirmation and cancellation of pending overdue reminders. Demo uses separate sample pets and treatments; actions there do not change real records. Real reminders continue normally while demo mode is active. Switching demo mode off cancels only demo reminders and returns to real records. Start each demo session with fresh samples.

During `4-spec`, investigate Android scheduling restrictions and choose a reliable mechanism. The learner explicitly allows revising the demo mechanism if necessary without compromising real reminder behavior. The investigation is technical work for that stage, not a resolved implementation decision here.

## States and Boundaries

- **No pets:** begin pet creation; after saving, offer treatment entry and a drawer explanation.
- **No treatments for a pet:** offer its first record without needing a previous record.
- **Notifications disabled:** warn without blocking save; do not imply that phone alerts will arrive.
- **Upcoming or overdue:** home cards make treatment status accessible without notifications; mark overdue cards clearly.
- **Confirmed:** selected administration date is visible and pending overdue reminders are cancelled.
- **Demo:** label sample data and accelerated timing; do not pass samples off as real care records.
- **Persistence:** retain saved pet and treatment records, settings, and photos between app sessions.
- **Invalid input or failed save/image selection:** apply the form rules below, explain invalid fields, and preserve entries for correction/retry. Do not claim success after a failed save. Cancelling or failing image selection leaves the form usable without a new photo.

## Acceptance Criteria

These checks define observable completion of the approved product behavior.

- Create two pets and keep their treatment records distinct; demonstrate with one if preferred.
- A treatment card shows the correct pet, last administration date, and next chosen date; opening it shows the full prior treatment details.
- A new treatment's medication, dose, and purchase source start blank. Copying from the same pet's last record fills only those three editable fields.
- Attach one optional treatment photo and display it with its record; saving without a photo works.
- Confirm today or select an earlier date; the saved administration date matches the selection and pending overdue reminders for that treatment are cancelled.
- Record A on October 10, schedule November 10, and confirm on November 12 using B: save B under the actual date selected, preserve the October record with A, and complete the pending November treatment without automatically copying medication details.
- After confirmation, decline scheduling another treatment: the completed record remains saved and no next treatment is scheduled. Accept the offer: manually enter a next due date.
- Save a manually selected next due date, change it later, and verify that pending reminders reflect the change.
- Receive the treatment reminder at 8 AM on the due date; postpone once to 4 PM that day without confirming administration. A second postponement is unavailable.
- Save a first treatment with its required next due date and verify its reminders are scheduled; the optional scheduling question appears only after confirming a pending treatment.
- With notifications disabled, see welcome/save warnings and successfully save a treatment anyway.
- In labeled demo mode, observe accelerated reminders, confirmation, and an overdue case. Real treatment data remains distinguishable.
- Enter demo mode through settings from any screen. Change sample records without changing real records; exit and verify demo reminders are cancelled while real records and reminders remain intact.
- Close and reopen the app and still find saved pet and treatment records.
- Welcome title and image can be changed and the enter button leads into the app.

## Product Decisions

- Track multiple pets; the demonstration may use only one.
- Track different flea product names; other medication categories remain deferred.
- Show last administration and next chosen date, without a separate expiration date.
- Manually choose the next due date when scheduling another treatment and allow editing later. Offer scheduling after confirmation, with a "Not now" option that leaves no next treatment scheduled.
- Confirm actual administration with today's date as a convenience default and allow backdating.
- Start treatment details blank, with explicit copying from the previous record available.
- Include one optional treatment photo.
- Use a four-day purchase lead and next-day overdue follow-up for the POC instead of custom timing per medication.
- Treatment reminder at 8 AM on the due date, with a single postponement to 4 PM the same day.
- Purchase and next-day overdue reminders both arrive at noon on their respective days.
- Save even when notifications are disabled, with warnings.
- Use earthy browns/sands and green accents.
- Review in Markdown.
- Settings gear available everywhere; its demo switch enters separate sample data, preserves real records/reminders, and cancels demo reminders when switched off.

## Approved Review Defaults

These defaults were proposed during Markdown review and accepted with final PRD approval, alongside the explicit medication-change clarification.

- **Name and appearance:** use "Pet Care" as the initial in-app title; allow customization. Defer light/dark switching. Pet photos are optional, as are date of birth, owner, and breed; only pet name is required. All image selection uses existing phone images, with one optional image per pet/treatment and a replaceable welcome image.
- **Treatment actions:** the medication area lets the owner select/add a pet and add its first record. The full treatment detail provides a "Record treatment" action for the pending dose. It opens the blank medication/dose/source fields, optional copy action and photo, and administration-date picker. Saving confirms that pending dose, then asks whether to schedule another. No copying occurs automatically.
- **Form rules:** require medication name and dose as entered text; purchase source and photo are optional. Do not calculate or interpret doses. Administration date cannot be in the future, and a chosen next due date must be later than that administration date. Pet birth date, if supplied, cannot be in the future. First entry requires a next due date; post-confirmation scheduling is optional as agreed.
- **Empty/error states:** with no pets, show "Add pet"; with no treatments, offer the first record. With no next treatment, show "No treatment scheduled" and provide a scheduling action. Invalid fields show an explanation and preserve entries. A failed save does not claim success; keep entries for retry. Cancelling or failing image selection leaves the form usable without a new photo.
- **Home cards:** show all pets' next scheduled flea treatments, ordered by next due date, including overdue ones so none disappear at a month boundary. Pets without a next date show "No treatment scheduled." Mark overdue cards clearly. Saved records and settings persist after reopening. Historical administration records remain available through treatment detail; no separate history browser is added.
- **Reminder edge cases:** use the phone's local time for the agreed schedule. If a reminder time has already passed when a schedule is created/edited, skip that elapsed alert rather than sending a burst of old alerts; show overdue status in the app and retain future alerts. Provide one next-day overdue alert only. Offer postponement only before 4 PM on the due day; after that, do not offer a postponement into the past. Confirmation also cancels any outstanding postponed alert for that dose. Editing the next date replaces outstanding alerts for the prior date.
- **Demo and navigation:** starting demo mode resets its samples, allowing the same walkthrough to be repeated. Home, medication/pets, and settings remain reachable; drawer entries for vaccinations, appointments, and providers are visibly unavailable and labeled "Later." No settings change requires leaving demo records exposed as real records.

## What We're Building

The approved flea-treatment loop, reminders, administration confirmation, and accelerated demo, plus approved interview additions: welcome customization, drawer navigation, pet creation with optional photo, optional treatment photo, and explicit copying of previous treatment details. Confirmation records what was actually administered without changing prior records.

These additions expand the original minimum-pet-identification boundary. The scope has been synchronized with final PRD approval.

The added surfaces and images increase work beyond the initial sketch. The 2–4-hour target is a planning constraint, not a verified estimate. `4-spec` must assess feasibility, especially reminders and demo timing; any cuts require learner agreement.

## Deferred From the POC

Vaccination tracking, appointment calendar, grooming and vet visits, provider directory, and other medication categories remain deferred to protect the flea-treatment loop. Per-medication reminder timing, a guided onboarding tutorial, and light/dark switching are also deferred. Camera capture, multiple attachments, and image editing are not included in the agreed single-photo addition.

## Non-Goals

No automatic treatment interval, coverage, or dose calculation. No separate treatment-expiration date. No requirement for a polished application or prize competition. Account, sharing, and synchronization features have not been requested and are outside this draft.

## Open Questions

No blocking product questions remain following approval. The medication-change clarification is incorporated in Administration Confirmation and Acceptance Criteria.

For `4-spec`: reliable Android scheduling for real reminders and the proposed accelerated demo, image selection/storage, and feasibility of the expanded POC. Technical findings may require a learner-approved revision. No implementation mechanism or exact delivery guarantee is established by this PRD.
