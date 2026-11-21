# Testing guide

## Kotlin unit tests

Run `./gradlew :app:testDebugUnitTest`. `EditorModelsTest` checks workspace sort order, extension-to-language labels, and insertion/replacement at a text selection without needing Android framework objects. `VersioningTest` checks semantic version ordering, prerelease precedence, and invalid input.

## Android build

Run `./gradlew :app:assembleDebug` with Android SDK platform 35 installed. The Android source uses platform SAF launchers, so interactive storage behavior still needs device/emulator verification: open a text file, create/open a workspace, expand a folder, create a file, edit, save, and relaunch to verify a persisted recent folder. Rotate between portrait and landscape and use a hardware keyboard for Ctrl+S/Ctrl+F.

## Rust

Run `cargo fmt --check`, `cargo test --workspace`, and `cargo clippy --workspace --all-targets`. The Rust library tests do not establish JNI integration; no JNI layer is currently wired into Android.

## Honest limitations

The terminal panel is explanatory UI until an authenticated Termux bridge is implemented. Git, debugger, extension marketplace, LSP, and Rust/JNI integrations are also unavailable and must not be tested as if operational. Android instrumentation/device tests are not yet present.
