# Technology stack

Rust stable, Cargo workspace, Kotlin, Android SDK 35, Jetpack Compose Material 3, Gradle Kotlin DSL. The Rust crates have no third-party dependencies. The Android build resolves AndroidX and Kotlin/Android Gradle plugins from Google Maven, Maven Central, and Gradle Plugin Portal. Android dependency versions are pinned in `app/build.gradle.kts` and root `build.gradle.kts`.

The app uses Android’s built-in `org.json` and HTTPS APIs for release checks and AI providers. JGit (`org.eclipse.jgit:org.eclipse.jgit:7.8.1.202611041320-r`) provides local Git operations and HTTPS clone/push/pull in ordinary app-private filesystem workspaces. JGit is distributed under Eclipse Distribution License 1.0 / BSD 3-Clause terms; see its upstream project and retain applicable notices when distributing. Git credentials use Android Keystore AES-GCM. SAF documents continue to use AndroidX DocumentFile and content URIs; they are not presented as filesystem-backed Git worktrees.

There is no JNI crate or Go service in this checkout. Termux is not a build dependency. Its documented RUN_COMMAND integration can start one-shot commands, but does not supply Reverso with a streaming interactive PTY/stdin channel; the app does not claim that its terminal panel executes commands.
