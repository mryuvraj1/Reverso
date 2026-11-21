# Code file explanations

## Android application

### `app/src/main/java/dev/reverso/MainActivity.kt`

Defines the launcher `MainActivity` and Compose `ReversoApp`. The app state contains open `EditorTab`s, the active tab, selected IDE activity, optional workspace, recent projects, status/output messages, panel state, and dialogs. The activity invokes Android `OpenDocument`, `OpenDocumentTree`, and `CreateDocument` contracts. `openUri`/`saveTab` use `ContentResolver` streams and report IO failures in status text. Workspace tree reads use `DocumentFile` only for a folder explicitly selected by the user. The responsive shell switches between a persistent rail/sidebar/editor on wide screens and activity destinations on phones.

`Header` exposes home, open, save, find, split, and terminal actions. `ActivityRail` and `MobileActivityBar` select Explorer, Search, Source Control, Run/Debug, Extensions, and Settings. `SidePanel` displays open editors and a workspace tree, including directory expansion and create-file/create-folder controls. `EditorArea` joins tabs, breadcrumbs, the input surface, and keyboard toolbar. `WelcomeScreen` offers local file/folder actions and recent folders. `ActivityPage` explicitly reports unimplemented Git, debugging, and Hub integrations. `BottomPanel` exposes Terminal, Problems, Output, Debug Console, and Git; only Output currently contains real application events.

Editor writes set the dirty flag. Ctrl+S saves an existing URI or opens Save As; Ctrl+F opens find/replace. `StatusBar` derives language, UTF-8, indentation display, and line/column from the active text. SAF grants are persisted when supported. The app intentionally has no broad storage permission. The release popup remains connected to `ReleaseChecker` and opens a verified GitHub release URL; APK installation is not attempted.

No native Rust/JNI, PTY bridge, Git engine, LSP, extension installer, or debugger is connected. The current editor input and syntax treatment are Android Compose implementations. Test changes by running `./gradlew :app:testDebugUnitTest :app:assembleDebug` with Android SDK platform 35.

### `app/src/main/java/dev/reverso/EditorModels.kt`

Defines `IdeActivity`, `BottomTool`, `EditorTab`, `WorkspaceItem`, `ProjectWorkspace`, and `RecentProject`. `languageForFile` maps common extensions to display language names. `compareWorkspaceEntries` compares directory status first and names case-insensitively; `sortWorkspaceItems` applies it to provider entries. `insertAtSelection` replaces the current UTF-16 Compose selection (or inserts four spaces for the toolbar's Tab key) and returns the updated immutable tab. These helpers keep editor state transformations testable outside composables.

### `app/src/main/java/dev/reverso/WorkspaceRepository.kt`

`RecentProjects` persists at most eight SAF tree URI/name pairs in app-private SharedPreferences JSON. `open` rehydrates a previously granted tree. `listWorkspaceItems` enumerates children through `DocumentFile`, sorts folders first, and traverses only expanded folders up to a bounded depth of five. Failed provider listing returns an empty child list rather than crashing rendering. Access is limited to user-granted folder URIs.

### `app/src/main/java/dev/reverso/Versioning.kt` and `ReleaseChecker.kt`

`Versioning.kt` parses and compares semantic versions including prerelease identifiers. `ReleaseChecker` fetches GitHub's latest release with timeouts and a 1 MB body cap, accepts only an HTTPS `github.com` URL, and returns a release only when it is newer than the installed `versionName`. Network/JSON failures are handled by the UI as no notice. It does not download or install APK files.

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
