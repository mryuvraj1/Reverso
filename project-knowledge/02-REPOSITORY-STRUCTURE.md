# Repository structure

REVERSO is currently one Git repository, not the seven-repository target architecture. It contains an Android app and Rust workspace.

- `app/src/main/java/dev/reverso/MainActivity.kt`: app UI state, responsive sidebar/editor/bottom-panel composition, SAF launchers, and feature routing.
- `app/src/main/java/dev/reverso/AiProvider.kt`: provider-specific HTTPS client, endpoint validation, saved model/endpoint configuration, and Android Keystore AES-GCM credential storage.
- `app/src/main/java/dev/reverso/EditorModels.kt`: tabs, activity/panel types, workspace items and editor transformations.
- `app/src/main/java/dev/reverso/WorkspaceRepository.kt`: SAF workspace enumeration and recent-folder persistence.
- `app/src/main/java/dev/reverso/ui`: theme/brand/editor components and AI settings/chat screens.
- `app/src/main/res`: original Reverso launcher icon and Android resources.
- `app/src/test`: JVM unit tests for editor models, AI provider configuration, and semantic version comparison.
- `crates/text-buffer` and `crates/editor-engine`: platform-independent Rust editing library. Android/JNI does not yet use this library.
- `.github/workflows/ci.yml`: Rust checks and Android test/build/lint plus APK artifact publication.
- `project-knowledge`: implementation notes and the generated file inventory.

## Android boundaries

The Compose UI delegates file reads/writes to Android's Storage Access Framework; it does not scan arbitrary device storage. AI requests are direct, opt-in HTTPS calls from device to a configured provider, with per-provider keys encrypted using Android Keystore. Reverso owns a terminal panel UI but no execution bridge exists. Git operations, debugging, extensions, language-server diagnostics, and port forwarding remain explicitly unavailable.
