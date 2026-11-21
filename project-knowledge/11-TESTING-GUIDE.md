# Testing guide

## Kotlin unit tests

Run `./gradlew :app:testDebugUnitTest`. `EditorModelsTest` checks workspace sorting, language labels, and cursor-aware insertion. `WorkspaceStorageTest` checks preservation of common source extensions, extension-derived language/MIME values, and rejection of path traversal/control characters. `AiProviderConfigTest` checks provider defaults/protocol mapping, HTTPS and endpoint-secret validation, required model handling, and opt-in code-context serialization. `VersioningTest` checks semantic version ordering, prerelease precedence, and malformed input. SAF provider operations require a device/runtime and are not simulated by JVM tests.

## Android build and manual checks

Run `./gradlew :app:assembleDebug lint` with Android SDK platform 35. On a device, verify exact-name create/save for `idk.py`, `main.js`, `index.html`, `style.css`, and `app.kt`; create a nested directory and file; reopen a selected workspace after app restart; and Save As into a different SAF folder. Verify rename/delete/copy/move with the device's actual SAF provider because support varies by provider. Also check rotation, keyboard shortcuts, and sidebar/bottom-panel toggles and resizing. For AI, configure a test provider/key, send a simple prompt without context, then send one with the context toggle. Confirm that provider errors are shown without exposing the key, New Chat clears the in-memory conversation, and Apply requires confirmation and marks the file dirty. Do not use a production key for test prompts unless the user accepts provider billing and data policies.

## Rust

Run `cargo fmt --check`, `cargo test --workspace`, and `cargo clippy --workspace --all-targets`. The Rust library tests do not establish JNI integration; no JNI layer is wired into Android.

## Verified boundaries

CI run [44903602597](https://github.com/mryuvraj1/Reverso/actions/runs/44903602597) passed on 2026-11-21. It ran `./gradlew testDebugUnitTest assembleDebug lint`, compiled the Android app, passed all 10 Kotlin unit tests, assembled the debug APK, and completed Android lint. Rust CI passed `cargo fmt --check`, `cargo test --workspace` (6 unit tests), and `cargo clippy --workspace --all-targets -- -D warnings`. `git diff --check` also passed locally. The local Termux environment has no Android SDK, so Android verification used GitHub Actions.

CI cannot verify provider credentials, paid API behavior, Keystore hardware behavior, Android SAF providers, exact provider-specific filename behavior, file-operation support, or touch gestures without a configured device/runtime. The release checker remains launch-only: WorkManager scheduling, update notification preferences, release history, APK checksum/download and package installation have not been implemented or tested. Terminal execution, Git, debugging, extension loading, language-server diagnostics, and port forwarding remain unavailable and must not be tested as operational.
