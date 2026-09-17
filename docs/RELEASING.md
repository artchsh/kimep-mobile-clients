# Releasing

Releases are built, signed and published by GitHub Actions — nothing needs to be built
locally. The workflow lives at `.github/workflows/android.yml`.

## One-time setup

The repository must have these secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i kimep-release.jks` (single line) |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | e.g. `kimep` |
| `KEY_PASSWORD` | key password |

They can be set from the CLI:

```bash
gh secret set KEYSTORE_BASE64   --body "$(base64 -i kimep-android/keystore/kimep-release.jks | tr -d '\n')"
gh secret set KEYSTORE_PASSWORD --body "<password>"
gh secret set KEY_ALIAS         --body "kimep"
gh secret set KEY_PASSWORD      --body "<password>"
```

## Cutting a release

1. **Bump the version** in `kimep-android/app/build.gradle.kts`:

   ```kotlin
   versionCode = 2          // must increase for every release
   versionName = "0.2-beta"
   ```

   `versionCode` is what Android uses to decide whether an APK is an upgrade — if it does
   not increase, devices will refuse the update.

2. **Commit and push** to `main`:

   ```bash
   git add kimep-android/app/build.gradle.kts
   git commit -m "release: v0.2-beta"
   git push origin main
   ```

3. **Tag and push the tag**:

   ```bash
   git tag -a v0.2-beta -m "KIMEP Mobile v0.2-beta"
   git push origin v0.2-beta
   ```

4. The workflow builds, signs, verifies and publishes the release. Watch it with:

   ```bash
   gh run watch
   gh release view v0.2-beta
   ```

Tags containing `beta`, `alpha` or `rc` are published as pre-releases automatically.

## What the workflow does

- Installs JDK 21 and the Android SDK (`platforms;android-37.0`, `build-tools;36.0.0`).
- Writes `kimep-android/keystore.properties` from the secrets and decodes the `.jks`.
- Builds `:app:assembleDebug` and `:app:assembleRelease`.
- Runs `apksigner verify --print-certs` on the release APK and fails if signing is wrong.
- Uploads both APKs as build artifacts (`apks-<sha>`).
- On a `v*` tag, creates the GitHub release and attaches the signed release APK.
- Dispatches the Pages workflow so the download page picks up the new version.

> Note: GitHub does not let events created with the default `GITHUB_TOKEN` trigger other
> workflows, so `pages.yml`'s own `release: published` trigger will not fire by itself. The
> explicit `gh workflow run pages.yml` step in `android.yml` is what refreshes the site.

If the secrets are not present (e.g. a fork or a pull request from a fork), the keystore
step is skipped and the release build falls back to the debug signing config, so the
pipeline still passes.

## Release-check without a tag

Run the workflow manually from the Actions tab (`workflow_dispatch`) or push to `main` —
both build and verify the APKs and publish them as artifacts, but do not create a release.

## The signing key

`kimep-android/keystore/kimep-release.jks` and `kimep-android/keystore.properties` are
git-ignored and are the only way to update an installed release build (Android requires
the same signature). **Back them up somewhere safe.** If they are lost, the app must be
uninstalled before a newly signed build can be installed.

The certificate is:

```
CN=KIMEP Mobile, OU=Personal, O=Personal, L=Almaty, C=KZ
SHA-256 183b1c40ae125cd925eb328207d64b0ccf081611b7543a7dd374796e220dfb26
```

## Manual build (fallback)

```bash
cd kimep-android
./gradlew :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
gh release create v0.2-beta app/build/outputs/apk/release/app-release.apk \
  --title v0.2-beta --prerelease
```
