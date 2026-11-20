# Data flow

## Rust editing path
Caller supplies text -> `Editor` stores `TextBuffer` -> action validates byte range -> buffer replaces text -> editor records old/new text and updates cursor -> undo/redo can replay that edit. Invalid offsets return an error and do not change the buffer.

## Android local file path
Activity creates `filesDir/workspace` -> Compose lists direct child files -> New creates an empty `.txt` file -> selection reads file into Compose state -> user edits the state -> Save writes state to the selected file. Files remain in app-private storage. Rust is not part of this path yet.
