# Pet Care

A local native Android learning project for the Build With AI: Basics hackathon.
Kotlin and Jetpack Compose, with an earthy sand, brown, and green palette.

## Current build

Step 1 implements the welcome screen, editable persistent welcome title, settings,
and home/drawer navigation. Medication opens an empty state; pet records and
notifications belong to later steps and are not implemented yet.

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
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The first build needs network access for pinned dependencies. The Gradle wrapper
verifies the distribution checksum. JVM bytecode targets Java 17; the build runs
on JBR 25. No Java 17 runtime is bundled with the app.

## Try step 1

Use the gear to change the welcome title. Blank names and names over 60 characters
are rejected. Save, close and reopen the app: the title should remain. Enter opens
Home; its menu opens a left drawer. Medication is an empty state; other sections
are labeled Later. The launcher label remains Pet Care. Your title is stored only
on this device; uninstalling or clearing app data removes it.

## Project guide

- `app/src/main/java/com/example/petcare/MainActivity.kt`: Android entry point.
- `ui/AppShell.kt`: Compose navigation and shared screen frame.
- `ui/PetCareViewModel.kt`: UI state and save coordination.
- `data/SettingsStore.kt`: DataStore title persistence.
- `ui/theme/Theme.kt`: approved colors.
- `devpost/`: approved scope, PRD, technical spec, and build checklist.

The learner profile and local SDK/credential files are excluded from Git.
