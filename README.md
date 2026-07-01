<div align="center">
<img src="ScrollFit_logo_preview.png" width="120" alt="ScrollFit logo" />

# ScrollFit

**Reduce doom scrolling on Instagram Reels and YouTube Shorts by pairing visible awareness with self-chosen friction.**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android) ![Kotlin](https://img.shields.io/badge/kotlin-Jetpack%20Compose-7F52FF?logo=kotlin) ![Gemini](https://img.shields.io/badge/AI-Gemini%20API-4285F4)

</div>

## What it does

ScrollFit tracks how much you actually scroll, then makes continuing to scroll cost something: real exercise reps, an adaptive daily limit, and a focus score that reacts to your behavior over time. Instead of a hard blocker that gets disabled after a day, it builds a habit loop — scroll debt, streaks, and points — around content you already use.

- **Screen-time tracking** for short-form apps (Reels, Shorts)
- **Exercise-based unlocks** — pushups/squats via on-device pose tracking (CameraX + MediaPipe) or manual rep counting
- **Adaptive limits** that tighten or loosen based on recent behavior
- **Focus scoring** and streaks to reinforce the habit, not just block usage
- Runs **fully offline and on-device** — no account, no cloud dependency for the core loop

## Tech stack

Kotlin, Jetpack Compose, CameraX, MediaPipe Pose Landmarker (on-device), Gradle. Optional Gemini API integration for AI-assisted features (see `.env.example`).

## Getting the app

**Build locally:**
1. Open in [Android Studio](https://developer.android.com/studio).
2. Copy `.env.example` to `.env` and set `GEMINI_API_KEY` if using the AI-assisted features.
3. Run on an emulator or physical device.

**No local Android setup?** See [HOW_TO_GET_THE_APK.md](HOW_TO_GET_THE_APK.md) — GitHub Actions builds a debug APK for you for free.

## Project structure

See [EXERCISE_TRACKING_SUMMARY.md](EXERCISE_TRACKING_SUMMARY.md) for the pose-tracking pipeline (camera → MediaPipe → rep verification → UI) and how it plugs into the dashboard.

## License

MIT
