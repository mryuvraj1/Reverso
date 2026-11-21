# REVERSO

**Code Anywhere. Build Everything.**

Reverso is an open-source, Android-first mobile development environment. This repository is the initial Phase 1 workspace: a platform-independent Rust editing library and a Kotlin/Jetpack Compose mobile IDE shell with Android Storage Access Framework file workflows.

## Current implementation

- Rust workspace: UTF-8 safe text edits, cursor and selection state, undo/redo, line/column lookup, search, and replace.
- Android app: responsive Reverso-branded dark IDE, SAF workspace and recent-project restoration, exact-name file/folder creation under the selected (including nested) directory, open/save/Save As, provider-backed rename/delete/copy/move, expandable Explorer with refresh and file type icons, editor tabs, syntax color, search and replace, line numbers, wide-screen split view, collapsible sidebar, animated/resizable bottom panel, touch keyboard toolbar, and Ctrl+S/Ctrl+Shift+S/Ctrl+F support.
- AI chat connects directly to OpenAI Chat Completions, Anthropic Messages, OpenRouter, or an OpenAI-compatible HTTPS endpoint. API keys are encrypted with Android Keystore, source context is opt-in, and applying a response requires confirmation.
- The bottom panel includes Terminal, Output, Problems, Debug Console, and Ports. Terminal execution, Git, debugging, extension installation, LSP diagnostics and Rust/JNI are shown as unavailable until their real backends are implemented.
- The update manager checks the official GitHub Releases API at launch and on a network-constrained WorkManager schedule, caches release history, supports stable/beta channels, and can post deduplicated Android notifications. It can download a compatible official APK, verify GitHub SHA-256 metadata when present, and hand it to Android’s user-confirmed installer. Missing checksums require explicit acknowledgement; missing APKs are reported as source-only.
- Git and GitHub workflows (repository detection, status, staging, commits, clone/push/pull) are not implemented. Rust/JNI loading, Termux bridge, cloud and Git execution are not implemented yet. Syntax coloring and project tree browsing are currently Android-side features.
- Update behavior has unit coverage for stable/beta selection, no-update behavior, source-only releases, notification deduplication, checksum comparison and cached offline failures. Real API, notification, APK transfer and Android installation flows require emulator/device verification. WorkManager is periodic and Android may defer its execution.
- SAF providers vary in rename/copy/move and filename handling. Automated unit tests cover extension mapping and safe names; actual picker/provider operations require Android device verification.

## Build

Rust (stable): `cargo fmt --check`, `cargo test --workspace`, `cargo clippy --workspace --all-targets`.
Android: install Android SDK platform 35, then run `./gradlew :app:testDebugUnitTest :app:assembleDebug` from the repository root. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project knowledge

See [project-knowledge/00-PROJECT-OVERVIEW.md](project-knowledge/00-PROJECT-OVERVIEW.md), [architecture](project-knowledge/01-ARCHITECTURE.md), [update system](project-knowledge/17-UPDATE-SYSTEM.md), [setup](project-knowledge/04-SETUP-GUIDE.md), [testing](project-knowledge/11-TESTING-GUIDE.md), and the [file index](project-knowledge/FILE-INDEX.md).

## Engineering documentation

The `project-knowledge/` directory documents architecture, workflows, file responsibilities, security boundaries, tests, release steps, and the complete file index. Start at [project overview](project-knowledge/00-PROJECT-OVERVIEW.md), [how it works](project-knowledge/05-HOW-IT-WORKS.md), [code explanations](project-knowledge/06-CODE-FILE-EXPLANATIONS.md), and [file index](project-knowledge/FILE-INDEX.md).

## License

Apache-2.0; see [LICENSE](LICENSE).
