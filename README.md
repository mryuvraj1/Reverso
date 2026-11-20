# REVERSO

**Code Anywhere. Build Everything.**

Reverso is an open-source, Android-first mobile development environment. This repository is the initial Phase 1 workspace: a platform-independent Rust editing library and a minimal Kotlin/Jetpack Compose Android file editor.

## Current implementation

- Rust workspace: UTF-8 safe text edits, cursor and selection state, undo/redo, line/column lookup, search, and replace.
- Android app: dark Compose editor, multiple tabs, Android document picker open/save, basic find and replace, and Ctrl+S/Ctrl+F hardware keyboard shortcuts.
- On startup, the app checks published GitHub Releases and shows an in-app notice when a newer stable release is available. The notice links to the release page; it does not download or install an update.
- Rust/JNI loading, syntax highlighting, project tree operations, Termux bridge, cloud, and Git integration are not implemented yet.

## Build

Rust (stable): `cargo fmt --check`, `cargo test --workspace`, `cargo clippy --workspace --all-targets`.
Android: install Android SDK platform 35, then run `./gradlew assembleDebug` from the repository root. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project knowledge

See [project-knowledge/00-PROJECT-OVERVIEW.md](project-knowledge/00-PROJECT-OVERVIEW.md), [architecture](project-knowledge/01-ARCHITECTURE.md), [setup](project-knowledge/04-SETUP-GUIDE.md), [testing](project-knowledge/11-TESTING-GUIDE.md), and the [file index](project-knowledge/FILE-INDEX.md).

## License

Apache-2.0; see [LICENSE](LICENSE).
