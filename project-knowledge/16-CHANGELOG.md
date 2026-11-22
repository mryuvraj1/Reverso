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
