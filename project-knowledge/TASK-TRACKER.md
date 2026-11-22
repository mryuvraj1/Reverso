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
- [x] Run Android and Rust CI for the update-service revision (GitHub Actions run 44947324260); Android SDK is unavailable locally. Device verification with SAF providers, update notifications, APK download/installation and live provider credentials remains required.
- [ ] Connect Android editor to Rust through JNI and establish offset conversion tests.
- [ ] Add Android instrumentation/device tests for SAF, sidebar/panel gestures, orientation, keyboard, and Keystore.
- [x] Add JGit-backed GitHub clone and Git status/staging/commit/branch/history/push/pull for app-private clones; live remote operations, device Keystore validation, OAuth/device flow, remote management and SAF Git support remain incomplete.
- [x] Run CI for JGit foundation: run 44997689939 passed Rust fmt/test/clippy and Android unit tests/build/lint; device and live remote checks remain pending.
- [ ] Implement a supported authenticated interactive Termux session bridge. RUN_COMMAND was investigated but provides one-shot command/result semantics rather than a live terminal; Reverso currently does not invoke it.

- [x] Trace launcher Activity, Compose navigation and actual theme/logo/update screens; make Updates a first-class phone destination, welcome action and Settings link.
- [x] Polish the in-use Reverso logo, enforce sans-serif typography, add release channel/GitHub link controls, preserve dismissals and show APK byte progress.
- [ ] Build, install, and visually compare this revised APK; current `adb devices` is empty and `pm list packages` shows Reverso is not installed in this environment. CI must build the APK; on-device comparison requires a connected device.
