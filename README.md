# UAMT Android App

The Ultimate Android Modding Toolkit, ported to a native Android Application.

## Features
- **Frida Injection:** Native and Smali strategies.
- **Custom Lib Injection:** Inject any .so into any APK.
- **Auto-Detection:** Smart detection of major libraries (Unity, IL2CPP, etc.).
- **Architecture Support:** Multi-arch selection (arm64-v8a, armeabi-v7a, x86, x86_64).
- **Zipalign & Signing:** Built-in APK finalization.
- **Terminal Integration:** Connect to running Frida gadgets.

## Build
This project uses Gradle.
- Run `./gradlew assembleDebug` to build the debug APK.
- GitHub Actions is configured to build on every push.

## Requirements (Local Testing)
To use the injection features locally on an Android device (via Termux or similar), you need:
- `patchelf`
- `openjdk`
- `aapt2`
- `apksigner`
- `zipalign`

These can be installed via the "Install and Update Tools" button in the app.
