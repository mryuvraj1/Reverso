# How it works

At app startup, `ReleaseChecker` requests the public latest-release metadata for `mryuvraj1/Reverso` over HTTPS on an IO dispatcher. It compares numeric version components with the installed `versionName`; when newer, Compose presents a dialog with release notes and a link to GitHub. Network failures are ignored so the editor remains usable offline. The app does not download or install updates.

On Android, `MainActivity` maintains open tabs in Compose state. New creates a blank in-memory tab. Open invokes Android’s document picker and reads the selected URI; Save writes through the content resolver or invokes the document picker for a new tab. Find/replace edits the active tab text. Ctrl+S and Ctrl+F are handled for external keyboards. The platform picker grants access to user-selected documents; the app does not request broad storage access. The Android editor still does not call Rust.

The Rust `TextBuffer` owns a `String`. Before slicing or replacing, it checks range order, length, and UTF-8 character boundaries. `Editor` tracks a cursor and optional anchor/head selection. Insertion replaces a selection or inserts at the cursor; each edit stores replaced and inserted text for undo/redo. Search reports byte ranges, and line/column lookup counts lines and Unicode scalar values.
