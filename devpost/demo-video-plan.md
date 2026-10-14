# Demo video preparation

Prepared 2026-10-12. Recording deferred to the next session at the learner's request. No video recorded or uploaded yet. This is a technical shot list, not narration or submission copy.

## Chosen sequence

Learner's requested content: personalize the welcome screen, add a pet, add a treatment, explore the drawer, edit the next dose date, and respond to one alert.

## Constraints checked

- Event overview requests a 1–3 minute screen recording; official rules say the video should be less than three minutes. Aim for roughly 2–2.5 minutes after editing, with room for the learner's introduction.
- Show the working Android app. Official rules specify a publicly visible YouTube or Vimeo upload and English materials or English translation.
- The authenticated submission form was not accessible through the public web read. Check its current video prompt before final export/upload.
- Sources checked 2026-10-12: [event overview](https://learn-ai-basics.devpost.com/), [official rules](https://learn-ai-basics.devpost.com/rules), [emulator recording instructions](https://developer.android.com/studio/run/emulator-record-screen).

## Draft shot list

Timings below are editing targets, not mandatory durations. Pause briefly at each result so it is readable. Record short clips independently; a mistake only requires repeating that clip.

| Clip | Action | Visible result | Approximate finished time |
|---|---|---|---|
| 1 | Open welcome, use gear to change title and choose an existing permitted image, save | Customized welcome screen | 20 seconds |
| 2 | Enter and add a fictional pet; fill name and whichever optional fields the learner wants to show | Saved pet, invitation to record treatment | 20 seconds |
| 3 | Record actual administration date, medication, dose, source and a future next date; save | Treatment details and next-date card | 30 seconds |
| 4 | Open left drawer; briefly show Medication and future sections | Working navigation, deferred sections visibly disabled | 10 seconds |
| 5 | Open treatment detail, edit next chosen date, save | Updated date, prior administration unchanged | 15 seconds |
| 6 | Enter labeled Demo; postpone the treatment alert, leave treatment unconfirmed through the postponed reminder, open the overdue alert, then record administration | Postponement saved, overdue follow-up delivered, administration saved with history preserved | 45–55 seconds, excluding waiting |

Allow a small introduction/end pause within the less-than-three-minute target. The learner supplies any narration/captions in their own words. A face camera is not needed to demonstrate the app. Chosen approach: learner voice narration with English captions. Record the screen first, then narration in short sections; try a short sample before the full recording. The learner writes their own narration and captions; assistance is limited to recording mechanics and spelling/grammar corrections.

## Chosen alert sequence

Learner decision: postpone first, let the treatment become overdue, then record administration. Use a single fresh Demo session for this sequence.

1. Enter Demo through Settings. Briefly show Demo Fido, Medication A history, the next date and the accelerated clock. The switch from the newly created real-mode pet must be visible.
2. Keep the app visible. Around 3 minutes 53 seconds after starting Demo, open the treatment notification shade, expand the notification if necessary and tap **Postpone until 4 PM** promptly. The full eligibility window is only 20 real seconds.
3. Show the saved postponement. At simulated 4 PM, the postponed reminder returns. Briefly show it, but do not record treatment yet. Close the shade and keep the app visible.
4. Around five minutes after Demo start, the next-day noon overdue alert appears. Show its overdue message and tap **Record treatment** (expand first if needed).
5. In the administration form, select the actual administration date and enter the medication actually given and dose. The date defaults to the current simulated date; choose an earlier date only if that is what the learner wants to demonstrate. No dose is actually being given to an animal in this recording.
6. Save. Show the saved administration and preserved Medication A history. Choose **Not now** when asked about scheduling another treatment, leaving the completed sample without a successor reminder.

Editing: retain a short view of the Demo banner/clock before and after cuts, so the jumps to 4 PM and next-day noon are understandable. Keep the real notification messages and action results readable. Trim waiting, not the result. Estimated useful alert footage is 45–55 seconds; total finished video should remain below three minutes.

Foreground Demo demonstrates this care flow with accelerated sample time. Separate real-alarm evidence is documented in `build-step-3.md`; do not imply this clip proves precise background timing.

## Recording setup for the next session

1. Use the existing Pixel 10 emulator and the debug app. Preserve existing records; do not clear app data to obtain an empty screen. Choose a distinct fictional pet for recording. Pet deletion is deferred, so avoid unnecessary repeated real-mode pet creation.
2. Have the chosen welcome image and fictional form values ready. Use images permitted for public display. Keep contact details and unrelated notifications out of the recording.
3. Open emulator Extended Controls → Record and Playback (documented shortcut: Ctrl+Shift+R). Record a short test and save as WebM; review it before recording the real clips. Availability/location in the installed emulator should be checked during that test.
4. Record screen clips first. Narration can be recorded separately if the learner chooses it. Keep source clips outside the tracked repository; the repository holds this plan, not large raw recordings.
5. For Demo: purchase notification appears a few seconds after start; treatment reminder about 3 minutes 53 seconds after start; overdue about five minutes if left unconfirmed. One sample day equals one real minute while the app is visible. Trim waiting with an obvious edit; retain the Demo label and clock around the cut.
6. Notification gestures: drag down from the top edge; tap title/message to open the record; tap the small arrow to expand action buttons. Postpone is available only before simulated 4 PM on the due day: the 8 AM–4 PM window lasts 20 real seconds.
7. Stop and save each clip, then check text readability and the visible outcome. If one clip goes wrong, keep the others. Decide editing/export mechanics after the short test; no extra tool installation or paid service is assumed.

## Remaining before recording

- [x] Alert response chosen: postpone → overdue → record administration.
- [x] Choose voice narration with learner-written English captions; test a short sample next session.
- [ ] Review current submission-form video instructions.
- [ ] Perform a short emulator recording test in the next session.
- [ ] Record and trim clips; check final runtime and readable results.
- [ ] Upload and verify reviewer access later, with learner authorization.

Scope for this session: video preparation only. Repository publication audit, submission answers and final submission remain separate shipping work.
