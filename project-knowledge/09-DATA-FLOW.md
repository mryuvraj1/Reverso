# Data flow

## Rust editing path

Caller supplies text -> `Editor` stores `TextBuffer` -> action validates byte range -> buffer replaces text -> editor records old/new text and updates cursor -> undo/redo can replay that edit. Invalid offsets return an error and do not change the buffer. Android is not wired to this path yet.

## Android local file path

User launches Android `OpenDocument` or `OpenDocumentTree` -> SAF returns a URI and scoped permission -> `ContentResolver` reads/writes file text or `DocumentFile` enumerates/creates items -> Compose updates the matching `EditorTab` -> Save writes through the same URI or launches wildcard-MIME Create Document with the tab name for an unsaved/Save As operation. New project records retain the selected parent tree URI plus child directory name; launch restoration reopens the child through that granted tree. Nested folder selection determines the parent for new items. Rename/delete and `DocumentsContract` copy/move mutate the selected provider and trigger Explorer refresh. No app-private workspace copy is made.

## Sidebar and bottom panel

Header sidebar button toggles layout state -> on wide screens the animated rail/sidebar enters/leaves the row and the editor takes/releases its width -> on phones the sidebar destination occupies the content viewport while open. Header terminal button opens/closes the bottom panel (selecting Terminal when opening). Panel tab selection changes content; drag delta changes panel height and the editor consumes the remaining space.

## AI request flow

User configures provider/endpoint/model/key -> endpoint passes HTTPS validation -> key is encrypted by AES-GCM with a provider-specific Android Keystore key; endpoint/model are saved separately in private preferences -> user opens AI Chat and enters a prompt -> optional file context is appended only when the explicit switch is on -> request body and provider headers are built for OpenAI-compatible chat completions or Anthropic Messages -> `HttpsURLConnection` posts on IO dispatcher with timeouts and redirect disabled -> bounded response JSON is parsed into text -> response is held in Compose memory -> Apply opens confirmation -> confirm replaces the active in-memory buffer and marks it dirty -> user saves explicitly. New Chat clears only the in-memory conversation.

## Update notice flow

App starts (or WorkManager runs) -> `ReleaseChecker` requests the fixed public Releases API over HTTPS on a background dispatcher, subject to cache/minimum interval -> draft/invalid tags are filtered and beta tags are optional -> metadata and timestamp are cached -> version is compared with installed `versionName` -> newer release can show an in-app dialog and a deduplicated notification. On user download action, `UpdateInstaller` bounds the APK transfer, validates source/size and SHA-256 when provided, stores it in cache and opens Android’s package installer after user confirmation/permission. Android performs signature validation and installation confirmation; absent APK is shown as source-only. See [17-UPDATE-SYSTEM.md](17-UPDATE-SYSTEM.md).
