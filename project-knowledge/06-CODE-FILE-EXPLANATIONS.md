# Code file explanations

## Android application

### `app/src/main/java/dev/reverso/MainActivity.kt`

Defines the launcher `MainActivity` and Compose `ReversoApp`. It coordinates open editor tabs, the active tab, selected sidebar activity, optional SAF workspace, bottom panel state, application output events, and in-memory AI conversation. Android `OpenDocument`, `OpenDocumentTree`, and `CreateDocument` contracts provide file access. `openUri`/`saveTab` use `ContentResolver` streams and surface IO errors in the status bar. Workspace tree reads use `DocumentFile` only for a user-selected folder.

The responsive layout defaults to a hidden sidebar on phones and a visible sidebar on wide screens. The header icon toggles it; activity destinations remain beside the editor on wide layouts. The bottom panel is initially closed, toggles from the terminal header icon, and changes the editor's height. The draggable handle changes the panel height within device bounds. `IdeSidebar` routes Explorer/Search to `SidePanel`, AI to `AiChatScreen`, and other activity destinations to clearly labeled tool pages. `EditorArea` keeps the selected editor/welcome view central.

SAF actions create/open/save real user-selected files and folders. A project picker grants a parent tree URI; project creation creates a child directory there and persists both the grant URI and child name so the project can be restored after restart. Save As uses the tab's actual display name and wildcard MIME so Android does not infer a `.txt` suffix. New files use an extension-derived MIME type and keep the user's exact name. Ctrl+S and Ctrl+Shift+S save and Save As; Ctrl+F opens search. Explorer file actions use `DocumentFile` rename/delete and `DocumentsContract` copy/move calls, report provider failures, and expose a refresh action. AI provider configuration is shown in `AiProviderSettingsScreen`; send requests run on `Dispatchers.IO` through `AiChatClient`. The user must opt in to attach the active file (limited to 12,000 characters). AI replies are memory-only and replacing an editor buffer requires a confirmation dialog; replacement marks the tab dirty. No prompt, key, or source content is written to the application output log. The release popup remains connected to `ReleaseChecker`; it only opens the release page, with no background scheduler, Android notification, APK download or installer flow yet.

The app still does not connect the editor to Rust/JNI, Termux, Git, a debugger, a language server, an extension runtime, or port forwarding.

### `app/src/main/java/dev/reverso/EditorModels.kt`

Defines `IdeActivity`, `BottomTool`, `EditorTab`, `WorkspaceItem`, `ProjectWorkspace`, and `RecentProject`. `languageForFile` maps common extensions to display language names. `compareWorkspaceEntries` compares directory status first and names case-insensitively; `sortWorkspaceItems` applies it to provider entries. `insertAtSelection` replaces the current UTF-16 Compose selection (or inserts four spaces for the toolbar's Tab key) and returns the updated immutable tab. These helpers keep editor state transformations testable outside composables.

### `app/src/main/java/dev/reverso/WorkspaceRepository.kt`

`RecentProjects` persists at most eight SAF tree URI/name pairs in app-private SharedPreferences JSON. A `child_name` optionally identifies a project folder inside the granted tree; launch restoration reopens that child without persisting an ungranted child URI. `listWorkspaceItems` enumerates children through `DocumentFile`, sorts folders first, and traverses only expanded folders up to a bounded depth of five. `validateDocumentName` rejects path separators, reserved dot names and control characters without rewriting valid input. `mimeForDocumentName` derives a small common-extension MIME map. `listWorkspaceDirectories` discovers bounded copy/move targets. `performWorkspaceFileAction` uses provider-backed rename/delete and `DocumentsContract` copy/move operations and reports unsupported provider operations instead of simulating success. Explorer refresh forces a new provider listing. Failed provider listing returns an empty child list rather than crashing rendering. Access is limited to user-granted folder URIs.

### `app/src/main/java/dev/reverso/Versioning.kt` and `ReleaseChecker.kt`

`Versioning.kt` parses and compares semantic versions including prerelease identifiers. `ReleaseChecker` fetches GitHub's latest release with timeouts and a 1 MB body cap, accepts only an HTTPS `github.com` URL, and returns a release only when it is newer than the installed `versionName`. Network/JSON failures are handled by the UI as no notice. It does not download or install APK files.

### `app/src/main/java/dev/reverso/AiProvider.kt`

Defines provider metadata and validation, per-provider configuration persistence, Android Keystore credential storage, and the HTTP client. OpenAI, OpenRouter, and custom OpenAI-compatible providers use the chat-completions wire shape; Anthropic uses its Messages shape and headers. `AiProviderConfig.validationError` requires an HTTPS endpoint without user info, query or fragment and requires a model ID. `AiConfigRepository` stores endpoint/model per provider, while `SecureCredentialStore` encrypts each key with AES-GCM and a separate non-exportable Android Keystore AES key. There is no plaintext fallback. `AiChatClient.complete` posts only after the user sends, uses timeouts, disables redirects, caps response/history size, and avoids logging body or credentials. It returns provider text or a generic bounded failure. Tests cover endpoint validation and request-context opt-in; actual credentials/provider behavior needs device verification.

### `app/src/main/java/dev/reverso/ui/AiScreens.kt`

`AiProviderSettingsScreen` edits provider-specific endpoint/model and a masked API key. It queries whether that provider already has an encrypted key so a blank key field can retain the saved one. `AiChatScreen` renders in-memory conversation, exposes Configure/New Chat, an opt-in active-file switch, a prompt field and Send action, and an explicit Apply response action. New Chat clears the conversation. The Apply callback is handled by `MainActivity` with a whole-buffer replacement confirmation. The screen never reads or displays the saved key.

### `app/src/main/java/dev/reverso/ui/ReversoTheme.kt`

Contains the original dark color tokens and applies them through Material 3 `darkColorScheme`. Change this central palette to adjust surfaces, text, accent, borders, and warning colors consistently.

### `app/src/main/java/dev/reverso/ui/Brand.kt` and `app/src/main/res/drawable/reverso_mark.xml`

`ReversoMark` renders the app's own opposing-arrow/diamond vector in a rounded surface. The same vector is used in the welcome and app icon resources; it is not a third-party logo.

### `app/src/main/java/dev/reverso/ui/EditorSurface.kt`

`EditorTabs` renders selectable/closable tabs and dirty state. `Breadcrumbs` identifies workspace and current file. `EditorSurface` binds `TextFieldValue` to Compose `BasicTextField`, applies syntax coloring, renders line numbers and optionally a decorative code minimap on wide split panes. `KeyboardToolbar` inserts programming punctuation at the current selection. It is presentation only and does not call the Rust engine.

### `app/src/main/java/dev/reverso/ui/SyntaxHighlight.kt`

`ReversoSyntaxHighlight` uses bounded, non-mutating regular-expression passes over the visible document text to apply colors to common keywords, numbers, quoted strings, and line comments. Offset mapping is identity because text is not inserted or removed. This is lightweight token coloring, not a language parser or LSP.

## Rust files

### `crates/text-buffer/src/lib.rs`

Defines `TextBuffer` and `BufferError`. Buffer edits use UTF-8 byte offsets; `validate` rejects reversed ranges, out-of-bounds offsets, and offsets inside a multibyte character. `replace`, `insert`, and `delete` return explicit errors or removed text. Tests cover Unicode boundaries and invalid offsets.

### `crates/editor-engine/src/lib.rs`

Defines cursor, selection, editor state, edit errors, snapshot undo/redo, line/column lookup, search, and replace-all. It depends on `reverso-text-buffer` and standard library only. Rust currently builds as a separate library; Android does not yet load it through JNI.

## Android resources and tests

`app/src/main/AndroidManifest.xml` declares the launcher and network permission needed by release checking. `app/src/main/res/mipmap-anydpi-v26/ic_reverso.xml`, `drawable/reverso_mark.xml`, and `values/colors.xml` define the adaptive icon. `EditorModelsTest.kt` covers sorting, extension mapping and insertion. `VersioningTest.kt` covers stable/prerelease ordering and malformed input. `app/build.gradle.kts` adds Compose Material icons, AndroidX DocumentFile, and JUnit.
