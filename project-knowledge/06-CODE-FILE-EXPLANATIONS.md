# Code file explanations

## Android application

### `app/src/main/java/dev/reverso/MainActivity.kt`

Defines the launcher `MainActivity` and Compose `ReversoApp`. It coordinates open editor tabs, the active tab, selected sidebar activity, optional SAF workspace, bottom panel state, application output events, and in-memory AI conversation. Android `OpenDocument`, `OpenDocumentTree`, and `CreateDocument` contracts provide file access. `openUri`/`saveTab` use `ContentResolver` streams and surface IO errors in the status bar. Workspace tree reads use `DocumentFile` only for a user-selected folder.

The launcher path is `MainActivity.onCreate` -> `ReversoTheme` -> `ReversoApp`. `EditorArea` is the active central editor/welcome composable; it uses `EditorSurface` and `EditorTabs`. `IdeSidebar` draws the activity rail and routes Explorer/Search to `SidePanel`, AI to `AiChatScreen`, and other destinations to their tool pages. The bottom developer panel is `BottomPanel`; it has a drag handle and selected-tab state. On phones the sidebar opens from the header menu and replaces the editor until toggled closed; wide screens place it beside the editor. The compact header moves file/save/find/split/panel controls into a working overflow menu below 420 dp to avoid clipping the brand and title. The activity rail uses a selected marker and Reverso mark. Explorer separates the workspace root, open editors and file tree visually.

SAF actions create/open/save real user-selected files and folders. A project picker grants a parent tree URI; project creation creates a child directory there and persists both the grant URI and child name so the project can be restored after restart. Save As uses the tab's actual display name and wildcard MIME so Android does not infer a `.txt` suffix. New files use an extension-derived MIME type and keep the user's exact name. Ctrl+S and Ctrl+Shift+S save and Save As; Ctrl+F opens search. Explorer file actions use `DocumentFile` rename/delete and `DocumentsContract` copy/move calls, report provider failures, and expose a refresh action. AI provider configuration is shown in `AiProviderSettingsScreen`; send requests run on `Dispatchers.IO` through `AiChatClient`. The user must opt in to attach the active file (limited to 12,000 characters). AI replies are memory-only and replacing an editor buffer requires a confirmation dialog; replacement marks the tab dirty. No prompt, key, or source content is written to the application output log. Release update behavior is described in [17-UPDATE-SYSTEM.md](17-UPDATE-SYSTEM.md). The activity checks on launch, passes API work to `Dispatchers.IO`, routes release notes to the dialog and settings, and owns user-initiated download/checksum acknowledgement/install permission flows. It does not itself parse GitHub JSON or stream APK bytes.

The status bar reports the managed JGit branch/change count when a Reverso clone is active, or explains that only Reverso-managed clone directories are supported. It no longer claims Git is always unavailable. The app still does not connect the editor to Rust/JNI or an interactive Termux bridge, and has no debugger, language server, extension runtime, or port forwarding. Git support for managed clones is detailed below.

### `app/src/main/java/dev/reverso/EditorModels.kt`

Defines `IdeActivity`, `BottomTool`, `EditorTab`, `WorkspaceItem`, `ProjectWorkspace`, and `RecentProject`. `languageForFile` maps common extensions to display language names. `compareWorkspaceEntries` compares directory status first and names case-insensitively; `sortWorkspaceItems` applies it to provider entries. `insertAtSelection` replaces the current UTF-16 Compose selection (or inserts four spaces for the toolbar's Tab key) and returns the updated immutable tab. These helpers keep editor state transformations testable outside composables.

### `app/src/main/java/dev/reverso/WorkspaceRepository.kt`

`RecentProjects` persists at most eight SAF tree URI/name pairs in app-private SharedPreferences JSON. A `child_name` optionally identifies a project folder inside the granted tree; launch restoration reopens that child without persisting an ungranted child URI. `listWorkspaceItems` enumerates children through `DocumentFile`, sorts folders first, and traverses only expanded folders up to a bounded depth of five. `validateDocumentName` rejects path separators, reserved dot names and control characters without rewriting valid input. `mimeForDocumentName` derives a small common-extension MIME map. `listWorkspaceDirectories` discovers bounded copy/move targets. `performWorkspaceFileAction` uses provider-backed rename/delete and `DocumentsContract` copy/move operations and reports unsupported provider operations instead of simulating success. Explorer refresh forces a new provider listing. Failed provider listing returns an empty child list rather than crashing rendering. Access is limited to user-granted folder URIs.

### `app/src/main/java/dev/reverso/Versioning.kt` and `ReleaseChecker.kt`

`Versioning.kt` parses and compares semantic versions including prerelease identifiers. `ReleaseChecker.kt` fetches the official repository's Releases API (up to 100 releases) without credentials, filters drafts/invalid tags and optionally prereleases, validates official release and asset URLs, picks an unambiguous compatible APK, and caches metadata/time. It applies timeouts, a 1.5 MB response cap, six-hour cache policy and one-minute request floor; errors preserve the cache. The file also owns update preferences and pure selection, deduplication, digest and fallback helpers. See the update system document for inputs, return models, failure behavior and security constraints.

### `app/src/main/java/dev/reverso/UpdateScheduler.kt`

`UpdateScheduler.configure(context)` creates or cancels one unique WorkManager periodic release check based on the automatic-check preference. The `ReleaseCheckWorker` uses the same API checker and retries when a request fails with no cached release. WorkManager requires network connectivity and uses a 24-hour interval that Android may defer. `UpdateNotification` creates the `Reverso Updates` channel, checks Android 13 permission and notification preferences, deduplicates by release tag, and posts a notification opening the app. It never starts an install.

### `app/src/main/java/dev/reverso/UpdateInstaller.kt`

`UpdateInstaller.download(context, release)` accepts a release containing a selected official APK asset, rejects invalid metadata and files above 200 MiB, bounds the transfer, verifies downloaded size and compares GitHub's SHA-256 digest if supplied. Failures delete the partial cache file. Its result records whether the digest was verified. `installIntent` exposes the cached APK through FileProvider with temporary read permission for Android's package installer; `installPermissionIntent` opens Android settings only if the user must grant install-from-this-source permission. The app cannot silently install packages.

### `app/src/main/java/dev/reverso/ui/UpdateSettingsScreen.kt`

Renders installed/latest version, manual check, last check/error, APK action, Stable/Stable + Beta selection, automatic/notification preferences, release history and a GitHub releases link. Download percentage comes from the installer callback. The screen receives state and callbacks from `MainActivity`; it does not perform networking, preferences or package installation itself. `MainActivity` passes callbacks to the screen in both compact and wide navigation layouts.

### `app/src/main/java/dev/reverso/AiProvider.kt`

Defines provider metadata and validation, per-provider configuration persistence, Android Keystore credential storage, and the HTTP client. OpenAI, OpenRouter, and custom OpenAI-compatible providers use the chat-completions wire shape; Anthropic uses its Messages shape and headers. `AiProviderConfig.validationError` requires an HTTPS endpoint without user info, query or fragment and requires a model ID. `AiConfigRepository` stores endpoint/model per provider, while `SecureCredentialStore` encrypts each key with AES-GCM and a separate non-exportable Android Keystore AES key. There is no plaintext fallback. `AiChatClient.complete` posts only after the user sends, uses timeouts, disables redirects, caps response/history size, and avoids logging body or credentials. It returns provider text or a generic bounded failure. Tests cover endpoint validation and request-context opt-in; actual credentials/provider behavior needs device verification.

### `app/src/main/java/dev/reverso/ui/AiScreens.kt`

`AiProviderSettingsScreen` edits provider-specific endpoint/model and a masked API key. It queries whether that provider already has an encrypted key so a blank key field can retain the saved one. `AiChatScreen` renders in-memory conversation, exposes Configure/New Chat, an opt-in active-file switch, a prompt field and Send action, and an explicit Apply response action. New Chat clears the conversation. The Apply callback is handled by `MainActivity` with a whole-buffer replacement confirmation. The screen never reads or displays the saved key.

### `app/src/main/java/dev/reverso/ui/ReversoTheme.kt`

Contains the original dark color tokens and applies them through Material 3 `darkColorScheme`. Change this central palette to adjust surfaces, text, accent, borders, and warning colors consistently.

### `app/src/main/java/dev/reverso/ui/Brand.kt` and `app/src/main/res/drawable/reverso_mark.xml`

`ReversoMark` renders the app's own R-shaped ribbon mark over a bordered raised surface. `reverso_mark.xml` supplies the accent-blue R stroke and mint return arrow; the same vector is used in the header, activity rail, welcome screen and adaptive launcher icon. `ReversoTheme.kt` applies the dark navy palette and explicitly sets Material text roles to SansSerif; code text remains intentionally monospace in `EditorSurface`.

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

`app/src/main/AndroidManifest.xml` declares internet, notification and package installation request permissions plus a non-exported update APK FileProvider. `app/src/main/res/xml/update_paths.xml` scopes the provider to update cache files; `drawable/ic_update_notification.xml` supplies the notification small icon. `app/build.gradle.kts` includes WorkManager and AndroidX Core for scheduling, notifications and FileProvider. `ReleaseUpdateTest.kt` covers stable/beta selection, source-only release, duplicate-notification guard, digest helper and cached offline fallback.

### `app/src/main/java/dev/reverso/GitRepository.kt`

`GitRepository` validates GitHub HTTPS clone URLs and repository directory names, clones into the app-private `filesDir/git-repositories` root, snapshots JGit status/branches/remotes/history, stages and unstages relative paths, commits with explicit author details, creates/checks out local branches, and pulls/pushes the configured remote. It rejects paths escaping the managed repository and deletes incomplete clone directories on failure. `GitCredentialStore` encrypts a user-supplied fine-grained token using AES-GCM and a non-exportable Android Keystore key; no credential is placed in the remote URL. JGit requires an ordinary filesystem, so this service does not treat SAF `content://` workspaces as local repositories. Errors become bounded user-visible messages. `GitRepositoryTest` covers local repository actions; remote network and device Keystore behavior still need device verification.

### `app/src/main/java/dev/reverso/ui/GitScreens.kt`

`GitPanelScreen` displays repository status, changed paths, branches, commit fields, history, remote URL and token settings, while dispatching actions through `GitScreenActions`. `GitCloneDialog` accepts the GitHub HTTPS URL, folder name and optional masked fine-grained token and tells users that the clone goes to app-private storage. These composables do not perform Git/network work themselves.

`WorkspaceRepository.kt` also supports `file://` streams and file operations for internal JGit clones; SAF documents continue through `ContentResolver`/`DocumentsContract`.
