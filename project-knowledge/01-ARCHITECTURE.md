# Architecture

## Current boundaries

`crates/text-buffer` owns UTF-8 text storage and checked byte-range edits. `crates/editor-engine` composes the buffer with cursor, selection, history, line/column, and search behavior. `app` is the Android shell; Compose currently stores `TextFieldValue` per editor tab and renders lightweight regex syntax color. `WorkspaceRepository` accesses only user-granted SAF tree URIs, keeps the parent grant separate from a project child directory, stores recent folder references in app preferences, and routes rename/delete/copy/move through Android document APIs. Creation targets the selected workspace/nested directory; Save As uses the entered tab name and wildcard MIME. This keeps platform storage and presentation concerns out of Rust, but Android does not yet call the Rust engine.

`MainActivity` owns UI navigation and coordinates SAF open/create/save callbacks. `ui` holds reusable theme, logo, tabs, editor surface, and toolbar composables. `EditorModels` contains immutable tab data and selection insertion helpers. The terminal UI is separate from command execution; no Termux bridge exists. Git, debugger, language server, extension and port-forwarding services are likewise disconnected and are reported as unavailable. The AI adapter is a direct Android-to-provider client rather than a Reverso service; provider credentials are encrypted with Android Keystore and are scoped by provider.

Release updates have a separate service boundary: `ReleaseChecker` owns the fixed GitHub API client, semantic filtering, and cache; WorkManager schedules periodic checks; `UpdateNotification` handles OS permission-aware notifications; `UpdateInstaller` handles bounded cache downloads and Android package-installer handoff. Compose only renders release state and collects explicit user actions. Android remains the authority for permission, signature verification and install confirmation. See [17-UPDATE-SYSTEM.md](17-UPDATE-SYSTEM.md).

## Intended direction

Android UI -> JNI adapter -> editor-engine -> text-buffer. Workspace and file lifecycle should move behind a document layer, while the UI renders state and maps user actions to engine commands. JNI must validate byte offsets and convert errors to explicit Kotlin results before it is introduced. Termux integration needs a separately authenticated interactive session bridge.

## Current constraints

Text offsets in Rust are UTF-8 byte offsets; line columns are 0-based Unicode scalar counts. Compose text selections use UTF-16 offsets. No conversion layer is implemented. Regex coloring is not a parser or language intelligence. SAF availability and provider behavior vary by Android provider; access is limited to granted URIs.
