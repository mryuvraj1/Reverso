# Build and release

Current Android debug build: `./gradlew assembleDebug`; the artifact is `app/build/outputs/apk/debug/app-debug.apk`. Current Rust validation: `cargo fmt --check && cargo test --workspace && cargo clippy --workspace --all-targets -- -D warnings`. Signed release configuration and Play Store publication are not set up. CI uploads the debug APK as a workflow artifact. The update manager reads the official Releases API and can hand a selected APK to Android’s installer. A published update must keep the same Android application ID and signing certificate as the installed app; verify signing continuity and user-confirmed upgrade on a device before treating update delivery as release-ready. See [17-UPDATE-SYSTEM.md](17-UPDATE-SYSTEM.md). Do not distribute the app as a production IDE until JNI, robust file errors, tests, and platform integration have been completed.

## AI provider runtime checks

The app builds without provider credentials. Live AI requests are deliberately excluded from CI because they require user credentials and could incur billing. On a test device, configure a provider and verify one prompt without code context, one prompt with explicit context, an invalid-key response, New Chat, and the apply confirmation. Never add keys to Gradle properties, source files, logs, or CI.
