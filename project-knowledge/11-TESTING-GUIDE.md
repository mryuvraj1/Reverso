# Testing guide

Rust unit tests are embedded beside the implementation in both crates. Run `cargo test --workspace`; format with `cargo fmt --check`; lint with `cargo clippy --workspace --all-targets -- -D warnings`. Android currently has no automated tests. Run `gradle assembleDebug lint` after installing Android SDK 35 and dependencies. CI declares these commands, but they have not been verified from this development environment because Rust and Android SDK tooling are missing.
