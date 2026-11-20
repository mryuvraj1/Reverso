# Code file explanations

## `crates/text-buffer/src/lib.rs`
Defines `TextBuffer` and `BufferError`. The buffer accepts owned initial text; reads return `&str`; `replace`, `insert`, and `delete` use UTF-8 byte offsets and return the deleted content or an explicit error. `validate` rejects reversed ranges, out-of-bounds offsets, and offsets inside a multibyte character. Used by `Editor`. It has no dependencies beyond Rust standard library. Tests cover multibyte edits and invalid offsets. Add buffer behavior here only when it is independent of cursor/history/UI state.

## `crates/editor-engine/src/lib.rs`
Defines `Cursor`, `Selection`, `EditError`, and `Editor`. `Editor::new` creates a document state from text; `set_cursor`/`set_selection` validate offsets; `insert` and `delete` record history; `undo`/`redo` replay snapshots; `line_column`, `find`, and `replace_all` provide basic indexing and search. Depends on `reverso-text-buffer`. Tests exercise edits, history, selection replacement, Unicode columns, and search. Error values preserve buffer failures. The editor currently stores a simple linear history and has no document identity or serialization.

## `app/src/main/java/dev/reverso/MainActivity.kt`
Hosts the Compose editor and document workflows. `EditorTab` stores a tab identifier, display title, optional `Uri`, text, and dirty flag. `ReversoApp` owns tab state, calls `OpenDocument` and `CreateDocument`, reads/writes user-selected content through `ContentResolver`, and reports failures in the status row. The editable field updates the active tab; find/replace applies to that tab. Ctrl+S and Ctrl+F are recognized for external keyboards. Storage access is limited to URIs selected by the user. Save and open errors are surfaced as status text. No Rust/JNI connection, syntax highlighting, recovery, or close-tab confirmation exists yet. To extend document behavior, keep URI I/O and tab metadata in a dedicated document repository and add tests for permission persistence and save failures.

## `app/src/main/java/dev/reverso/ReleaseChecker.kt`
Queries the public GitHub latest-release API with connection/read timeouts and a response-size cap. It parses tag, release notes, and release URL using Android’s `org.json`, checks that the release page host is `github.com`, and parses the tag (or release name when the tag is not semantic) and compares semantic versions, including prerelease identifiers, to the installed app version. It returns `AvailableRelease` only when the release is newer; HTTP/network/JSON errors are handled by the caller so the editor remains available offline. It does not download or install APKs. The app invokes it once per process launch from an IO dispatcher.

## Build files
`Cargo.toml` declares a two-crate workspace. Root `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts` pin the Android build plugins, SDK, and dependencies. `AndroidManifest.xml` declares the launcher activity; `styles.xml` sets the dark system bars. CI workflow runs both toolchains on GitHub Actions.
