# Device support

Reference notes on what the Android app runs on and where it is rough. Verified against
the built release APK, not assumed.

## Range

| | |
|---|---|
| Minimum | **Android 8.0 Oreo (API 26)** |
| Target | **Android 17 (API 37)** |
| ABIs | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` (universal APK) |
| Screens | `small`, `normal`, `large`, `xlarge` |
| Hardware | none required beyond a touchscreen + network |

Covers phones, tablets, foldables, Chromebooks (ARC), Samsung DeX, split‑screen and
freeform multi‑window, and both 64‑bit and 32‑bit ARM devices.

## Why it is broadly compatible (verified)

- **`resizeableActivity` is not declared** → resizable by default, so DeX, multi‑window and
  foldable continuity work.
- **No `configChanges` / `screenOrientation`** → the Activity *is* recreated on
  fold/unfold/rotate, but state survives: data lives in ViewModels, and the bottom‑bar tab
  index, calendar scroll position and login fields use `rememberSaveable`.
- **No Google Play Services / Firebase dependency** (deps are pure AndroidX + Ktor/OkHttp +
  Coil) → runs on devices without GMS.
- **Adaptive icon** (`mipmap-anydpi-v26`) → themed/monochrome icons on Android 13+.
- Main screens use a `Scaffold`, so system‑bar insets are applied; edge‑to‑edge is on.

Inspect the built APK:

```bash
aapt2 dump badging app-release.apk | grep -E "sdkVersion|native-code|supports-screens"
```

## Known limitations

1. **Large screens are not optimized.** There is no `WindowSizeClass` handling — the layout
   is a single column stretched to full width, so on a Fold inner display or tablet the
   cards are very wide with long lines. Functional, not tuned.
2. **OEM battery managers.** Samsung, Xiaomi, Huawei, Oppo, Vivo and others aggressively
   restrict background work and can delay or drop exact alarms → late/missing class
   reminders. The app already uses `setExactAndAllowWhileIdle` and reschedules on boot, but
   it does **not** yet offer a battery‑optimization exemption shortcut.
3. **Login screen insets.** It is vertically centred and does not apply `systemBars`
   padding explicitly, so in landscape on a very short screen the content could sit under
   the status/navigation bars.
4. **Foldable half‑open / tabletop posture** is not specially handled.

## Possible future work (not implemented)

- Cap content to ~600 dp max width and centre it on large screens.
- Add a battery‑optimization exemption shortcut in Settings (needs
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).
- Test on foldable/tablet emulator images (e.g. Pixel Fold, Pixel Tablet) plus a 32‑bit ARM
  device.
- Apply explicit system‑bar insets on the login screen.
- Supporting Android 5–7 would require lowering `minSdk` and adding `java.time`
  desugaring, notification‑channel guards and `AlarmManager` compatibility. **Not planned.**

## Suggested test matrix

| Target | Why |
|---|---|
| Pixel‑class phone, Android 16 | baseline (already tested on a Nothing Phone) |
| Foldable image (inner + cover, fold/unfold) | layout + activity recreation |
| Tablet / large screen image | stretched layout, insets |
| 32‑bit ARM or API 26 device | lowest supported API |
| De‑Googled device | no GMS dependency |
