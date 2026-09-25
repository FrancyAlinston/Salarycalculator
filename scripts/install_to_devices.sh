#!/usr/bin/env bash
set -e

# Target Devices Registry
DEVICES=(
    "192.168.8.178:38089" # Pixel 8 Pro (Primary)
    "192.168.8.168:39873" # Second Phone
    "192.168.8.168:5555"
)

APK_FILE="${1:-APKs/Salarycalculator-v25.2-alpha-debug.apk}"

if [ ! -f "$APK_FILE" ]; then
    echo "APK not found at $APK_FILE, searching in APKs/..."
    APK_FILE=$(ls -t APKs/*.apk 2>/dev/null | head -n 1)
fi

echo "Deploying APK: $APK_FILE"

for dev in "${DEVICES[@]}"; do
    echo "Checking device $dev..."
    if adb connect "$dev" 2>&1 | grep -q "connected to"; then
        echo "Installing to $dev..."
        adb -s "$dev" install -r "$APK_FILE" || echo "Install failed on $dev"
        adb -s "$dev" shell am start -n com.example.salarycalculator/.MainActivity || true
    else
        echo "Device $dev is not available or offline."
    fi
done
