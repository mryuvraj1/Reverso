# Module dependencies

`reverso-text-buffer` -> Rust standard library only. `reverso-editor-engine` -> `reverso-text-buffer` and Rust standard library. Android `app` -> Activity Compose, Compose UI, Material 3, and Kotlin coroutines APIs supplied through the Compose runtime. The Android module does not depend on Rust/JNI. There are no cyclic dependencies. Keep buffer independent of Android and keep UI code from becoming the source of editor semantics.
