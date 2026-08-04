# APK Patcher - Game Resolution Spoofing Tool

Patches Android APKs to run games at custom resolution (360×806@160dpi) with 60fps cap. Injects DisplayMetrics hooks directly into DEX bytecode.

**Tested:** Minecraft (works well), Roblox (may be detected by anti-cheat)

## Features

- ✅ Extracts and decompiles APK
- ✅ Injects DisplayMetrics spoofing hooks via Baksmali
- ✅ Recompiles DEX bytecode with patches
- ✅ Repackages and signs APK
- ✅ Full CLI tool (ready for CI/CD)

## Prerequisites

```bash
# Linux/Mac
apt-get install -y default-jdk gradle

# macOS
brew install openjdk gradle
```

## Build

```bash
./gradlew build

# Creates fat JAR with all dependencies
./gradlew shadowJar  # or use standard build
```

## Usage

### Direct JAR

```bash
java -jar build/libs/apk-patcher-1.0.0.jar input.apk output.apk

# With signing
java -jar build/libs/apk-patcher-1.0.0.jar input.apk output.apk --sign

# Verbose output
java -jar build/libs/apk-patcher-1.0.0.jar input.apk output.apk -v
```

### Gradle run

```bash
./gradlew run --args="minecraft.apk minecraft-patched.apk --sign"
```

## How It Works

1. **Extract** — Unpacks APK (ZIP format)
2. **Decompile** — DEX → Smali (Android bytecode) using Baksmali
3. **Inject Hooks** — Replaces DisplayMetrics calls with spoofed versions
4. **Recompile** — Smali → DEX using Smali
5. **Repackage** — Rebuilds APK with patched DEX
6. **Sign** — Signs with debug keystore (optional)

## Spoofed Resolution

```
Width:     360px
Height:    806px
DPI:       160dpi
Density:   1.0
FPS Cap:   60fps (via frame rate limiter)
```

## Project Structure

```
apk-patcher/
├── build.gradle.kts              # Gradle config with Baksmali/Smali deps
├── settings.gradle.kts
├── README.md
└── src/main/kotlin/
    ├── com/idk/apk/
    │   ├── Main.kt               # CLI entry point (Clikt)
    │   └── patcher/
    │       └── ApkPatcher.kt      # Core patcher logic
    └── com/idk/gamecloner/
        └── MetricsHook.kt        # Hook library (embedded in patched APK)
```

## GitHub Actions Setup

Add workflow file to `.github/workflows/patch.yml`:

```yaml
name: Patch APK

on:
  workflow_dispatch:
    inputs:
      apk_url:
        description: 'APK file URL or path'
        required: true

jobs:
  patch:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '11'
      - run: ./gradlew build
      - run: java -jar build/libs/apk-patcher-1.0.0.jar input.apk output.apk --sign
      - uses: actions/upload-artifact@v3
        with:
          name: patched-apk
          path: output.apk
```

## Limitations

- **Roblox** — Will likely detect bytecode patches (has anti-tamper checks)
- **CODM** — Anti-cheat will block this (checks for hooked methods)
- **Minecraft** — Works well (minimal anti-cheat checks)

## Debugging

Enable verbose mode:

```bash
java -jar build/libs/apk-patcher-1.0.0.jar input.apk output.apk -v
```

Check patched APK with:

```bash
unzip -l patched.apk | grep classes.dex
```

## Signing

For production signing, generate a keystore:

```bash
keytool -genkey -v \
  -keystore ~/.android/release.keystore \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias release
```

Then modify `ApkPatcher.kt` to use custom keystore path.

## License

MIT - Use freely for educational purposes
