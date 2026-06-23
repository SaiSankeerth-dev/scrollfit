# How to get app-debug.apk (free, via GitHub) — no PC build needed

ScrollFit cannot be compiled on the machine that wrote it (no Android SDK there).
GitHub builds it for you for free. ~5 minutes of setup, then the APK downloads to your phone or PC.

## Steps
1. Create a free GitHub account if you don't have one.
2. Create a new repository (e.g. `scrollfit`). Keep it Public (free Actions minutes).
3. Upload this whole `ScrollFit_l1` folder to the repo:
   - Easiest: on the repo page, "Add file" > "Upload files" > drag everything in this folder > Commit.
   - (Or use `git push` if you know git.)
4. Go to the repo's **Actions** tab. The "Build ScrollFit APK" workflow starts automatically.
   - If it doesn't, click it and press **Run workflow**.
5. Wait for it to finish (green check = success).
6. Open the finished run > scroll to **Artifacts** > download **ScrollFit-debug-apk**.
7. Unzip it, copy `app-debug.apk` to your phone, tap it, allow "install from unknown sources".

## If the build FAILS (red X) — this is expected the first time
This code has never been compiled. The first run will likely show real compiler errors.
Open the failed step's log, copy the FIRST error (and a few lines around it), and send it back.
Each error is fixable; we iterate until the run goes green and the APK appears.

## What this APK is
ScrollFit V1: tracking + dashboard + limits + streak + points + overlays + exercise logic,
all running locally and offline. Camera verification + friends/cloud are not in V1
(they need a device camera and a backend, respectively).
