# Repository structure

- `Cargo.toml`: Rust workspace and shared lint policy.
- `crates/text-buffer`: UTF-8-safe text storage/edit primitive.
- `crates/editor-engine`: editor state and editing operations.
- `app`: Android Gradle module and Compose application.
- `.github/workflows/ci.yml`: Rust and Android CI jobs.
- `project-knowledge`: implementation notes and file inventory.

This is a single repository at present. The proposed separate GitHub repositories have not been created; see roadmap.
