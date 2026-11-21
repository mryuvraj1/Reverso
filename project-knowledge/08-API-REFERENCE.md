# API reference

## AI provider requests

All AI requests are direct Android-to-provider HTTPS calls. They are initiated only by the user pressing Send. Requests are non-streaming and use a 15 second connect timeout, 30 second read timeout, 1 MB maximum response body, and a total chat-content bound of 64,000 characters. Redirects are disabled. No API key is included in a URL. Errors report a provider label and HTTP status, not the provider's response body.

### OpenAI and OpenAI-compatible providers

- Method: `POST`
- Default OpenAI endpoint: `https://api.openai.com/v1/chat/completions`; OpenRouter endpoint: `https://openrouter.ai/api/v1/chat/completions`. The Custom provider accepts a user-entered HTTPS endpoint.
- Authentication: `Authorization: Bearer <key>`; OpenRouter additionally receives `HTTP-Referer` and `X-Title`.
- Request body: `{ "model": string, "stream": false, "messages": [{ "role": "system"|"user"|"assistant", "content": string }] }`. OpenRouter model IDs are entered in the provider's author/model format.
- Response field: `choices[0].message.content`.
- Errors: non-2xx status becomes a generic provider/status message; malformed JSON or missing text becomes an IO error message.
- Docs: [OpenAI Chat Completions](https://platform.openai.com/docs/api-reference/chat), [OpenRouter chat completion](https://openrouter.ai/docs/api/api-reference/chat/send-chat-completion-request).

### Anthropic Messages

- Method: `POST`
- Endpoint: `https://api.anthropic.com/v1/messages`.
- Authentication/version: `x-api-key: <key>` and `anthropic-version: 2023-06-01`.
- Request body: `{ "model": string, "max_tokens": 2048, "system": string, "messages": [{ "role": "user"|"assistant", "content": string }] }`.
- Response field: concatenated `content[]` text blocks where `type` is `text`.
- Errors: same bounded generic HTTP/JSON handling as above.
- Docs: [Anthropic Messages API](https://docs.anthropic.com/en/api/messages), [Anthropic model lifecycle](https://docs.anthropic.com/en/docs/about-claude/model-deprecations).

The provider settings screen stores endpoint and model per provider. The API key remains separately encrypted per provider using Android Keystore. An empty key is accepted only when a key for that same provider is already stored.

## GitHub release lookup

- Method: `GET`
- Endpoint: `https://api.github.com/repos/mryuvraj1/Reverso/releases?per_page=100`
- Authentication: none; the configured public repository is fixed in `ReleaseChecker.kt`.
- Request headers: `Accept: application/vnd.github+json`, `User-Agent: Reverso-Android`, `X-GitHub-Api-Version: 2022-11-28`.
- Read fields: `tag_name`, `name`, `body`, `html_url`, `published_at`, `draft`, `prerelease`, and uploaded asset `name`, `state`, `browser_download_url`, `digest`, `size`.
- Filtering: draft releases and invalid semantic-version tags are skipped. Prereleases are retained in history but excluded from update selection unless beta is enabled. Only release links and APK assets on HTTPS paths for `github.com/mryuvraj1/Reverso` are accepted.
- Cache and rate handling: successful metadata is cached locally for six hours; requests are rate-limited client-side to one per minute. Response is bounded to 1.5 MB, timeouts apply, and HTTP errors (including GitHub rate limits) are shown while cached releases remain available.
- Response: internal `AvailableRelease` list; selected fields are `tag`, `name`, `notes`, `url`, `publishedAt`, `prerelease`, and optional compatible `apk` (`name`, `url`, `digest`, `size`). This is not a public Reverso server API.
- Failures: network/timeouts/HTTP errors return cached releases plus an error string; malformed entries are ignored. No credential is sent.
- APK flow: compatible official APK metadata is downloaded only after user action, size and SHA-256 are checked when available, and Android's package installer is opened only after user action. Missing checksum requires acknowledgement; missing APK is source-only. No silent install is performed.
- References: [GitHub Releases REST API](https://docs.github.com/en/rest/releases/releases), [Android WorkManager persistent tasks](https://developer.android.com/develop/background-work/background-tasks/persistent), [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

## Rust library interfaces

Rust library APIs are crate interfaces, not a stable cross-language contract. `TextBuffer` exposes UTF-8 byte-range edits; `Editor` provides cursor/selection, undo/redo, line/column, search and replace. Fallible calls return `EditError`/`BufferError`. No JNI ABI is implemented.
