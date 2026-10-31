#!/usr/bin/env bash
set -e

PHONE_APK="${1:-$(ls -t APKs/Salarycalculator-v[0-9]*.apk 2>/dev/null | grep -v 'wear' | head -n 1)}"
WEAR_APK="${2:-$(ls -t APKs/Salarycalculator-wear-v[0-9]*.apk 2>/dev/null | head -n 1)}"

echo "Phone APK: $PHONE_APK"
echo "Wear APK:  $WEAR_APK"

python - "$PHONE_APK" "$WEAR_APK" << 'EOF'
import os
import sys
import socket
import subprocess
import concurrent.futures

phone_apk = sys.argv[1] if len(sys.argv) > 1 else ""
wear_apk = sys.argv[2] if len(sys.argv) > 2 else ""

adb_candidates = [
    r"C:\Android\sdk\platform-tools\adb.exe",
    "/c/Android/sdk/platform-tools/adb.exe",
    "adb"
]
ADB = next((p for p in adb_candidates if os.path.exists(p)), "adb")

device_ips = [
    "192.168.8.232", # Samsung Galaxy S25 Ultra (SM-S938B)
    "192.168.8.162", # Samsung Galaxy Watch 8 Classic (SM-L505F)
    "192.168.8.246", # Pixel 8 Pro (Current DHCP)
    "192.168.8.178", # Pixel 8 Pro (Alternate)
    "192.168.8.168", # Redmi Note 10 Pro
]

subprocess.run([ADB, "start-server"], capture_output=True)

def scan_ip(ip):
    def check(p):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(0.15)
        try:
            if s.connect_ex((ip, p)) == 0:
                return p
        except Exception:
            pass
        finally:
            s.close()
        return None

    ports = [5555] + list(range(30000, 50000))
    with concurrent.futures.ThreadPoolExecutor(max_workers=256) as ex:
        return [r for r in ex.map(check, ports) if r]

for ip in device_ips:
    print(f"Scanning active wireless debugging ports on {ip}...")
    open_ports = scan_ip(ip)
    if not open_ports:
        print(f"  No open ADB ports found on {ip} (device offline or Wi-Fi sleeping).")
        continue
    print(f"  Open ports on {ip}: {open_ports}")
    for p in open_ports:
        serial = f"{ip}:{p}"
        conn = subprocess.run([ADB, "connect", serial], capture_output=True, text=True)
        out = (conn.stdout or "") + (conn.stderr or "")
        if "connected to" in out.lower() or "already connected" in out.lower():
            chars = subprocess.run([ADB, "-s", serial, "shell", "getprop", "ro.build.characteristics"], capture_output=True, text=True).stdout.strip()
            model = subprocess.run([ADB, "-s", serial, "shell", "getprop", "ro.product.model"], capture_output=True, text=True).stdout.strip()
            if not model:
                continue
            print(f"  ✓ Connected to {serial} — Model: {model} (Characteristics: {chars})")
            if "watch" in chars.lower():
                target_apk = wear_apk
                launch_cmp = "com.example.salarycalculator/com.example.salarycalculator.wear.MainActivity"
            else:
                target_apk = phone_apk
                launch_cmp = "com.example.salarycalculator/.MainActivity"

            if target_apk and os.path.exists(target_apk):
                print(f"  Installing {target_apk} onto {serial}...")
                inst = subprocess.run([ADB, "-s", serial, "install", "-r", target_apk], capture_output=True, text=True)
                print(f"  {inst.stdout.strip()} {inst.stderr.strip()}")
                subprocess.run([ADB, "-s", serial, "shell", "am", "start", "-n", launch_cmp], capture_output=True)
                break

print("Deployment cycle finished.")
EOF
