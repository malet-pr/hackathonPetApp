# Pet Care

A local native Android learning project for the Build With AI: Basics hackathon.
Kotlin and Jetpack Compose, with an earthy sand, brown, and green palette.

## Current build

Steps 1–3 implement the welcome screen, persistent custom title, pet creation,
and the first flea-treatment record with an optional manually chosen next date, which can be added or edited later. Home cards
and full details survive reopening. Phone reminders use Android's inexact alarms.
Treatment confirmation preserves previous medication records; reminders can be
postponed once until 4 PM on the due day. Photos and accelerated demo mode belong
to later build steps.

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
are available in step 3. Notification taps open the corresponding pet's detail;
Record treatment opens the form, and Postpone rechecks eligibility.

Debug builds offer **Settings → Development check → Test background reminder
(20 seconds)**. Enable notifications, tap it, and press the phone's Home button.
Watch for **Diagnostic — background reminder**. This uses the same real alarm API
but does not change treatment dates or create treatment records. It is a development
test, separate from the accelerated sample mode planned for step 5. Inexact delivery
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

## Project guide

- `app/src/main/java/com/example/petcare/MainActivity.kt`: Android entry point.
- `ui/AppShell.kt`: Compose navigation and shared screen frame.
- `ui/PetCareViewModel.kt`: UI state and save coordination.
- `data/SettingsStore.kt`: DataStore title persistence.
- `data/PetCareDatabase.kt`: Room entities and queries; exported schema in `app/schemas`.
- `data/TreatmentRepository.kt`: validated, atomic record saves and next-date creation/editing.
- `reminders/ReminderRules.kt`: local dates, due-time rules, and late-delivery eligibility.
- `reminders/AlarmScheduler.kt`: real inexact alarms and idempotent reconciliation.
- `reminders/ReminderReceiver.kt`: checked background delivery.
- `ui/TreatmentScreens.kt`: actual-date/next-date pickers and treatment details.
- `ui/theme/Theme.kt`: approved colors.
- `devpost/`: approved scope, PRD, technical spec, and build checklist.

The learner profile and local SDK/credential files are excluded from Git.
