# Contributing

How to build, verify and change this codebase without breaking it.

- **What the architecture is** — [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- **Why it is that way** — [`docs/DECISIONS.md`](docs/DECISIONS.md)

---

## The verification gate

A change is not done until the verification build passes and no test was lost.
Two gates, both real tasks in the root `build.gradle.kts`:

    ./gradlew verify           # every change, every time. No device needed.
    ./gradlew verifyOnDevice   # before opening a PR. Needs a device/emulator.

- **`verify`** = `:app:assembleDebug` + `:app:testDebugUnitTest` + `:app:test`.
  Both test tasks are listed on purpose: an Android module has only
  `testDebugUnitTest`, a pure-Kotlin module (`:core:model`, `:core:domain`,
  `:core:testing`, …) only `test`. Naming one silently skips the other's tests.
- **`verifyOnDevice`** = `verify` + `:app:connectedDebugAndroidTest`.

CI runs `verify` on every pull request. The local command is deliberately the
same one, so a red check can be reproduced without translating a CI step back
into Gradle tasks.

**`verifyOnDevice` is not optional before a PR.** It is the only thing that
checks the failures which destroy user data — all of them Room behaviour, none
of them reproducible off-device:

| Instrumented test | What only it can catch |
| --- | --- |
| `CatDaoTest` | `@Transaction` actually serializing concurrent toggles |
| `CatDatabaseMigrationTest` | `MIGRATION_2_3` copying every column correctly |
| `CatDatabaseUpgradeTest` | a v1 database opening instead of crashing |

A JVM fake cannot stand in for any of them — `FakeCatDao` runs the transaction
block inline, so it proves the logic and not the atomicity. An instrumented
suite nobody runs is no suite at all.

Start an emulator first (`emulator -avd <name>`, or from Android Studio);
`connectedDebugAndroidTest` fails with no device attached.

Also useful:

| Command | Use |
| --- | --- |
| `./gradlew build` | assemble + all unit tests + lint (adds lint over `verify`) |
| `./gradlew lint` | Android lint |
| `./gradlew projects` | list every Gradle module |
| `./gradlew :app:dependencies --configuration debugRuntimeClasspath` | inspect the resolved graph |

Build JDK: **17**, via the Gradle toolchain.

---

## Where things live

| Thing | Location |
| --- | --- |
| Business models, repository interfaces, use cases | `app/src/main/java/com/example/catslist/domain/` |
| Repository impls, remote/local sources, mappers, DI modules | `…/data/` |
| Room entity / DAO / database / migrations | `…/data/local/` |
| Committed Room schemas | `app/schemas/` |
| One MVI screen (State/Event/StateHolder/VM/Screen/ErrorHandler) | `…/presentation/<name>/` |
| Shared UI: theme, components, `UiText`, `SnackbarNotifier` | `…/presentation/` |
| `NavDisplay` + back stack | `…/presentation/navigation/` |
| `MainDispatcherRule` and shared test fakes | `app/src/test/java/com/example/catslist/testing/` |
| Device tests (Room behaviour, migrations, upgrades) | `app/src/androidTest/` |
| Every dependency and version | `gradle/libs.versions.toml` |
| `verify` / `verifyOnDevice` | root `build.gradle.kts` |

The Kotlin package mirrors the directory path. The project is single-module by
decision, not by accident — see [ADR-0001](docs/DECISIONS.md#adr-0001) for the
target module graph and the trigger for splitting.

---

## Do not change without saying so

- `gradle/libs.versions.toml` version bumps — call them out explicitly in the PR;
  never bump a version as a side effect of another change.
- `gradle/wrapper/`, `gradlew`, `gradlew.bat`.
- `build-logic/`, once it exists — its convention plugins configure every module.
- `.idea/` and generated `build/` directories.

---

## Conventions

- **New dependency** → add to `gradle/libs.versions.toml`, reference as `libs.…`.
  Never hardcode `"group:name:version"` in a build file.
- **Screen status** is one sealed `UiStatus`, never `isXVisible` booleans.
- **No `var` state on a ViewModel** outside the `StateFlow` — model it in
  `XxxState`.
- **Screen arguments** do not come from `SavedStateHandle` (Navigation 3). Use
  Hilt assisted injection — [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) §6.
- **Strings** live in `strings.xml`, grouped by screen. ViewModels and
  StateHolders use `UiText`, never `context.getString`.
- **Previews** render the stateless `XxxContent` with fake data, never
  `hiltViewModel()`. One per screen, two at most.
- **Read-modify-write on a `MutableStateFlow` uses `update { }`**, never
  `state.value = f(state.value)` — see [ADR-0015](docs/DECISIONS.md#adr-0015).
- **Every `onEvent` branch that calls a suspend or fallible operation handles
  errors the same way as its sibling branches.** Don't write one branch carefully
  and leave the next one bare — that asymmetry is what let a real crash ship here
  once already. Every such branch goes through `ViewModel.launchCatching`.
  **Not a bare `runCatching`**: it catches `CancellationException` too, so
  leaving a screen mid-request gets reported to the user as a failure. See
  [ADR-0013](docs/DECISIONS.md#adr-0013).
- **A feature never depends on another feature.**

---

## Commits

- One logical change per commit. Every commit builds and passes tests.
- **One-line message: a bracket tag, then a capitalized imperative summary. No
  body** — rationale belongs in the PR description, and durable reasoning belongs
  in [`docs/DECISIONS.md`](docs/DECISIONS.md).

  | Tag | Use |
  | --- | --- |
  | `[TECH]` | **engineering-only work** — library/plugin upgrades, new deps, build tooling, the version catalog, the Gradle wrapper, tests, and documentation |
  | `[FEATURE]` | new or changed user-facing behavior |
  | `[FIX]` | bug fixes, and corrections to existing code — refactors, renames, cleanups |
  | `[MERGE]` | merge commits only |

  `[TECH]` is **not** a catch-all for "no user-facing change" — a refactor or
  rename that touches no dependency is `[FIX]`.

  Example: `[TECH] Modernize Gradle/AGP/Kotlin toolchain, target JDK 17`

- Name any forced dependency bump in the PR description.
- No attribution trailers — no `Co-Authored-By:`, no "Generated with" line.
- Don't stage `.idea/`, `build/`, or anything listed in `.gitignore`.

---

## Branches

Branch off the integration branch (`dev`) — never commit straight to it:

    git switch -c <prefix>/<short-kebab-name>

| Prefix | For | Commits |
| --- | --- | --- |
| `tech/` | dependencies, build tooling, tests, documentation | `[TECH]` |
| `feature/` | new or changed behavior | `[FEATURE]` |
| `bugfix/` / `fix/` | bug fixes, refactors, renames, cleanups | `[FIX]` |
| `merge/` | long-running integration branches | `[MERGE]` |

One branch → one PR into `dev`.

**A branch's commits all match its prefix's tag.** If work of a different kind
turns up mid-branch — a real bug found while testing a `tech/` branch, say — it
does not go on the current branch. Branch off `dev` (or off the current tip if
it depends on uncommitted work, then rebase once that merges) and open a
separate PR.

**Review the branch diff before opening the PR.** The verification build catches
compile errors and failing tests; it does not catch a misplaced side effect (UI
code in the data layer), an unhandled failure path, a main-thread blocking call,
or an `onEvent` branch guarded differently from its siblings. Every one of those
has shipped here at least once.
