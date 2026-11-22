# File index

Inventory generated from the actual repository tree. Build outputs and dependency caches (`build`, `target`, `.gradle`, `node_modules`, `dist`) are excluded.

| File | Purpose | Main responsibility | Related modules |
|---|---|---|---|
| `.github/ISSUE_TEMPLATE/bug_report.md` | GitHub configuration | CI, issue or pull request workflow | Repository |
| `.github/ISSUE_TEMPLATE/feature_request.md` | GitHub configuration | CI, issue or pull request workflow | Repository |
| `.github/pull_request_template.md` | GitHub configuration | CI, issue or pull request workflow | Repository |
| `.github/workflows/ci.yml` | GitHub configuration | CI, issue or pull request workflow | Repository |
| `.gitignore` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `CHANGELOG.md` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `CODE_OF_CONDUCT.md` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `CONTRIBUTING.md` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `Cargo.lock` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `Cargo.toml` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `LICENSE` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `README.md` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `SECURITY.md` | Project guidance/configuration | Project entry point, policy, history, or build manifest | Workspace |
| `app/build.gradle.kts` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `app/src/main/AndroidManifest.xml` | Project file | Project configuration or supporting resource | Workspace |
| `app/src/main/java/dev/reverso/AiProvider.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/EditorModels.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/GitRepository.kt` | Android Kotlin source | JGit repository operations and encrypted GitHub credentials | Android Git |
| `app/src/main/java/dev/reverso/MainActivity.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/ReleaseChecker.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/UpdateInstaller.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/UpdateScheduler.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/Versioning.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/WorkspaceRepository.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/ui/AiScreens.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/ui/Brand.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/ui/EditorSurface.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/ui/GitScreens.kt` | Compose UI component | Git clone and source-control screens | Android Git/UI |
| `app/src/main/java/dev/reverso/ui/ReversoTheme.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/ui/SyntaxHighlight.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/java/dev/reverso/ui/UpdateSettingsScreen.kt` | Android Kotlin source | Application behavior and data model | Android app |
| `app/src/main/res/drawable/ic_update_notification.xml` | Android resource | Application manifest, UI resource, or icon | Android app |
| `app/src/main/res/drawable/reverso_mark.xml` | Android resource | Application manifest, UI resource, or icon | Android app |
| `app/src/main/res/mipmap-anydpi-v26/ic_reverso.xml` | Android resource | Application manifest, UI resource, or icon | Android app |
| `app/src/main/res/values/colors.xml` | Android resource | Application manifest, UI resource, or icon | Android app |
| `app/src/main/res/values/styles.xml` | Android resource | Application manifest, UI resource, or icon | Android app |
| `app/src/main/res/xml/update_paths.xml` | Android resource | Application manifest, UI resource, or icon | Android app |
| `app/src/test/java/dev/reverso/AiProviderConfigTest.kt` | Android unit test | Automated behavior verification | Android app/tests |
| `app/src/test/java/dev/reverso/EditorModelsTest.kt` | Android unit test | Automated behavior verification | Android app/tests |
| `app/src/test/java/dev/reverso/GitRepositoryTest.kt` | Android unit test | Real local Git repository operation coverage | Android Git |
| `app/src/test/java/dev/reverso/ReleaseUpdateTest.kt` | Android unit test | Automated behavior verification | Android app/tests |
| `app/src/test/java/dev/reverso/VersioningTest.kt` | Android unit test | Automated behavior verification | Android app/tests |
| `app/src/test/java/dev/reverso/WorkspaceStorageTest.kt` | Android unit test | Automated behavior verification | Android app/tests |
| `build.gradle.kts` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `crates/editor-engine/Cargo.toml` | Rust source/configuration | Rust editor library implementation or crate setup | Rust core |
| `crates/editor-engine/src/lib.rs` | Rust source/configuration | Rust editor library implementation or crate setup | Rust core |
| `crates/text-buffer/Cargo.toml` | Rust source/configuration | Rust editor library implementation or crate setup | Rust core |
| `crates/text-buffer/src/lib.rs` | Rust source/configuration | Rust editor library implementation or crate setup | Rust core |
| `gradle.properties` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `gradle/wrapper/gradle-wrapper.jar` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `gradle/wrapper/gradle-wrapper.properties` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `gradlew` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `gradlew.bat` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
| `project-knowledge/00-PROJECT-OVERVIEW.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/01-ARCHITECTURE.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/02-REPOSITORY-STRUCTURE.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/03-TECHNOLOGY-STACK.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/04-SETUP-GUIDE.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/05-HOW-IT-WORKS.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/06-CODE-FILE-EXPLANATIONS.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/07-MODULE-DEPENDENCIES.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/08-API-REFERENCE.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/09-DATA-FLOW.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/10-SECURITY-DESIGN.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/11-TESTING-GUIDE.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/12-BUILD-AND-RELEASE.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/13-TROUBLESHOOTING.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/14-ROADMAP.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/15-DECISIONS-AND-TRADEOFFS.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/16-CHANGELOG.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/17-UPDATE-SYSTEM.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/18-GIT-AND-TERMUX.md` | Project documentation | Git operation and Termux integration boundaries | Project knowledge |
| `project-knowledge/FILE-INDEX.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `project-knowledge/TASK-TRACKER.md` | Project documentation | Architecture, setup, workflow, or implementation reference | Project knowledge |
| `settings.gradle.kts` | Build/configuration | Build setup, dependency lock or repository settings | Workspace |
