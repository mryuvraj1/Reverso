# Security design

## Storage and local execution

The app requests Internet access and does not request broad storage access. User files are accessed through Android's document picker and persisted SAF grants. App backup is disabled. The app does not execute file contents. The Rust crates forbid unsafe code and use no external crates.

## AI credentials and requests

API keys are encrypted with AES-GCM using a non-exportable AES key in Android Keystore. Each supported provider has a separate Keystore alias and ciphertext preference key. Provider selection, endpoint and model are stored separately from keys. Clearing a provider removes that provider's key and configuration. No plaintext fallback exists: if Keystore encryption/decryption fails, AI requests fail and the user must save the credential again.

AI endpoints must be HTTPS URLs without user information, query parameters or fragments. Redirects are disabled to prevent forwarding an authorization header to a redirect destination. OpenAI/OpenRouter use a Bearer header; Anthropic uses `x-api-key` and its version header. Custom OpenAI-compatible hosts are user-selected and receive the key by design, so users should only enter endpoints they trust. Requests go directly from the Android app to the provider. AI use is opt-in per Send action; code context is not attached unless the user enables the per-message switch. Attached code is capped at 12,000 characters. Conversations are in-memory only. Responses are capped at 1 MB, and prompts/history have explicit character bounds. Secrets and prompt bodies are not added to Reverso's output log.

Provider billing, data handling and retention are governed by the provider account. The app cannot guarantee provider-side handling.

## Release checking

The app makes unauthenticated HTTPS requests only to the fixed public GitHub Releases API and validates release/asset origins. APK downloads are bounded and checked against GitHub SHA-256 when available; absent digest requires user acknowledgement. APKs stay in app cache behind a non-exported FileProvider. Android controls unknown-source permission, package signature validation and final installation confirmation. Details: [17-UPDATE-SYSTEM.md](17-UPDATE-SYSTEM.md).

## Unavailable backends

There is no Termux command bridge, cloud execution, Git implementation, debugger, or extension loader. Future execution/extension work must establish authentication, isolation and permissions before exposing commands or untrusted code.
