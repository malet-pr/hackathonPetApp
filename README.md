# Pet Care

A local native Android learning project for the Build With AI: Basics hackathon.
Kotlin and Jetpack Compose, with an earthy sand, brown, and green palette.

## Current build

The app implements the welcome screen, persistent custom title and image, pet creation,
and the first flea-treatment record with an optional manually chosen next date, which can be added or edited later. Home cards
and full details survive reopening. Phone reminders use Android's inexact alarms.
Treatment confirmation preserves previous medication records; reminders can be
postponed once until 4 PM on the due day. Optional pet details and local photos are
available. A separate accelerated Demo session lets you practice the full loop without
editing real pet records or real alarm requests.

## Open and run

1. Open this repository root in Android Studio and let Gradle sync.
2. Set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**
   to the bundled JetBrains Runtime 25.
3. Start an Android emulator (API 26 or later; API 37 used for initial verification).
4. Select the `app` configuration and press Run.

For terminal builds, set `JAVA_HOME` to your Android Studio `jbr` directory and
`ANDROID_HOME` to your SDK directory, or set `sdk.dir` in an ignored
`local.properties` file. Then run:

```sh
./gradlew :app:assembleDebug :app:testVerificationUnitTest :app:lintDebug
./gradlew :app:connectedVerificationAndroidTest
```

Keep Android Studio's build variant on **debug** for your normal app. Automated
tests target a separate **verification** variant/application ID, so test-install
cleanup cannot erase your debug app records. These tests do not open the app UI.

The first build needs network access for pinned dependencies. The Gradle wrapper
verifies the distribution checksum. JVM bytecode targets Java 17; the build runs
on JBR 25. No Java 17 runtime is bundled with the app.

## Try step 1

Use the gear to change the welcome title. Blank names and names over 60 characters
are rejected. Save, close and reopen the app: the title should remain. Enter opens
Home; its menu opens a left drawer. Medication opens pet treatment records; other
sections are labeled Later. The launcher label remains Pet Care. Your title is stored only
on this device; uninstalling or clearing app data removes it.

## Try step 2

Enter, add a pet, and accept the invitation to record its first treatment. Medication
and dose are required; source is optional. Choose the actual date given (today or
earlier). The next date is optional; if supplied, it must be strictly after the actual date. Saving without one shows “No next treatment selected; no reminders scheduled.” Use Schedule next treatment or Edit next date in detail to add/change a date later. No interval or dose is
calculated. Tap a home card to see the full prior record.

The welcome screen and treatment form warn when notifications are disabled. Saving
still works. Enable reminders through the contextual prompt or system notification
settings. Reminder requests use local device time: purchase at noon four days
before, treatment at 8 AM, and one overdue reminder at noon the next day. Elapsed
request times are skipped; Android may delay delivery. Confirmation/postponement
are available through the confirmation flow. Notification taps open the corresponding pet's detail;
Record treatment opens the form, and Postpone rechecks eligibility.

Debug builds offer **Settings → Development check → Test background reminder
(20 seconds)**. Enable notifications, tap it, and press the phone's Home button.
Watch for **Diagnostic — background reminder**. This uses the same real alarm API
but does not change treatment dates or create treatment records. It is a development
test, separate from accelerated Demo mode. Inexact delivery
may take longer than 20 seconds. The diagnostic receiver is absent from release builds.

Force-stop, power-off and clearing storage prevent normal delivery. Opening again
reconciles saved plans; boot, clock/time-zone changes and app updates also trigger
recovery. Disabling notifications does not replay missed alerts later. Alarm
scheduling failure keeps the saved record and offers a retry.

## Try step 3

Open a pet with a next treatment and tap **Record treatment**. Medication, dose and
source start blank; **Copy from last treatment** fills those three fields explicitly,
leaving dates unchanged and all copied fields editable. Choose the actual administration
date (today or an earlier date no earlier than the previous treatment), enter what was
given, and save. Earlier records remain visible. Remaining reminders for the completed
plan are cancelled. Choose **Not now** in the next-treatment question, or **Yes** to
schedule another date. Cancelling scheduling never undoes the saved administration.

**Postpone until 4 PM** is available once before 4 PM on the due day. It does not
record a dose. Both notification actions and in-app actions use the same checked
operations; stale reminders cannot confirm a dose or replenish postponement. The
phone's local date/time governs availability. Android can still deliver reminders late.

## Try step 4

Add a pet with only a name, or supply an optional birth date, owner, breed and
photo. Birth dates cannot be in the future. Open its full record and use **Edit pet
details** to change those fields later. Choose an existing phone image for an
administered treatment; it appears in that record and has a larger view. The
**Copy from last treatment** action leaves photos blank.

Use the gear on any screen to choose a welcome image. Save changes, close and
reopen the app, and check that the welcome image, pet photo and treatment photo
remain. Cancel a picker or the form to keep the previous saved image. Removing
the welcome image and saving restores the default illustration. Selected images
are copied to private app storage; the app never deletes the phone's original.
Images are limited to 10 MB and displayed as reduced-size previews. No gallery
or camera permission is requested. Local records, settings and photos are
excluded from Android backup/transfer; uninstalling or clearing storage removes
them.

## Try step 5: accelerated Demo

Enable phone notifications, then open the gear and turn on **Demo mode**. A fresh
fictional Demo Fido appears with a previous Medication A record and a chosen next
date four sample days ahead. A visible **DEMO** banner shows simulated date/time.
One sample day passes per real minute while the app is visible; leaving the app
pauses this clock. Demo data is temporary and restarts from the same opening point
each time you choose **Reset Demo samples**.

The purchase notification appears shortly after entering Demo. Leave the sample
unconfirmed to see the treatment reminder at about 3 minutes 53 seconds and the
overdue reminder at about 5 minutes. At treatment time, **Postpone until 4 PM**
moves that sample reminder about 20 real seconds. Android's notifications are
clearly titled Demo. You can instead record a treatment with a different medication,
choose the actual administration date, and verify that the old sample record stays
in history while the remaining sample alerts clear. Reset to try the overdue path
after a confirmation path, or the reverse.

In the emulator, drag down from the top edge to open the notification shade.
Click the notification title or message to open the pet's record. The small arrow
expands the notification to show **Record treatment** and, during the due morning,
**Postpone until 4 PM**. At this accelerated speed, the 8 AM–4 PM postponement
window lasts only 20 real seconds, so have the shade ready near treatment time.

For a demo video, record the full purchase-to-overdue path continuously on the
emulator, then trim the waiting into a short video with visible edits. Use a fresh
Demo session to show confirmation and medication change. These foreground sample
notifications demonstrate the care flow; the debug diagnostic and real inexact
alarms are the separate background-delivery checks. The clock does not advance
while the app is hidden, so keep it visible during the five-minute run.

Turn Demo mode off in Settings to return to your real records. Reset and exit
remove sample notifications and sample photos. Real reminders continue to use
the real clock and may arrive while Demo is active; opening one returns to real
mode. Closing and reopening the app starts in real mode. Notification permission
can still be denied: Demo records and clock work, but phone notifications do not.

## Project guide

- `app/src/main/java/com/example/petcare/MainActivity.kt`: Android entry point.
- `ui/AppShell.kt`: Compose navigation and shared screen frame.
- `ui/PetCareViewModel.kt`: UI state and save coordination.
- `data/SettingsStore.kt`: DataStore title and welcome-image persistence.
- `data/PhotoStore.kt`: private image import and bounded decoding.
- `demo/AcceleratedClock.kt`, `demo/DemoSession.kt`, `demo/DemoEventPump.kt`: temporary sample clock, records and notification crossings.
- `data/PetCareDatabase.kt`: Room entities and queries; exported schema in `app/schemas`.
- `data/TreatmentRepository.kt`: validated, atomic record saves and next-date creation/editing.
- `reminders/ReminderRules.kt`: local dates, due-time rules, and late-delivery eligibility.
- `reminders/AlarmScheduler.kt`: real inexact alarms and idempotent reconciliation.
- `reminders/ReminderReceiver.kt`: checked background delivery.
- `ui/TreatmentScreens.kt`: actual-date/next-date pickers and treatment details.
- `ui/theme/Theme.kt`: approved colors.
- `devpost/`: approved scope, PRD, technical spec, and build checklist.

The learner profile and local SDK/credential files are excluded from Git.
