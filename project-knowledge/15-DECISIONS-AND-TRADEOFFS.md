# Decisions and tradeoffs

- Begin as one repository because the existing `mryuvraj1/Reverso` repository is the only confirmed target; split only when cross-repository contracts and ownership are established.
- Use std-only Rust crates to keep the editing core small and dependency-light.
- Use app-private Android storage to avoid broad storage permissions in this initial editor. Tradeoff: users cannot browse arbitrary projects yet.
- Keep Android and Rust unconnected until byte-offset conversion, JNI error mapping, and native build are testable. The current app is functional for basic files but does not use the Rust engine.
- Use byte offsets in Rust because they safely address UTF-8 strings; convert for UI display at the platform boundary.

## AI stays direct and opt-in

The first client sends a user-triggered request directly to the configured HTTPS provider. This avoids putting provider keys in an unimplemented cloud service. AES-GCM ciphertext is stored in private preferences and encryption keys remain in Android Keystore; each provider has its own key. The active file is not included unless the user switches on context for that message. Responses are not applied until confirmation. This supports basic explain/generate/edit workflows while leaving streaming, diff application, cancellation and provider-specific policy handling for future work.

## Mobile panel behavior

On wide screens the sidebar shares the row with the editor and opening/closing it changes editor width. On phones the sidebar becomes a focused destination instead of forcing an unusably narrow editor. The bottom panel consumes vertical space and can be resized; unavailable terminals/ports/debug panels show explicit backend status rather than fake data.
