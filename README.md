# APK Patcher

Patches Android APKs to run games at custom resolution (360×806@160dpi) with 60fps cap.

## Build

```bash
./gradlew build
```

## Usage

```bash
java -jar build/libs/apk-patcher-1.0.0.jar input.apk output.apk
java -jar build/libs/apk-patcher-1.0.0.jar input.apk output.apk --sign
```

## What it does

- Injects DisplayMetrics hooks via Baksmali
- Forces 360×806@160dpi resolution
- Caps FPS at 60
- Repackages and signs APK

Works with Minecraft. May be detected by Roblox anti-cheat.
