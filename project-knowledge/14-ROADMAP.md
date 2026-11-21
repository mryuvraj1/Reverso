# Roadmap

## Phase 1 — in progress

Rust text-buffer/editor primitives and the responsive Android IDE shell are implemented. Android supports user-granted SAF file/workspace operations, tabs, search/replace, basic syntax colors, line numbers, keyboard actions, sidebar/bottom panel toggles and resizing. Remaining: connect Android to Rust with JNI and offset conversion, improve dirty-tab close/recovery, and add Android device tests.

## Phase 2

Investigate and implement a supported Termux interactive session bridge and Git workflow. The current terminal panel does not execute commands.

## Phase 3

LSP and code intelligence.

## Phase 4

Android application builder and Gradle integration.

## Phase 5

Separate Go cloud workspace with isolation and resource controls.

## Phase 6

Extension SDK and Hub.

## Phase 7 — foundation started

The Android client has opt-in AI chat for OpenAI-compatible providers and Anthropic, per-provider HTTPS/model configuration, Android Keystore-encrypted API keys, optional source context, and confirmed whole-response apply. Remaining: verify provider flows on-device with user-supplied test credentials, add streaming/cancellation if justified, and decide safe patch/diff workflows and supported provider compatibility. There is no Reverso AI backend.

The named multi-repository split, Termux, cloud, Hub, docs site and community site remain planned and are not present here.
