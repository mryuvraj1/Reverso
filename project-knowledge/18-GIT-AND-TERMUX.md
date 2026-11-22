# Git and Termux integration status

## Git implementation

`app/src/main/java/dev/reverso/GitRepository.kt` owns Git operations. It uses Eclipse JGit on an ordinary filesystem directory, validates GitHub HTTPS clone URLs and destination names, and restricts managed repository paths to the app-private `filesDir/git-repositories` directory. `MainActivity.kt` routes clone and source-control actions to this service on `Dispatchers.IO`; `ui/GitScreens.kt` renders status, staged/unstaged files, branch controls, commit fields, history, and push/pull actions. `WorkspaceRepository.kt` supports `file://` streams and file operations for these managed workspaces while retaining ContentResolver I/O for SAF documents.

Supported in the current code: public/private GitHub HTTPS clone; repository detection for managed clones; status/change listing; stage, unstage and stage-all; commit with user-supplied author identity; local branch create/switch; recent history; push current branch to origin; pull the configured tracking branch. Clone progress is shown as an indeterminate operation state. Failure messages are displayed in the source-control UI. The current branch and remote are visible.

The source-control UI describes the limitation: Android SAF providers return document URIs and may not provide a normal `.git` filesystem tree. Therefore clone uses Reverso's app-private directory, not an arbitrary SAF folder. A cloned repository is managed as a recent workspace and its private files persist across process restarts, but Android app-data clearing/uninstall removes it. There is no export/move-to-SAF workflow yet. JGit does not currently implement remote editing, OAuth/device flow, submodule/LFS handling, conflict resolution UI, or upstream setup for newly created branches. Private access uses a user-entered fine-grained GitHub token, encrypted with an Android Keystore AES-GCM key.

### Git API and errors

These are internal Kotlin service operations, not HTTP endpoints. Network traffic is JGit's Git smart HTTP over HTTPS to the validated GitHub origin.

| Operation | Inputs | Result | Failure behavior |
|---|---|---|---|
| `validateCloneUrl` | URL string | `null` when accepted | Human-readable validation message |
| `clone` | Android context, HTTPS URL, destination folder, optional token | app-private repo directory | Rejects invalid/duplicate names, deletes partial clone, throws a user-visible operation error |
| `snapshot` | managed repo directory | branch, changed paths, remotes, branch list, up to 40 commits (empty history for an unborn branch) | Invalid repository or JGit error returned to UI |
| `stage` / `unstage` / `stageAll` | repo directory and optional relative path | updates Git index | validates relative paths; reports Git/JGit errors |
| `commit` | repo directory, message and author name/email | commit object ID | requires staged content and valid fields; JGit errors shown in UI |
| `createBranch` / `checkout` | repo directory and branch name | switches local branch | validates names/existence and reports checkout errors |
| `pull` / `push` | repo directory and optional encrypted token | JGit result | requires tracking config for pull; rejected remote updates are reported |

No GitHub REST API is used for repository operations. Release monitoring is a separate unauthenticated GitHub Releases API client. Git clone and push/pull are not asserted as device-tested until exercised with a real networked repository and credentials.

## Termux integration investigation

Termux documents the `com.termux.RUN_COMMAND` intent to `com.termux.app.RunCommandService`. The calling app must declare and receive the user-granted `com.termux.permission.RUN_COMMAND`; the Termux user must opt in with `allow-external-apps=true`. Package visibility must also be configured for Android 11+. Termux documents command path, arguments, working directory and stdin extras, plus an optional `PendingIntent` callback. Its result arrives after the command completes; a foreground result is a bounded combined terminal transcript (up to about 100 KiB). This interface is not a live PTY with streaming output and continuing interactive stdin. `Termux:API` exposes device APIs, not an interactive shell transport.

Reverso currently does not send RUN_COMMAND intents and does not claim that the bottom Terminal panel executes commands. It remains an output/status panel. An authenticated companion bridge with an explicitly permissioned protocol, session lifecycle, bounded output, and interactive input is still required for the requested terminal experience. Do not expose an unauthenticated exported command service or present one-shot command completion as interactive terminal support.

Official integration references: [Termux RUN_COMMAND Intent](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent) and [Termux app source](https://github.com/termux/termux-app).

## Maintenance and verification

JGit tests in `GitRepositoryTest.kt` create a temporary real repository and exercise status, stage, commit, branch creation/switching, history and URL validation. Android runtime verification is needed for Keystore-backed token save/restore, clone, private authentication, push, pull, conflicts and the UI. Do not paste production credentials into logs or automated test fixtures. Keep JGit's upstream Eclipse Distribution License 1.0 / BSD 3-Clause notices in redistributed builds.
