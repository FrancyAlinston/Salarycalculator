# Forgejo Actions Runner for Salary Calculator CI/CD

This directory provides a ready-to-run containerized runner (`act_runner`) to execute [`.forgejo/workflows/release.yaml`](file:///home/d3fault/Documents/Projects/Salarycalculator/.forgejo/workflows/release.yaml) on your own infrastructure.

---

## 🚀 Quick Setup (1 Minute)

### 1. Get Your Registration Token
1. Open Forgejo: `https://forgejo.449100.xyz`
2. Go to **Salarycalculator** repo $\rightarrow$ **Settings** $\rightarrow$ **Actions** $\rightarrow$ **Runners**
   *(Or Site Administration $\rightarrow$ Actions $\rightarrow$ Runners if setting up globally)*
3. Click **Create new runner** and copy the registration token string.

---

### 2. Start the Runner

Run on your server / local machine with Docker:

```bash
cd ci/forgejo-runner

# Export your registration token (or set it in a .env file)
export RUNNER_TOKEN="<PASTE_YOUR_FORGEJO_RUNNER_TOKEN_HERE>"

# Start the runner container
docker compose up -d
```

---

### 3. Verification

Check runner status in your terminal:
```bash
docker compose logs -f
```
In Forgejo (**Settings $\rightarrow$ Actions $\rightarrow$ Runners**), the runner will appear with a green **Idle / Ready** badge.

Whenever a commit is pushed to `main` or a new version tag `v*` is created, the runner will automatically:
1. Run `./gradlew test`
2. Build release and debug APKs (`./gradlew assembleDebug assembleRelease`)
3. Create a Forgejo Release with download attachments!
