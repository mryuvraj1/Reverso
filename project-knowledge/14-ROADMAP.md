# Roadmap

## Phase 1 — in progress
Rust text buffer/editor primitives exist with unit tests. Android has a Compose document editor with user-selected open/save, tabs, find/replace, and external keyboard shortcuts. The app also checks the latest published GitHub release at startup and shows a notice for newer versions. Remaining: modular document/history/line-index/search crates, JNI bridge and native build integration, dirty-tab close/recovery, robust document metadata, and Android tests.
## Phase 2
Investigate and implement a supported Termux session bridge and Git workflow. No integrated terminal is currently claimed.
## Phase 3
LSP and code intelligence.
## Phase 4
Android application builder and Gradle integration.
## Phase 5
Separate Go cloud workspace with isolation and resource controls.
## Phase 6
Extension SDK and Hub.
## Phase 7
AI and collaboration.

The named multi-repository split, Termux, cloud, Hub, docs site, and community site remain planned and are not present here.
