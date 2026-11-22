# How it works

## Open and edit a file

1. The user taps Open File and Android's `OpenDocument` picker returns a content URI.
2. Reverso attempts to persist the picker permission, reads UTF-8 text from `ContentResolver`, and creates or activates an `EditorTab` keyed by URI.
3. `EditorSurface` displays the tab's `TextFieldValue`; typing updates text and sets its dirty flag.
4. The syntax transformation colors text without changing character offsets. The toolbar inserts punctuation at the current selection.
5. Save writes UTF-8 to the selected URI. An unsaved tab invokes Android's Create Document picker. IO failures appear in the status bar.

## Workspace and responsive navigation

The header sidebar icon toggles a left activity rail and page. On wide layouts it occupies a sidebar column while the editor remains visible and changes width; on phones the sidebar is a full-width destination so the editor retains a usable width. The Explorer lists files in an explicitly granted SAF tree. Directory taps toggle expansion; traversal is limited to expanded folders and depth five. New file/folder actions call `DocumentFile.createFile` / `createDirectory`. Search examines currently open editors.

The bottom panel is initially closed. The header terminal icon toggles it; selecting a tab changes its contents. It animates open/closed and has a touch drag handle to resize its height. Output lists actual Reverso events. Terminal, Problems, Debug Console, and Ports report their current unavailable backend rather than fabricating sessions or data.

## AI provider and chat flow

1. In Settings or AI Chat, the user chooses OpenAI, Anthropic, OpenRouter, or OpenAI-compatible, enters an HTTPS endpoint/model as needed, and supplies a key.
2. The key is encrypted using AES-GCM. Its AES key is created and retained by Android Keystore; ciphertext and a fresh IV are stored in private preferences. Each provider has a separate key alias and ciphertext entry. Reverso never displays the saved secret.
3. The user enters a prompt. The “Include active file” switch is off by default. When enabled, the current file text (up to 12,000 characters) is explicitly attached to that message. Conversations remain in memory only and “New chat” clears them.
4. `AiChatClient` sends a bounded, non-streaming JSON request over HTTPS directly from the device to the selected provider using that provider's authentication and wire format. No AI request is made until the user taps Send.
5. The response is shown in chat. “Apply response to editor” opens a confirmation; confirming replaces the current editor buffer and marks it dirty. The user must save to persist it.

Requests use connection/read timeouts and response-size limits. Provider HTTP failures show status codes without echoing server response bodies, which might contain sensitive data. Reverso does not proxy requests or log prompts, source content, or credentials. Provider billing, privacy and retention policies remain applicable.

## Release update flow

The app checks the official releases API on launch when automatic checks are enabled and WorkManager schedules network-constrained periodic checks. Reverso caches release history and timestamp, applies stable/beta preferences and shows release notes for a newer version. A user can request a bounded APK download; the app checks the GitHub checksum when present and asks the user to continue if the digest is absent, then opens Android's package installer. Android owns final signature verification and confirmation. See [17-UPDATE-SYSTEM.md](17-UPDATE-SYSTEM.md).

The Rust crates implement UTF-8-safe editing separately. Android does not call the Rust engine through JNI yet.


## Git workflow

The Source Control destination can clone a validated HTTPS GitHub repository into Reverso's private app workspace. The new repo is opened and remembered. JGit performs status, stage/unstage, commit, local branch create/switch, history, pull and push. User-entered GitHub fine-grained tokens are encrypted with Android Keystore. SAF workspaces continue to be stored through their document URIs and are not assumed to be JGit-compatible filesystem directories. See [18-GIT-AND-TERMUX.md](18-GIT-AND-TERMUX.md).
