#!/bin/bash

# Setup script for Android project
# This script generates the Gradle wrapper if it doesn't exist

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Setting up Android project..."

# Check if gradlew exists and can be executed
if [ ! -x ./gradlew ]; then
    echo "Gradle wrapper not properly set up. Please ensure:"
    echo "1. You have Gradle installed (https://gradle.org/install/)"
    echo "2. Or use Android Studio to open this project"
    echo ""
    echo "To initialize with system Gradle:"
    echo "  gradle wrapper --gradle-version 8.5"
    exit 1
fi

echo "✓ Gradle wrapper found"
echo ""
echo "Project setup complete. You can now:"
echo "  - Open with Android Studio (recommended)"
echo "  - Or run: ./gradlew assembleDebug"
