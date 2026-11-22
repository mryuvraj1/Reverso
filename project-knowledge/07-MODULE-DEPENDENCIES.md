# Module dependencies

`reverso-text-buffer` depends on Rust standard library only. `reverso-editor-engine` depends on `reverso-text-buffer` and standard library. Android `app` uses Activity Compose, Compose UI/Material 3, AndroidX DocumentFile, AndroidX WorkManager, AndroidX Core, Kotlin coroutines, Android/JCA Keystore, JGit 7.8.1 and JUnit. There are no module cycles. JGit is a pure-Java Git implementation but requires filesystem access; it is only opened for app-managed clones under `filesDir/git-repositories`. SAF-backed project file I/O stays behind `WorkspaceRepository` and does not enter JGit. JGit's Eclipse Distribution License 1.0 / BSD 3-Clause licensing must be preserved in redistributed notices.

No Termux library is included. No JNI layer currently connects the Kotlin editor to Rust. Keep storage/provider adapters separate from editor UI and keep remote Git operations behind `GitRepository`.
