#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd -- "$SCRIPT_DIR/.." && pwd)

"$PROJECT_ROOT/scripts/validate-project.sh"
exec "$PROJECT_ROOT/gradlew" --no-daemon testDebugUnitTest \
    --tests com.example.smsforwarder.RouteMatcherBenchmarkTest
