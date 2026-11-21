# Build and release

Current Android debug build: `./gradlew assembleDebug`; the artifact is `app/build/outputs/apk/debug/app-debug.apk`. Current Rust validation: `cargo fmt --check && cargo test --workspace && cargo clippy --workspace --all-targets -- -D warnings`. Signed release configuration and Play Store publication are not set up. CI uploads the debug APK as a workflow artifact. The in-app notice reads published stable GitHub Releases; a releasable update must keep the same Android application ID and signing certificate as the installed app. Do not distribute the app as a production IDE until JNI, robust file errors, tests, and platform integration have been completed.

## AI provider runtime checks

The app builds without provider credentials. Live AI requests are deliberately excluded from CI because they require user credentials and could incur billing. On a test device, configure a provider and verify one prompt without code context, one prompt with explicit context, an invalid-key response, New Chat, and the apply confirmation. Never add keys to Gradle properties, source files, logs, or CI.
