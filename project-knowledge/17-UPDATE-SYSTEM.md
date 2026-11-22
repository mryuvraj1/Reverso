# Android release update system

## Scope and status

The Android client checks the official public GitHub Releases API for `mryuvraj1/Reverso`. It reads release tags, names, notes, publication times and uploaded assets. Stable releases are the default channel; prereleases are excluded until the user enables beta updates. Drafts and tags that do not parse as semantic versions are ignored. Checks are unauthenticated and contain no GitHub credential.

The implemented path includes launch and manual checks, a periodic WorkManager request, an in-app release dialog, optional Android notifications, cached release history, APK asset selection, bounded APK download, SHA-256 verification when GitHub provides a digest, and an explicit Android package-installer handoff. Android still requires the user to permit installs from Reverso where applicable and confirm the package installation. This is not a silent updater. No compatible APK results in a source-only message. If no digest exists, the app asks for explicit acknowledgement before opening Android's installer.

The repository has no published Android release in this checkout, so the real network discovery, download, signature continuity and install/update sequence have not been exercised on a device. CI unit tests cover deterministic selection and helper behavior; they are not an end-to-end APK installation test.

## Components

### `app/src/main/java/dev/reverso/ReleaseChecker.kt`

Owns release metadata and preferences. `ReleaseChecker.check(context, includeBeta, force)` requests `GET https://api.github.com/repos/mryuvraj1/Reverso/releases?per_page=100` on the caller's background dispatcher. It sets GitHub's media type, API version and a descriptive User-Agent, sends no Authorization header, caps the body at 1.5 MB, applies connection/read timeouts, and allows only HTTPS release links and APK links whose path belongs to the configured repository. It filters drafts, rejects invalid semantic-version tags, and chooses a universal APK first, then an ABI-named asset, then a sole APK only when its name does not identify an incompatible ABI.

Release JSON and the last successful check time are cached in private SharedPreferences. A successful response is cached for six hours; all calls are subject to a one-minute minimum network interval, including forced/manual checks. Failed checks preserve cached release history and return the failure message. HTTP 403 with a rate-limit reset header includes the retry time. `history`, `lastChecked`, `latestPublished`, and `latestNewer` expose the cached/check results to Compose. A latest release is not necessarily an update: `latestNewer` compares it with the installed `versionName` via `Versioning.kt`.

`UpdatePreferences` stores automatic-check, beta-channel, notification, last-notified and remind-later values. These are user preferences and release metadata, not secrets. `selectNewerRelease`, `releaseNeedsNotification`, `sha256DigestMatches` and `releaseCheckFailure` are pure helpers covered by `ReleaseUpdateTest`.

### `app/src/main/java/dev/reverso/UpdateScheduler.kt`

`UpdateScheduler.configure` enqueues one unique, network-constrained periodic worker at a 24-hour interval, or cancels it when automatic checks are disabled. Android/WorkManager may defer the work based on system scheduling and battery conditions; the interval is not an exact alarm. `ReleaseCheckWorker` invokes the same checker and retries when no cached result is available after an error.

`UpdateNotification` creates the Android channel named **Reverso Updates**, checks the user preference, system notification state and Android 13 runtime permission, then posts one notification for a release tag. It records the tag only after the notify call succeeds, preventing repeated notifications for the same release while allowing a later retry when permission was unavailable. Notifications open Reverso; they do not install anything.

### `app/src/main/java/dev/reverso/UpdateInstaller.kt`

`UpdateInstaller.download` accepts only a selected official APK asset. It rejects invalid/oversized metadata, redirects only to GitHub's release asset host, caps the transfer at 200 MiB, verifies downloaded length and checks GitHub's `sha256:` asset digest when present. Failed downloads are deleted. An absent digest is surfaced to the UI; the caller requires user acknowledgement before continuing. APKs are stored under app cache, exposed only through the non-exported AndroidX `FileProvider` scoped to `cache/updates/`, and opened with Android's package installer intent. If Android requires unknown-source permission, the app opens the system settings page and resumes only after returning with permission. The user must confirm installation in Android.

### UI integration

`app/src/main/java/dev/reverso/ui/UpdateSettingsScreen.kt` shows installed/latest versions, last check, error, release history, automatic-check/beta/notification preferences and manual check/download/view actions. `MainActivity.kt` owns coroutine dispatch, launch check, popup/remind-later state, permission requests, download progress/errors, checksum acknowledgement and installer handoff. Settings are exposed in both responsive navigation layouts. Update dependencies are `androidx.work:work-runtime-ktx` and `androidx.core:core-ktx`; `AndroidManifest.xml` declares internet, notifications, package install request permission and the FileProvider. `app/src/main/res/xml/update_paths.xml` limits FileProvider access to the update cache.

## User flows

1. On launch, if automatic checking is enabled, Reverso checks the API off the main thread, updates the cache and displays a dialog for a newer release unless that tag was deferred with “Remind Me Later”.
2. The same successful discovery may post a notification when notification preference and OS permission allow it. A worker can also notify when the app is not foregrounded.
3. Settings can force a check (still respecting the one-minute request guard), enable beta releases, enable/disable scheduled checks and notification preferences, inspect cached release history, or open the official release page.
4. Download requires a compatible APK. Reverso validates origin, size and checksum metadata, downloads to cache, checks the byte count and SHA-256 if supplied, then describes verification status. The user initiates the Android installer; Android owns package/signature validation and final confirmation.
5. If no compatible APK is attached, the interface identifies the release as source-only and offers its official release page.

## Security and failure handling

- Only the fixed public repository is queried; users cannot configure another repository or inject a URL.
- No personal access token or GitHub credential is used.
- API and downloads use HTTPS, timeouts and size limits. API release and asset links are host/path validated before use.
- Downloaded package bytes are not treated as trusted solely because the API responded. A provided GitHub SHA-256 digest must match. When no digest exists, Reverso clearly requires the user to continue; Android's package signature and confirmation remain mandatory.
- FileProvider is non-exported and grants temporary read access only to the APK URI.
- Offline/API errors leave cached history visible and show an error. Rate limits are reported; the app does not retry in a tight loop. WorkManager retries eligible background failures under its backoff policy.
- Notification denial disables delivery but does not prevent in-app checks or dialogs.

## Verification

`app/src/test/java/dev/reverso/ReleaseUpdateTest.kt` tests newer stable/no-update selection, beta filtering, source-only release visibility, notification tag de-duplication, checksum comparison and cache preservation on a simulated offline failure. CI additionally runs Android unit tests, `assembleDebug` and lint. No test currently performs a live API request, receives an OS notification, downloads an actual APK, or drives Android's package installer. Those cases require a configured emulator/device and published APK signed with the same key as the installed build. Before release, verify notification permission on API 33+, WorkManager execution, offline and rate-limit behavior, no-APK and checksum-absent releases, correct checksum and mismatch rejection, install permission flow, signature continuity and user-confirmed upgrade.

## Maintainer changes

Keep `API_URL` fixed to the official repository. Do not add a token. When changing cache schema, validate older cached JSON and update tests. When changing asset selection, account for universal and ABI-specific APKs and reject ambiguity. Do not weaken digest failure handling or bypass Android's package installer. Update this document, `08-API-REFERENCE.md`, `09-DATA-FLOW.md`, security/testing/release notes, `FILE-INDEX.md` and both changelogs whenever the flow changes.
