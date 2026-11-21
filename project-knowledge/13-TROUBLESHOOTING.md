# Troubleshooting

- `cargo: not found`: install Rust stable and Cargo.
- Android SDK platform missing: install platform 35 and configure `ANDROID_HOME` or `ANDROID_SDK_ROOT`.
- Gradle dependency resolution failure: confirm access to Google Maven, Maven Central, and Gradle Plugin Portal.
- App shows no files: Reverso only displays the user-selected SAF workspace. Choose a folder with Open Folder or Create New Project; files elsewhere on device are not scanned.
- Cursor offsets: Rust APIs use UTF-8 byte offsets. Do not pass Compose offsets until a conversion/bridge is implemented.

- No update notice: automatic checks may be disabled; otherwise the app uses the official GitHub Releases API and network availability. Drafts/invalid tags are ignored, prereleases need the beta channel, same/older versions are not updates, and a remind-later choice suppresses that release tag. Cached history remains visible after network errors. WorkManager execution may be delayed by Android.

## AI provider setup and failures

- Configure a provider in the sidebar Settings page or AI Chat page. Supply its model ID and an HTTPS endpoint. OpenAI/OpenRouter endpoints are prefilled; model IDs are intentionally configurable.
- A key saved for one provider is not reused for another. Save a key under the selected provider. Keys are encrypted with Android Keystore; after app reinstall, data restore, or Keystore invalidation, save the key again.
- HTTP errors show only provider and status code. Check the model identifier, endpoint, account access/quota and provider status. Request bodies are not logged by Reverso.
- AI requests require Internet access and may incur provider charges. Source code is sent only after enabling the “Include active file” switch for that message.
- If AI setup says the key is missing after switching providers, configure that provider's own key. Custom providers must implement the OpenAI-compatible chat-completions request/response shape.

## SAF file and workspace issues

- New workspace files are created inside the selected project folder (or the last selected nested folder). If file operations affect a provider outside the current view, use Explorer's refresh control.
- Save As passes the tab's exact name with wildcard MIME. If a third-party document provider still changes the name, check the name returned by that provider in the editor tab.
- Recent workspaces need the persisted SAF tree permission. If Android/provider revokes it, reopen the parent folder and select the project again.
- Copy/move depend on `DocumentsContract` support from the selected provider. An unsupported action reports an error; use Save As or the provider's file manager when needed.
