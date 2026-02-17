#!/bin/bash
set -e

echo "Building VolumeControl..."

# Compile with swiftc targeting Catalina x86_64
swiftc -sdk $(xcrun --sdk macosx --show-sdk-path) \
       -target x86_64-apple-macosx10.15 \
       -framework AppKit -framework Foundation \
       -framework Network \
       Sources/*.swift -o VolumeControl

echo "Compilation successful"

# Create .app bundle
mkdir -p VolumeControl.app/Contents/MacOS
cp VolumeControl VolumeControl.app/Contents/MacOS/
cp Info.plist VolumeControl.app/Contents/

echo ".app bundle created at VolumeControl.app"

# Strip quarantine to bypass Gatekeeper on unsigned app
xattr -cr VolumeControl.app || true

echo "Build complete! Run ./install.sh to install the app."
