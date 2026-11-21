# File index

Inventory generated from the actual repository tree. Build outputs and dependency caches (`build`, `target`, `.gradle`, `node_modules`, `dist`) are excluded. Regenerate after adding/removing files.

| File | Purpose | Main responsibility | Related modules |
|---|---|---|---|
| `.github/ISSUE_TEMPLATE/bug_report.md` | GitHub configuration | CI, issue or pull request workflow | Repository |
| `.github/ISSUE_TEMPLATE/feature_request.md` | GitHub configuration | CI, issue or pull request workflow | Repository |
| `.github/pull_request_template.md` | GitHub configuration | CI, issue or pull request workflow | Repository |
| `.github/workflows/ci.yml` | GitHub configuration | CI, issue or pull request workflow | Repository |
| `.gitignore` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `CHANGELOG.md` | Project guidance | Project entry point, policy or history | Workspace |
| `CODE_OF_CONDUCT.md` | Project guidance | Project entry point, policy or history | Workspace |
| `CONTRIBUTING.md` | Project guidance | Project entry point, policy or history | Workspace |
| `Cargo.lock` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `Cargo.toml` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `LICENSE` | Project guidance | Project entry point, policy or history | Workspace |
| `README.md` | Project guidance | Project entry point, policy or history | Workspace |
| `SECURITY.md` | Project guidance | Project entry point, policy or history | Workspace |
| `app/build.gradle.kts` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `app/src/main/AndroidManifest.xml` | Project file | Project configuration or resource | Workspace |
| `app/src/main/java/dev/reverso/AiProvider.kt` | Android Kotlin source | Provider API client and encrypted credential/config storage | Android AI |
| `app/src/main/java/dev/reverso/EditorModels.kt` | Android Kotlin source | Editor, navigation and workspace models | Android app |
| `app/src/main/java/dev/reverso/MainActivity.kt` | Android Kotlin source | Responsive IDE layout, SAF workflows and feature routing | Android app |
| `app/src/main/java/dev/reverso/ReleaseChecker.kt` | Android Kotlin source | Release notification and semantic version comparison | Android app |
| `app/src/main/java/dev/reverso/UpdateInstaller.kt` | Android Kotlin source | Bounded APK download, digest verification and Android installer handoff | Android updates |
| `app/src/main/java/dev/reverso/UpdateScheduler.kt` | Android Kotlin source | Periodic release checks and permission-aware release notifications | Android updates |
| `app/src/main/java/dev/reverso/Versioning.kt` | Android Kotlin source | Release notification and semantic version comparison | Android app |
| `app/src/main/java/dev/reverso/WorkspaceRepository.kt` | Android Kotlin source | Persisted SAF workspace access, name/MIME validation, and file operations | Android app |
| `app/src/main/java/dev/reverso/ui/AiScreens.kt` | Compose UI component | AI provider settings and chat interface | Android AI/UI |
| `app/src/main/java/dev/reverso/ui/Brand.kt` | Compose UI component | Reverso mark composable | Android branding |
| `app/src/main/java/dev/reverso/ui/EditorSurface.kt` | Compose UI component | Editor tabs, breadcrumbs, text surface and keyboard toolbar | Android editor UI |
| `app/src/main/java/dev/reverso/ui/ReversoTheme.kt` | Compose UI component | Dark palette and Material 3 theme | Android design system |
| `app/src/main/java/dev/reverso/ui/SyntaxHighlight.kt` | Compose UI component | Lightweight syntax color transformation | Android editor UI |
| `app/src/main/java/dev/reverso/ui/UpdateSettingsScreen.kt` | Compose UI component | Update preferences, release status and release history interface | Android updates/UI |
| `app/src/main/res/drawable/ic_update_notification.xml` | Android resource | Small icon for the Reverso Updates notification channel | Android updates |
| `app/src/main/res/drawable/reverso_mark.xml` | Android resource | Brand, app icon or UI theme resource | Android app |
| `app/src/main/res/mipmap-anydpi-v26/ic_reverso.xml` | Android resource | Brand, app icon or UI theme resource | Android app |
| `app/src/main/res/values/colors.xml` | Android resource | Brand, app icon or UI theme resource | Android app |
| `app/src/main/res/values/styles.xml` | Android resource | Brand, app icon or UI theme resource | Android app |
| `app/src/main/res/xml/update_paths.xml` | Android resource | Restricts FileProvider access to the downloaded update cache | Android updates/security |
| `app/src/test/java/dev/reverso/AiProviderConfigTest.kt` | Kotlin unit test | Regression coverage for editor, AI config or versions | Android app tests |
| `app/src/test/java/dev/reverso/EditorModelsTest.kt` | Kotlin unit test | Regression coverage for editor, AI config or versions | Android app tests |
| `app/src/test/java/dev/reverso/ReleaseUpdateTest.kt` | Kotlin unit test | Regression coverage for release selection, cache and digest helpers | Android updates tests |
| `app/src/test/java/dev/reverso/VersioningTest.kt` | Kotlin unit test | Regression coverage for editor, AI config or versions | Android app tests |
| `app/src/test/java/dev/reverso/WorkspaceStorageTest.kt` | Kotlin unit test | Regression coverage for exact filenames and extension-derived types | Android app tests |
| `build.gradle.kts` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `crates/editor-engine/Cargo.toml` | Cargo manifest | Rust crate metadata and dependencies | Rust workspace |
| `crates/editor-engine/src/lib.rs` | Rust source | UTF-8 editing engine implementation | Rust workspace |
| `crates/text-buffer/Cargo.toml` | Cargo manifest | Rust crate metadata and dependencies | Rust workspace |
| `crates/text-buffer/src/lib.rs` | Rust source | UTF-8 editing engine implementation | Rust workspace |
| `gradle.properties` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle wrapper | Reproducible Android build launcher | Android build |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle wrapper | Reproducible Android build launcher | Android build |
| `gradlew` | Gradle wrapper | Reproducible Android build launcher | Android build |
| `gradlew.bat` | Gradle wrapper | Reproducible Android build launcher | Android build |
| `project-knowledge/00-PROJECT-OVERVIEW.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/01-ARCHITECTURE.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/02-REPOSITORY-STRUCTURE.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/03-TECHNOLOGY-STACK.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/04-SETUP-GUIDE.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/05-HOW-IT-WORKS.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/06-CODE-FILE-EXPLANATIONS.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/07-MODULE-DEPENDENCIES.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/08-API-REFERENCE.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/09-DATA-FLOW.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/10-SECURITY-DESIGN.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/11-TESTING-GUIDE.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/12-BUILD-AND-RELEASE.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/13-TROUBLESHOOTING.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/14-ROADMAP.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/15-DECISIONS-AND-TRADEOFFS.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/16-CHANGELOG.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `project-knowledge/17-UPDATE-SYSTEM.md` | Project documentation | Update service architecture, security, flows and verification | Project knowledge/Android updates |
| `project-knowledge/TASK-TRACKER.md` | Project documentation | Engineering implementation and maintenance notes | Project knowledge |
| `settings.gradle.kts` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
