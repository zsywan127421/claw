#!/bin/bash
set -e

echo "========================================"
echo "  TermuxDeepSeek Build Script"
echo "========================================"

cd "$(dirname "$0")"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

if [ ! -d "$ANDROID_HOME" ]; then
    echo "ERROR: ANDROID_HOME not set. Please install Android SDK."
    exit 1
fi

if [ ! -f "gradlew" ]; then
    echo "Downloading Gradle Wrapper..."
    mkdir -p gradle/wrapper
    curl -sL https://services.gradle.org/distributions/gradle-8.2-bin.zip -o gradle.zip
    unzip -q gradle.zip
    ./gradle-8.2/bin/gradle wrapper
    rm -rf gradle.zip gradle-8.2
fi

chmod +x gradlew
echo "Building Android APK..."
./gradlew assembleDebug --no-daemon

echo ""
echo "========================================"
echo "  Build Complete!"
echo "========================================"
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
