# Repository structure

REVERSO is currently one Git repository, not the seven-repository target architecture. It contains an Android app and Rust workspace. The Android presentation is separated into the `dev.reverso.ui` package; SAF workspace and recent-project logic live in `WorkspaceRepository.kt`; editor state helpers live in `EditorModels.kt`.

- `app/src/main/java/dev/reverso/MainActivity.kt`: Compose app state, navigation, SAF launchers, IDE shell and screens.
- `app/src/main/java/dev/reverso/ui`: reusable theme, Reverso mark, editor tabs, line-number editor, syntax styling and keyboard toolbar.
- `app/src/main/res`: original Reverso adaptive launcher icon and Android resources.
- `app/src/test`: pure model and version-comparison unit tests.
- `crates/text-buffer`: UTF-8-safe text storage and edit primitive.
- `crates/editor-engine`: Rust editor state and operations.
- `.github/workflows/ci.yml`: Rust checks and Android build/artifact publication.
- `project-knowledge`: implementation notes and file inventory.

## Android boundaries

The Compose UI owns presentation state and delegates document reads/writes to Android's Storage Access Framework. It does not scan arbitrary device storage. Reverso's UI provides a terminal panel, but no execution bridge exists yet. Git actions, debugging, extensions, LSP diagnostics, and Rust/JNI editing are clearly marked unavailable.
