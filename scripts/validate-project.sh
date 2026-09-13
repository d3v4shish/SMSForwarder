#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd -- "$SCRIPT_DIR/.." && pwd)

required_files=(
    README.md TODO.md BUILD.md ARCHITECTURE.md BENCHMARKS.md HOTSPOTS.md
    gradlew app/src/main/AndroidManifest.xml
    app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
    app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
    app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png
    app/src/main/java/com/example/smsforwarder/MainActivity.kt
    app/src/main/java/com/example/smsforwarder/SmsReceiver.kt
    app/src/main/java/com/example/smsforwarder/RouteMatcher.kt
)

for file in "${required_files[@]}"; do
    test -f "$PROJECT_ROOT/$file" || {
        echo "Missing required file: $file" >&2
        exit 1
    }
done

grep -q 'SMS_RECEIVED' "$PROJECT_ROOT/app/src/main/AndroidManifest.xml"
grep -q 'android.permission.RECEIVE_SMS' "$PROJECT_ROOT/app/src/main/AndroidManifest.xml"
grep -q 'android.permission.SEND_SMS' "$PROJECT_ROOT/app/src/main/AndroidManifest.xml"
grep -q 'android:icon="@mipmap/ic_launcher"' "$PROJECT_ROOT/app/src/main/AndroidManifest.xml"
grep -q '@mipmap/ic_launcher_foreground' "$PROJECT_ROOT/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"
grep -q 'goAsync()' "$PROJECT_ROOT/app/src/main/java/com/example/smsforwarder/SmsReceiver.kt"
grep -q 'newSingleThreadExecutor' "$PROJECT_ROOT/app/src/main/java/com/example/smsforwarder/SmsReceiver.kt"
grep -q 'Enter a sender or message condition' "$PROJECT_ROOT/app/src/main/java/com/example/smsforwarder/MainActivity.kt"
grep -q 'technicalText(route.destination' "$PROJECT_ROOT/app/src/main/java/com/example/smsforwarder/MainActivity.kt"
grep -q '<color name="primary">#2563EB</color>' "$PROJECT_ROOT/app/src/main/res/values/colors.xml"
grep -q '<color name="success">#16A34A</color>' "$PROJECT_ROOT/app/src/main/res/values/colors.xml"
grep -q '<color name="warning">#F59E0B</color>' "$PROJECT_ROOT/app/src/main/res/values/colors.xml"
grep -q '<color name="error">#DC2626</color>' "$PROJECT_ROOT/app/src/main/res/values/colors.xml"
grep -q 'JavaVersion.VERSION_17' "$PROJECT_ROOT/app/build.gradle.kts"
grep -q 'jvmTarget = "17"' "$PROJECT_ROOT/app/build.gradle.kts"
if grep -R -Eq '<service|WorkManager|AlarmManager' "$PROJECT_ROOT/app/src/main"; then
    echo "Unexpected persistent or scheduled background component found." >&2
    exit 1
fi

echo "Static project validation passed."
