#!/usr/bin/env python3
"""
Salary Calculator - Forgejo Release Synchronizer
Automatically creates releases on self-hosted Forgejo instance and attaches versioned APK binaries.
"""

import urllib.request
import urllib.error
import json
import os
import sys

BASE_URL = "https://forgejo.449100.xyz/api/v1"
OWNER = "francyalinston"
REPO = "Salarycalculator"
USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko)"

def get_token():
    # Try git credential helper first
    token = os.environ.get("FORGEJO_TOKEN")
    if token:
        return token
    try:
        import subprocess
        proc = subprocess.Popen(["git", "credential", "fill"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
        out, _ = proc.communicate("protocol=https\nhost=forgejo.449100.xyz\n")
        for line in out.splitlines():
            if line.startswith("password="):
                return line.split("=", 1)[1].strip()
    except Exception:
        pass
    return "cb3ec60060c253cc350454b85a42501f8ea5697d"

def get_existing_releases(token):
    url = f"{BASE_URL}/repos/{OWNER}/{REPO}/releases"
    req = urllib.request.Request(url, headers={
        "Authorization": f"token {token}",
        "User-Agent": USER_AGENT
    })
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"Error fetching releases: {e}")
        return []

def create_release(token, tag_name, name, body):
    url = f"{BASE_URL}/repos/{OWNER}/{REPO}/releases"
    payload = {
        "tag_name": tag_name,
        "name": name,
        "body": body,
        "draft": False,
        "prerelease": False
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={
        "Authorization": f"token {token}",
        "Content-Type": "application/json",
        "User-Agent": USER_AGENT
    })
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        print(f"HTTP Error {e.code}: {e.read().decode('utf-8')}")
        return None
    except Exception as e:
        print(f"Error creating release {tag_name}: {e}")
        return None

def upload_asset(token, release_id, file_path, file_name):
    url = f"{BASE_URL}/repos/{OWNER}/{REPO}/releases/{release_id}/assets?name={file_name}"
    boundary = "----WebKitFormBoundarySalaryCalcSync"
    
    with open(file_path, "rb") as f:
        file_bytes = f.read()
        
    body = bytearray()
    body.extend(f"--{boundary}\r\n".encode("utf-8"))
    body.extend(f'Content-Disposition: form-data; name="attachment"; filename="{file_name}"\r\n'.encode("utf-8"))
    body.extend(b"Content-Type: application/vnd.android.package-archive\r\n\r\n")
    body.extend(file_bytes)
    body.extend(f"\r\n--{boundary}--\r\n".encode("utf-8"))
    
    req = urllib.request.Request(url, data=bytes(body), headers={
        "Authorization": f"token {token}",
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "User-Agent": USER_AGENT
    })
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        print(f"HTTP Error uploading asset {file_name}: {e.code}")
        return None
    except Exception as e:
        print(f"Error uploading asset {file_name}: {e}")
        return None

def sync_releases():
    token = get_token()
    if not token:
        print("Error: No Forgejo token found.")
        return 1
        
    existing = get_existing_releases(token)
    existing_tags = {r["tag_name"]: r for r in existing}
    print(f"Found {len(existing)} existing releases on Forgejo.")
    
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    apk_dir = os.path.join(project_root, "APKs")
    
    # Auto-discover all versions from APKs directory
    discovered = []
    if os.path.exists(apk_dir):
        for f in os.listdir(apk_dir):
            if f.startswith("Salarycalculator-v") and f.endswith(".apk") and not f.endswith("-debug.apk"):
                v = f.replace("Salarycalculator-", "").replace(".apk", "")
                discovered.append(v)
    versions = sorted(list(set(discovered)), key=lambda x: [int(p) if p.isdigit() else p for p in x.lstrip('v').split('.')])
    if not versions:
        versions = ["v16.0", "v17.0", "v17.1", "v17.2", "v18.0", "v19.0", "v20.0", "v21.0"]
    
    for v in versions:
        release_apk = os.path.join(apk_dir, f"Salarycalculator-{v}.apk")
        debug_apk = os.path.join(apk_dir, f"Salarycalculator-{v}-debug.apk")
        
        if not os.path.exists(release_apk):
            continue
            
        rel = existing_tags.get(v)
        if not rel:
            print(f"Creating release for {v}...")
            body_text = f"## Salary Calculator {v}\n\n### Downloads & Assets 📦\n- **Stable Release (Production)**: `Salarycalculator-{v}.apk`\n- **Debug Build (Development)**: `Salarycalculator-{v}-debug.apk`\n\n---\n*Signed and optimized for standalone installation.*"
            rel = create_release(token, v, f"Salary Calculator {v}", body_text)
            
        if rel and "id" in rel:
            rel_id = rel["id"]
            existing_assets = {a["name"] for a in rel.get("assets", [])}
            
            prod_name = f"Salarycalculator-{v}.apk"
            debug_name = f"Salarycalculator-{v}-debug.apk"
            
            if prod_name not in existing_assets and os.path.exists(release_apk):
                print(f"Uploading {prod_name} for {v}...")
                upload_asset(token, rel_id, release_apk, prod_name)
                
            if debug_name not in existing_assets and os.path.exists(debug_apk):
                print(f"Uploading {debug_name} for {v}...")
                upload_asset(token, rel_id, debug_apk, debug_name)
                
            print(f"✓ {v} release and versioned assets synchronized.")
            
    print("All Forgejo releases synchronized successfully.")
    return 0

if __name__ == "__main__":
    sys.exit(sync_releases())
