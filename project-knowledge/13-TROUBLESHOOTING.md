# Troubleshooting

- `cargo: not found`: install Rust stable and Cargo.
- Android SDK platform missing: install platform 35 and configure `ANDROID_HOME` or `ANDROID_SDK_ROOT`.
- Gradle dependency resolution failure: confirm access to Google Maven, Maven Central, and Gradle Plugin Portal.
- App shows no files: current workspace is app-private; files created elsewhere will not appear.
- Cursor offsets: Rust APIs use UTF-8 byte offsets. Do not pass Compose offsets until a conversion/bridge is implemented.

- No update notice: the app checks published stable GitHub Releases at startup; drafts, prereleases, tags without a published release, same/older versions, and network failures do not trigger a notice.

## AI provider setup and failures

- Configure a provider in the sidebar Settings page or AI Chat page. Supply its model ID and an HTTPS endpoint. OpenAI/OpenRouter endpoints are prefilled; model IDs are intentionally configurable.
- A key saved for one provider is not reused for another. Save a key under the selected provider. Keys are encrypted with Android Keystore; after app reinstall, data restore, or Keystore invalidation, save the key again.
- HTTP errors show only provider and status code. Check the model identifier, endpoint, account access/quota and provider status. Request bodies are not logged by Reverso.
- AI requests require Internet access and may incur provider charges. Source code is sent only after enabling the “Include active file” switch for that message.
- If AI setup says the key is missing after switching providers, configure that provider's own key. Custom providers must implement the OpenAI-compatible chat-completions request/response shape.
