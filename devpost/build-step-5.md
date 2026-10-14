# Step 5 — Isolated accelerated Demo

Status: implementation, mechanical checks and learner check complete; committed as `bf1d470`; final review and source-led learning wrap-up complete.
Checked 2026-10-12 on Pixel 10 emulator, Android API 37.2, Buenos Aires time zone.

## Implemented

- Settings can start, reset or exit a clearly labeled Demo. Each entry creates a new fictional Demo Fido, an earlier Medication A record and a pending plan. Samples use a separate in-memory Room database; no sample plan enters the real alarm scheduler.
- An injected clock advances 1,440 simulated milliseconds per real millisecond only while the Activity is visible. All sample form date defaults, validation, card overdue state and postponement rules use that clock. The displayed Demo banner explains its date/time and foreground-only behavior.
- A foreground event pump notices purchase, treatment, postponed and overdue crossings, records handled bits once and posts Android notifications on a separate Demo channel with session-specific identities. Notification actions route only to the active sample session. Real notification actions leave Demo and open the real record.
- Sample photos use a separate private directory. Reset and exit clear sample notifications, database and photos, without cancelling real reminders or changing real records. A cold app start returns to real mode and clears stale sample notifications/files. Welcome title and image remain shared persistent settings, with that distinction shown in Settings.
- README explains a continuous five-minute purchase-to-overdue recording path and a fresh-session confirmation path, with clearly edited short-video excerpts.

## Mechanical verification

- `JAVA_HOME=/home/nuria/Applications/android-studio/jbr ./gradlew :app:assembleDebug :app:testVerificationUnitTest :app:connectedVerificationAndroidTest :app:lintDebug --offline` passed: 9 JVM tests and 12 emulator tests, no failures or skips; lint passed.
- Controllable-clock test verifies one real minute advances one sample day, 20 real seconds advances eight sample hours, and a background interval adds no simulated time. Reminder-rule test confirms the 8 AM-to-4 PM postponement is 20 real seconds at the approved ratio.
- In-memory Room test verifies sample purchase/treatment/overdue crossings are handled once, confirmation suppresses an overdue crossing, and real records, a real pending alarm and a real displayed notification survive sample operations and Demo notification cancellation. Photo test confirms sample-file cleanup leaves real photos intact.
- Direct UI walkthrough in the isolated `.verification` app showed the fictional card and [Demo banner](images/step-5-demo-home.png). Android posted a notification titled `Demo — Demo Fido — Flea treatment` on the Demo channel after the purchase crossing. A temporary lifecycle trace showed the exact same simulated instant before backgrounding and on return more than ten seconds later; the diagnostic trace was removed from source. Reset removed the old session notification and generated a fresh session identity. Exit cleared sample notifications and showed the verification app's real-mode empty state.
- Installed the updated normal debug APK without clearing its data. It reopened; a read-only snapshot still found Practice Fido's three administrations and completed plan, and Android still listed unrelated future real reminder alarms. Temporary database snapshots were deleted.

The emulator has separately delivered a real postponed alarm about six minutes after its nominal time; see `build-step-3.md`. A locked physical-phone background pass has not yet been run. Demo notifications are foreground-flow evidence, not a substitute for that real-alarm test.

## Learner check completed

2026-10-12 feedback: the learner observed the overdue notification without confirmation and verified that exiting Demo preserves real records. They initially could not open a notification, then noted uncertainty about using the emulator's notification shade. A direct repeat in the isolated verification app opened the purchase notification's body to Demo Fido. The treatment notification's Postpone action opened the record and saved the single-use postponement; the postponed notification appeared after simulated 4 PM, and its Record treatment action opened the confirmation form. A late attempt correctly showed the after-4-PM rejection. No routing failure was reproduced and no application-code change was made for this report. README now explains dragging down from the top edge, tapping the body versus the expansion arrow, and the 20-real-second postponement window. The learner retried successfully and confirmed the problem was emulator interaction.

Completed check: in the normal app, open Settings and start Demo. Check the fictional record and changing banner, then watch purchase and treatment notifications. Try Postpone once during the simulated due morning; it should return about 20 real seconds later. Record a different medication and chosen actual date, inspect the preserved Medication A history and absence of remaining alerts. Reset for an overdue case without confirming. Exit and check that the user's real records remain, and that no Demo notification stays visible. Report anything broken or confusing before the step-5 commit and final review.

2026-10-12 learner sign-off: all requested Demo checks passed, including changed medication with Medication A retained in history, postponement, overdue notification, and preservation of real records after exit. The notification interaction was the final missing check. Learner will make the slice-5 commit personally, consistent with previous slices.

Checkpoint verified: `bf1d470` (`Add isolated accelerated demo and complete care journey`), clean working tree before recording completion, HEAD matching the upstream tracking ref. Learner reports committed and pushed.
