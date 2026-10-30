#!/usr/bin/env bash
set -e

DEVICE_IPS=(
    "192.168.8.246" # Pixel 8 Pro (Current DHCP)
    "192.168.8.178" # Pixel 8 Pro (Alternate)
    "192.168.8.168" # Redmi Note 10 Pro
)

PHONE_APK="${1:-$(ls -t APKs/Salarycalculator-v[0-9]*.apk 2>/dev/null | grep -v 'wear' | head -n 1)}"
WEAR_APK="${2:-$(ls -t APKs/Salarycalculator-wear-v[0-9]*.apk 2>/dev/null | head -n 1)}"

echo "Phone APK: $PHONE_APK"
echo "Wear APK:  $WEAR_APK"

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
            echo "✓ Active device confirmed at $target!"
            CHARACTERISTICS=$(adb -s "$target" shell getprop ro.build.characteristics 2>/dev/null || echo "")
            if echo "$CHARACTERISTICS" | grep -q "watch"; then
                echo "Deploying Wear OS build ($WEAR_APK) to watch at $target..."
                if [ -f "$WEAR_APK" ] && adb -s "$target" install -r "$WEAR_APK"; then
                    echo "✓ Successfully installed Wear OS app on $target"
                    adb -s "$target" shell am start -n com.example.salarycalculator.wear/.MainActivity || true
                    break
                fi
            else
                echo "Deploying Phone build ($PHONE_APK) to $target..."
                if [ -f "$PHONE_APK" ] && adb -s "$target" install -r "$PHONE_APK"; then
                    echo "✓ Successfully installed Phone app on $target"
                    adb -s "$target" shell am start -n com.example.salarycalculator/.MainActivity || true
                    break
                fi
            fi
        fi
    done
done

echo "Deployment cycle finished."
