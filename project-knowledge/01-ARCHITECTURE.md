# Architecture

## Current boundaries

`crates/text-buffer` owns UTF-8 text storage and checked byte-range edits. `crates/editor-engine` composes the buffer with cursor, selection, history, line/column, and search behavior. `app` is the Android shell; Compose currently stores `TextFieldValue` per editor tab and renders lightweight regex syntax color. `WorkspaceRepository` accesses only user-granted SAF tree URIs and stores recent folder references in app preferences. This keeps platform storage and presentation concerns out of Rust, but Android does not yet call the Rust engine.

`MainActivity` owns UI navigation and coordinates SAF open/create/save callbacks. `ui` holds reusable theme, logo, tabs, editor surface, and toolbar composables. `EditorModels` contains immutable tab data and selection insertion helpers. The terminal UI is separate from command execution; no Termux bridge exists. Git, debugger, language server, and extension services are likewise disconnected and are reported as unavailable.

## Intended direction

Android UI -> JNI adapter -> editor-engine -> text-buffer. Workspace and file lifecycle should move behind a document layer, while the UI renders state and maps user actions to engine commands. JNI must validate byte offsets and convert errors to explicit Kotlin results before it is introduced. Termux integration needs a separately authenticated interactive session bridge.

## Current constraints

Text offsets in Rust are UTF-8 byte offsets; line columns are 0-based Unicode scalar counts. Compose text selections use UTF-16 offsets. No conversion layer is implemented. Regex coloring is not a parser or language intelligence. SAF availability and provider behavior vary by Android provider; access is limited to granted URIs.
