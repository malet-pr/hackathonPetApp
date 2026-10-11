# Build step 1 verification

Date: 2026-10-11. Status: mechanical verification and learner check passed; committed as `c401f99`.
Git checkpoint verified after the learner committed it.

## Delivered

Native Kotlin/Compose app, local dog vector, approved earthy palette, welcome
screen, persistent custom title, settings sheet, Home empty state, left drawer,
Medication empty state and Later labels for deferred sections. Photo customization,
pet/treatment entry and reminders remain in their approved later slices.

## Environment and commands

- Pixel 10 AVD, `emulator-5554`, x86_64. ADB reports Android 17, API 37,
  boot completed. Learner selected the API 37.2 system image in Device Manager.
- JBR 25.0.3, Java/Kotlin bytecode target 17; Gradle wrapper 9.6.0, AGP 9.4.0.
- `JAVA_HOME=/home/nuria/Applications/android-studio/jbr ./gradlew --version`: passed.
- `JAVA_HOME=/home/nuria/Applications/android-studio/jbr ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest`: passed.
- Unit tests: 2 passed. Validate blank/oversize rejection and trimming at the limit.
- Instrumentation: 1 passed. Save title to file-backed DataStore, close its coroutine
  scope, open a new store against the same file, verify saved value.
- Lint: 0 errors, 3 newer-version advisories. No rules suppressed. The pinned,
  compiled version set is recorded in spec.md and the version catalog.
- Installed debug APK and cold launched via ADB: passed.

## Direct emulator journey

Used Android input commands and UI hierarchy assertions on actual Compose screens:

1. Open settings, erase title, Save: inline `Enter a title.` appears.
2. Save `Fido and Friends`: header changes.
3. Force-stop app and cold launch: `Fido and Friends` is reloaded.
4. Enter: Home shows `A fresh start`.
5. Open drawer: Vaccinations is visible; choose Medication: `No pets yet`.
6. Android Back returns to Home then Welcome.
7. Restore default `Pet Care` title for the learner's own check.

Inspected a rendered screenshot: dog art centered and uncropped, sand background,
brown text, green button, accessible settings gear and Enter button within insets.
Evidence: [Welcome screen](images/step-1-welcome.png).

## Repairs and limits

Initial lint crashed internally; a rerun completed analysis. The report then caught
an API 27-only navigation-bar XML attribute in the API 26 base style; removed it
(the activity already manages system bars through edge-to-edge). Unused resources
were removed and the vector intrinsic size reduced without changing its viewport.

Espresso 3.7.0 crashed before UI assertions on this emulator because
`InputManager.getInstance` was absent. Removed the Espresso-dependent test and
its dependencies; verified the UI directly and storage through instrumentation.
This is not a claim of broad Android-version coverage. Rotation was not included
in the final direct UI check. No background reminders are implemented or verified.

## Learner check

Completed on 2026-10-11: the learner confirmed title persistence, approved the colors,
and reported that behavior matches the specification. No changes requested.
The learner committed the checkpoint as `c401f99`; verified the commit and marked
slice 1 complete.
