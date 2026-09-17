# KIMEP Mobile — Reverse-Engineered API Reference

Documented from live capture of the iOS app **Kimep Mobile** (`kz.kimep.kimepmobile`, v3.0.8)
on 2026-09-17. Client is a Flutter app (`User-Agent: Dart/2.16 (dart:io)`); backend is
ASP.NET MVC 5.2 behind IIS 10 / ARR.

Scope of this doc: **authentication, schedule, and grades.** Other endpoints that were
captured but are out of scope: `contacts/contacts`, `news/_news`, `lectures/directory`,
`avatar/thumb`.

---

## 1. Overview

| | |
|---|---|
| Base URL | `https://www.kimep.kz/ext/mobile` |
| Transport | HTTPS/2, valid public certificate (UserTrust/Comodo). **No certificate pinning.** |
| Content type | `application/json; charset=UTF-8` |
| Auth | Session GUID returned by login, sent as `"id"` in every request body |
| Date format | ASP.NET JSON date: `"\/Date(1787598000000)\/"` (epoch milliseconds) |
| Time format | OLE/base-1900 time: negative epoch ms, see §5 |

### Required request headers

```
Content-Type: application/json; charset=UTF-8
Accept: application/json
User-Agent: Dart/2.16 (dart:io)      # any UA works in practice
```

There is **no** `Authorization` header, no API key, and no signature. The `id` GUID is the
entire credential for all calls after login.

---

## 2. Authentication

### `POST /ext/mobile/auth/login`

Request:

```json
{
  "StudentId": "12345678",
  "Password": "<plaintext password>",
  "Token": "null"
}
```

- `StudentId` — student number (string).
- `Password` — plaintext password (server requires TLS; no client-side hashing).
- `Token` — string `"null"` as sent by the iOS app. Likely intended for a device/push token
  and is not required to be a real value.

Response `200`:

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "ExpiredOn": "\/Date(1790754833223)\/"
}
```

- `id` — session GUID. **Use this as the `id` field in all other requests.**
- `ExpiredOn` — epoch ms when the session expires. Observed session lifetime was ~30 days;
  when expired, re-run login.

> There is no separate refresh token and no logout endpoint was observed. A new login
> simply returns a new GUID.

---

## 3. Profile (needed to show the student's name)

### `POST /ext/mobile/personal/info`

Request: `{"id":"<GUID>"}`

Response `200`:

```json
{
  "StudentID": 12345678,
  "FirstName": "Alex",
  "LastName": "Example",
  "ProgramID": "BSc",
  "XRay": "\/Date(1785783600000)\/"
}
```

| Field | Type | Notes |
|---|---|---|
| `StudentID` | int | |
| `FirstName` / `LastName` | string | |
| `ProgramID` | string | program code, e.g. `"BSc"` |
| `XRay` | date | a date the server reports (purpose unconfirmed) |

---

## 4. Schedule

### `POST /ext/mobile/schedule/personal`

Request: `{"id":"<GUID>"}`

Response `200` — array of scheduled class meetings:

```json
[
  {
    "ID": 117400,
    "WeekDay": "Monday",
    "Semester": "F2026",
    "Title": "Example Course",
    "CourseID": "EX101",
    "Hall": "#101/Example bld.",
    "Instructor": "Jane Doe, Ph.D",
    "LDrive": "\\\\l-drive\\lecture\\Jane Doe",
    "Section": "1",
    "Time_From": "\/Date(-2209136400000)\/",
    "Time_To": "\/Date(-2209131900000)\/",
    "Date_From": "\/Date(1787598000000)\/",
    "Date_To": "\/Date(1797274800000)\/"
  }
]
```

| Field | Type | Notes |
|---|---|---|
| `ID` | int | schedule row id |
| `WeekDay` | string | `Monday`…`Sunday` |
| `Semester` | string | e.g. `F2026`, `S2026`, `SU1/2026` |
| `Title` | string | course title |
| `CourseID` | string | internal course code (not the catalogue code) |
| `Hall` | string | room / building |
| `Instructor` | string | teacher name |
| `LDrive` | string \| null | path on the `\\l-drive\lecture` share; null if none |
| `Section` | string | section number |
| `Time_From` / `Time_To` | time | class start/end, see §5 |
| `Date_From` / `Date_To` | date | semester span, see §5 |

### `POST /ext/mobile/schedule/_finalexams`

Request: `{"id":"<GUID>"}` → array. Returned `[]` during capture (no upcoming finals),
so its item schema is **not yet confirmed**. Fields are expected to resemble a schedule row.

---

## 5. Dates and times

All date/time values are `/Date(<milliseconds>)/` strings.

### Absolute dates (`Date_From`, `Date_To`, `ExpiredOn`, `XRay`)

Milliseconds since Unix epoch, **UTC**. KIMEP local time is UTC+5. Example:
`1787598000000` → `2026-08-24 19:00 UTC` = `2026-08-25 00:00 Almaty` (i.e. local midnight).

### Time-of-day (`Time_From`, `Time_To`)

Encoded on the OLE Automation base date **1899-12-30 UTC**, which makes the raw value
negative. The backend serialises these using KIMEP's **legacy pre-2024 offset of UTC+6**
(Kazakhstan unified on UTC+5 in March 2024), so convert to a UTC clock time and then
**add 6 hours** for the correct local wall-clock time. Reading them as +5 makes every
lesson appear one hour early.

| Field | Raw | UTC | Local (+6) |
|---|---|---|---|
| `Time_From` | `-2209136400000` | 07:00 | **13:00** |
| `Time_To` | `-2209131900000` | 08:15 | **14:15** |
| `Time_From` | `-2209125600000` | 10:00 | **16:00** |
| `Time_To` | `-2209121100000` | 11:15 | **17:15** |

So the first class above runs **13:00–14:15 local**.

#### Conversion snippets

Dart:

```dart
DateTime parseAspNet(String v) {
  final ms = int.parse(RegExp(r'\d+|-\d+').firstMatch(v)!.group(0)!);
  return DateTime.fromMillisecondsSinceEpoch(ms, isUtc: true);
}

// absolute date in Almaty
final dateFrom = parseAspNet(json['Date_From']).add(const Duration(hours: 5));

// time-of-day (ignore the 1899-12-30 date part; legacy UTC+6)
final t = parseAspNet(json['Time_From']).add(const Duration(hours: 6));
final hhmm = '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';
```

Kotlin:

```kotlin
fun parseAspNet(v: String): Instant =
    Instant.ofEpochMilli(Regex("-?\\d+").find(v)!!.value.toLong())

// absolute date in Almaty (UTC+5)
val dateFrom = parseAspNet(json.getString("Date_From")).plus(5, ChronoUnit.HOURS)

// time-of-day (legacy UTC+6)
val t = parseAspNet(json.getString("Time_From")).plus(6, ChronoUnit.HOURS)
val hhmm = "%02d:%02d".format(
    t.atZone(ZoneOffset.UTC).hour, t.atZone(ZoneOffset.UTC).minute
)
```

---

## 6. Grades

### `POST /ext/mobile/schedule/GPACRS` — cumulative GPA

Request: `{"id":"<GUID>"}`

Response `200`:

```json
{ "GPA": 3.25, "CreditsEarned": 87, "CreditsTaken": 120 }
```

| Field | Type | Notes |
|---|---|---|
| `GPA` | double | cumulative GPA (4.33 scale) |
| `CreditsEarned` | int | |
| `CreditsTaken` | int | |

### `POST /ext/mobile/schedule/final_grades` — grade history

Request: `{"id":"<GUID>"}`

Response `200`:

```json
[
  { "ID": 12345678, "Semester": "SU1/2026", "Title": "Example Course A",
    "Grade": "B", "Point": 3.0 },
  { "ID": 12345678, "Semester": "S2026", "Title": "Example Course B",
    "Grade": "A-", "Point": 3.67 }
]
```

| Field | Type | Notes |
|---|---|---|
| `ID` | int | student id (same on every row) |
| `Semester` | string | e.g. `S2026`, `F2025`, `SU1/2025` |
| `Title` | string | course title |
| `Grade` | string | letter grade (`A+`, `B`, `C-`, `F`, `Pass`, …) |
| `Point` | double | grade points (4.33 scale; `Pass` = 0) |

### `POST /ext/mobile/schedule/AssessmentScores` — current-term scores

Request: `{"id":"<GUID>"}`

Response `200`:

```json
[
  {
    "Semester": "F2026",
    "Code": "EX101",
    "TitleCourses": "Example Course",
    "Registration": "x",
    "Score1": null,
    "Score2": null,
    "Score3": null,
    "FinalAssessment": "Allowed"
  }
]
```

| Field | Type | Notes |
|---|---|---|
| `Semester` | string | |
| `Code` | string | course catalogue code |
| `TitleCourses` | string | course title (note the pluralized name) |
| `Registration` | string | e.g. `"x"` |
| `Score1` / `Score2` / `Score3` | double \| null | assessment components (null until graded) |
| `FinalAssessment` | string | e.g. `"Allowed"` |

---

## 7. Minimal client flow

1. `POST /auth/login {StudentId, Password, Token:"null"}` → store `id` (+ `ExpiredOn`).
2. Persist the GUID securely; re-login after `ExpiredOn`.
3. `POST /schedule/personal {id}` → schedule grid.
4. `POST /schedule/GPACRS {id}` → GPA header.
5. `POST /schedule/final_grades {id}` → transcript.
6. `POST /schedule/AssessmentScores {id}` → current-term scores.
7. Optional: `POST /personal/info {id}` → name for the profile header.

## 8. cURL example

```bash
BASE=https://www.kimep.kz/ext/mobile

# login
curl -s "$BASE/auth/login" \
  -H 'Content-Type: application/json; charset=UTF-8' \
  -H 'Accept: application/json' \
  -d '{"StudentId":"12345678","Password":"<pw>","Token":"null"}'

ID="<id from login response>"

curl -s "$BASE/schedule/personal" -H 'Content-Type: application/json; charset=UTF-8' \
  -d "{\"id\":\"$ID\"}"
curl -s "$BASE/schedule/GPACRS"     -H 'Content-Type: application/json; charset=UTF-8' \
  -d "{\"id\":\"$ID\"}"
curl -s "$BASE/schedule/final_grades" -H 'Content-Type: application/json; charset=UTF-8' \
  -d "{\"id\":\"$ID\"}"
```

---

## 9. Notes, caveats, and open questions

- **Security:** the session GUID is a bearer token with no rotation/refresh and is the only
  thing protecting the account. Treat it like a password. Passwords are sent in plaintext
  inside the TLS tunnel.
- **Scope of the GUID:** unconfirmed whether it is bound to a device, IP, or is freely
  reusable across clients. It was accepted from a replayed client during capture.
- **`_finalexams` schema** is unconfirmed (returned an empty array).
- **Not documented / not needed:** `lectures/directory` (file share browser),
  `news/_news`, `contacts/contacts`, `avatar/thumb/{id}`.
- **Endpoints never observed** that likely exist: logout, news mark-as-read
  (the model has a `ReadOn` field), push-token registration (the login `Token` field).
- **Availability:** the iOS app was removed from the App Store but the backend remains live.
  No version handshake or app-attestation was observed, so a third-party client is accepted.
