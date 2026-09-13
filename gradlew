#!/usr/bin/env sh
# Small, checked-in Gradle bootstrap for clean checkouts without a wrapper JAR.
set -eu

APP_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.10.2
GRADLE_SHA256=31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26
GRADLE_HOME_DIR="${GRADLE_USER_HOME:-$APP_ROOT/.gradle-user-home}/wrapper/dists/gradle-$GRADLE_VERSION"
GRADLE_DIR="$GRADLE_HOME_DIR/gradle-$GRADLE_VERSION"
GRADLE_ZIP="$GRADLE_HOME_DIR/gradle-$GRADLE_VERSION-bin.zip"

command -v java >/dev/null 2>&1 || {
    echo "gradlew requires JDK 17 or newer on PATH." >&2
    exit 1
}

if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
    mkdir -p "$GRADLE_HOME_DIR"
    if [ ! -f "$GRADLE_ZIP" ]; then
        TEMP_ZIP="$GRADLE_ZIP.download"
        rm -f "$TEMP_ZIP"
        GRADLE_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
        if command -v curl >/dev/null 2>&1; then
            curl --fail --location --retry 3 --output "$TEMP_ZIP" "$GRADLE_URL"
        elif command -v wget >/dev/null 2>&1; then
            wget --output-document="$TEMP_ZIP" "$GRADLE_URL"
        else
            echo "gradlew requires curl or wget to download Gradle $GRADLE_VERSION" >&2
            exit 1
        fi
        mv "$TEMP_ZIP" "$GRADLE_ZIP"
    fi
    if command -v sha256sum >/dev/null 2>&1; then
        ACTUAL_SHA256=$(sha256sum "$GRADLE_ZIP" | awk '{print $1}')
    elif command -v shasum >/dev/null 2>&1; then
        ACTUAL_SHA256=$(shasum -a 256 "$GRADLE_ZIP" | awk '{print $1}')
    else
        echo "gradlew requires sha256sum or shasum to verify Gradle $GRADLE_VERSION" >&2
        exit 1
    fi
    if [ "$ACTUAL_SHA256" != "$GRADLE_SHA256" ]; then
        echo "Gradle $GRADLE_VERSION download checksum did not match." >&2
        exit 1
    fi
    command -v unzip >/dev/null 2>&1 || {
        echo "gradlew requires unzip to unpack Gradle $GRADLE_VERSION" >&2
        exit 1
    }
    unzip -q -o "$GRADLE_ZIP" -d "$GRADLE_HOME_DIR"
fi

exec "$GRADLE_DIR/bin/gradle" -p "$APP_ROOT" "$@"
