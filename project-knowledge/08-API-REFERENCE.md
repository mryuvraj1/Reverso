# API reference

There are no HTTP endpoints or public network APIs in this repository.

Rust library APIs are Rust crate interfaces, not a stable cross-language contract. `TextBuffer::new`, `as_str`, `len_bytes`, `replace`, `insert`, and `delete` expose UTF-8 byte-range editing. `Editor::new`, `text`, cursor/selection setters, insert/delete, undo/redo, `line_column`, `find`, and `replace_all` expose current editing behavior. Fallible calls return `EditError`/`BufferError`; callers should not assume byte offsets equal user-visible character positions. No JNI ABI is implemented.
