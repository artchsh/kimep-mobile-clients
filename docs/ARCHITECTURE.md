# Android architecture

Single-module Kotlin app (`kimep-android/app`), Jetpack Compose + Material 3, no DI
framework (a small hand-rolled container) and no navigation library (state-driven
screens). Package root: `kz.kimep.mobile`.

## Package layout

```
kz.kimep.mobile
├── KimepApp           Application; creates notification channels, owns AppContainer
├── MainActivity       edge-to-edge host, requests POST_NOTIFICATIONS
├── di/AppContainer    wires api + stores + repositories (singletons)
├── data/
│   ├── KimepApi               Ktor HTTP client, one method per endpoint
│   ├── KimepRepository        remote calls -> Result<T>, writes session
│   ├── CalendarRepository     loads assets/calendar.json (cached in memory)
│   ├── ApiDate                ASP.NET date/time decoding
│   ├── Errors                 Throwable -> user-facing message
│   ├── SessionStore           DataStore: session GUID + profile
│   ├── SettingsStore          DataStore: reminder toggles + scheduled ids
│   ├── ScheduleCache          DataStore: last good timetable (JSON)
│   ├── AcademicCalendar       maps semester codes -> midterm weeks
│   ├── model/                 @Serializable DTOs
│   └── notify/                reminder scheduling + notifications
├── ui/                Compose screens (see below)
└── vm/                ViewModels, one per screen
```

## Data flow

```
Compose screen ──collects──> ViewModel.uiState (Compose state)
        │                         │
        │ user actions            │ viewModelScope
        v                         v
   ViewModel ──────────────> KimepRepository ──> KimepApi ──> https://www.kimep.kz/ext/mobile
                                   │
                                   ├──> ScheduleCache  (stale-while-revalidate)
                                   ├──> SessionStore   (session GUID)
                                   └──> ReminderManager (AlarmManager)
```

Screens are dumb: they render `uiState` and forward intents. ViewModels own coroutines and
expose an immutable `data class` state via `mutableStateOf`. Repositories return
`Result<T>` and never touch Compose.

## API layer

`KimepApi` is a thin Ktor client (`OkHttp` engine, kotlinx.serialization). Every call is a
JSON `POST` whose body carries the session GUID as `"id"`; there is no `Authorization`
header. See `docs/KIMEP_Mobile_API.md`.

```kotlin
suspend fun schedule(id: String): List<ClassMeeting> = post("schedule/personal", IdRequest(id))
```

`post` is an `inline reified` helper that throws `ApiException(status, …)` on non-2xx.

## Storage

| Store | DataStore file | Contents |
|---|---|---|
| `SessionStore` | `kimep_session` | session GUID, student id, name, program, expiry |
| `SettingsStore` | `kimep_settings` | reminder toggles, ids of scheduled alarms |
| `ScheduleCache` | `kimep_cache` | last good timetable + finals (JSON), updated-at |

`SessionStore.state` emits a `Loading` placeholder first (`onStart`). Consumers that need
the real value must use `first { it !is SessionState.Loading }` — taking `.first()` returns
the placeholder.

### Stale-while-revalidate

`ScheduleViewModel.init` reads `ScheduleCache`, renders it immediately, then calls the
network. The fresh response overwrites the cache and the UI. A failed refresh keeps the
cached data on screen (only a cold, empty load shows an error).

## Reminders

| Class | Role |
|---|---|
| `ReminderManager` | single entry point; reads settings + session, fetches the schedule, (re)schedules everything |
| `ReminderScheduler` | computes occurrences and sets `AlarmManager` exact alarms |
| `ReminderReceiver` | `BroadcastReceiver` that posts the notification |
| `BootReceiver` | rebuilds alarms after reboot / app update |
| `Notifications` | channel definitions |

Request codes are deterministic — `meetingId * 1000 + weekIndex * 2 + leadKind`
(`leadKind` 0 = 1 hour, 1 = 10 minutes) — so previously scheduled alarms can be cancelled
without persisting each one. Only the meeting *ids* are stored (in `SettingsStore`).
Finals use a separate code base. Alarms are exact when
`AlarmManager.canScheduleExactAlarms()`; otherwise they fall back to inexact.

`ReminderManager.refresh()` is called on app start (via the schedule load), after a
schedule refresh, after a settings change, and after reboot.

## Date & time

All API dates are ASP.NET JSON dates: `"\/Date(<epoch millis>)\/"`. Two encodings exist
(`data/ApiDate.kt`):

- **Absolute dates** (`Date_From`, `Date_To`, `ExpiredOn`): epoch ms, interpreted at
  **UTC+5**.
- **Time-of-day** (`Time_From`, `Time_To`): encoded on the OLE base date 1899-12-30 with the
  university's **legacy UTC+6** offset (Kazakhstan moved to UTC+5 in 2024). Reading these
  as +5 shows every lesson an hour early, so times use a fixed +6.

A fixed offset is used deliberately instead of `ZoneId` so 1899 dates aren't affected by
timezone-database history. Alarm times are then built from the decoded local wall-clock
time in the device's default zone.

## UI

State-driven, no Navigation library. `KimepRoot` observes `SessionStore.state` and shows:

```
Loading -> LoadingState
LoggedOut -> LoginScreen
LoggedIn  -> MainScreen (bottom bar: Schedule · Grades · Calendar · Profile)
```

- `MainScreen` owns the top bar, bottom bar and an in-place **Settings** screen (gear
  action), so there's no back-stack to manage.
- `ScheduleScreen` has a Classes/Finals filter chip row, highlights today, and shows a
  midterm banner when `ScheduleUiState.midterm` is set.
- `CalendarScreen` computes `Past/Current/Future` per event, marks **Now**/**Next**, and
  scrolls the list to the first current-or-upcoming item on load.
- Theme: Material 3 with **dynamic colour** on Android 12+, a hand-tuned fallback palette,
  light/dark, edge-to-edge.

## Dependency wiring

`AppContainer` (created lazily by `KimepApp`) holds the singletons. Screens obtain the
container from the Application and pass what they need to `viewModel(factory = …)`
factories. There is no reflection-based DI.

## Adding an endpoint

1. Add a `@Serializable` DTO in `data/model/Models.kt` (match the JSON keys exactly, mark
   unknown fields optional).
2. Add a method to `KimepApi` using the `post` helper.
3. Wrap it in `KimepRepository` as `Result<T>`.
4. Surface it through a ViewModel state and a screen.

The academic calendar is offline data: add fields to `data/model/CalendarModels.kt` and
update `tools/parse_calendar.py` that generates `assets/calendar.json`.

## Known limitations

- `_finalexams` schema is unconfirmed (it returned an empty array during capture); the
  `FinalExam` DTO is intentionally permissive. Align it once the endpoint returns data.
- Release builds keep `isMinifyEnabled = false` (R8 rules for kotlinx.serialization/Ktor are
  not tuned yet).
- The academic calendar asset is regenerated manually each academic year.
