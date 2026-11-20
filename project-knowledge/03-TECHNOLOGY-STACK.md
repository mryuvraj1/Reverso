# Technology stack

Rust stable, Cargo workspace, Kotlin, Android SDK 35, Jetpack Compose Material 3, Gradle Kotlin DSL. The Rust crates have no third-party dependencies. The Android build resolves AndroidX and Kotlin/Android Gradle plugins from Google Maven, Maven Central, and Gradle Plugin Portal. Android dependency versions are pinned in `app/build.gradle.kts` and root `build.gradle.kts`.

There is no JNI crate, Go service, TypeScript application, or Termux dependency in this checkout.
