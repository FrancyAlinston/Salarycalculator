#!/usr/bin/env bash
set -e

DEVICE_IPS=(
    "192.168.8.246" # Pixel 8 Pro (Current DHCP)
    "192.168.8.178" # Pixel 8 Pro (Alternate)
    "192.168.8.168" # Redmi Note 10 Pro
)

APK_FILE="${1:-APKs/Salarycalculator-v25.2-alpha-debug.apk}"

if [ ! -f "$APK_FILE" ]; then
    APK_FILE=$(ls -t APKs/*.apk 2>/dev/null | head -n 1)
fi

echo "Deploying APK: $APK_FILE"

for ip in "${DEVICE_IPS[@]}"; do
    echo "Scanning active wireless debugging ports on $ip..."
    OPEN_PORTS=$(nmap -T4 -p 5555,35000-48000 "$ip" --open 2>/dev/null | awk -F'/' '/open/ {print $1}' || true)
    
    if [ -z "$OPEN_PORTS" ]; then
        echo "No open ADB ports found on $ip (device offline or Wi-Fi debug off)."
        continue
    fi

    for port in $OPEN_PORTS; do
        target="$ip:$port"
        echo "Attempting connect to $target..."
        adb connect "$target" 2>&1 >/dev/null || true
        # Verify the device is actually online in adb devices
        if adb devices | grep "$target" | grep -v "offline" | grep -q "device"; then
            echo "✓ Active device confirmed at $target! Installing..."
            if adb -s "$target" install -r "$APK_FILE"; then
                echo "✓ Successfully installed on $target"
                adb -s "$target" shell am start -n com.example.salarycalculator/.MainActivity || true
                break
            fi
        fi
    done
done

echo "Deployment cycle finished."
