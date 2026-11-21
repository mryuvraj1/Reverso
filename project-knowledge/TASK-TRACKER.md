# Engineering task tracker

- [x] Inspect local workspace and existing Reverso GitHub repository.
- [x] Add Rust workspace with UTF-8-safe buffer and editor operations.
- [x] Add Android Compose editor with SAF open/save, tabs, and find/replace.
- [x] Add branded responsive IDE shell, recent SAF workspaces, file/folder creation and search.
- [x] Add a toggleable responsive sidebar and resizable animated bottom panel with Terminal, Output, Problems, Debug Console and Ports tabs.
- [x] Add provider-specific AI configuration and chat for OpenAI Chat Completions, Anthropic Messages, OpenRouter and custom OpenAI-compatible HTTPS APIs.
- [x] Encrypt API keys with provider-specific Android Keystore AES-GCM keys; keep keys out of logs and opt file context in per prompt.
- [x] Require confirmation before replacing the active editor buffer with an AI response.
- [x] Add unit tests for provider config/endpoint security and AI context opt-in.
- [ ] Run Android CI for this revision; device verification with real provider credentials remains required.
- [ ] Connect Android editor to Rust through JNI and establish offset conversion tests.
- [ ] Add Android instrumentation/device tests for SAF, sidebar/panel gestures, orientation, keyboard, and Keystore.
- [ ] Implement supported authenticated Termux interactive session integration and Git in Phase 2.
