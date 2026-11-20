# Troubleshooting

- `cargo: not found`: install Rust stable and Cargo.
- Android SDK platform missing: install platform 35 and configure `ANDROID_HOME` or `ANDROID_SDK_ROOT`.
- Gradle dependency resolution failure: confirm access to Google Maven, Maven Central, and Gradle Plugin Portal.
- App shows no files: current workspace is app-private; files created elsewhere will not appear.
- Cursor offsets: Rust APIs use UTF-8 byte offsets. Do not pass Compose offsets until a conversion/bridge is implemented.
