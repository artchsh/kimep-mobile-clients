# KIMEP Mobile Clients

Unofficial clients for **KIMEP University**'s student portal. The Android app is here
today; an updated iOS client is planned in the same repository.

The apps talk to the university's **old mobile user-facing APIs** — the same ones the
deprecated store app used — and reproduce the useful parts (schedule, grades, academic
calendar) with a native interface, offline caching and class reminders.

[![Release](https://img.shields.io/github/v/release/artchsh/kimep-mobile-clients?include_prereleases&label=release)](https://github.com/artchsh/kimep-mobile-clients/releases/latest)
[![Android CI](https://github.com/artchsh/kimep-mobile-clients/actions/workflows/android.yml/badge.svg)](https://github.com/artchsh/kimep-mobile-clients/actions/workflows/android.yml)

**➡️ [Download the latest APK](https://github.com/artchsh/kimep-mobile-clients/releases/latest)**

<p align="center">
  <img src="docs/screenshots/schedule.png" width="210" alt="Schedule" />
  &nbsp;
  <img src="docs/screenshots/grades.png" width="210" alt="Grades" />
  &nbsp;
  <img src="docs/screenshots/calendar.png" width="210" alt="Academic calendar" />
</p>
<p align="center">
  <img src="docs/screenshots/login.png" width="210" alt="Login" />
  &nbsp;
  <img src="docs/screenshots/settings.png" width="210" alt="Reminders" />
  &nbsp;
  <img src="docs/screenshots/finals.png" width="210" alt="Finals" />
</p>

## Features

- **Login** with Student ID and password; the session is stored on device and refreshed
  automatically.
- **Schedule** — weekly timetable grouped by weekday, with the **current day highlighted**,
  a **midterm‑week banner** derived from the academic calendar, and pull‑to‑refresh.
  Cached locally so it renders instantly on launch and updates in the background
  (stale‑while‑revalidate).
- **Finals** — a Classes/Finals switch for the exam timetable (populated once the
  university publishes it).
- **Grades** — cumulative GPA and credits, current‑term assessment scores, and the full
  transcript grouped by semester with colour‑coded grade badges.
- **Academic calendar** — the official PDF parsed into structured data: browse both
  academic years and all semesters, with the ongoing event marked **Now**, the next one
  **Next**, and past events dimmed. Opens scrolled to the current event.
- **Reminders** — notifications **1 hour** and **10 minutes** before each class, plus a
  final‑exam reminder. Each is individually togglable.
- **Optional, consent‑gated analytics** — anonymous usage events (screens, taps, retention)
  sent to a Umami instance. Off by default, no personal data, no real URLs. See
  **[docs/ANALYTICS.md](docs/ANALYTICS.md)**.
- **Material You** dynamic colour, light/dark theme, edge‑to‑edge.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Ktor (OkHttp engine) · kotlinx.serialization ·
DataStore · AlarmManager · Coil. AGP 9 with built‑in Kotlin, `minSdk 26`, `targetSdk 37`.

See **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** for how the app is put together.

## Repository layout

```
kimep-android/          Android app (Gradle project)
  app/src/main/java/kz/kimep/mobile/
    data/               models, API client, repositories, caches, notifications
    di/                 hand-rolled dependency container
    ui/                 Compose screens + theme
    vm/                 view models
  app/src/main/assets/calendar.json   parsed academic calendar
  keystore.properties   release signing (git-ignored)
docs/
  KIMEP_Mobile_API.md   reverse-engineered API reference
  CAPTURE_SETUP.md      how the API was captured from a stock iPhone
  ARCHITECTURE.md       Android app architecture
  RELEASING.md          how to cut a release (CI signs automatically)
  DEVICE_SUPPORT.md     supported OS/ABIs/form factors and known limitations
  ANALYTICS.md          opt-in anonymous analytics (Umami) and the consent flow
tools/
  parse_calendar.py     KIMEP calendar PDF -> calendar.json
capture.py              mitmproxy addon used during reverse engineering
dnsmasq.conf            DNS redirect used for the reverse-proxy phase
```
> An `ios/` client is planned; the repository name reflects that.

## Building (Android)

```bash
cd kimep-android
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # release APK
```

Requires JDK 17+ and an Android SDK with API 37 (`local.properties` → `sdk.dir`, or the
`ANDROID_SDK_ROOT` environment variable).

Release builds are signed from `kimep-android/keystore.properties` (git‑ignored):

```properties
storeFile=keystore/kimep-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

If that file is missing, the release build falls back to the debug signing config so a
fresh clone still compiles.

## Releases & CI/CD

`.github/workflows/android.yml` runs on pushes to `main`, pull requests,
`workflow_dispatch`, and `v*` tags:

1. Sets up JDK 21 and the Android SDK (`platforms;android-37.0`, `build-tools;36.0.0`).
2. Decodes the release keystore from repository secrets into `keystore.properties`.
3. Builds debug and release, **verifies the release signature**, and uploads both APKs as
   build artifacts.
4. On a `v*` tag, publishes a GitHub release with the signed APK. Tags containing
   `beta` / `alpha` / `rc` are marked as pre-releases.

Required repository secrets:

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | base64 of `kimep-release.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

If the secrets are absent (for example on a fork), signing is skipped and the release
build falls back to debug signing instead of failing.

To cut a release, bump `versionCode`/`versionName` in
`kimep-android/app/build.gradle.kts`, then push a tag. Full steps:
**[docs/RELEASING.md](docs/RELEASING.md)**.

## How the API was reverse engineered

The iOS app was captured on a stock (non‑jailbroken) iPhone: mitmproxy for the proxy
phase, then a **DNS redirect + reverse proxy** once the Flutter client turned out to
ignore the iOS system proxy. The full, reproducible procedure — including pitfalls and
teardown — is in **[docs/CAPTURE_SETUP.md](docs/CAPTURE_SETUP.md)**.

The resulting endpoint reference is in
**[docs/KIMEP_Mobile_API.md](docs/KIMEP_Mobile_API.md)**.

## Academic calendar pipeline

KIMEP publishes the calendar as PDFs (KAZ/RUS/ENG). `tools/parse_calendar.py` downloads the
English ones and reconstructs the four‑column table from word coordinates
(`pdftotext -bbox`), emitting `calendar.json` that ships as an app asset. Re‑run it each
academic year:

```bash
python3 tools/parse_calendar.py     # needs poppler (pdftotext)
```

## Privacy & analytics

Analytics is **opt-in and disabled unless configured**. On first launch the app shows a
plain‑language consent screen; nothing is sent before the user agrees, and it can be turned
off any time in **Settings → Privacy**.

It collects only anonymous usage events — which screens are opened, which controls are
tapped, and a random on‑device ID used to measure retention. It never collects names,
student IDs, passwords, sessions, grades, courses or any content you view, and it never
sends real URLs. Full details and the event list: **[docs/ANALYTICS.md](docs/ANALYTICS.md)**.

## Disclaimer

Unofficial and not affiliated with KIMEP University. Personal educational project that
uses the university's old mobile user-facing APIs. No credentials or captured personal
data are committed to this repository.

## Built with AI assistance

In the interest of transparency: the current MVP was created with the help of AI —
**DeepSeek V4.1 Flash**, running in the **OpenCode** harness under an **OpenCode GO**
subscription — alongside manual review and testing on a real device.
