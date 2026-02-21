#!/bin/bash

# Reinstall Android app while preserving configured devices
# Usage: ./reinstall.sh

PACKAGE="com.volumecontrol.android"
APK_PATH="./VolumeControl.apk"
BACKUP_DIR="/tmp/volume-control-backup-$(date +%s)"
DATASTORE_SUBDIR="files/datastore"
DEVICE="ZY22GN7CXW"

echo "🔄 Volume Control Android - Reinstall with Device Preservation"
echo "=============================================================="

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ Error: APK not found at $APK_PATH"
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ Error: No Android device connected"
    echo "   Connect device via USB and enable USB debugging"
    exit 1
fi

echo ""
echo "📦 Step 1: Backing up configured devices..."
mkdir -p "$BACKUP_DIR"

# Check if app is installed and has datastore
if adb -s $DEVICE shell "run-as $PACKAGE test -d $DATASTORE_SUBDIR" 2>/dev/null; then
    # Get list of files in datastore directory using run-as
    FILES=$(adb -s $DEVICE shell "run-as $PACKAGE ls $DATASTORE_SUBDIR" 2>/dev/null | tr -d '\r')

    if [ -n "$FILES" ]; then
        echo "   Found datastore files:"
        # Pull each file using run-as
        for file in $FILES; do
            adb -s $DEVICE shell "run-as $PACKAGE cat $DATASTORE_SUBDIR/$file" > "$BACKUP_DIR/$file" 2>/dev/null && \
                echo "   ✓ Backed up: $file"
        done
    else
        echo "   ℹ️  Datastore directory is empty (first install?)"
    fi
else
    echo "   ℹ️  No existing app or device configuration found"
fi

echo ""
echo "🗑️  Step 2: Uninstalling old version..."
if adb -s $DEVICE uninstall "$PACKAGE" > /dev/null 2>&1; then
    echo "   ✓ Old version uninstalled"
else
    echo "   ℹ️  App was not installed"
fi

echo ""
echo "📲 Step 3: Installing new version..."
if adb -s $DEVICE install "$APK_PATH" > /dev/null 2>&1; then
    echo "   ✓ New version installed"
else
    echo "❌ Failed to install APK"
    exit 1
fi

echo ""
echo "⏳ Step 4: Waiting for app initialization (10 seconds)..."
for i in {1..10}; do
    echo -n "."
    sleep 1
done
echo " Done"

# Check if backup exists and restore
if [ -n "$(ls -A $BACKUP_DIR 2>/dev/null)" ]; then
    echo ""
    echo "📥 Step 5: Restoring device configuration..."

    # Create datastore directory on device
    adb -s $DEVICE shell "run-as $PACKAGE mkdir -p $DATASTORE_SUBDIR" 2>/dev/null

    # Push each file back using temp location
    RESTORE_SUCCESS=0
    for file in "$BACKUP_DIR"/*; do
        if [ -f "$file" ]; then
            filename=$(basename "$file")
            # Push to temp location first
            if adb -s $DEVICE push "$file" "/data/local/tmp/$filename" > /dev/null 2>&1; then
                # Then copy from temp to app's datastore using run-as
                if adb -s $DEVICE shell "run-as $PACKAGE cp /data/local/tmp/$filename $DATASTORE_SUBDIR/$filename" > /dev/null 2>&1; then
                    # Cleanup temp file
                    adb -s $DEVICE shell "rm /data/local/tmp/$filename" > /dev/null 2>&1
                    echo "   ✓ Restored: $filename"
                    RESTORE_SUCCESS=1
                else
                    echo "   ⚠️  Could not restore: $filename"
                fi
            else
                echo "   ⚠️  Could not copy: $filename"
            fi
        fi
    done

    if [ $RESTORE_SUCCESS -eq 1 ]; then
        echo "   ✓ Device configuration successfully restored!"
    else
        echo "   ⚠️  Restore may have failed - check devices in app"
    fi
else
    echo ""
    echo "ℹ️  No backup found to restore (new install)"
fi

echo ""
echo "✅ Reinstall Complete!"
echo ""
echo "📝 Next steps:"
echo "   1. Close and reopen the Volume Control app on your device"
echo "   2. Your devices should be restored!"
echo "   3. If missing, reconfiguration takes ~1 minute"
echo ""
echo "📂 Backup saved to: $BACKUP_DIR"
echo "   (Keep this for 24h in case you need manual restore)"
