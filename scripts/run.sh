#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd -- "$SCRIPT_DIR/.." && pwd)

"$PROJECT_ROOT/scripts/build.sh"
command -v adb >/dev/null 2>&1 || {
    echo "adb is required to install and run the app." >&2
    exit 1
}
adb install -r "$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
adb shell am start -n com.example.smsforwarder/.MainActivity
