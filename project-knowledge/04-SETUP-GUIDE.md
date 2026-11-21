# Setup guide

## Rust
Install Rust stable with rustup, then from the repository root run `cargo test --workspace`. Formatting and lint commands are `cargo fmt --check` and `cargo clippy --workspace --all-targets`.

## Android
Install JDK 17, Android SDK platform 35 and a compatible Android Gradle Plugin toolchain. Set `ANDROID_HOME`/`ANDROID_SDK_ROOT` for your environment. Run `./gradlew assembleDebug` from the root. The committed wrapper pins Gradle 8.9. First build needs dependency network access. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The app does not request broad storage access. Choose files or workspace folders through Android SAF; recent tree URIs are saved in app-private preferences when Android allows persisted access.
