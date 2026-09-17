# Analytics (Umami)

The Android app can send **anonymous, privacy-conscious usage events** to a self-hosted
or cloud **Umami** instance. It is **opt-in**, **off by default**, and does nothing at all
until both a host is configured *and* the user accepts the consent screen.

This is a proof of concept. The data is used purely for analytics — what is used, what is
not, and whether people come back.

## Privacy stance

- **No personal data, ever.** No student ID, name, password, session GUID, grades, course
  titles or any content the user views.
- **No real URLs.** Events are posted with a synthetic URL (`/app/<event>`), so the session
  GUID embedded in avatar URLs can never leak.
- **No OS identifiers.** No advertising ID, no location, no contacts. The only identifier
  is a random UUID generated on the device.
- **Consent-gated.** `Analytics.track()` checks consent on every call; before consent (or
  after opting out) the event is dropped before any network call is made.
- **Opt-out is immediate** and reachable any time in Settings → Privacy. Turning it off
  also forgets the anonymous identifier.
- **Best-effort.** Sends are fire-and-forget on a background scope; failures are ignored
  and never affect the UI.

## What is collected

| Event | When | Data |
|---|---|---|
| `app_open` | process launch, once consent is granted | — |
| `screen_view` | each tab / the Settings screen | `screen` = schedule, grades, calendar, profile, settings |
| `login` | login attempt completes | `result` = success \| failure |
| `logout` | sign-out tapped | — |
| `schedule_tab` | Classes/Finals switch | `tab` |
| `schedule_refresh` | pull-to-refresh | — |
| `grades_tab` | Current/Transcript switch | `tab` |
| `calendar_filter` | academic year or semester changed | `year`, `semester` |
| `reminder_toggle` | a reminder switch changed | `kind`, `enabled` |
| `consent_decision` | consent granted/denied in Settings | `decision` |

Every event also carries: `anon` (random device UUID), `installAgeDays` (for retention),
`appVersion`, plus the platform's `language` and `screen` size.

## Retention

Umami counts "visitors" server-side from hashed request attributes. Since all requests come
from the same app on the same device, returning users are grouped consistently in practice,
and `installAgeDays` gives an explicit dimension to segment by install age.

## Configuration

Analytics stays **disabled when no host is configured** — the app then ships with a no-op
implementation and the consent screen is skipped entirely.

Provide the host + website id in any of these ways (first match wins):

```properties
# kimep-android/local.properties  (git-ignored)
umami.host=https://umami.example.com
umami.websiteId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

- Gradle properties: `-Pumami.host=… -Pumami.websiteId=…`
- Environment variables: `UMAMI_HOST`, `UMAMI_WEBSITE_ID` (used by CI)

They are compiled into `BuildConfig.UMAMI_HOST` / `BuildConfig.UMAMI_WEBSITE_ID`. After
changing them, run `./gradlew clean :app:assembleDebug` to be safe.

In CI, set the repository secrets `UMAMI_HOST` and `UMAMI_WEBSITE_ID`; the workflow passes
them to Gradle as environment variables. When the secrets are absent, release builds simply
have analytics disabled.

## Umami API

Targets **Umami v2**: `POST <host>/api/send` with

```json
{
  "type": "event",
  "payload": {
    "website": "<website id>",
    "hostname": "kimep-mobile-app",
    "url": "/app/schedule_tab",
    "title": "schedule_tab",
    "screen": "1224x2720",
    "language": "en-US",
    "name": "schedule_tab",
    "data": { "anon": "…", "installAgeDays": 3, "appVersion": "0.2-beta", "tab": "finals" }
  }
}
```

If you run an older Umami (v1 `POST /api/collect`), adjust the request body in
`data/analytics/Analytics.kt` — it is built in one place.

## Consent flow

1. On first launch, before anything else, `PrivacyConsentScreen` is shown with the plain
   language notice (collected / never collected / why / who sees it / your control).
2. **Agree and continue** → consent is stored, tracking begins, `consent_decision` is sent.
3. **Continue without sharing** → consent stored as denied; nothing is ever sent.
4. The decision can be changed any time in **Settings → Privacy**, which also lets the user
   re-read the notice.

Consent, the anonymous id and the first-seen timestamp live in a separate DataStore file
(`kimep_analytics`).

## Where it lives

| File | Role |
|---|---|
| `data/analytics/AnalyticsStore.kt` | consent, anonymous id, install age |
| `data/analytics/Analytics.kt` | `Analytics` interface, `NoOpAnalytics`, `UmamiAnalytics`, event names |
| `ui/PrivacyConsent.kt` | consent screen + notice body/dialog |
| `di/AppContainer.kt` | selects `NoOpAnalytics` vs `UmamiAnalytics` from `BuildConfig` |
| `ui/KimepRoot.kt` | gates on consent, records `app_open` |
