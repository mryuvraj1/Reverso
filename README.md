# REVERSO

**Code Anywhere. Build Everything.**

Reverso is an open-source, Android-first mobile development environment. This repository is the initial Phase 1 workspace: a platform-independent Rust editing library and a minimal Kotlin/Jetpack Compose Android file editor.

## Current implementation

- Rust workspace: UTF-8 safe text edits, cursor and selection state, undo/redo, line/column lookup, search, and replace.
- Android app: local app-private workspace, create/open/save text files, and an editable dark Compose screen.
- Rust/JNI loading, syntax highlighting, multi-tab editing, project tree operations, external storage picker, Termux bridge, cloud, and Git integration are not implemented yet.

## Build

Rust (stable): `cargo fmt --check`, `cargo test --workspace`, `cargo clippy --workspace --all-targets`.
Android: install Android SDK 35 and Gradle 8.9+; then run `gradle assembleDebug` from the repository root. The Gradle wrapper is not included in this initial workspace.

## Project knowledge

See [project-knowledge/00-PROJECT-OVERVIEW.md](project-knowledge/00-PROJECT-OVERVIEW.md), [architecture](project-knowledge/01-ARCHITECTURE.md), [setup](project-knowledge/04-SETUP-GUIDE.md), [testing](project-knowledge/11-TESTING-GUIDE.md), and the [file index](project-knowledge/FILE-INDEX.md).

## License

Apache-2.0; see [LICENSE](LICENSE).
