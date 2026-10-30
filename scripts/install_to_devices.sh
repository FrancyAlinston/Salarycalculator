#!/usr/bin/env bash
set -e

ADB_BIN="$(command -v adb 2>/dev/null || true)"
if [ -z "$ADB_BIN" ] && [ -f "/c/Android/sdk/platform-tools/adb.exe" ]; then
    ADB_BIN="/c/Android/sdk/platform-tools/adb.exe"
elif [ -z "$ADB_BIN" ] && [ -f "C:/Android/sdk/platform-tools/adb.exe" ]; then
    ADB_BIN="C:/Android/sdk/platform-tools/adb.exe"
else
    ADB_BIN="${ADB_BIN:-adb}"
fi

DEVICE_IPS=(
    "192.168.8.246" # Pixel 8 Pro (Current DHCP)
    "192.168.8.178" # Pixel 8 Pro (Alternate)
    "192.168.8.168" # Redmi Note 10 Pro
    "192.168.8.232" # Samsung Galaxy Watch 8 Classic / LAN Target
)

PHONE_APK="${1:-$(ls -t APKs/Salarycalculator-v[0-9]*.apk 2>/dev/null | grep -v 'wear' | head -n 1)}"
WEAR_APK="${2:-$(ls -t APKs/Salarycalculator-wear-v[0-9]*.apk 2>/dev/null | head -n 1)}"

echo "Phone APK: $PHONE_APK"
echo "Wear APK:  $WEAR_APK"

scan_open_ports() {
    local target_ip="$1"
    if command -v nmap >/dev/null 2>&1; then
        nmap -T4 -p 5555,35000-48000 "$target_ip" --open 2>/dev/null | awk -F'/' '/open/ {print $1}' || true
    elif command -v python >/dev/null 2>&1; then
        python -c "
import socket
ip = '$target_ip'
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.settimeout(0.35)
if s.connect_ex((ip, 5555)) == 0:
    print(5555)
s.close()
" 2>/dev/null || true
    fi
}

install_to_serial() {
    local target="$1"
    echo "✓ Active device confirmed at $target!"
    CHARACTERISTICS=$("$ADB_BIN" -s "$target" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r' || echo "")
    MODEL=$("$ADB_BIN" -s "$target" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "")
    echo "  Device Model: $MODEL (Characteristics: $CHARACTERISTICS)"
    if echo "$CHARACTERISTICS" | grep -q "watch"; then
        echo "  Deploying Samsung Galaxy Watch 8 Classic Wear OS build ($WEAR_APK) to $target..."
        if [ -f "$WEAR_APK" ] && "$ADB_BIN" -s "$target" install -r "$WEAR_APK"; then
            echo "  ✓ Successfully installed Wear OS app on $target"
            "$ADB_BIN" -s "$target" shell am start -n com.example.salarycalculator/com.example.salarycalculator.wear.MainActivity || true
            return 0
        fi
    else
        echo "  Deploying Phone build ($PHONE_APK) to $target..."
        if [ -f "$PHONE_APK" ] && "$ADB_BIN" -s "$target" install -r "$PHONE_APK"; then
            echo "  ✓ Successfully installed Phone app on $target"
            "$ADB_BIN" -s "$target" shell am start -n com.example.salarycalculator/.MainActivity || true
            return 0
        fi
    fi
    return 1
}

# 1. Check already-attached ADB devices first (USB or Wireless)
EXISTING_SERIALS=$("$ADB_BIN" devices 2>/dev/null | tr -d '\r' | awk 'NR>1 && $2=="device" {print $1}' || true)
for serial in $EXISTING_SERIALS; do
    install_to_serial "$serial" || true
done

# 2. Scan registered wireless IPs
for ip in "${DEVICE_IPS[@]}"; do
    echo "Scanning active wireless debugging ports on $ip..."
    OPEN_PORTS=$(scan_open_ports "$ip")
    
    if [ -z "$OPEN_PORTS" ]; then
        echo "No open ADB ports found on $ip (device offline or Wi-Fi debug off)."
        continue
    fi

    for port in $OPEN_PORTS; do
        target="$ip:$port"
        echo "Attempting connect to $target..."
        "$ADB_BIN" connect "$target" 2>&1 >/dev/null || true
        if "$ADB_BIN" devices | tr -d '\r' | grep "$target" | grep -v "offline" | grep -q "device"; then
            if install_to_serial "$target"; then
                break
            fi
        fi
    done
done

echo "Deployment cycle finished."
