# Build step 3 verification

Date: 2026-10-11. Status: implementation saved; mechanical checks and learner review passed; stage approved. Additional manual postponement/alarm-clearing check deferred to 2026-10-12 morning. Learner check and commit pending.

## Delivered

- Record treatment opens blank medication/dose/source fields with actual administration date defaulting to today, permitting earlier dates no earlier than the prior administration.
- Explicit Copy from last treatment fills only medication/dose/source; dates remain unchanged and copied text stays editable.
- Confirmation atomically inserts the actual administration and completes the pending plan. Prior records are preserved; stale, wrong-pet and duplicate confirmations are rejected.
- Confirmation reconciles pending alarms and displayed notifications, then offers optional successor scheduling. Declining or cancelling scheduling preserves the saved administration without a new plan.
- Once-only postponement before 4 PM on the due day replaces the morning reminder with a 4 PM request. Revision changes invalidate old queued events; recovery/date editing cannot replenish postponement.
- Notification Record treatment and Postpone actions open the activity and use the same checked operations as detail. Rotation does not replay a launch action. Time-zone checks resolve the current phone zone when used.
- No schema change. Photos and accelerated demo remain later slices.

## Evidence before the interruption

Device: Pixel 10 AVD, emulator-5554, Android 17/API 37. Automated tests use the separate verification installation and temporary databases, preserving normal debug app records.

Commands passed before the crash:

```sh
JAVA_HOME=/home/nuria/Applications/android-studio/jbr ./gradlew :app:assembleDebug :app:testVerificationUnitTest :app:lintDebug :app:connectedVerificationAndroidTest
JAVA_HOME=/home/nuria/Applications/android-studio/jbr ./gradlew :app:connectedVerificationAndroidTest
```

Final pre-crash test totals: 8 JVM tests and 10 Android tests, all passing. A later small change resolves the current time zone for notification actions on every call; final rebuild/recheck pending below.

New tests prove:

- October 10 medication A remains unchanged; November confirmation creates medication B with the selected November 10 actual date, rather than November 12 recording date. Close/reopen verifies both database records and completed plan.
- Duplicate/stale/wrong-pet confirmation, blank fields and invalid dates cannot write another administration.
- Injected SQLite failure when completing the plan rolls back the new administration too.
- Postponement persists after reopen, rejects a second use, wrong day and 4 PM, and stays consumed after date editing.
- Real Android PendingIntent identities and displayed notifications are removed on postponement/confirmation. Only new postponed/overdue requests remain after postponement; repeated recovery remains consistent. Notification actions match eligibility.

The notification test initially asserted before Android's asynchronous notification service finished posting. It now waits for the expected active-notification count with a bounded timeout; assertions passed. No production rules were relaxed.

## Direct UI evidence

Passed in the isolated verification app with sample Lifecycle Fido:

1. First record: October 9, medication A, dose A, clinic A; next date October 11.
2. Record treatment opens today’s date and blank medication/dose/source fields. Blank medication save is rejected.
3. Select October 10; Copy from last treatment fills A/dose/source without changing October 10.
4. Cancel and reopen a blank form. Save medication B/dose B/clinic B on October 10.
5. Decline successor scheduling. Detail shows No treatment scheduled and both October 10 B and October 9 A.
6. Screenshot visually inspected: [preserved history](images/step-3-confirmed-history.png).

The interrupted walkthrough was resumed and passed after recovery:

7. Explicit Record notification launch for the completed plan leaves detail open; it cannot reopen confirmation.
8. The same launch action for a current plan opens a blank form, defaulting actual date to October 11, without writing an administration.
9. Save sample medication C, accept successor scheduling, then cancel the empty schedule form. No next plan is created and C remains saved.
10. Cold-launch: all three sample records (October 9 A, October 10 B, October 11 C) remain visible, with No treatment scheduled.

These route checks launch the notification's explicit activity action via ADB; actual notification action contents and cancellation were separately checked by instrumentation. The emulator's on-screen keyboard appeared after recovery; dismissing it exposed the save controls as expected.

## Recovery and remaining checks

After the crash, Git confirmed all implementation files and the screenshot survived. Last completed checkpoint remains `388f4e3` (step 2); step 3 is uncommitted. ADB initially found no device. Learner is restarting Android Studio.

Recovered emulator reconnected as emulator-5554. Final assembleDebug/assembleVerification, 8 JVM tests and lint passed after the current-zone adjustment. Completed the route/successor/restart UI checks above. Final device rerun passed all 10 Android tests after the current-zone adjustment (18 total including JVM tests). Lint has 0 errors and the same 4 previously recorded warnings. Debug update installed successfully without clearing data. Direct inspection confirmed the existing custom title, prior administration/dose/source, and working Record treatment action. The saved next date was October 12; an initial assertion assumed the older October 22 test value, so the actual saved value was inspected and left unchanged. Learner reported the requested hands-on checks worked as expected and explicitly approved the stage on 2026-10-11. Approved implementation is checkpointed; an additional manual postponement/alarm-clearing check is deferred to tomorrow morning at the learner’s request.

## Limits

The emulator was after 4 PM during the direct walkthrough; the in-app postpone action was correctly unavailable. Morning eligibility, one-use limits, real alarm replacement and notification cancellation were exercised with injected clocks in Android tests, without changing device time or normal records. No real-time wait for a 4 PM production alarm or physical-phone/locked-device delivery was performed in this slice. The background diagnostic evidence remains in build-step-2.md; interactive accelerated postponement is planned in step 5.

## Session Handoff

Stop for today at learner request. Resume Monday morning, 2026-10-12 (Buenos Aires), with the manual postponement and future-alarm-clearing test described in checklist.md, before slice 4. Inspect Android alarms before/after learner actions rather than waiting for future delivery. No automation was requested or created.
