---
doc: spec
status: approved
---

# Pet Care — Technical Spec

## How This Works, In Plain Language

The app runs on Android with screens written in Kotlin using Jetpack Compose. Room keeps pets, completed administrations, and planned treatments in a local database. DataStore saves the welcome title and image choice; selected images are copied into the app's own files so records still work after reopening. No account, server, or synchronization is needed.

A completed administration records what actually happened. A planned treatment records a due date and reminder progress. Confirming November's treatment creates a new administration with the actual medication and date, completes the pending plan, and leaves October's record untouched. The app then offers to schedule another treatment.

For real reminders, the app asks Android to wake it near the requested times, even when the app is closed. Android may deliver later to save battery; the learner accepts that for flea treatments. For the demo, a separate accelerated clock runs while the app is visible and posts real notifications labeled Demo. One simulated day passes per real minute; that demonstration is not evidence of background timing reliability.

## The Core Journey Through the System

Implements `prd.md > The Core Journey`, `Treatment Detail and Entry`, and `Administration Confirmation`.

1. On launch, read saved settings and real database records. Show the welcome image/title, notification warning if needed, and enter button.
2. The owner adds a pet. Validate its fields, import an optional photo, and save the pet. Offer its first treatment and explain the drawer briefly.
3. First treatment entry requires medication, dose, and actual date; next due date is optional. Copying previous details is unavailable without a previous administration. Save the administration and optional next plan together, then request future reminders only for a plan. Without a date, save no plan and show “No next treatment selected; no reminders scheduled.”
4. Home cards observe database changes and show the latest administered date plus the next planned date, including overdue plans and pets with no next treatment. Tapping a card opens that pet's treatment detail; prior records remain accessible within that detail.
5. An Android alarm arrives with a plan ID, revision, and reminder kind. Reload the plan and ignore stale, completed, or already handled requests. If notifications are allowed, show the pet-specific reminder.
6. Record treatment opens blank medication/dose/source fields, optional copy/photo, and a date picker defaulting to today. Save a new administration and complete the plan in one database transaction. Cancel all remaining alerts for that plan and remove its displayed notifications.
7. Ask whether to schedule another. No leaves the administration saved with no pending plan; Yes opens manual next-date entry. Cancelling that optional prompt has the same effect as No. If a subsequent schedule save fails, the completed administration remains saved and scheduling can be retried.
8. Editing a next date replaces its future alarm requests. One postponement moves the treatment reminder to 4 PM on the same due day without confirming it.
9. Demo mode swaps the visible records and clock to isolated sample data. Real alarms keep using the real database and real clock. Exiting cancels sample notifications and returns to real records.

## Stack

Learner-agreed choices: fresh Kotlin/Compose app; Room, DataStore, and local photo files; flexible real alarms; foreground accelerated demo; separate administrations and plans. The derived implementation details below were accepted with final technical-spec approval.

| Piece | Planned choice | Documentation |
|---|---|---|
| Project | One Android app module, Empty Activity, Kotlin Gradle scripts | [Project setup](https://developer.android.com/studio/projects/create-project) |
| Build | AGP 9.4.0, Gradle wrapper 9.6.0; validate the pair with an empty build first | [AGP compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes) |
| Kotlin/Compose | AGP built-in Kotlin plus matching Compose compiler plugin; stable Compose BOM and Material 3 | [Built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin), [Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [BOM](https://developer.android.com/develop/ui/compose/bom) |
| Screens | Activity Compose, Lifecycle ViewModel, coroutine Flow; small typed route stack without a navigation framework | [Activity](https://developer.android.com/jetpack/androidx/releases/activity), [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel), [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) |
| Database | Room 2.8.5 with KSP code generation | [Room releases](https://developer.android.com/jetpack/androidx/releases/room), [KSP](https://kotlinlang.org/docs/ksp-quickstart.html) |
| Settings | Preferences DataStore 1.2.1 | [DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore) |
| Photos | Activity photo picker and bounded Bitmap decoding of app-owned files | [Photo picker](https://developer.android.com/training/data-storage/shared/photo-picker), [Storage](https://developer.android.com/training/data-storage/app-specific), [Bitmap decoding](https://developer.android.com/topic/performance/graphics/load-bitmap) |
| Real alerts | Android AlarmManager, BroadcastReceiver, NotificationCompat | [Alarm scheduling](https://developer.android.com/develop/background-work/services/alarms), [Notifications](https://developer.android.com/develop/ui/views/notifications/build-notification) |
| Checks | JUnit for rules, Room instrumentation for real persistence, Compose UI checks for the core flow | [Android testing](https://developer.android.com/training/testing), [Compose tests](https://developer.android.com/develop/ui/compose/testing) |

Approved baseline: minimum Android 8/API 26, compile/target API 37 (installed base platform 37.0), Java/Kotlin bytecode target 17. Use Android Studio's bundled JBR 25 for Gradle; Gradle supports running on Java 25 from 9.1.0. The app's minimum supported Android version is separate from the JDK used on the computer. [JDK guidance](https://developer.android.com/build/jdks), [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html).

### Build JDK Versus Bytecode Target

The learner raised Java 17 support lifetime as a concern. Target 17 is only the compiler output format; this plan does not install, run builds on, or bundle a Java 17 JDK. Gradle runs on Studio's JBR 25, and Android build tools convert JVM bytecode to DEX for Android's runtime. Security updates and maintenance apply to the actual build JDK, Android tools, dependencies, and supported device OS; the output format itself is not a separately installed runtime requiring a Java 17 support contract. Keep the build JDK updated with Studio and verify its compatibility with the pinned Gradle version.

The reason to retain output target 17 is Android toolchain compatibility, not a need to depend on an old JDK distribution. Raising the output target is possible only after checking Kotlin/AGP/D8 support; it is a separate choice from using a maintained JDK for builds. The target was accepted with final spec approval, including the build-JDK versus output-format clarification. [Android's explanation of JDK and compilation targets](https://developer.android.com/build/jdks).

Slice 1 resolved baseline (2026-10-11): AGP 9.4.0, checksum-pinned Gradle 9.6.0, AGP built-in Kotlin 2.2.10 and matching Compose compiler plugin 2.2.10, Compose BOM 2026.09.00, Activity 1.13.0, Lifecycle 2.11.0, Core 1.19.1, DataStore 1.2.1, coroutines 1.11.0. JUnit 4.13.2, AndroidX test runner 1.7.0 and ext.junit 1.3.0 support the storage tests. These versions are pinned in `gradle/libs.versions.toml`. Slice 2 pins Room 2.8.5 and KSP 2.3.12; exported schema version 1 is retained under app/schemas. No separate Kotlin Android plugin is applied. See `build-step-1.md` for verification outcomes and device details.

## Where It Runs and How Someone Tries It

Project root: `/home/nuria/PROJECTS/DevpostLearn/PetApp`. All hackathon documents remain under its `devpost/` directory. Create the app at this root during 5-build, preserving existing documents and curriculum files. No project generation, dependency install, or app code has been done during this spec stage.

Planned identifiers: display title Pet Care, application ID/namespace `com.example.petcare`, launcher `MainActivity`. The custom welcome title does not rename the installed launcher entry.

Read-only checks found Android Studio at `/home/nuria/Applications/android-studio`, SDK at `/home/nuria/Android/Sdk`, platform-tools and emulator binaries, API 37.0/37.1 platforms, and Build Tools 36.0.0. Shell Java is 25.0.4 and Studio JBR is 25.0.3. The learner created and started a Pixel 10 AVD during slice 1. ADB verified emulator-5554 running Android 17/API 37 and boot completed. The physical phone's Android version/connection remains to be checked.

After the first build creates the Gradle wrapper, run from the project root:

```bash
export JAVA_HOME=/home/nuria/Applications/android-studio/jbr
export ANDROID_HOME=/home/nuria/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$JAVA_HOME/bin:$PATH"
./gradlew --version
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The exports apply only to the current terminal. Align Android Studio's Gradle JDK with the same runtime. Initial dependency downloads and missing SDK/device setup may require network access; this is not a claim that the project builds offline before dependencies are cached.

In Android Studio, open this project root, use Device Manager to select/create an API 26+ phone AVD (prefer API 33+ to exercise notification permission), start it, select the app configuration, and press Run. Grant notifications when prompted. With exactly one connected test device, the command-line alternative is:

```bash
./gradlew :app:installDebug
adb shell am start -n com.example.petcare/.MainActivity
./gradlew :app:connectedDebugAndroidTest
```

With multiple connected devices, select one explicitly using Android Studio or `ANDROID_SERIAL`; do not install indiscriminately. Test the physical phone after enabling its developer/USB debugging settings and checking its Android version. Avoid showing personal notifications in the recording.

For the required demo video, use the emulator's Record screen control and labeled demo mode. The four-day purchase lead means purchase-to-overdue takes roughly five real minutes at the agreed ratio; record that full flow and trim waiting into a short, clearly edited video rather than changing the speed silently. Capture confirmation and medication change in a fresh demo session if necessary. [Emulator recording](https://developer.android.com/studio/run/emulator-record-screen).

Submission requires the short demo video and a public GitHub repository. A local native app plus recording is sufficient; no web deployment, Play Store publishing, or paid service is required by this plan. Document reproducible build/run steps in README; publishing remains a later shipping step.

## Look and Feel

Implements `prd.md > Look and Feel`, `Welcome and Settings`, and `Home and Navigation Drawer`.

Use Material 3 controls with an explicit light earthy palette, keeping dynamic device colors off: sand background, warm cream surfaces, dark brown text, and muted green primary actions. During spec review, the learner supplied the following palette, accepted with final spec approval and replacing the smaller initial token proposal:

| Role | Color | Purpose |
|---|---|---|
| background | `#F3EBDD` | App background |
| surface | `#FFF8EF` | Cards, sheets, dialogs |
| surfaceVariant | `#E8DDCD` | Field containers, subtle grouping |
| onBackground | `#34281F` | Text on background |
| onSurface | `#34281F` | Text on surfaces |
| primary | `#426044` | Main actions |
| onPrimary | `#FFFFFF` | Text on primary |
| primaryContainer | `#D7E3D2` | Selected chips, lighter green areas |
| onPrimaryContainer | `#223324` | Text on light green areas |
| outline | `#8B7C6E` | Borders and outlines |
| error | `#B24A3F` | Validation and destructive states |
| onError | `#FFFFFF` | Text on error |

Use these as the starting theme tokens and check contrast and legibility on the actual controls during the build; the supplied palette has not yet been visually verified. Map Material 3's additional surface/container roles consistently to this palette so controls do not fall back to unrelated default colors. Use system typography, accessible touch targets, recognizable settings/menu icons, and clear status text rather than color alone.

Provide a local default pet illustration and image placeholders without adding an image-generation service. Display the welcome title above a large centered image. Use a shared screen shell so settings is always available, a left drawer inside the app, and home reminder cards. Keep deferred feature entries disabled and visibly labeled Later. Light/dark switching stays deferred.

## Components

### App Shell and Screen Model

Implements `prd.md > Screens and Layout`.

One activity and one app-level ViewModel coordinate Welcome, Home, Pets, PetForm, TreatmentDetail, TreatmentForm, and ScheduleForm routes. Settings is an overlay reachable from all routes; the drawer links home and medication/pets. A small typed back stack and Android BackHandler handle Back; no separate navigation dependency is needed for this POC. Keep IDs, rather than whole database entities, in routes. Opening a real notification selects real mode and its real treatment detail so it cannot accidentally act on samples.

ViewModel holds form drafts across rotation and settings overlays. Prevent duplicate saves while a request is running. Back/cancel discards the unsaved form; saved records and settings persist across restart. Do not imply unsaved forms survive process death. Settings/demo switching clears route/form IDs that belong to the previous mode. SavedStateHandle may preserve small route identifiers; it must not save photos or whole records.

### Local Database and Treatment Repository

Implements `prd.md > Administration Confirmation`, `Treatment Detail and Entry`, and `States and Boundaries`.

The repository is the single writer for pets, administrations, and plans. DAO queries emit Flow so screens reflect committed changes. Validate fields before writes and enforce plan ownership. First entry inserts its administration and, if a date was supplied, its next plan in one Room transaction. No next date means no plan row. Later scheduling uses the most recent administration; editing validates the pending plan ID/revision, increments its revision only when the date changes, clears handled flags, preserves postponedOnce, and leaves administration history untouched. Reconciliation cancels old alarm identities and displayed notifications after a date change. Confirmation inserts a new administration and marks the matching plan completed atomically; never update an older medication record. Repeated taps/duplicate requests for an already completed plan must not insert another administration.

Only one pending flea-treatment plan per pet in this POC. Scheduling a successor is a separate action after confirmation; its failure cannot roll back the already saved administration. Preserve old administrations and completed plans. Treatment detail lists prior administrations within the same surface, with expandable details/photo, while the main card shows the latest administered date. No separate history browsing screen is added.

### Settings and Photo Storage

Implements `prd.md > Welcome and Settings`, `Pet Record`, and `Treatment Detail and Entry`.

Preferences DataStore stores custom welcome title and image path. Keep demo mode/session transient so a cold restart safely opens real mode. Settings persist; sample data does not need to persist.

Use `PickVisualMedia` with ImageOnly and its document-selection fallback. Copy the selected stream on an IO dispatcher into a generated private filename before referencing it from a saved record. Keep form imports staged until save; release unreferenced staged files on cancel/failure. Replace a saved image reference only after successful import and database/settings update; never delete the original user-selected file. Use bounded thumbnail decoding for display, and retain the selected image copy for a larger detail view. Failed import leaves the previous saved photo intact and lets the form be saved without a new photo. No broad gallery permission, camera capture, cloud upload, or external image-loading library is needed.

Disable automatic app backup/transfer of the local database, settings, and files in the POC manifest/backup rules so local-only is explicit. Uninstall/clear-storage removes data; export/restore and device migration remain deferred. Photo picker providers can be cloud-backed, but the app itself makes no photo-upload request.

### Reminder Rules

Implements `prd.md > Reminder Schedule and Due-Date Changes` and `Approved Review Defaults`.

A pure Kotlin rules function maps a pending plan and its clock/zone to purchase at due-date minus four days at noon, treatment at 8 AM on the due day, and overdue at noon the following day. A postponed treatment uses 4 PM instead. Determine overdue status after the due day has passed without confirmation, not when a notification happened to arrive. Persist the date as a calendar date and derive real alarm instants in the current local zone; recompute on time-zone/time changes. Material date picker UTC values must be converted to calendar dates without shifting the user's selected day.

Skip elapsed reminder times on creation/edit/recovery rather than burst old alerts. A queued inexact alarm delivered late is not the same as newly scheduling an elapsed alert: evaluate its still-current eligibility, suppress purchase reminders after the due day begins, and suppress treatment/postponement alerts once the due day has passed. Delayed next-day overdue delivery is allowed while the plan remains pending. No old alert may appear after confirmation. Disabled notifications must not cause indefinite retry or a later burst.

Postpone is available only once, before 4 PM on the due day, and does not confirm. Persist the one-use flag. Editing the due date increments its revision and resets reminder delivery tracking for that date, but does not replenish the one-use postponement for the same plan; a new successor plan gets its own allowance.

### Real Alarm Scheduler and Recovery

Implements `prd.md > Reminder Schedule and Due-Date Changes` and `Notifications Disabled`.

Use one-shot `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, ...)` requests with explicit immutable PendingIntents. This is the learner-approved inexact approach: requested times are guidelines and battery-saving delays are accepted. Do not request exact-alarm access for this POC. See the future option below.

Identity includes real mode, plan ID, revision, and kind; notification identity includes real mode, plan ID, and kind. Use unique intent data, not extras alone, so pets and alert kinds cannot overwrite each other. Cancellation retrieves matching PendingIntent identities and removes pending alarms plus displayed notifications. An alarm receiver uses `goAsync` for bounded database access and always finishes. Serialize reminder delivery with confirmation/edit operations, recheck the plan's revision/status and notification permission just before posting, and use stable notification IDs to replace duplicate delivery rather than multiply cards.

The database is authoritative because OS alarm operations cannot share its transaction. Store a durable needs-reconciliation marker before alarm changes. After successful changes, clear it. Reconcile idempotently after writes, at launch, and on BOOT_COMPLETED, time/time-zone changes, and app replacement; cancel stale identities, then schedule only still-future eligible events. Generate old identities from the plan ID, all its earlier revisions, and each reminder kind so cleanup does not depend on Android exposing an alarm inventory. Completed plans stay available for cancellation cleanup. A crash between commit and scheduling is recovered on the next reconciliation. An OS scheduling failure keeps the saved record and shows a retry/status message; never report the whole database save as failed after it committed.

Register only necessary manifest receivers and exported flags. Boot recovery uses RECEIVE_BOOT_COMPLETED; the POC assumes normal device unlock and has no direct-boot database. Force-stopping the app, switching the device off, or clearing its storage limits delivery; opening again reconciles surviving saved plans. Cancellation must remain correct even when an old alarm was already queued. Persist which reminder kinds have been handled to avoid re-posting them during reconciliation; do not claim exactly-once notification delivery across a crash at the database/OS boundary.

### Notification Delivery and Permission UI

Implements `prd.md > Notifications Disabled` and `Administration Confirmation`.

Create distinct real and demo notification channels on API 26+, with stable real/demo tags. Request POST_NOTIFICATIONS on API 33+ through a contextual explanation and system prompt; denial keeps records usable. Check app and relevant channel availability when returning to the app and before showing save warnings. Settings offers a link to system notification settings.

A notification opens its matching pet/plan detail. The Record treatment action opens the form rather than confirming immediately; notification dismissal never counts as administration. Offer Postpone only when the stored rules permit it; route the notification action through the same checked repository operation. Confirmation cancels all remaining plan alarms and its displayed notifications, including postponed ones. Keep actual medication fields blank unless the owner invokes Copy from last treatment.

### Accelerated Demo Clock and Session

Implements `prd.md > Accelerated Demo Mode`.

Use a separate in-memory Room database with the same schema/repository rules and a separate temporary photo namespace. Always inject the real database and real clock into OS alarm/boot receivers, regardless of what is visible. Seed each demo entry with a fictional pet, prior medication A administration, and a pending plan. Never seed into the real database or disable real alarms when demo starts.

For the demo, derive simulated time from accumulated foreground elapsed time at a factor of 1440: one real second advances 24 simulated minutes. Pause when the app is no longer visible and resume without advancing through that absence; display Demo, the simulated date/time, and the foreground-only timing rule. Both form date defaults/validation and reminder rules use that same simulated clock. Keep selected administration dates editable in demo too.

Start the sample clock just before the purchase reminder on the simulated day four days before the due date. A coroutine checks crossings while the app is visible and sends real Demo notifications through the shared delivery formatting/rules, without scheduling sample OS alarms. Simulated 8 AM-to-4 PM postponement lasts 20 real seconds. Purchase-to-overdue spans about five real minutes; the short video can use clearly edited excerpts. Track handled events so returning from settings/rotation does not replay a reminder.

Tapping a sample notification resumes the active sample session only; ignore stale session IDs after exit. Exit stops the coroutine, cancels only demo-tagged notifications, clears samples/temp files, and restores real-mode routes. Cold start uses real mode and clears stale demo notifications. This demo verifies rules, display, and interaction, while separate real-alarm tests verify background delivery.

## Data Model

Implements `prd.md > Administration Confirmation`, `Pet Record`, and `States and Boundaries`. Dates use ISO local-date text; instants use epoch milliseconds. Real records live in one private Room database; demo records use a separate in-memory instance.

| Record | Fields and relationships | Update and persistence |
|---|---|---|
| Pet | ID; required name; optional birthDate, owner, breed, photoPath | Saved locally; retained between sessions; never shared automatically |
| Administration | ID, petId FK; actualDate, medicationName, doseText; optional purchaseSource/photoPath; recordedAt; optional completedPlanId FK with unique index | Append on first entry or confirmation; original records remain unchanged; recordedAt is audit time, never substituted for actualDate |
| TreatmentPlan | ID, petId FK; sourceAdministrationId FK; dueDate; status PENDING/COMPLETED; revision; postponedOnce; reminder-handled flags; needsReconciliation | First entry or successor scheduling creates it; due edits revise it; confirmation completes it; at most one pending plan per pet enforced transactionally |
| Settings | welcomeTitle, welcomeImagePath in Preferences DataStore | Updated from settings; persisted separately from records |
| UI/session | current route, unsaved form, saving/error state, demo-session ID and simulated clock | ViewModel/in-memory; no claim of unsaved form persistence after process death |

Use sourceAdministrationId to relate a pending plan to the last treatment, without storing its product as a promise about the next dose. The newly confirmed administration has a unique completedPlanId to prevent double confirmation. Completed plans retain their due date so the app can distinguish planned and actual dates. Copy looks up the same pet's latest administration by actualDate, then recordedAt/ID for ties, and copies only medication/dose/source.

Enforce the approved form rules: trim/require pet name, medication, and dose text; optional source/photo; no future birth/administration dates; next due date strictly after its associated administration date. First entry can omit the next date, and post-confirmation scheduling can be declined. A selected next date must be valid; no null-date plan is created. A no-date save shows the no-reminders message and leaves a Schedule next treatment action available in detail. Before confirmation, show explicit validation if a selected actual date precedes the plan's source administration, so history is not silently made inconsistent; this safeguard was accepted with final spec approval.

## File Structure

Planned files, not existing app code. Keep one app module and group related database classes together; split further only when the build makes it useful.

```text
PetApp/
├── devpost/
│   ├── learner-profile.md           # Personal learning context, ignored by Git
│   ├── scope.md                     # Approved scope
│   ├── prd.md                       # Approved product behavior
│   └── spec.md                      # This draft; checklist comes in 5-build
├── .gitignore                       # Preserve profile/credential rules; add build/local files
├── README.md                        # Build, run, reminders, demo, local-data limits
├── settings.gradle.kts              # One app module and repositories
├── build.gradle.kts                 # Plugin declarations
├── gradle.properties                # Portable project build configuration
├── gradlew / gradlew.bat             # Reproducible wrapper entry points
├── gradle/
│   ├── libs.versions.toml           # Exact compatible dependency/plugin pins
│   └── wrapper/                     # Wrapper properties and generated wrapper JAR
└── app/
    ├── build.gradle.kts             # SDK targets, Compose, Room/KSP, tests
    ├── schemas/                     # Exported Room schema for future migrations
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml  # Launcher, permission, receivers, backup policy
        │   ├── java/com/example/petcare/
        │   │   ├── PetCareApp.kt    # Application and simple dependency container
        │   │   ├── MainActivity.kt  # Compose host, activity results and intents
        │   │   ├── data/
        │   │   │   ├── PetCareDatabase.kt  # Entities, DAO, Room database
        │   │   │   ├── TreatmentRepository.kt # Transactions, invariants, queries
        │   │   │   ├── SettingsStore.kt      # Preferences DataStore
        │   │   │   └── PhotoStore.kt         # Private imports and bounded decoding
        │   │   ├── reminders/
        │   │   │   ├── ReminderRules.kt      # Dates, eligibility, postponement
        │   │   │   ├── AlarmScheduler.kt     # Real inexact alarms and reconcile
        │   │   │   ├── ReminderReceiver.kt   # Checked delivery/action handler
        │   │   │   ├── RecoveryReceiver.kt   # Boot, time changes, app replacement
        │   │   │   └── NotificationHelper.kt # Channels, IDs, permissions, actions
        │   │   ├── demo/
        │   │   │   ├── DemoSession.kt        # Isolated Room instance and sample IDs
        │   │   │   └── DemoClock.kt          # Foreground accelerated elapsed time
        │   │   └── ui/
        │   │       ├── PetCareViewModel.kt   # Screen data, form operations, mode
        │   │       ├── AppShell.kt           # Typed routes, drawer, settings gear
        │   │       ├── WelcomeScreen.kt
        │   │       ├── HomeScreen.kt
        │   │       ├── PetScreens.kt         # Pet list and creation
        │   │       ├── TreatmentScreens.kt   # Detail, entry, next-date form
        │   │       ├── SettingsSheet.kt
        │   │       └── theme/Theme.kt        # Earthy Material 3 colors/type
        │   └── res/
        │       ├── drawable/                # Default pet art, launcher/alert icons
        │       ├── values/strings.xml        # User-visible copy
        │       └── xml/                     # Backup exclusion rules
        ├── test/java/com/example/petcare/
        │   ├── ReminderRulesTest.kt         # Boundary dates, postpone, elapsed alerts
        │   └── DemoClockTest.kt             # Ratio, pause, same-date validation
        └── androidTest/java/com/example/petcare/
            ├── TreatmentPersistenceTest.kt  # File-backed Room close/reopen and A/B case
            ├── AlarmLifecycleTest.kt        # IDs, edit, completion, recovery
            └── CoreJourneyTest.kt           # First entry, confirmation, demo isolation
```

Ignore `local.properties`, `.gradle/`, build outputs, emulator/device metadata, signing material, and recordings containing personal information; retain wrapper files and Room schemas. Preserve existing credential/profile ignore rules. Publishing checks belong to build/ship; no credentials are requested in chat.

## External Services and Dependencies

No runtime API endpoints, authentication keys, hosted database, paid subscriptions, or upload service. Dependencies come from Google Maven, Maven Central, and the Gradle plugin portal during setup/build; use official documented artifacts and pin versions. No artificial runtime quota or hosting cost is introduced. Android system APIs provide picker, alarms, channels, and notifications on the device. Major dependency documentation is linked in Stack.

## Important Failure Modes

- **Permission denied, muted channel, or delayed alarm:** save remains successful; show the notification warning and status, link to system settings, and keep in-app due/overdue cards usable. Never promise exact timing.
- **Database/file save failure or interrupted alarm update:** preserve form entries when nothing committed; if records committed but scheduling failed, show that distinction, keep the reconciliation marker, and retry. Image import failure leaves existing records/photos intact.
- **Wrong/stale plan or mode:** compare plan revision/status and real/demo namespace before any delivery or confirmation. Reject duplicate confirmations, suppress obsolete alerts, and never apply demo actions to real records.

## Verification Plan

Implements `prd.md > Acceptance Criteria`.

- First checkpoint: resolve/pin the version tuple and build the empty app with the selected JDK, SDK, and wrapper. Verify a runnable AVD and physical-phone API before implementation depends on them.
- JVM rule checks: purchase/due/overdue local dates, month/year rollover, time-zone conversion, skip-past behavior, once-only postponement and 4 PM cutoff; date picker must not shift dates in Buenos Aires or another zone. Check demo acceleration and pause semantics with a controllable clock, not wall-clock waiting.
- File-backed Room instrumentation: save October A, schedule November, confirm B with the selected actual date; close/reopen the database and assert A unchanged, B saved, plan completed, no duplicate after a second confirmation. Verify first-entry atomicity, optional successor scheduling, separate pets, and demo database isolation.
- Scheduler checks: edits replace old identities, confirmation cancels remaining requests and displayed notifications, stale queued alarm is ignored, reconciliation after an interrupted update is idempotent, and restart/time-zone recovery derives requests from current data.
- UI walkthrough: settings/welcome customization, optional photos surviving restart, empty states, copy button only affecting the three agreed fields, notification denial permitting save, and no-next-plan state.
- Demo evidence: record actual labeled notifications, postponement, a changed medication confirmation, and overdue case; reset and exit without touching real data or cancelling real alarms.
- Separate physical-phone background evidence: schedule a short test alarm through the real scheduler, background the app/lock the screen, and observe delivery. Repeat cancellation/edit tests and reboot recovery where practical. Label any shortened test time as diagnostic; an in-app notification-preview button is not evidence of background delivery. Record device/API and observed delay; do not assert an exact deadline for inexact alarms.
- Run assembleDebug, unit tests, lintDebug, and targeted connectedDebugAndroidTest checks as their features land. See `build-step-1.md` and `build-step-2.md` for implementation evidence. Unit checks use `:app:testVerificationUnitTest`; device instrumentation uses `:app:connectedVerificationAndroidTest` against a separate `.verification` application ID, preserving the learner’s debug app data when the runner cleans up.

## What Was Simplified and Why

- Local Room/DataStore/files avoid server/account setup; device synchronization and export/restore are deferred.
- One app module and a simple dependency container avoid a dependency-injection framework and additional architectural layers.
- Platform image selection and local decoding cover optional photos without camera capture or cloud storage.
- Inexact real alarms accept timing flexibility for flea treatment; foreground demo timing avoids idle throttling of compressed days.
- Only flea treatment, one pending plan per pet, one overdue reminder, and one postponement are implemented. Other medications and per-medication timing remain later work.

## Future Option: More Precise Reminders

The learner asked whether exact-alarm access could support later medication categories, such as antibiotics or heart medication. This is a retained technical option, not an addition to the flea-treatment POC.

A future precision-sensitive user-facing feature can assess `AlarmManager.setExactAndAllowWhileIdle` with a PendingIntent. On recent Android versions it ordinarily needs `SCHEDULE_EXACT_ALARM` special access, checked with `canScheduleExactAlarms()`, separately from notification permission. `USE_EXACT_ALARM` has narrower use cases and must not be substituted casually. Revocation cancels exact alarms, so permission-change recovery would be required. [Exact-alarm access](https://developer.android.com/develop/background-work/services/alarms).

Exact scheduling improves timing precision; it is not an absolute delivery guarantee. Idle frequency limits, device availability, force-stop, and notification settings still matter. Validate multiple pets' nearby reminders and actual supported devices before making reliability claims. The foreground accelerated demo cannot prove this behavior. A future timing-sensitive extension needs an explicit permission/fallback design and its own background tests. [AlarmManager API](https://developer.android.com/reference/android/app/AlarmManager).

## Decisions and Open Issues

**Learner-agreed:** fresh Kotlin/Compose; local Room/DataStore/files; flexible background real alarms and foreground one-day-per-minute demo; separate administration history and pending plans; preserve the future exact-alarm option. Earthy styling, photos, copy action, real/demo isolation, actual-date confirmation, and optional successor scheduling carry forward from the approved PRD.

**Genuine uncertainty clarified:** exact-alarm permission versus reliable notification timing. Official documentation establishes the option and limits; this POC accepts inexact delivery. Build evidence must separately check real background behavior and demo interactions.

**Derived implementation details accepted at review:** API 26 minimum, version baseline and application ID, one pending plan per pet, private images/backup exclusion, route shell, receiver recovery, single-use postpone behavior across due-date edits, demo pause/reset behavior, and validation that confirmation does not precede its source administration. These were proposed during technical planning and accepted with final approval, rather than attributed to earlier learner statements.

**Remaining build investigations:** exact compatible library/plugin pins and an empty build, configured AVD, physical-phone API/connection, and observed background delay/recovery. These have explicit first-checkpoint checks; they do not conceal an unresolved stack choice. If a phone is below the approved minimum, agree on an alternative before installing. Version-resolution changes that preserve the agreed stack are routine; product changes require learner agreement.

The expanded POC contains several screens, durable photos, and real background behavior. The original 2–4-hour target is not a tested delivery estimate; build in verified steps and preserve all approved features unless the learner agrees to a cut. The learner approved this technical plan after review, including the supplied 12-role palette and the Java-target maintenance clarification. Technical planning is complete; 5-build is the next stage.


## Revisions

- 2026-10-11 learner feedback: first-entry next date is optional; saving without one shows the no-reminders message and creates no plan. Add a next date later from detail. Date editing moves into slice 2, replacing old reminders while preserving administered history.
