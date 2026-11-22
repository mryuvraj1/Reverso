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


## Git clone and source control

User supplies a GitHub HTTPS URL and destination name in `GitCloneDialog` -> `GitRepository` validates the credential-free host/path and clones via JGit into `filesDir/git-repositories` -> optional fine-grained token is read from AES-GCM/Android Keystore storage -> successful clone becomes a `file://` DocumentFile workspace and is persisted as an internal recent repo -> `GitPanelScreen` requests status/branches/history -> stage/commit/branch/pull/push actions run on IO and refresh the snapshot -> results or bounded failure text return to the source-control UI. SAF URI workspaces do not flow into JGit.

## Termux boundary

No command flow is currently wired. The bottom Terminal panel renders an explicit unavailable state. The Termux documented RUN_COMMAND intent is a potential one-shot command interface, with user permission and Termux opt-in; its completion callback is not interactive terminal streaming. Reverso does not send those intents or fabricate output. See [18-GIT-AND-TERMUX.md](18-GIT-AND-TERMUX.md).


## Update navigation and APK delivery

Launcher `MainActivity.onCreate` -> `ReversoTheme` -> `ReversoApp` owns release-check state. The Updates activity rail, welcome action, and Settings shortcut all select `IdeActivity.UPDATES`, which renders `UpdateSettingsScreen`. Manual/automatic checks call the fixed Releases API on IO -> cache metadata and time -> choose stable or beta candidates -> render version/history/error and optionally open a dismissible newer-release dialog. Download streams the selected official APK into app cache, reports byte progress to Compose, validates size and SHA-256 when supplied, then waits for the user's install action and hands the file to Android's package installer. Android handles install permission and final confirmation. No silent installation occurs.
