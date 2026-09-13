# Build

## Prerequisites

- JDK 17 or newer
- Android SDK Platform 36 and Build Tools 35.0.0 or newer
- `ANDROID_HOME` or `ANDROID_SDK_ROOT` pointing to that SDK
- `curl` or `wget` for the first Gradle bootstrap

The project pins Gradle, Android Gradle Plugin, Kotlin, compile SDK 36, and target SDK 35 versions. The first build downloads and SHA-256-verifies the pinned Gradle distribution before resolving Maven artifacts; later builds use the Gradle cache.

The app's Java and Kotlin output is pinned to Java 17 bytecode. A newer JDK can run the build as long as it can produce Java 17 output.

## Commands

Run these from the repository root:

```bash
scripts/build.sh
scripts/run.sh                 # requires an already-connected device/emulator
scripts/test.sh
scripts/benchmark.sh
```

`scripts/run.sh` builds, installs, and starts the launcher activity using `adb`. All scripts determine the repository root from their own location, so their behavior does not depend on the current working directory.
