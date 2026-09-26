#!/usr/bin/env bash
# Script to deploy Salary Calculator Web App directly to Cloudflare Pages
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
WEB_DIR="$ROOT_DIR/web"

echo "========================================================="
echo " ☁️  Deploying Salary Calculator to Cloudflare Pages"
echo "========================================================="
echo "Directory: $WEB_DIR"

if command -v wrangler &> /dev/null; then
    echo "Running: wrangler pages deploy web --project-name salarycalculator"
    wrangler pages deploy "$WEB_DIR" --project-name salarycalculator
elif command -v npx &> /dev/null; then
    echo "Running: npx wrangler pages deploy web --project-name salarycalculator"
    npx wrangler pages deploy "$WEB_DIR" --project-name salarycalculator
else
    echo "ℹ️ Wrangler CLI is not installed locally. You can deploy this repository to Cloudflare Pages by:"
    echo "1. Connecting your Git repository (GitHub/Forgejo) to Cloudflare Pages dashboard."
    echo "2. Set 'Build output directory' to: web"
    echo "3. No build command required (static HTML5/ES6/PWA)."
fi

echo "✓ Cloudflare Pages deployment bundle ready in $WEB_DIR"
