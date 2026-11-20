# Data flow

## Rust editing path
Caller supplies text -> `Editor` stores `TextBuffer` -> action validates byte range -> buffer replaces text -> editor records old/new text and updates cursor -> undo/redo can replay that edit. Invalid offsets return an error and do not change the buffer.

## Android local file path
Activity creates `filesDir/workspace` -> Compose lists direct child files -> New creates an empty `.txt` file -> selection reads file into Compose state -> user edits the state -> Save writes state to the selected file. Files remain in app-private storage. Rust is not part of this path yet.

## Update notice flow
App starts -> `ReleaseChecker` requests the latest published release metadata over HTTPS on a background dispatcher -> version tag is compared with installed `versionName` -> if newer, Compose shows a dialog -> user can dismiss it or open the HTTPS release page in the browser. The app does not install or download an update.
