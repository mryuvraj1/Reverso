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
- Endpoint: `https://api.github.com/repos/mryuvraj1/Reverso/releases/latest`
- Authentication: none; the repository is public.
- Request headers: `Accept: application/vnd.github+json`, `User-Agent: Reverso-Android`, `X-GitHub-Api-Version: 2026-03-10`.
- Read fields: `tag_name`, `name`, `body`, and `html_url`. A newer semantic version triggers an in-app notice; the user can open the verified GitHub release page. No APK download/install occurs.

## Rust library interfaces

Rust library APIs are crate interfaces, not a stable cross-language contract. `TextBuffer` exposes UTF-8 byte-range edits; `Editor` provides cursor/selection, undo/redo, line/column, search and replace. Fallible calls return `EditError`/`BufferError`. No JNI ABI is implemented.
