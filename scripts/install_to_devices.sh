#!/usr/bin/env bash
set -e

PHONE_APK="${1:-$(ls -t APKs/Salarycalculator-v[0-9]*.apk 2>/dev/null | grep -v 'wear' | grep -v 'watchface' | head -n 1)}"
WEAR_APK="${2:-$(ls -t APKs/Salarycalculator-wear-v[0-9]*.apk 2>/dev/null | head -n 1)}"
WF_APK="${3:-$(ls -t APKs/Salarycalculator-watchface-v[0-9]*.apk 2>/dev/null | head -n 1)}"

echo "Phone APK:     $PHONE_APK"
echo "Wear APK:      $WEAR_APK"
echo "WatchFace APK: $WF_APK"

PYTHON_BIN="$(command -v python 2>/dev/null || command -v python3 2>/dev/null || echo /c/Python314/python.exe)"
"$PYTHON_BIN" - "$PHONE_APK" "$WEAR_APK" "$WF_APK" << 'EOF'
import os
import sys
import time
import socket
import struct
import threading
import subprocess
import concurrent.futures

phone_apk = sys.argv[1] if len(sys.argv) > 1 else ""
wear_apk = sys.argv[2] if len(sys.argv) > 2 else ""
wf_apk = sys.argv[3] if len(sys.argv) > 3 else ""

adb_candidates = [
    r"C:\Android\sdk\platform-tools\adb.exe",
    "/mnt/c/Android/sdk/platform-tools/adb.exe",
    "/c/Android/sdk/platform-tools/adb.exe",
    "adb"
]
ADB = next((p for p in adb_candidates if os.path.exists(p)), "adb")

device_ips = [
    "192.168.8.162", # Samsung Galaxy Watch 8 Classic (SM-L505F)
    "192.168.8.15",  # Samsung Galaxy Watch 8 Classic (Alternate DHCP)
    "192.168.8.232", # Samsung Galaxy S25 Ultra (SM-S938B)
    "192.168.8.246", # Pixel 8 Pro (Current DHCP)
    "192.168.8.178", # Pixel 8 Pro (Alternate)
    "192.168.8.168", # Redmi Note 10 Pro
]

subprocess.run([ADB, "start-server"], capture_output=True)

def discover_mdns_ports():
    """Fast mDNS SRV discovery bound to the 192.168.8.x LAN interface."""
    discovered = {}
    try:
        s_probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s_probe.connect(("192.168.8.1", 80))
        local_ip = s_probe.getsockname()[0]
        s_probe.close()

        pkt = struct.pack("!HHHHHH", 0, 0, 1, 0, 0, 0)
        for part in "_adb-tls-connect._tcp.local".split("."):
            b = part.encode("utf-8")
            pkt += struct.pack("!B", len(b)) + b
        pkt += b"\x00" + struct.pack("!HH", 12, 1)

        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((local_ip, 0))
        sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_IF, socket.inet_aton(local_ip))
        sock.settimeout(1.2)
        sock.sendto(pkt, ("224.0.0.251", 5353))

        t0 = time.time()
        while time.time() - t0 < 1.8:
            try:
                data, addr = sock.recvfrom(4096)
                ip = addr[0]
                for i in range(len(data) - 1):
                    p = struct.unpack("!H", data[i:i+2])[0]
                    if 30000 <= p <= 65535:
                        ts = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                        ts.settimeout(0.2)
                        if ts.connect_ex((ip, p)) == 0:
                            discovered.setdefault(ip, set()).add(p)
                        ts.close()
            except Exception:
                break
        sock.close()
    except Exception:
        pass
    return discovered

mdns_map = discover_mdns_ports()
if mdns_map:
    print(f"mDNS discovered active ADB endpoints: {mdns_map}")

def scan_ip(ip):
    if ip in mdns_map and mdns_map[ip]:
        return sorted(list(mdns_map[ip]))

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
        time.sleep(2.0) # Allow TLS 1.3 wireless debugging handshake to complete
        out = (conn.stdout or "") + (conn.stderr or "")
        if "connected to" in out.lower() or "already connected" in out.lower():
            chars = subprocess.run([ADB, "-s", serial, "shell", "getprop", "ro.build.characteristics"], capture_output=True, text=True).stdout.strip()
            model = subprocess.run([ADB, "-s", serial, "shell", "getprop", "ro.product.model"], capture_output=True, text=True).stdout.strip()
            if not model:
                continue
            print(f"  ✓ Connected to {serial} — Model: {model} (Characteristics: {chars})")
            if "watch" in chars.lower():
                subprocess.run([ADB, "-s", serial, "shell", "svc", "power", "stayon", "true"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "system", "screen_off_timeout", "300000"], capture_output=True)
                # Keep Wi-Fi connected and active when watch is charging and prevent sleeping
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "global", "cw_enable_wifi_when_charging", "1"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "global", "stay_on_while_plugged_in", "15"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "global", "wifi_sleep_policy", "2"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "global", "wifi_stay_awake_while_charging", "1"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "global", "wifi_on", "1"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "global", "adb_enabled", "1"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "global", "adb_wifi_enabled", "1"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "settings", "put", "secure", "clockwork_wifi_setting", "on"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "dumpsys", "deviceidle", "whitelist", "+com.example.salarycalculator"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "dumpsys", "deviceidle", "whitelist", "+com.example.salarycalculator.watchface"], capture_output=True)
                subprocess.run([ADB, "-s", serial, "shell", "dumpsys", "deviceidle", "whitelist", "+com.google.android.apps.wearable.settings"], capture_output=True)
                stop_ka = False
                def keep_awake():
                    while not stop_ka:
                        subprocess.run([ADB, "-s", serial, "shell", "input", "keyevent", "224"], capture_output=True)
                        time.sleep(3.0)
                t = threading.Thread(target=keep_awake, daemon=True)
                t.start()
                if wf_apk and os.path.exists(wf_apk):
                    print(f"  Installing WFF Watch Face {wf_apk} onto {serial}...")
                    inst_wf = subprocess.run([ADB, "-s", serial, "install", "-r", wf_apk], capture_output=True, text=True)
                    print(f"  {inst_wf.stdout.strip()} {inst_wf.stderr.strip()}")
                if wear_apk and os.path.exists(wear_apk):
                    print(f"  Installing Wear App {wear_apk} onto {serial}...")
                    inst = subprocess.run([ADB, "-s", serial, "install", "-r", wear_apk], capture_output=True, text=True)
                    print(f"  {inst.stdout.strip()} {inst.stderr.strip()}")
                stop_ka = True
                subprocess.run([ADB, "-s", serial, "shell", "am", "start", "-n", "com.example.salarycalculator/com.example.salarycalculator.wear.MainActivity"], capture_output=True)
                break
            else:
                if phone_apk and os.path.exists(phone_apk):
                    print(f"  Installing Phone App {phone_apk} onto {serial}...")
                    inst = subprocess.run([ADB, "-s", serial, "install", "-r", phone_apk], capture_output=True, text=True)
                    print(f"  {inst.stdout.strip()} {inst.stderr.strip()}")
                    subprocess.run([ADB, "-s", serial, "shell", "am", "start", "-n", "com.example.salarycalculator/.MainActivity"], capture_output=True)
                    break

print("Deployment cycle finished.")
EOF
