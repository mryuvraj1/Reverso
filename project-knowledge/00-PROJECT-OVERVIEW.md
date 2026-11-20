# Reverso project overview

Reverso targets an Android-first, mobile developer environment. This checkout is an early Phase 1 foundation, not the full product. It contains a Rust text editing core and an Android Compose app for local text files. There is no terminal, Git client, language server, cloud service, extension system, or working native bridge yet.

The workspace uses Rust for deterministic editing behavior and Kotlin for Android UI and filesystem access. The Android app opens and saves user-selected documents through Android’s document picker. It checks for a newer published GitHub release at startup and displays an in-app notice.

Start with the [architecture](01-ARCHITECTURE.md), [setup guide](04-SETUP-GUIDE.md), and [roadmap](14-ROADMAP.md).
