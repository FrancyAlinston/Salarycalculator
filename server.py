#!/usr/bin/env python3
"""
Salary Calculator Web Server & SQLite REST API for Docker & UGOS NAS Deployment
Serves the web client with SPA routing, CORS headers, healthcheck endpoints,
and SQLite persistence API for multi-device sync on NAS.
"""
import http.server
import json
import mimetypes
import os
import socketserver
import sqlite3
import sys
import time

# Ensure proper MIME types are registered
mimetypes.init()
mimetypes.add_type("application/javascript", ".js")
mimetypes.add_type("application/json", ".json")
mimetypes.add_type("application/manifest+json", ".webmanifest")
mimetypes.add_type("image/svg+xml", ".svg")
mimetypes.add_type("text/css", ".css")

PORT = int(os.environ.get("PORT", sys.argv[1] if len(sys.argv) > 1 else 8080))
HOST = os.environ.get("HOST", "0.0.0.0")
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
WEB_DIR = os.path.join(BASE_DIR, "web")
DATA_DIR = os.path.join(BASE_DIR, "data")

os.makedirs(DATA_DIR, exist_ok=True)
DB_PATH = os.path.join(DATA_DIR, "salarycalculator.db")

def init_db():
    """Initialize SQLite tables for NAS persistence."""
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at REAL NOT NULL
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS shifts (
                date TEXT PRIMARY KEY,
                shift_type TEXT NOT NULL,
                hours REAL DEFAULT 0,
                ot_hours REAL DEFAULT 0,
                notes TEXT DEFAULT '',
                updated_at REAL NOT NULL
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS backups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT DEFAULT 'default',
                bundle_json TEXT NOT NULL,
                created_at REAL NOT NULL
            )
        """)
        conn.commit()
        conn.close()
        print(f"✅ SQLite Database initialized at {DB_PATH}")
    except Exception as e:
        print(f"⚠️ Warning: Could not initialize SQLite DB: {e}")

init_db()

class SalaryCalculatorHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

    def _send_json(self, data, status_code=200):
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self.end_headers()
        self.wfile.write(json.dumps(data).encode("utf-8"))

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.end_headers()

    def do_GET(self):
        # Healthcheck endpoint for Docker & UGOS monitoring
        if self.path in ("/api/status", "/api/health", "/health", "/status"):
            return self._send_json({
                "status": "healthy",
                "service": "salary-calculator-web",
                "version": "26.5",
                "database": "sqlite3",
                "environment": os.environ.get("DOCKER_ENV", "false")
            })

        # API: Get backup bundle
        if self.path.startswith("/api/backup"):
            try:
                conn = sqlite3.connect(DB_PATH)
                cursor = conn.cursor()
                cursor.execute("SELECT bundle_json, created_at FROM backups ORDER BY id DESC LIMIT 1")
                row = cursor.fetchone()
                conn.close()
                if row:
                    return self._send_json({
                        "success": True,
                        "backup": json.loads(row[0]),
                        "created_at": row[1]
                    })
                return self._send_json({"success": False, "message": "No backups found on NAS"}, 404)
            except Exception as e:
                return self._send_json({"success": False, "error": str(e)}, 500)

        # API: Get shifts
        if self.path.startswith("/api/shifts"):
            try:
                conn = sqlite3.connect(DB_PATH)
                cursor = conn.cursor()
                cursor.execute("SELECT date, shift_type, hours, ot_hours, notes FROM shifts ORDER BY date ASC")
                rows = cursor.fetchall()
                conn.close()
                shifts = {
                    row[0]: {
                        "type": row[1],
                        "hours": row[2],
                        "ot_hours": row[3],
                        "notes": row[4]
                    } for row in rows
                }
                return self._send_json({"success": True, "shifts": shifts})
            except Exception as e:
                return self._send_json({"success": False, "error": str(e)}, 500)

        # API: Get settings
        if self.path.startswith("/api/settings"):
            try:
                conn = sqlite3.connect(DB_PATH)
                cursor = conn.cursor()
                cursor.execute("SELECT key, value FROM settings")
                rows = cursor.fetchall()
                conn.close()
                settings = {row[0]: json.loads(row[1]) for row in rows}
                return self._send_json({"success": True, "settings": settings})
            except Exception as e:
                return self._send_json({"success": False, "error": str(e)}, 500)

        # Check if requesting a direct file in web/
        path_without_query = self.path.split("?")[0].lstrip("/")
        file_path = os.path.join(WEB_DIR, path_without_query)

        # SPA fallback: If not a file and not an asset with extension, serve index.html
        if path_without_query and not os.path.exists(file_path) and "." not in os.path.basename(path_without_query):
            self.path = "/index.html"

        return super().do_GET()

    def do_POST(self):
        # API: Save/Sync backup bundle
        if self.path.startswith("/api/backup"):
            try:
                content_length = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(content_length)
                data = json.loads(body.decode("utf-8"))

                email = data.get("email", "default")
                bundle = data.get("bundle", data)

                conn = sqlite3.connect(DB_PATH)
                cursor = conn.cursor()
                cursor.execute(
                    "INSERT INTO backups (email, bundle_json, created_at) VALUES (?, ?, ?)",
                    (email, json.dumps(bundle), time.time())
                )
                conn.commit()
                conn.close()
                return self._send_json({"success": True, "message": "Backup saved to NAS SQLite DB"})
            except Exception as e:
                return self._send_json({"success": False, "error": str(e)}, 500)

        # API: Upsert shifts
        if self.path.startswith("/api/shifts"):
            try:
                content_length = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(content_length)
                data = json.loads(body.decode("utf-8"))

                shifts = data.get("shifts", data)
                conn = sqlite3.connect(DB_PATH)
                cursor = conn.cursor()
                now = time.time()
                for date_str, shift_data in shifts.items():
                    shift_type = shift_data if isinstance(shift_data, str) else shift_data.get("type", "DUTY")
                    hours = shift_data.get("hours", 0) if isinstance(shift_data, dict) else 0
                    ot_hours = shift_data.get("ot_hours", 0) if isinstance(shift_data, dict) else 0
                    notes = shift_data.get("notes", "") if isinstance(shift_data, dict) else ""
                    cursor.execute("""
                        INSERT INTO shifts (date, shift_type, hours, ot_hours, notes, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT(date) DO UPDATE SET
                            shift_type = excluded.shift_type,
                            hours = excluded.hours,
                            ot_hours = excluded.ot_hours,
                            notes = excluded.notes,
                            updated_at = excluded.updated_at
                    """, (date_str, shift_type, hours, ot_hours, notes, now))
                conn.commit()
                conn.close()
                return self._send_json({"success": True, "count": len(shifts)})
            except Exception as e:
                return self._send_json({"success": False, "error": str(e)}, 500)

        # API: Save settings
        if self.path.startswith("/api/settings"):
            try:
                content_length = int(self.headers.get("Content-Length", 0))
                body = self.rfile.read(content_length)
                settings = json.loads(body.decode("utf-8"))

                conn = sqlite3.connect(DB_PATH)
                cursor = conn.cursor()
                now = time.time()
                for key, val in settings.items():
                    cursor.execute("""
                        INSERT INTO settings (key, value, updated_at)
                        VALUES (?, ?, ?)
                        ON CONFLICT(key) DO UPDATE SET
                            value = excluded.value,
                            updated_at = excluded.updated_at
                    """, (key, json.dumps(val), now))
                conn.commit()
                conn.close()
                return self._send_json({"success": True, "saved": len(settings)})
            except Exception as e:
                return self._send_json({"success": False, "error": str(e)}, 500)

        return self._send_json({"error": "Endpoint not found"}, 404)

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "SAMEORIGIN")
        
        # Cache control for static assets vs HTML
        if self.path.endswith(".html") or self.path in ("/", "") or self.path.startswith("/api"):
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        else:
            self.send_header("Cache-Control", "public, max-age=86400")
            
        super().end_headers()

    def log_message(self, format, *args):
        # Clean logging format
        sys.stdout.write(f"[{self.log_date_time_string()}] {self.address_string()} - {format % args}\n")
        sys.stdout.flush()

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    print(f"🚀 Salary Calculator Web Server & SQLite API starting on http://{HOST}:{PORT}")
    print(f"📁 Web Root: {WEB_DIR}")
    print(f"📁 Data Directory: {DATA_DIR}")
    print(f"💾 SQLite Database: {DB_PATH}")
    
    with socketserver.TCPServer((HOST, PORT), SalaryCalculatorHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server.")
