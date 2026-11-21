# How it works

## Open and edit a file

1. The user taps Open File and Android's `OpenDocument` picker returns a content URI.
2. Reverso attempts to persist the picker permission, reads UTF-8 text from `ContentResolver`, and creates or activates an `EditorTab` keyed by URI.
3. `EditorSurface` displays the tab's `TextFieldValue`; typing updates text and sets its dirty flag.
4. The syntax transformation colors text without changing character offsets. The toolbar inserts punctuation at the current selection.
5. Save writes UTF-8 to the selected URI. An unsaved tab invokes Android's Create Document picker. IO failures appear in the status bar.

## Open a workspace

1. Open Folder launches Android's document-tree picker.
2. Reverso persists read/write tree permissions where available and wraps the selected URI in `DocumentFile`.
3. It saves the folder's URI and display name in app-private preferences, bounded to eight recent entries.
4. The Explorer lists immediate children. Selecting a directory toggles expansion; enumeration is limited to expanded folders and depth five. Selecting a file reads it into an editor tab.
5. New file/folder actions call `DocumentFile.createFile` / `createDirectory` under the granted root.

## Search and tools

Search examines only currently open editor text. Find/replace changes the active buffer and marks it dirty. Output shows actual app events. Terminal, Git, debugger, diagnostics, and extension panels explain that their backends are not connected, rather than fabricating results.
