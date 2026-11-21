# Testing guide

## Kotlin unit tests

Run `./gradlew :app:testDebugUnitTest`. `EditorModelsTest` checks workspace sorting, language labels, and cursor-aware insertion. `AiProviderConfigTest` checks provider defaults/protocol mapping, HTTPS and endpoint-secret validation, required model handling, and opt-in code-context serialization. `VersioningTest` checks semantic version ordering, prerelease precedence, and malformed input. Keystore operations and live provider requests require an Android runtime and user credentials; they are not faked in local unit tests.

## Android build and manual checks

Run `./gradlew :app:assembleDebug lint` with Android SDK platform 35. On a device, verify open/save, workspace create/expand, rotation, external keyboard shortcuts, and the sidebar and bottom panel toggles/resizing. For AI, configure a test provider/key, send a simple prompt without context, then send one with the context toggle. Confirm that provider errors are shown without exposing the key, New Chat clears the in-memory conversation, and Apply requires confirmation and marks the file dirty. Do not use a production key for test prompts unless the user accepts provider billing and data policies.

## Rust

Run `cargo fmt --check`, `cargo test --workspace`, and `cargo clippy --workspace --all-targets`. The Rust library tests do not establish JNI integration; no JNI layer is wired into Android.

## Verified boundaries

CI run [44903602597](https://github.com/mryuvraj1/Reverso/actions/runs/44903602597) passed on 2026-11-21. It ran `./gradlew testDebugUnitTest assembleDebug lint`, compiled the Android app, passed all 10 Kotlin unit tests, assembled the debug APK, and completed Android lint. Rust CI passed `cargo fmt --check`, `cargo test --workspace` (6 unit tests), and `cargo clippy --workspace --all-targets -- -D warnings`. `git diff --check` also passed locally. The local Termux environment has no Android SDK, so Android verification used GitHub Actions.

CI cannot verify provider credentials, paid API behavior, Keystore hardware behavior, Android SAF providers, or touch gestures without a configured device/runtime. Terminal execution, Git, debugging, extension loading, language-server diagnostics, and port forwarding remain unavailable and must not be tested as operational.
