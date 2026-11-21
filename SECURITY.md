# Security Policy

Report suspected vulnerabilities privately through GitHub's **Report a vulnerability** feature for this repository. Include affected commit/version, reproduction steps, and impact. Do not publish exploit details before a fix is available.

## Current application boundaries

- File access is scoped to Android Storage Access Framework grants; no broad storage permission is requested. The app does not execute project code.
- AI credentials are encrypted with AES-GCM and provider-specific Android Keystore keys. There is no plaintext fallback.
- AI calls are opt-in and sent directly to the user's HTTPS provider endpoint. Custom endpoints receive the configured key, so only trusted URLs should be used. Source context is off by default and explicitly attached per request. Prompts and source content are not written to Reverso logs. Provider billing and data-handling policies apply.
- Integrated Termux execution, cloud command execution, Git mutation, debugger, and untrusted extensions are not implemented.

See [project-knowledge/10-SECURITY-DESIGN.md](project-knowledge/10-SECURITY-DESIGN.md) for implementation details.
