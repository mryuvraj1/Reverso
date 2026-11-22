# Project knowledge changelog

## 2026-11-22 — File handling and narrow-screen polish

Documented exact-extension Save As/create, nested workspace targets, persistent SAF tree grants and startup restoration, Explorer refresh and file icons, storage-provider rename/delete/copy/move, keyboard resizing and the launch-only update-check boundary.

## 2026-11-22 — SAF filename and workspace persistence fixes

Documented exact-name Save As, extension-derived MIME types, child-project persistence via the original granted tree URI, auto-restoration of the latest workspace, nested creation targets, provider-backed rename/delete/copy/move, and manual Explorer refresh. Recorded device-only SAF verification requirements and the still-incomplete background update/notification/APK installer system.

## 2026-11-21 — CI verification

Recorded successful Android and Rust CI for the IDE and AI provider changes; the workflow uploaded a debug APK artifact.

## 2026-11-21 — Toggleable IDE panels and AI provider client

Documented the responsive hide/show sidebar and editor space recovery, animated draggable bottom panel and Ports destination. Added provider-specific AI setup/API, encrypted per-provider Android Keystore keys, opt-in file context, bounded direct HTTPS requests, in-memory chats, and explicit whole-buffer apply confirmation. Documented that provider credentials and Android device runtime are required for live API/Keystore verification.

## 2026-11-21 — Professional mobile IDE shell

Documented Reverso branding, responsive navigation, workspace tree, file editing, syntax-colored editor surface and unavailable backend states.

## 2026-11-22

Implemented the official GitHub Releases update manager: stable/beta filtering, launch and periodic checks, cached release history, settings, Android notification channel and deduplication, bounded official APK download, SHA-256 verification when provided, source-only reporting, and Android installer handoff with explicit user confirmation. Added release selection/cache/checksum and ABI asset-selection tests and update architecture/security documentation. CI run 44947324260 passed Rust formatting/tests/Clippy and Android unit-test/build/lint jobs, including supported/incompatible ABI asset selection. A debug APK was uploaded. Device-level network, notification, APK download and installer verification remains pending.

## 2026-11-22 — Git foundation and integration boundaries

Added JGit-backed GitHub clone and source control for app-private managed repositories, with encrypted fine-grained token storage, status/staging/commit/branches/history/push/pull UI, and local repository tests. Added `file://` workspace I/O for those clones. Documented SAF filesystem limits and Termux RUN_COMMAND semantics; interactive terminal integration remains unimplemented. Live remote/device verification remains required.

## 2026-11-22 — JGit API compatibility correction

Updated remote URL snapshotting to call JGit's public `getURIs()` accessor after CI caught that the `uris` field is private.

## 2026-11-22 — Unborn Git repository support

Git snapshots now return an empty history before the first commit, matching Git's unborn-branch state.

## 2026-11-22 — Git foundation CI verification

CI run 44997689939 passed Rust formatting/tests/Clippy and Android unit tests/build/lint for commit `977905c`, including the new JGit real-local-repository tests; CI uploaded a debug APK. Live GitHub network operations and Android device workflows remain unverified.

## 2026-11-22 — Make updates reachable and polish live Reverso UI

Traced the launcher `MainActivity` and phone navigation. Moved release preferences into a first-class Updates activity reachable from the rail, welcome screen and Settings shortcut. Added explicit Stable/Stable + Beta controls, View on GitHub, release-dialog dismissal persistence and byte-based APK download progress. Set Material typography explicitly to sans-serif and revised the actual adaptive launcher/header logo vector. ADB currently reports no attached devices, so install/launch visual comparison remains unverified.


The existing update settings were nested inside the phone-collapsed sidebar's Settings page. The user-visible route is now a dedicated Updates activity and welcome action, with a Settings shortcut. Dismissing an update dialog now stores the remind-later tag.

## 2026-11-22 — Compose API compatibility

Applied sans-serif fonts per Material typography role and replaced an unavailable generated icon with the supported link icon after CI compilation identified pinned-library API differences.
