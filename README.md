# BFIT

BFIT is an Android fitness and nutrition assistant focused on practical daily tracking. It combines personalized planning, barcode-based nutrition lookup, AI-assisted meal recognition, weekly progress reporting, and local-plus-cloud data storage.

## Overview

The app supports the complete day-to-day flow for a user:

1. Create a plan based on body metrics and goal.
2. Track meals and nutrition using barcode scan or manual additions.
3. Monitor daily calories and protein.
4. Log body weight and visualize trends.
5. Use AI assistance for meal recognition and chat support.

## Core Features

### 1. Personalized Planner

- BMI-based plan generation (bulk, lean, maintain).
- Daily meal and workout plan view.
- Day completion tracking.
- Grocery list support.

### 2. Barcode Nutrition Scanner

- Camera-based barcode scanning using ML Kit + CameraX.
- Product and nutrition fetch from OpenFoodFacts.
- Quick add to daily log.

### 3. Weekly Progress Report

- Weekly calories and protein summary.
- Completed-days indicator for adherence.
- Logged-days insight for consistency.

### 4. Weight Log and Graph

- Save daily weight entries.
- Interactive 7-day and 30-day trend chart.
- Latest value and delta indicators.

### 5. AI Meal Recognition

- Analyze meal photos via on-device image labeling.
- Estimated calories and macro split.
- Add recognized meal result to daily log.

### Additional Functional Areas

- AI chat coach powered by Gemini.
- Supplement store flow with purchase history.
- Firebase auth (email/password and Google sign-in).
- Demo mode for local usage without authentication.

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| Language | Kotlin |
| UI | XML + Material 3 |
| Architecture | Repository pattern |
| Local Data | Room |
| Cloud Data | Firebase Firestore |
| Auth | Firebase Authentication + Google Sign-In |
| Networking | Retrofit + Gson |
| Barcode Scanning | ML Kit Barcode Scanning + CameraX |
| Image Labeling | ML Kit Image Labeling |
| AI Chat | Google Generative AI SDK |
| Charts | MPAndroidChart |

## Project Structure

```text
BFIT-master/
   app/                    Android application
      src/main/java/com/example/bfit/
      src/main/res/
   bfit-web/               Optional web prototype (Vite + TypeScript)
   gradle/
   build.gradle.kts
   settings.gradle.kts
```

## Prerequisites

- Android Studio (latest stable recommended)
- JDK 17
- Android SDK (compileSdk 36 configured in project)
- Gradle wrapper included (8.13)
- Firebase project for auth/firestore features

## Android Setup

### 1. Clone

```bash
git clone https://github.com/pushpraj-core/BFIT.git
cd BFIT
```

### 2. Firebase Configuration

1. Create or use a Firebase project.
2. Add an Android app with package name:

    `com.example.bfit`

3. Download `google-services.json` and place it at:

    `app/google-services.json`

4. Enable providers used by the app:

- Authentication (Email/Password and Google)
- Firestore Database

### 3. Configure local.properties

Add these keys to your root `local.properties`:

```properties
# OpenFoodFacts / app API key placeholder used by BuildConfig.API_KEY
apiKey=YOUR_API_KEY

# Gemini key for chat features
gemini.apiKey=YOUR_GEMINI_API_KEY

# Google Sign-In Web Client ID from Firebase project settings
google.webClientId=YOUR_WEB_CLIENT_ID
```

### 4. Build and Run

Windows:

```powershell
./gradlew.bat assembleDebug
./gradlew.bat installDebug
```

macOS/Linux:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Optional Web Module

The repository also contains `bfit-web`, a Vite + TypeScript module.

```bash
cd bfit-web
npm install
npm run dev
```

## CI/CD

This repository includes GitHub Actions workflows for quality enforcement and release automation:

- `android-ci.yml`:
   - Runs on pushes and pull requests to `master`/`main`
   - Executes `lintDebug`, `testDebugUnitTest`, and `assembleDebug`
   - Uploads debug APK and reports as build artifacts
   - Fails the pipeline on quality/build errors

- `android-release.yml`:
   - Runs on version tags such as `v1.0.0` or manual dispatch
   - Builds release APK
   - Uses signing secrets if provided
   - Uploads the APK to GitHub Release and workflow artifacts

### Recommended Branch Protection

Enable branch protection for `master` and require CI status checks before merge.

## GitHub Release Setup

For signed release builds in GitHub Actions, add these repository secrets:

- `API_KEY`
- `GEMINI_API_KEY`
- `GOOGLE_WEB_CLIENT_ID`
- `GOOGLE_SERVICES_JSON` (base64-encoded `google-services.json`)
- `ANDROID_KEYSTORE_BASE64` (base64-encoded keystore)
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

To publish a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will generate and attach the APK to the release.

## Data and Architecture Notes

- Room is used as local persistent storage for performance and offline continuity.
- Firestore stores user profile, plans, logs, chat history, supplements, and purchases.
- The app follows a repository-based approach to centralize data operations.
- Weight logs are stored in Room and surfaced in analytics UI.

## Build Configuration Summary

- Namespace: `com.example.bfit`
- Min SDK: 24
- Target SDK: 34
- Compile SDK: 36
- Java/Kotlin target: 17
- Room schema migration included up to DB version 5

## Testing and Validation Checklist

Before release or demo, verify:

1. Login flow (email/password, Google, demo mode).
2. Planner generation and daily log updates.
3. Barcode scan and nutrition ingestion.
4. Progress screen weekly report and chart rendering.
5. Meal recognition and add-to-log flow.
6. Store and purchase history screens.

## Troubleshooting

- Build fails with Java-related error:
   Ensure JDK 17 is installed and configured in Android Studio and `JAVA_HOME`.

- Google sign-in fails:
   Confirm `google.webClientId` and SHA fingerprints are correctly set in Firebase.

- Firestore reads/writes fail:
   Verify Firestore is enabled and security rules allow your test user context.

- Gemini responses fail:
   Confirm `gemini.apiKey` is valid and has required API access.

## Contribution

1. Create a feature branch.
2. Keep commits focused and descriptive.
3. Run build checks before opening a pull request.
4. Include screenshots for UI changes.

## License

No license file is currently included in this repository. Add a `LICENSE` file if you want explicit open-source licensing.
