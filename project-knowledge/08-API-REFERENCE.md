# API reference

There are no HTTP endpoints or public network APIs in this repository.

Rust library APIs are Rust crate interfaces, not a stable cross-language contract. `TextBuffer::new`, `as_str`, `len_bytes`, `replace`, `insert`, and `delete` expose UTF-8 byte-range editing. `Editor::new`, `text`, cursor/selection setters, insert/delete, undo/redo, `line_column`, `find`, and `replace_all` expose current editing behavior. Fallible calls return `EditError`/`BufferError`; callers should not assume byte offsets equal user-visible character positions. No JNI ABI is implemented.

## GitHub release lookup used by Android

- Method: `GET`
- Endpoint: `https://api.github.com/repos/mryuvraj1/Reverso/releases/latest`
- Authentication: none; the repository is public.
- Request headers: `Accept: application/vnd.github+json`, `User-Agent: Reverso-Android`, `X-GitHub-Api-Version: 2026-03-10`.
- Read fields: `tag_name`, `name`, `body`, and `html_url`. The app accepts a semantic version from the tag or, if the tag is not a version, from the release name.
- Result: `200` with latest published release metadata; non-200 responses and network errors are treated as “no update notice.” The app validates that the release page uses HTTPS on `github.com`.
- User flow: when a newer stable version is found, the app displays a dialog. The user may dismiss it or open the release page. The app does not download or install packages.
