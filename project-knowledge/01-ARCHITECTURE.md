# Architecture

## Current boundaries

`crates/text-buffer` owns UTF-8 text storage and checked byte-range edits. `crates/editor-engine` composes the buffer with cursor, selection, history, line/column, and search behavior. `app` is an Android shell; it presently edits a Kotlin `String` in Compose and performs app-private file I/O. The Android screen does not call the Rust engine yet. This separation keeps platform concerns out of the Rust library, but means the two editors are not behaviorally unified until JNI integration is added.

## Intended direction

Android UI -> JNI adapter -> editor-engine -> text-buffer. Workspace and file lifecycle should move behind a document layer, while the UI renders state and maps user actions to engine commands. JNI must validate byte offsets and convert errors to explicit Kotlin results before it is introduced.

## Current constraints

Text offsets in Rust are UTF-8 byte offsets; line columns are 0-based Unicode scalar counts. The Android editor uses Compose text input offsets, whose mapping is not implemented. Avoid wiring these offsets directly without a tested conversion layer.
