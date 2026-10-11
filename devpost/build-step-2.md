# Build step 2 verification

Date: 2026-10-11. Status: mechanical checks and learner review passed; learner committed/pushed `388f4e3`.

## Delivered

- Multiple named pets; after saving, invitation to add the first flea treatment and a drawer explanation.
- First-treatment form: actual date defaults to today and can be earlier; medication/dose required, source optional, manually selected next date optional and, if supplied, strictly after actual date. Saving without one reports that no reminders are scheduled. Add/edit a date later from detail. No dose or interval calculation.
- File-backed Room entities/queries and an atomic administration-plus-plan transaction; separate historical administration and pending plan. Exported version 1 schema retained.
- Persisted home cards ordered by next date, no-record/no-plan states, overdue status, and full previous treatment details.
- Real inexact purchase/due/overdue alarms, durable reconciliation markers, stable per-plan/revision/kind identities, checked late/stale/duplicate delivery, and boot/time/time-zone/update recovery.
- Contextual notification permission, disabled warnings on welcome/entry, system settings link, and successful record saving despite disabled notifications.
- Debug-only, clearly labeled 20-second background diagnostic through the same AlarmScheduler.setAlarm API. Available in Settings; absent from release builds.

Confirmation, postponement and copy-previous remain in step 3;
photos/profile extras and accelerated demo remain in steps 4–5.

## Build and automated evidence

Pixel 10 x86_64 AVD, emulator-5554, Android 17/API 37. Learner selected the API 37.2 image.
JBR 25.0.3; JVM target 17; existing AGP 9.4.0/Gradle 9.6.0 retained.
Room 2.8.5 and KSP 2.3.12 resolved and compiled; pins in version catalog.

Full automated command:

```sh
JAVA_HOME=/home/nuria/Applications/android-studio/jbr ./gradlew :app:assembleDebug :app:testVerificationUnitTest :app:lintDebug :app:connectedVerificationAndroidTest
```

Result: BUILD SUCCESSFUL; 7 JVM tests and 5 device instrumentation tests passed.
A final diagnostic-availability check and clearer scheduling-warning wording were
then rebuilt and linted with `:app:assembleDebug :app:lintDebug`, both passing.
Lint: 0 errors, 4 warnings (three pinned-version update advisories and the deliberately
exported debug-only diagnostic receiver). No rules disabled.

Tests cover:

- local reminder times across year/month boundaries and DST, elapsed/handled events skipped, stale/completed/late events rejected, UTC calendar-picker conversion;
- disk-backed Room close/reopen for two distinct pets, actual dates/products/source/next dates retained, duplicate first-entry rejected;
- injected SQLite trigger failure during plan insertion rolls back the administration too;
- future administration and invalid next dates write no records;
- distinct pet/kind alarm identities, repeated reconciliation, marker clearing, handled-event suppression, cancellation of completed-plan identities;
- existing DataStore disk persistence and title validation.

An initial alarm test incorrectly expected a canceled/recreated PendingIntent to
retain its Binder token; corrected to check the intended current identity exists.
All final assertions passed. No app data was rolled back to address test failures.

## Direct UI evidence

Using actual emulator UI hierarchy assertions and input:

1. Notifications disabled warning visible on welcome.
2. Empty pet name rejected; create a clearly named test pet, Practice Fido.
3. Accept the first-treatment invitation. Select actual date October 10 rather than today's October 11 through the standard calendar.
4. Enter Practice Medication A / One tablet / Practice Clinic. Initial implementation rejected saving without a next date. Learner feedback revised this rule; the final optional-date behavior is verified below.
5. Choose October 22. Save succeeds with notifications disabled; full detail shows the entered dose/source and actual date.
6. Force-stop/cold-launch: warning, pet card, last-given October 10 and next-chosen October 22 remain.
7. Enable reminders through the contextual explanation and Android's Allow prompt.
8. After isolated instrumentation runs and APK update, the normal app's test record still exists.
9. Settings shows the background test button; pressing it closes settings and requests the diagnostic.

Inspected [treatment detail](images/step-2-treatment-detail.png): readable sand/cream/brown/green layout, full details and next date within insets. The screenshot now shows the isolated Verification Pup after editing its date. One Practice Fido verification record remains in the normal emulator app; it is not accelerated demo mode. The learner can add a separate pet for their check.

## Actual background delivery

Scheduled the debug diagnostic via an explicit broadcast; immediately pressed the
phone Home button. Launcher was the top resumed activity at verification.

- Request created at epoch 1791737244562 ms.
- Requested trigger: 1791737264562 ms (20 seconds later).
- Actual receiver time: 1791737279565 ms.
- Observed delay after requested trigger: **15.003 seconds**; total about **35 seconds**.
- Permission enabled; NotificationManager contained the actual titled notification:
  **Diagnostic — background reminder**.
- Screenshot: [background notification](images/step-2-background-notification.png).
- AlarmManager inspection separately showed three real requests for the test plan:
  October 18 noon, October 22 8 AM, October 23 noon in the emulator's current local zone (UTC).

This demonstrates actual inexact background delivery on this emulator, not an exact
deadline or physical-device/Doze/reboot reliability guarantee. Physical-phone
background/locked-screen checks remain part of the later integrated verification.
The diagnostic does not alter real treatment dates; it is separate from the planned
foreground accelerated demo.

## Test install isolation and limits

Gradle's device-test cleanup removed the app under test during early runs. Tests now
target a separate `verification` build type/application ID (`com.example.petcare.verification`),
leaving the normal `debug` app (`com.example.petcare`) intact. AGP names both unit and
instrumentation tasks after the chosen test build type; README/spec commands updated.
Use debug in Android Studio for the normal app. The verification APK has no debug-only
diagnostic receiver and is not the app to use for the learner walkthrough.

Receiver rules and recovery identities were checked as above. A physical phone,
reboot, a full production-date wait, and pending-treatment confirmation have not been
verified in this slice. Notification clicks are wired to the matching pet detail;
full action/confirmation behavior is tested with step 3.

## Learner check

Pending: run debug, add a pet/treatment, try invalid input and notifications disabled,
inspect its card/detail, reopen to check persistence, and use the labeled background
diagnostic. Collect feedback before the slice commit and completion checkbox.

## Optional next-date revision — 2026-10-11

Learner requested optional first-entry next dates and editable chosen dates. Scope,
PRD, spec and checklist updated; date editing moves from slice 3 into slice 2.
No date creates only an administration, no plan row. The first form offers Clear
next date before saving. The save confirmation says “No next treatment selected;
no reminders scheduled.” Detail offers Schedule next treatment or Edit next date.
Date edits leave medication/history unchanged and replace future reminders;
unchanged dates do not reset handled flags. Stale editor revisions are rejected.

Re-ran assembleDebug, testVerificationUnitTest, lintDebug and
connectedVerificationAndroidTest: all passed, **7 JVM tests and 6 device tests**.
Added a file-backed no-date/add/edit/reopen test verifying the original administration,
revision tracking, stale-edit rejection, validation, and retained postponement use.
Expanded the OS alarm test to verify all old revision identities disappear, new
ones exist, and the other pet's requests remain intact. Existing rule tests reject
stale-revision delivery. Normal debug app records/settings were preserved.

Direct UI walkthrough passed in the separate verification APK: save Verification Pup’s administration without a next date; see No treatment scheduled and Schedule next treatment; choose October 22; edit to October 24; cold-restart and verify October 24 plus the unchanged October 11 medication/dose. See updated screenshot. Normal debug is installed for the learner's retry.
Learner reviewed and approved these changes, then committed/pushed `388f4e3`.
