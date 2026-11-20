# Build Instructions — Imista Forward

## Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK with:
  - `platform-tools`
  - `platforms;android-35`
  - `build-tools;35.0.0`
- Gradle 8.7 (wrapper included)

## Clone
```bash
git clone https://github.com/pushparaj9749/Imista.git
cd Imista
git checkout arena/cf8a3fb5-imista  # current feature branch
```

## Local Release Build

### Debug (no signing needed)
```bash
./gradlew :sender:assembleDebug
./gradlew :receiver:assembleDebug
```
APKs in `sender/build/outputs/apk/debug/` and `receiver/...`.

### Release with signing
1. Create a keystore:
```bash
keytool -genkey -v -keystore imista-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias imista
```
2. Create `~/.gradle/gradle.properties` **or** project `gradle.properties` (never commit!) with:
```
MYAPP_RELEASE_STORE_FILE=/path/to/imista-release.jks
MYAPP_RELEASE_STORE_PASSWORD=yourStorePass
MYAPP_RELEASE_KEY_ALIAS=imista
MYAPP_RELEASE_KEY_PASSWORD=yourKeyPass
```
3. In `sender/build.gradle.kts` and `receiver/build.gradle.kts`, add inside `android {`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file(findProperty("MYAPP_RELEASE_STORE_FILE") as String)
        storePassword = findProperty("MYAPP_RELEASE_STORE_PASSWORD") as String
        keyAlias = findProperty("MYAPP_RELEASE_KEY_ALIAS") as String
        keyPassword = findProperty("MYAPP_RELEASE_KEY_PASSWORD") as String
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        ...
    }
}
```
4. Build:
```bash
./gradlew :sender:assembleRelease :receiver:assembleRelease
```
Outputs renamed by CI to `Imista-Forward-Sender-v1.0.0.apk` / `Receiver`.

If no signing config provided, release builds use debug keystore and CI logs a warning — acceptable for verification but not Play Store.

## CI Build (GitHub Actions)
`.github/workflows/build.yml`:
- Installs JDK 17, Android SDK 35, Gradle.
- Runs `shared/sender/receiver` unit tests.
- Validates `TELEGRAM_BOT_KEY` secret format if present (without printing).
- Builds both release APKs, generates `SHA256SUMS.txt`, uploads artifacts.
- On tag `v*`, creates GitHub Release with APKs.

## Verify APK
```bash
sha256sum artifacts/*.apk
adb install Imista-Forward-Sender-v1.0.0.apk
adb install Imista-Forward-Receiver-v1.0.0.apk
adb shell pm list packages | grep imista
```

Check:
- Different package IDs (`com.imista.forward.sender` vs `receiver`)
- Permissions: Sender requests Notification Listener + Device Admin optional; Receiver only Notification Listener optional.
- No embedded secrets: `strings Imista-Forward-Sender.apk | grep -i token` should be empty.

## Troubleshooting
- **No JDK**: Install Temurin 17, set `JAVA_HOME`.
- **SDK not found**: Set `ANDROID_SDK_ROOT` or `ANDROID_HOME`, run `sdkmanager --install "platforms;android-35"`.
- **R8 shrinking errors**: Check `proguard-rules.pro`; we keep models, security.
- **Telegram test fails**: Ensure bot token correct, chat started with bot, chat ID numeric.
