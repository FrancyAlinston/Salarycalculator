#!/usr/bin/env python3
"""
Deploy Salary Calculator Web Container to UGREEN UGOS NAS Docker Management App
Target: NVMe Volume 2 (/volume2/docker/salarycalculator) with UGOS DB Auto-Registration
"""
import base64
import os
import sqlite3
import sys
import tarfile
import tempfile
import time
from pathlib import Path

try:
    import paramiko
except ImportError:
    print("Paramiko not found, installing...")
    os.system("pip install paramiko")
    import paramiko

HOST = "192.168.8.100"
USERNAME = "Francy"
PASSWORD = "Alen@2020"
TARGET_DIR = "/volume2/docker/salarycalculator"
LEGACY_DIR = "/volume1/docker/salarycalculator"
PORT = 8085

print(f"🚀 Deploying Salary Calculator to UGOS NAS NVMe Volume ({TARGET_DIR})...")

project_dir = Path(__file__).resolve().parent.parent
temp_tar = Path(tempfile.gettempdir()) / "salarycalculator-src.tar.gz"

exclude_names = {
    ".git", ".gradle", "build", "app/build", "__pycache__",
    ".pytest_cache", "venv", ".venv", "scratch", ".idea", ".vscode",
    "APKs", "local.properties"
}

def exclude_filter(tarinfo):
    name = Path(tarinfo.name).name
    if name in exclude_names or any(part in exclude_names for part in Path(tarinfo.name).parts):
        return None
    return tarinfo

print(f"📦 Creating source tarball from {project_dir}...")
with tarfile.open(temp_tar, "w:gz") as tar:
    tar.add(project_dir, arcname=".", filter=exclude_filter)

print(f"✅ Tarball created ({round(temp_tar.stat().st_size / 1024 / 1024, 2)} MB)")

docker_compose_content = """services:
  salarycalculator-web:
    build: .
    container_name: salarycalculator-web
    restart: always
    ports:
      - "8085:8080"
    volumes:
      - ./web:/app/web:ro
      - ./data:/app/data
    environment:
      - DOCKER_ENV=true
      - PORT=8080
      - HOST=0.0.0.0
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/status"]
      interval: 30s
      timeout: 10s
      retries: 3
"""

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    print(f"🔌 Connecting to {USERNAME}@{HOST}...")
    ssh.connect(HOST, username=USERNAME, password=PASSWORD, timeout=15)
    print("✅ Connected to NAS.")

    print("🛑 Stopping previous container and cleaning legacy paths...")
    stdin, stdout, stderr = ssh.exec_command(f"echo '{PASSWORD}' | sudo -S docker rm -f salarycalculator-web 2>/dev/null || true")
    stdout.channel.recv_exit_status()
    stdin, stdout, stderr = ssh.exec_command(f"echo '{PASSWORD}' | sudo -S rm -rf {LEGACY_DIR} 2>/dev/null || true")
    stdout.channel.recv_exit_status()

    print(f"📁 Creating target project folder in NVMe {TARGET_DIR}...")
    stdin, stdout, stderr = ssh.exec_command(f"echo '{PASSWORD}' | sudo -S mkdir -p {TARGET_DIR}/data {TARGET_DIR}/web")
    stdout.channel.recv_exit_status()
    stdin, stdout, stderr = ssh.exec_command(f"echo '{PASSWORD}' | sudo -S chmod -R 777 {TARGET_DIR}")
    stdout.channel.recv_exit_status()

    print("📤 Uploading source files to NVMe Volume...")
    stdin, stdout, stderr = ssh.exec_command(f"cat > {TARGET_DIR}/salarycalculator-src.tar.gz")
    with open(temp_tar, 'rb') as f:
        stdin.write(f.read())
    stdin.close()
    stdout.channel.recv_exit_status()

    print("📂 Extracting files on NAS...")
    stdin, stdout, stderr = ssh.exec_command(f"cd {TARGET_DIR} && tar -xzf salarycalculator-src.tar.gz && rm salarycalculator-src.tar.gz")
    stdout.channel.recv_exit_status()

    print("📝 Writing docker-compose configuration files...")
    stdin, stdout, stderr = ssh.exec_command(f"cat << 'EOF' > {TARGET_DIR}/docker-compose.yaml\n{docker_compose_content}\nEOF")
    stdout.channel.recv_exit_status()
    stdin, stdout, stderr = ssh.exec_command(f"cat << 'EOF' > {TARGET_DIR}/docker-compose.yml\n{docker_compose_content}\nEOF")
    stdout.channel.recv_exit_status()

    print("🔒 Setting NAS directory and data storage permissions...")
    stdin, stdout, stderr = ssh.exec_command(
        f"echo '{PASSWORD}' | sudo -S chmod -R 777 {TARGET_DIR} && "
        f"echo '{PASSWORD}' | sudo -S chmod -R 777 {TARGET_DIR}/data && "
        f"echo '{PASSWORD}' | sudo -S chown -R root:root {TARGET_DIR}/data"
    )
    stdout.channel.recv_exit_status()

    print(f"🐳 Building and launching container on NVMe Volume (Port {PORT})...")
    stdin, stdout, stderr = ssh.exec_command(
        f"cd {TARGET_DIR} && echo '{PASSWORD}' | sudo -S docker compose -f docker-compose.yaml up -d --build"
    )

    channel = stdout.channel
    def safe_print(text):
        try:
            sys.stdout.buffer.write(text.encode('utf-8', errors='replace'))
            sys.stdout.buffer.flush()
        except Exception:
            pass

    while not channel.exit_status_ready():
        if channel.recv_ready():
            safe_print(channel.recv(1024).decode('utf-8', errors='replace'))
        if channel.recv_stderr_ready():
            safe_print(channel.recv_stderr(1024).decode('utf-8', errors='replace'))
        time.sleep(0.1)

    while channel.recv_ready():
        safe_print(channel.recv(1024).decode('utf-8', errors='replace'))
    while channel.recv_stderr_ready():
        safe_print(channel.recv_stderr(1024).decode('utf-8', errors='replace'))

    exit_status = channel.recv_exit_status()
    print(f"\n✨ Docker compose execution completed with exit code {exit_status}")

    print("\n📋 Registering Project in UGOS Docker App Database...")
    db_register_script = f"""
import sqlite3
from datetime import datetime

db_path = '/volume1/@appstore/com.ugreen.docker/db/docker_info_log.db'
conn = sqlite3.connect(db_path)
cur = conn.cursor()
now = datetime.now().isoformat()

cur.execute('''
INSERT OR REPLACE INTO compose (name, state, path, content, app_id, container_num, created_at, updated_at)
VALUES (?, 1, ?, '', '', 1, ?, ?)
''', ('salarycalculator', '{TARGET_DIR}/docker-compose.yaml', now, now))

conn.commit()

cur.execute("SELECT name, state, path, container_num FROM compose WHERE name='salarycalculator'")
row = cur.fetchone()
print(f"Registered UGOS Project: {{row}}")
conn.close()
"""
    b64_db = base64.b64encode(db_register_script.encode('utf-8')).decode('ascii')
    stdin, stdout, stderr = ssh.exec_command(f"echo '{PASSWORD}' | sudo -S python3 -c 'import base64; exec(base64.b64decode(\"{b64_db}\").decode(\"utf-8\"))'")
    print(stdout.read().decode('utf-8'))

    # Verify docker ps
    print("\n🔍 Checking running docker containers in UGOS:")
    stdin, stdout, stderr = ssh.exec_command(f"echo '{PASSWORD}' | sudo -S docker ps --filter name=salarycalculator-web")
    output = stdout.read().decode('utf-8')
    print(output)

except Exception as e:
    print(f"❌ Deployment error: {e}")
    sys.exit(1)
finally:
    ssh.close()
    if temp_tar.exists():
        temp_tar.unlink()

print(f"\n🎉 Successfully deployed and registered Salary Calculator on UGOS NVMe Volume 2!")
print(f"🌐 Access URL: http://{HOST}:{PORT} or http://localhost:{PORT}")
