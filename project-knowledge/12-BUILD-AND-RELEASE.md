# Build and release

Current Android debug build: `gradle assembleDebug`. Current Rust validation: `cargo fmt --check && cargo test --workspace && cargo clippy --workspace --all-targets -- -D warnings`. The Gradle wrapper, signed release configuration, artifact publication, and release process are not yet set up. Do not distribute the app as a production IDE until JNI, robust file errors, tests, and platform integration have been completed.
