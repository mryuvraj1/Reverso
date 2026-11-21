# Reverso project overview

Reverso targets an Android-first, mobile developer environment. This checkout is an early Phase 1 foundation, not the full product. It contains a Rust text editing core and a Kotlin/Jetpack Compose IDE shell. The app supports user-granted SAF workspaces, exact-extension file creation and Save As, nested creation, recent workspace restore, provider-backed file operations, basic syntax-colored editing, and a professional Reverso-branded responsive layout with toggleable/resizable panels and opt-in direct AI provider chat. The existing GitHub release check is launch-only; periodic update monitoring, notifications, APK verification/download and installation are not implemented.

The phone UI uses touch-sized activity navigation and a focused content destination; wide layouts show a rail, Explorer/Search sidebar and editor. The Android editor is still Compose-backed and does not call Rust through JNI. The UI exposes terminal, Git, debugger, extensions, and diagnostics areas, but explains that their real backends are not connected. No commands are run and no extension is installed by those panels.

The app checks for a newer published GitHub release on launch and shows an in-app notice with a release-page link. It does not run periodic checks, post Android notifications, download APKs, or install updates. AI supports OpenAI-compatible chat providers and Anthropic Messages. Per-provider API keys are encrypted with Android Keystore; file context is opt-in and chat is held in memory.

Start with the [architecture](01-ARCHITECTURE.md), [setup guide](04-SETUP-GUIDE.md), [how it works](05-HOW-IT-WORKS.md), and [roadmap](14-ROADMAP.md).
