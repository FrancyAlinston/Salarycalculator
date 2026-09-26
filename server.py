#!/usr/bin/env python3
"""
Salary Calculator Web Server for Docker & UGOS NAS Deployment
Serves the web client with SPA routing, CORS headers, and healthcheck endpoints.
"""
import http.server
import json
import mimetypes
import os
import socketserver
import sys

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

class SalaryCalculatorHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

    def do_GET(self):
        # Healthcheck endpoint for Docker & UGOS monitoring
        if self.path in ("/api/status", "/api/health", "/health", "/status"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.end_headers()
            response = {
                "status": "healthy",
                "service": "salary-calculator-web",
                "version": "26.5",
                "environment": os.environ.get("DOCKER_ENV", "false")
            }
            self.wfile.write(json.dumps(response).encode("utf-8"))
            return

        # Check if requesting a direct file in web/
        path_without_query = self.path.split("?")[0].lstrip("/")
        file_path = os.path.join(WEB_DIR, path_without_query)

        # SPA fallback: If not a file and not an asset with extension, serve index.html
        if path_without_query and not os.path.exists(file_path) and "." not in os.path.basename(path_without_query):
            self.path = "/index.html"

        return super().do_GET()

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "SAMEORIGIN")
        
        # Cache control for static assets vs HTML
        if self.path.endswith(".html") or self.path in ("/", ""):
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
    print(f"🚀 Salary Calculator Web Server starting on http://{HOST}:{PORT}")
    print(f"📁 Web Root: {WEB_DIR}")
    print(f"📁 Data Directory: {DATA_DIR}")
    
    with socketserver.TCPServer((HOST, PORT), SalaryCalculatorHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server.")
