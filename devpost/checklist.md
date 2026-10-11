---
doc: checklist
status: approved
---

# Build Checklist

Build mode: learn. Approved build order; this session stops after slice 1 and its hands-on check/checkpoint.

Session boundary: the learner authorized the first part only for this session: setup and the first runnable app. Complete slice 1, its learner check and checkpoint, then stop. Later slices retain the approved full scope but are not authorized for automatic continuation in this session.

Git prerequisite: Git was initialized with learner approval. Keep /devpost/learner-profile.md and credential files ignored; inspect intended staged changes before each commit. Checklist and learn mode were approved before implementation.

We build small working slices, each adding behavior that can be tried immediately. Bootstrapping is inside the first slice. The treatment/reminder kernel follows next, before finishing profile/image customization, so it is tested early.

## Slices

- [x] **1. Launch the themed app and save a custom welcome title**
  Becomes usable: A runnable native Android app with the approved earthy palette, default centered pet illustration, welcome title, enter button, and settings gear. Change the title, close/reopen the app, and see it retained. Enter opens the home empty state and left drawer; deferred features are visibly unavailable. No treatment functions are represented as complete yet.
  Why now: Delivers a real welcome/settings interaction while proving the Kotlin/Compose/JDK/SDK/Gradle baseline and a working device. It includes project setup rather than leaving a standalone plumbing step. This is the stopping boundary requested for the current session.
  PRD ref: `prd.md > The Core Journey`, `prd.md > Welcome and Settings`, `prd.md > Home and Navigation Drawer`, `prd.md > Look and Feel`, `prd.md > States and Boundaries`
  Spec ref: `spec.md > Stack`, `spec.md > Build JDK Versus Bytecode Target`, `spec.md > Where It Runs and How Someone Tries It`, `spec.md > Look and Feel`, `spec.md > App Shell and Screen Model`, `spec.md > Settings and Photo Storage`, `spec.md > File Structure`
  Build: Create one app module at the existing project root without overwriting curriculum/documents. Pin compatible stable plugins/dependencies, use JBR 25 with bytecode target 17, and record resolved versions in spec.md. Verify/create a runnable emulator profile with the learner as needed. Add the shared shell, default local art, theme, basic drawer/back navigation, and title-only settings backed by DataStore. Keep unavailable functions clearly unfinished; photo and demo settings arrive in their owning slices. Add portable run instructions and build/local-file ignores.
  Verify (mechanical): Run ./gradlew --version, :app:assembleDebug, :app:testDebugUnitTest, and :app:lintDebug with the selected JDK. Install/launch on one explicitly selected emulator or phone. Run a focused device check of the Compose screens that changes the title and verifies navigation, plus a restart check proving the saved title reloads. Inspect an actual screenshot for layout and the supplied palette. Record exact versions, target device/API, commands, and outcomes; do not mark the slice complete on compilation alone.
  Learner check: Open the project root in Android Studio, run the app on the selected emulator, enter the home screen, open/close the drawer, change the welcome title through the gear, and close/reopen the app. Report whether the title persists and the layout/colors match your intention. Briefly show MainActivity, AppShell, and SettingsStore as the relevant route.
  Commit: `Add runnable Compose app with persistent welcome title`

- [ ] **2. Save a pet and first flea treatment, with a real next-dose reminder**
  Becomes usable: Create pets, record the medication actually given and its date, choose a next date, and see persisted home cards and treatment detail. A next plan schedules real phone reminders; notifications-disabled warnings do not block saving.
  Why now: Exercises the distinctive care-record-to-reminder loop early, before adding optional photo/customization work. Proves real persistence and the unfamiliar background notification behavior together.
  PRD ref: `prd.md > Pet Record`, `prd.md > Treatment Detail and Entry`, `prd.md > Reminder Schedule and Due-Date Changes`, `prd.md > Notifications Disabled`, `prd.md > Acceptance Criteria`
  Spec ref: `spec.md > Local Database and Treatment Repository`, `spec.md > Data Model`, `spec.md > Reminder Rules`, `spec.md > Real Alarm Scheduler and Recovery`, `spec.md > Notification Delivery and Permission UI`, `spec.md > Verification Plan`
  Build: Add Room entities/DAO/repository, pet creation and first-treatment form, standard calendar pickers, required-field validation, atomic administration/plan save, due-date cards/detail and no-record states. Implement initial real alarm scheduling, receiver eligibility checks, channels/permission warnings, unique identities, durable reconciliation and startup/boot/time-change recovery. Leave optional photos and profile extras to slice 4. Add a test-only short schedule through the real scheduler to verify background delivery without changing production treatment dates or claiming foreground previews prove background reliability.
  Verify (mechanical): Run assemble, unit tests and lint. Use file-backed Room instrumentation to close/reopen and verify pets, administration and plan survive. Check first-entry transaction failure does not leave a half-record, pets stay separate, selected dates do not shift time zones, and elapsed reminders are skipped. Install on a selected device and observe a labeled diagnostic alarm while the app is backgrounded; verify denied permission still permits saving. Record observed delay and device/API rather than claiming exact timing.
  Learner check: Add a pet and treatment, inspect its next-date card/full detail, restart the app, and check the record persists. Try invalid input and notification denial. Observe the background diagnostic notification and describe anything confusing about the record-to-reminder flow.
  Commit: `Add persisted flea treatment records and background reminders`

- [ ] **3. Confirm the actual dose, change plans, and postpone once**
  Becomes usable: Complete a pending treatment with its actual medication/date, preserving prior history; optionally schedule another. Edit next dates, copy prior details deliberately, postpone once to 4 PM, and cancel obsolete/overdue alerts correctly.
  Why now: Closes the core loop and proves the learner's A/B medication example before adding convenience features. Cancellation, stale alarms, and duplicate confirmation are tested while the system is still small.
  PRD ref: `prd.md > Administration Confirmation`, `prd.md > Treatment Detail and Entry`, `prd.md > Reminder Schedule and Due-Date Changes`, `prd.md > Approved Review Defaults`, `prd.md > Acceptance Criteria`
  Spec ref: `spec.md > Local Database and Treatment Repository`, `spec.md > Reminder Rules`, `spec.md > Real Alarm Scheduler and Recovery`, `spec.md > Notification Delivery and Permission UI`, `spec.md > Data Model`, `spec.md > Verification Plan`
  Build: Add Record treatment with blank actual medication/dose/source fields and optional Copy from last treatment. Default actual date to today while allowing valid earlier dates. Atomically create the new administration and complete its plan; cancel pending/displayed reminders. Offer optional successor scheduling only after confirmation. Add due-date edits, once-only postpone action, stale revision suppression, retained history in detail, and reconciliation of interrupted alarm changes.
  Verify (mechanical): Run the required build/lint/unit checks and targeted Room/alarm instrumentation. Persist October A, confirm November using B and an explicitly selected actual date, close/reopen and assert A unchanged, B correct, plan complete and duplicate confirmation rejected. Verify No next schedule leaves none, due edits replace old identities, stale queued alarms cannot post, and postpone is unavailable after use or 4 PM. Verify cancellation and recovery on the selected device.
  Learner check: Reproduce the A/B scenario, backdate an administration, inspect both records, decline then create a next schedule, edit its date and try the diagnostic postponement/cancellation path. Explain any difference from the intended confirmation flow.
  Commit: `Add actual-dose confirmation and reminder lifecycle controls`

- [ ] **4. Keep optional pet details and photos with the records**
  Becomes usable: Save optional birth date/owner/breed and pet photo, attach a treatment photo, and replace the welcome image. Images and settings remain visible after reopening, with usable cancel/failure behavior.
  Why now: Adds the approved personal context and images after the central record/reminder behavior is verified. Reuses one photo-import mechanism across the three uses.
  PRD ref: `prd.md > Pet Record`, `prd.md > Treatment Detail and Entry`, `prd.md > Welcome and Settings`, `prd.md > States and Boundaries`, `prd.md > Acceptance Criteria`
  Spec ref: `spec.md > Settings and Photo Storage`, `spec.md > App Shell and Screen Model`, `spec.md > Look and Feel`, `spec.md > Important Failure Modes`, `spec.md > Verification Plan`
  Build: Complete optional pet fields, single existing-image selection, staged private-file import and bounded decoding, photo display in treatment detail, welcome image customization and placeholder/error states. Preserve originals on replacement failure; clean up only unreferenced app-owned staged files. Complete backup exclusion rules for local database/settings/files.
  Verify (mechanical): Build/lint and run targeted device checks for photo import/display across restart, optional fields remaining optional, invalid birth dates, cancellation, and a simulated import failure retaining the prior photo. Confirm no broad gallery/camera permission or upload service was added and notification/core journey checks still pass.
  Learner check: Select a pet picture, attach a box photo to a treatment, replace the welcome picture, reopen the app, and inspect all three. Cancel selection and verify existing saved pictures remain intact.
  Commit: `Add persistent pet details and optional record photos`

- [ ] **5. Practice the complete care loop in isolated accelerated demo mode**
  Becomes usable: Enter labeled sample mode through settings, observe one day per minute while visible, receive real Demo notifications, confirm/postpone or let a sample go overdue, reset, and return to untouched real records/reminders.
  Why now: Reuses the verified care rules and notification formatting. Demonstrates the entire loop without replacing background verification with simulation or risking real records.
  PRD ref: `prd.md > Accelerated Demo Mode`, `prd.md > Administration Confirmation`, `prd.md > Acceptance Criteria`
  Spec ref: `spec.md > Accelerated Demo Clock and Session`, `spec.md > Notification Delivery and Permission UI`, `spec.md > App Shell and Screen Model`, `spec.md > Where It Runs and How Someone Tries It`, `spec.md > Verification Plan`
  Build: Add separate in-memory Room samples/temp-image namespace, injected simulated date/time, foreground elapsed clock and lifecycle pause, clearly labeled notifications and settings switch/reset. Route real notifications to real mode and reject stale sample sessions. Exit cancels only sample tasks/notifications. Complete README instructions, limitations and the five-minute continuous recording path with clearly edited short-video excerpts.
  Verify (mechanical): Run build/lint/unit and targeted instrumentation/core-journey checks. Verify the 1440 ratio, 20-second demo postponement, lifecycle pause, date picker defaults, single handled events, and exit/reset cancellation. Compare real records and alarms before/after sample actions. Record actual Demo notifications and retain separate physical-device background evidence. Complete relevant full-journey regression checks once integration is finished.
  Learner check: Start a fresh demo, watch its clock/notifications, confirm a different medication and try an overdue case in a new session. Switch out and confirm real records are unchanged. Explore the whole POC and note final problems or refinements.
  Commit: `Add isolated accelerated demo and complete care journey`

## Hands-on Checkpoints

- [x] Early usable behavior explored — after slice 1, learner tries navigation/title persistence/colors and feedback is resolved before its commit. This is today's stopping boundary.
- [ ] Core treatment/reminder loop explored — after slice 3, learner checks the changed-medication history and alert cancellation before image/demo work. This is distinct from the early shell check.
- [ ] Final kick-the-tires exploration and feedback completed — after slice 5, learner explores the complete app and real/demo separation.

In learn mode, each slice additionally waits for its Learner check before its commit. Fast mode retains these planned hands-on checkpoints. Regardless of mode, stop after slice 1 in the current session unless the learner explicitly extends the session scope.

## Final Review

- [ ] Final review complete — feedback resolved and learner confirms ready to ship

No final-review feedback yet. Add agreed fixes here as unchecked tasks when the learner explores the completed POC; verify, commit, and have affected behavior retried before checking them off.

## Code Tour and App Map

- [ ] Learning activity complete — guided route, focused alternative, prior practice connected, or brief recap
- [ ] Optional edit and transfer reflection addressed — offered/declined/already covered/not applicable as appropriate
- [ ] `devpost/app-map.html` generated from finished code, checked, and shown, including a project-grounded practice to reuse

Activity and evidence: Not started. Use the actual administration-save-to-reminder path or another real moment connected to the learner's Kotlin/Android understanding goal.
Route and stops: To be chosen from finished code, covering two or three relevant locations without a quiz.
Edit outcome: Not yet offered; offer one optional safe edit or connect prior practice if already completed.
Reflection: Not yet offered; personal answers belong only in the ignored learner profile.
Activity mode: Planned live app/editor guided route; record the actual activity when completed.

## Revisions

- Slice 1 verification adjustment: Espresso 3.7.0 crashes before UI assertions on the selected API 37 emulator (`InputManager.getInstance` is absent). Use direct emulator interactions for the Compose journey and a separate Android instrumentation test that closes/reopens a file-backed DataStore. No runtime feature or stack change. Removed the unusable Espresso-dependent UI test and its dependencies.
- Initial lint analysis crashed inside IntelliJ PSI while a source import cleanup overlapped analysis (cause not confirmed). A subsequent analysis completed; no lint rules were disabled.


Slice 1 mechanical verification and learner check passed on 2026-10-11; see `build-step-1.md`. The learner confirmed the colors and specified behavior and chose to make the commit personally. Verified checkpoint: `c401f99` (`Add runnable Compose app with persistent welcome title`). Slice 1 complete; session stops here.
