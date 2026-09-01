# MM Assistant (Voice AI & Security)

MM Assistant is a personalized voice assistant and device utility app for Android built with Jetpack Compose, Kotlin Coroutines, Room Database, Material 3, and Gemini Live AI integration.

---

## 🚀 Building the APK

### 1. Automated Build via GitHub Actions (Recommended)
This repository includes an automated GitHub Actions CI/CD workflow at [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

- **Automatic Trigger:** Every time you `git push` to `main` or `master`, GitHub Actions automatically:
  1. Sets up JDK 17 and the Gradle wrapper.
  2. Generates the debug keystore and configures `.env`.
  3. Builds the APK via `./gradlew assembleDebug`.
  4. Uploads the generated APK (`MMAssistant-debug-apk`) as an artifact.
  5. Automatically creates a **GitHub Release** with the downloadable `MMAssistant-debug.apk`.
- **Manual Trigger:** Go to the **Actions** tab on GitHub -> select **Build Android APK** -> click **Run workflow**.

---

### 2. Building Locally via Terminal / Command Line

#### On Linux / macOS:
```bash
# Make gradlew executable
chmod +x ./gradlew

# Build Debug APK
./gradlew assembleDebug
```
The resulting APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

#### On Windows (Command Prompt / PowerShell):
```cmd
gradlew.bat assembleDebug
```

---

### 3. Opening and Building in Android Studio
1. Open **Android Studio** (Koala / Ladybug or newer).
2. Click **File -> Open...** and select this project's root folder.
3. Wait for Gradle Sync to complete.
4. Select **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.

---

## 📁 Project Structure & Required Files
- `gradlew` & `gradlew.bat`: Gradle wrapper execution scripts.
- `gradle/wrapper/gradle-wrapper.jar` & `gradle-wrapper.properties`: Self-contained Gradle 8.11.1 distribution wrapper.
- `build.gradle.kts` & `settings.gradle.kts`: Root Gradle build scripts and plugin management.
- `app/build.gradle.kts`: Android application module configuration, dependencies, and signing configs.
- `.env.example`: Default environment variable template for the Secrets Gradle Plugin.
- `.github/workflows/build-apk.yml`: Automated GitHub Actions APK build and release workflow.
