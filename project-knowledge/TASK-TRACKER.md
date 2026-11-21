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
- [x] Preserve typed filename extensions, create files under the selected SAF workspace/nested folder, persist the parent tree grant, and restore the latest workspace at startup.
- [x] Add SAF provider-backed rename/delete/copy/move actions, file type icons, Save As and refresh; provider/device behavior still needs device verification.
- [x] Implement stable/beta update service, launch and periodic WorkManager checks, update preferences/history/notifications, bounded official APK download with optional GitHub SHA-256 verification, and Android installer handoff. Device end-to-end verification remains pending.
- [ ] Run Android and Rust CI for the update-service revision; local Android SDK is unavailable. Device verification with SAF providers, update notifications, APK download/installation and live provider credentials remains required.
- [ ] Connect Android editor to Rust through JNI and establish offset conversion tests.
- [ ] Add Android instrumentation/device tests for SAF, sidebar/panel gestures, orientation, keyboard, and Keystore.
- [ ] Implement supported authenticated Termux interactive session integration and Git in Phase 2.
