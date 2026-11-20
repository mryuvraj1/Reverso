# Setup guide

## Rust
Install Rust stable with rustup, then from the repository root run `cargo test --workspace`. Formatting and lint commands are `cargo fmt --check` and `cargo clippy --workspace --all-targets`.

## Android
Install JDK 17, Android SDK platform 35 and a compatible Android Gradle Plugin toolchain. Set `ANDROID_HOME`/`ANDROID_SDK_ROOT` for your environment. Run `gradle assembleDebug` from the root. This initial repository does not include a Gradle wrapper, so use an installed compatible Gradle (8.9 or newer). First build needs dependency network access.

The app creates an internal directory named `workspace` under its private files directory at runtime. It does not request broad storage access.
