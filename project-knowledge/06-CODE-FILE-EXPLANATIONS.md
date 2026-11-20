# Code file explanations

## `crates/text-buffer/src/lib.rs`
Defines `TextBuffer` and `BufferError`. The buffer accepts owned initial text; reads return `&str`; `replace`, `insert`, and `delete` use UTF-8 byte offsets and return the deleted content or an explicit error. `validate` rejects reversed ranges, out-of-bounds offsets, and offsets inside a multibyte character. Used by `Editor`. It has no dependencies beyond Rust standard library. Tests cover multibyte edits and invalid offsets. Add buffer behavior here only when it is independent of cursor/history/UI state.

## `crates/editor-engine/src/lib.rs`
Defines `Cursor`, `Selection`, `EditError`, and `Editor`. `Editor::new` creates a document state from text; `set_cursor`/`set_selection` validate offsets; `insert` and `delete` record history; `undo`/`redo` replay snapshots; `line_column`, `find`, and `replace_all` provide basic indexing and search. Depends on `reverso-text-buffer`. Tests exercise edits, history, selection replacement, Unicode columns, and search. Error values preserve buffer failures. The editor currently stores a simple linear history and has no document identity or serialization.

## `app/src/main/java/dev/reverso/MainActivity.kt`
Creates the Compose host and implements the first Android file editing flow. `ReversoApp` owns selected file, displayed text, status, and file list using Compose state. It creates, lists, reads, and writes plain files under app-private storage. It has no external file picker, tab model, editor-engine/JNI connection, or robust save error UI. Keep I/O and state handling explicit as the app is split into repositories/view models.

## Build files
`Cargo.toml` declares a two-crate workspace. Root `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts` pin the Android build plugins, SDK, and dependencies. `AndroidManifest.xml` declares the launcher activity; `styles.xml` sets the dark system bars. CI workflow runs both toolchains on GitHub Actions.
