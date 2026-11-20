# Decisions and tradeoffs

- Begin as one repository because the existing `mryuvraj1/Reverso` repository is the only confirmed target; split only when cross-repository contracts and ownership are established.
- Use std-only Rust crates to keep the editing core small and dependency-light.
- Use app-private Android storage to avoid broad storage permissions in this initial editor. Tradeoff: users cannot browse arbitrary projects yet.
- Keep Android and Rust unconnected until byte-offset conversion, JNI error mapping, and native build are testable. The current app is functional for basic files but does not use the Rust engine.
- Use byte offsets in Rust because they safely address UTF-8 strings; convert for UI display at the platform boundary.
