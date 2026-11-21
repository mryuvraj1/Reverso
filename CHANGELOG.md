# Changelog

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
