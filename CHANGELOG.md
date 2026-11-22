# Changelog

## Unreleased

- Exposed update controls through the actual phone navigation, welcome screen, and Settings; polished the active Reverso logo and sans-serif dark theme. CI run 44999601869 passed Android tests/build/lint and Rust checks; the debug APK artifact is available from the workflow run. Device installation and visual comparison remain unverified.

- Add official GitHub Releases update checks, stable/beta settings, WorkManager scheduling, cached release history, in-app notices and deduplicated Android notifications.
- Add bounded universal/device-ABI-compatible APK downloads, GitHub SHA-256 verification when supplied, and explicit user-confirmed Android installer handoff; report releases without APK assets as source-only.
- Preserve exact source filenames and add SAF-backed project storage/file operations (see current implementation notes in `project-knowledge/`).

## Unreleased

- Fixed extension handling for SAF Save As, preserving typed names with wildcard MIME and selecting file MIME from the actual suffix.
- Preserved SAF parent-tree grants for created projects, restored the latest workspace at launch, and routed new items to the selected nested folder.
- Added Explorer refresh, file type icons, exact-name validation, and provider-backed rename/delete/copy/move actions with explicit unsupported-provider errors.
- Added keyboard Save As, `adjustResize`, and status-bar truncation to reduce narrow-screen overlap.
- Added a toggleable animated sidebar that leaves the editor visible and resizes it on wide screens; phones use a focused full-width sidebar destination.
- Added an animated open/close, touch-resizable bottom panel with Terminal, Output, Problems, Debug Console and Ports destinations.
- Added real opt-in AI chat for OpenAI Chat Completions, Anthropic Messages, OpenRouter and custom OpenAI-compatible HTTPS endpoints.
- Added provider-specific endpoint/model preferences and AES-GCM encrypted API keys backed by Android Keystore; code context is opt-in and capped.
- Added in-memory New Chat and an explicit confirmation before an AI response replaces the active file buffer.
- Added AI endpoint/config/context tests and updated engineering/security documentation.

## 0.1.0 — 2026-11-22

- Added JGit-based GitHub clone and source control for Reverso-managed app-private repositories.
- Added encrypted fine-grained GitHub token storage and local repository operation tests.
- Documented the SAF Git limitation and that the terminal panel does not yet provide interactive Termux execution.

- Fixed compilation against JGit's public remote URI accessor.

- Handle repositories with no initial commit in Git status/history.

- CI run 44997689939 passed Rust checks, Android tests/build/lint, and uploaded a debug APK.


## UI and update discoverability

- Added direct Updates navigation from the phone activity rail, welcome screen, and Settings shortcut.
- Clarified Stable/Beta release channel selection, added a GitHub releases link, persisted popup dismissal, and displayed APK transfer progress.
- Set explicit sans-serif Material typography and refined the in-use Reverso vector logo for the header and adaptive launcher icon.
- Adjusted typography and release-link icons to the APIs available in the pinned Compose Material version.
