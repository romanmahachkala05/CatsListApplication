# Decision record

[`ARCHITECTURE.md`](ARCHITECTURE.md) says *what* this codebase does. This file says *why*, and what
was given up for it.

Each entry follows the ADR shape — **Context**, **Decision**, **Consequences**,
and the alternatives that were rejected where the rejection is the interesting
part. Entries are append-only: a decision that turns out to be wrong is not
edited, it is **superseded** by a later one, so the reasoning stays legible in
both directions.

**What belongs here.** If another engineer could reasonably make a different
choice without knowing why this one was made, it is an ADR. If the answer is
obvious from the code, it is a comment. "Cancellation semantics", "fake vs mock"
and "which Room fallback" qualify; "used a `HashSet` for O(1) lookup" does not.

> Entries 1–8 were reconstructed from commit history and the architecture spec
> rather than written at the time; the reasoning is genuine, the format is
> retrofitted. From ADR-0009 onward each was recorded as the decision was made.

Several entries record defects that were introduced *by this modernization* —
not inherited from the 2022 app. ADR-0013, ADR-0014 and ADR-0015 are all bugs
written during the rewrite, found afterwards, reproduced, and fixed with tests
that fail without the fix. They are here because finding them was the work.

| # | Decision | Status |
| --- | --- | --- |
| [0001](#adr-0001) | Single-module Clean Architecture | Accepted |
| [0002](#adr-0002) | Kotlin DSL + version catalog, no hardcoded versions | Accepted |
| [0003](#adr-0003) | Compose with an explicit MVI screen contract | Accepted |
| [0004](#adr-0004) | Hilt for dependency injection | Accepted |
| [0005](#adr-0005) | Real Room migrations, not destructive fallback | Accepted |
| [0006](#adr-0006) | Coil instead of Glide | Accepted |
| [0007](#adr-0007) | No UI side effects below the presentation layer | Accepted |
| [0008](#adr-0008) | Fetch the feed in pages, not one request per cat | Accepted |
| [0009](#adr-0009) | Navigation 3 | Accepted |
| [0010](#adr-0010) | Stay on AGP 8.x | Accepted |
| [0011](#adr-0011) | A narrow Snackbar collaborator, not an event bus | Accepted |
| [0012](#adr-0012) | Hand-written fakes, no mocking library | Accepted |
| [0013](#adr-0013) | `launchCatching`, never bare `runCatching` | Accepted |
| [0014](#adr-0014) | Atomic favorite toggle via `@Transaction` | Accepted |
| [0015](#adr-0015) | Dedupe a page on write, not with an in-flight guard | Accepted |
| [0016](#adr-0016) | Feed errors are not retryable | **Superseded** by 0019 |
| [0017](#adr-0017) | Version-scoped destructive fallback | Accepted |
| [0018](#adr-0018) | Two-tier verification | Accepted |
| [0019](#adr-0019) | Make the feed resubscribable instead | Accepted |
| [0020](#adr-0020) | One serialization library | Accepted |
| [0021](#adr-0021) | Derive versionCode from the version name | Accepted |
| [0022](#adr-0022) | Split the app into Gradle modules | Accepted |

---

## ADR-0001

### Single-module Clean Architecture

**Accepted** · 2026-09-11

**Context.** The 2022 codebase had no layering: an activity reached into a Room
database through a `CatStorage` singleton, and models carried Room annotations
all the way into the UI. The app is two screens.

**Decision.** Clean Architecture in one Gradle module — `presentation → domain ←
data`. `domain` depends on nothing Android: no SDK, no Compose, no Room, no
Hilt. `data` implements interfaces that `domain` declares.

**Alternative rejected.** A `:core:*` / `:feature:*` module split. Module
boundaries buy compile-time enforcement of the layering and parallel builds —
neither of which pays for itself at two screens, while the ceremony is
immediate. Package boundaries carry the same design with none of the cost.

**Consequences.** The layering is a convention, not a compiler guarantee:
nothing stops someone importing Room from `presentation` except review.
[`ARCHITECTURE.md`](ARCHITECTURE.md) §2b specifies the target module graph so the split, when it
happens, is a mechanical move rather than a redesign. The trigger is a second
feature team or a build slow enough to notice — not repo size.

**Review when:** a second team needs to own a feature independently, the build
is slow enough that parallel module compilation would pay, or the layering has
been violated often enough that review is clearly not catching it.

---

## ADR-0002

### Kotlin DSL + version catalog, no hardcoded versions

**Accepted** · 2026-09-11

**Context.** Versions were string literals spread across Groovy build files,
some of them duplicated at different values.

**Decision.** Groovy → Kotlin DSL, every version in
`gradle/libs.versions.toml`, referenced as `libs.…`. A hardcoded
`"group:name:version"` in a build file is a review failure.

**Consequences.** One place to see the dependency surface, and a dependency bump
becomes a one-line diff a reviewer can actually check. Kotlin DSL costs slightly
slower configuration in exchange for type safety and IDE completion. The catalog
is also what makes "never bump a version as a silent side effect of another
change" an enforceable rule rather than a wish.

---

## ADR-0003

### Compose with an explicit MVI screen contract

**Accepted** · 2026-09-11

**Context.** Screens were XML plus activities holding mutable fields, with
visibility toggled by hand — the state a screen was actually in existed only as
a combination of flags nobody could enumerate.

**Decision.** Compose, and every screen gets the same six parts: `XxxState`
(immutable, one sealed `UiStatus` — never `isXVisible` booleans), `XxxEvent`,
`XxxStateHolder` (the only thing that mutates state), `XxxErrorHandler`, a thin
`XxxViewModel` that only orchestrates, and a stateless `XxxContent` the previews
render.

**Consequences.** Mutually exclusive loading/content/error states become
unrepresentable, because `UiStatus` is one value rather than independent boolean
flags. It does not make every nonsensical `State` unconstructable — other fields
can still be combined in ways that mean nothing — but it removes the class of
bug that boolean soup is made of. The cost is
boilerplate: six files for a screen that could be one, which only pays off once
a screen has more than two states. It also makes the ViewModel testable without
Compose, which is what most of the test suite relies on.

---

## ADR-0004

### Hilt for dependency injection

**Accepted** · 2026-09-11

**Context.** Collaborators were constructed inline or reached through
singletons, so nothing could be substituted in a test.

**Decision.** Hilt, with scoping stated explicitly: `@Singleton` for the
repository and the database, `@ViewModelScoped` for StateHolders and
ErrorHandlers, qualifiers for anything ambiguous (`@Dispatcher(IO)`).

**Consequences.** Compile-time verification of the graph, which a
service-locator approach does not give. The cost is KSP build time and error
messages that point at generated code. Scoping had to become deliberate: the
ViewModel and its ErrorHandler both depend on `IXxxStateHolder`, and the
`@ViewModelScoped` binding is what makes those two injection points resolve to
the same instance — so both operate on one state object without it being passed
by hand. The scope alone would not do it; the shared dependency is half the
mechanism.

---

## ADR-0005

### Real Room migrations, not destructive fallback

**Accepted** · 2026-09-11

**Context.** The app shipped `fallbackToDestructiveMigration()`. Every schema
change silently deleted every favorite the user had saved, on upgrade, with no
error and no way to know it had happened.

**Decision.** Real `Migration` objects, `exportSchema = true`, schema JSON
committed to the repo, and migration tests against them.

**Consequences.** Schema changes become deliberate work rather than a free
action, which is the point. The committed schema JSON doubles as a review
signal: a *new* `N.json` in a diff is a new version, while a *modified* existing
one is a signal that the schema may have changed without a version bump — which
Room does not treat as a build failure. Other things can touch that file (an
export-configuration change, a Room version that emits the format differently),
so it is a prompt to check rather than proof on its own. See
[`ARCHITECTURE.md`](ARCHITECTURE.md) §11b.

---

## ADR-0006

### Coil instead of Glide

**Accepted** · 2026-09-12

**Context.** Image loading used Glide, via an `AndroidView` bridge once the UI
moved to Compose.

**Decision.** Coil 3, which is Compose-native and Kotlin/coroutines-first.

**Consequences.** `AsyncImage` composes directly, so the interop layer and its
manual lifecycle handling disappear. Glide is the more mature library with the
larger feature surface; none of that surface was in use here.

Not yet done: Coil and Retrofit each build their own default `OkHttpClient`, so
the app runs two. Supplying one shared, configured client to both is a loose end,
not a decision.

---

## ADR-0007

### No UI side effects below the presentation layer

**Accepted** · 2026-09-12

**Context.** `CatImageDownloader` — a `data` class — showed a `Toast` directly.
It was untestable, it assumed a main thread, and it meant the data layer decided
what the user saw.

**Decision.** `data` and `domain` never produce UI. A failure travels back as a
return value or an exception; `presentation` decides what to show. The
downloader became a `suspend` function on an injected dispatcher behind a
domain-owned `ImageDownloader` port.

**Consequences.** The download path is testable off-device with a fake, and it
is main-safe by construction. One more interface and one more Hilt binding to
carry. Recorded as an anti-pattern in [`ARCHITECTURE.md`](ARCHITECTURE.md) §12 so it does not creep
back.

---

## ADR-0008

### Fetch the feed in pages, not one request per cat

**Accepted** · 2026-09-12

**Context.** The feed issued one HTTP request per cat, because the original code
called the random-image endpoint in a loop.

**Decision.** One request per page using the API's `limit` parameter, with a
`PAGE_SIZE` constant in the repository.

**Consequences.** A page of ten cats now takes one HTTP request instead of ten,
so the API's rate limit stops being a practical ceiling. The endpoint can return the same cat more
than once, so the repository has to deduplicate — see ADR-0015, where getting
that deduplication subtly wrong turned out to be a crash.

---

## ADR-0009

### Navigation 3

**Accepted** · 2026-09-13

**Context.** The two screens were a `TabRow` inside one composable, so there was
no back stack, no per-destination state scoping, and no route to a third screen
that is pushed rather than switched.

**Decision.** Navigation 3 (`NavDisplay` + `rememberNavBackStack` + typed
`NavKey`s), with the two destinations as peers replaced on the back stack rather
than pushed.

**Alternative rejected.** Navigation Compose. It is the stable, well-documented
option. Navigation 3's state-driven back stack and typed `NavKey`s fit this
app's navigation model better than a route-based API with `SavedStateHandle`
argument passing, and adopting the older one now would mean migrating later.

**Consequences.** Typed keys instead of string routes, and the back stack is
ordinary state the app owns rather than a framework object it queries.
Navigation 3 is new, so the documentation is thin and community answers mostly
do not exist yet — several of its APIs here were confirmed by reading the
published sources rather than the guides.

`rememberViewModelStoreNavEntryDecorator` is deliberately *not* used: it
requires lifecycle 2.11, which requires compileSdk 37, which requires AGP 9.1 —
see ADR-0010. ViewModels are therefore activity-scoped rather than
destination-scoped, which is acceptable while both destinations are top-level
and permanent.

**Review when:** a destination needs its own ViewModel lifecycle — a detail screen
whose state should die with it — or Navigation 3's API changes in a way that
invalidates the shapes used here.

---

## ADR-0010

### Stay on AGP 8.x

**Accepted** · 2026-09-13

**Context.** Adding Navigation 3 pulled in a lifecycle 2.11 bump, which failed
the build with thirteen AAR metadata errors: lifecycle 2.11 wants compileSdk 37,
which wants AGP 9.1. Navigation 3 itself only requires AGP ≥ 8.9.1.

**Decision.** Revert the lifecycle bump, drop the unused
`lifecycle-viewmodel-navigation3`, and stay on AGP 8.13 / Gradle 8.13.

**Consequences.** Moving to AGP 9 changes the Kotlin/Gradle integration and
would pull in unrelated build-system migration work with no benefit to this
project today. Staying on 8.x keeps that separate from the navigation work. The cost is deferred: destination-scoped ViewModels stay
unavailable until the AGP 9 move happens, and it has to happen eventually.

The useful lesson is in the diagnosis, not the outcome — the build failure
presented as "Navigation 3 is incompatible", and the actual culprit was a
transitive bump made in the same commit. Changing one thing at a time is what
made that separable.

**Review when:** a dependency this project actually needs requires AGP 9, or the
destination-scoped ViewModels deferred in ADR-0009 become necessary. Not before:
being on the newest AGP is not itself a reason.

---

## ADR-0011

### A narrow Snackbar collaborator, not an event bus

**Accepted** · 2026-09-13

**Context.** Each screen owned its own one-shot effect channel, so a Snackbar
raised by a screen died when the user switched destinations — the download
confirmation was the visible symptom, since the download outlives the screen.

**Decision.** One app-level `SnackbarNotifier`, injected as a `@Singleton`,
collected in exactly one place (`CatsNavDisplay`) above the `NavDisplay`.

**Boundary, deliberately.** This is for transient app-level feedback that does
not belong to a screen's own state. Anything that *is* screen-specific — a
dialog, an error the screen has to sit in — stays in that screen's state. Other
app-level surfaces get their own collaborator rather than widening this one. The
failure mode being avoided is a global event bus that everything publishes to
and nothing owns.

**Alternative rejected.** Exposing it through a Hilt `@EntryPoint` so
composables could fetch it. That is a service locator wearing a DI annotation:
the dependency stops being visible in any signature. It is injected into the
activity and passed down from the composition root instead.

**Consequences.** A Snackbar survives a destination switch. It is backed by a
`Channel`, which *distributes* rather than broadcasts — every message goes to
exactly one collector, chosen non-deterministically if more than one collects —
so the single-consumer contract is load-bearing and documented on the interface.
Fanning out to several collectors would need a `SharedFlow` instead.

**Review when:** a second app-level surface appears (a dialog host, a toast) — that
gets its own collaborator, and if three of them accumulate the boundary should be
rethought rather than widened. Also if anything ever needs to collect these
messages in two places, which the `Channel` cannot serve.

---

## ADR-0012

### Hand-written fakes, no mocking library

**Accepted** · 2026-09-14

**Context.** The test suite needed substitutes for the repository, the DAO, the
API service, the downloader and the notifier.

**Decision.** Real in-memory implementations backed by `MutableStateFlow`. No
MockK, no Mockito.

**Reasoning.** The repository has *behaviour*, not just calls: favoriting a cat
has to change what the feed emits, without a re-fetch. A mocked
`coEvery { repo.feed } returns flowOf(...)` returns a static list, so the test
asserts the script the author wrote rather than the behaviour. Reproducing
re-emission from a mock means putting a `MutableStateFlow` inside it — a fake
with extra ceremony. A fake also implements the interface contract directly, so
a change to that interface surfaces as a compilation error in the fake;
interaction-based assertions instead verify configured calls at runtime.

**Consequences.** A fake is production-quality code that can drift from the real
thing, and one did: `FakeCatDao` accepted duplicate inserts where Room's default
`ABORT` rejects them, which hid the bug in ADR-0014. It now extends the real
`CatDao` and inherits the transaction body, so the logic under test is the
shipped one. What a JVM fake still cannot reproduce is Room's atomicity — that
needs a device, which is ADR-0018.

Where MockK would genuinely win is a port with no behaviour, only "was it
called": `FakeImageDownloader` is 18 lines that one `coVerify` would replace.
That was not worth a dependency here; at a larger scale it would be.

**Review when:** the interfaces being faked grow wide enough that maintaining the
fakes costs more than the fidelity buys, or a dependency that cannot be
substituted by hand has to be tested.

---

## ADR-0013

### `launchCatching`, never bare `runCatching`

**Accepted** · 2026-09-14

**Context.** Two `onEvent` branches called suspend repository methods in a bare
`viewModelScope.launch` with no handling at all, while the branch beside them
had a full `try`/`catch`. An uncaught exception in `viewModelScope` reaches the
uncaught-exception handler and can terminate the process. A third branch used
`runCatching { }.onFailure { }`, which catches `CancellationException` too — so
leaving the screen mid-fetch was reported to the user as "couldn't load a cat".

**Decision.** One primitive, `ViewModel.launchCatching(onFailure) { }`, which
rethrows `CancellationException` and routes everything else to the caller's
handler. Every fallible event handler goes through it.

**Consequences.** The rule becomes mechanical rather than a matter of care: a
branch either uses the primitive or is visibly wrong in review. `onFailure` is
what varies — a Snackbar for a one-off action that failed while the screen is
otherwise fine, the screen's `ErrorHandler` for a failure it has to sit in.

The project's own written convention had cited
`runCatching { }.onFailure { }` as the pattern to follow — the rule recommended
the bug. Corrected in the same change; it now points at `launchCatching`
(see [`CONTRIBUTING.md`](../CONTRIBUTING.md)). The general principle is worth more than the helper: **cancellation is
not failure**. Catching `CancellationException` without rethrowing it breaks
cancellation propagation for that coroutine and its children — the coroutine
carries on as though nothing had happened.

Scope of the rule: `runCatching` is not bad in general — it is fine around work
that is not cancellable. What this project bans is a bare `runCatching` around
cancellable coroutine work, where cancellation is silently absorbed. The title
is a project convention, not a universal Kotlin rule.

---

## ADR-0014

### Atomic favorite toggle via `@Transaction`

**Accepted** · 2026-09-14

**Context.** `toggleFavorite` read `isFavorite` and then wrote, as two separate
DAO calls. Each tap launches its own coroutine, so two quick taps could both
read "not a favorite" before either wrote, and both insert — aborting on the
primary key and crashing the app. Reproduced on-device as
`SQLiteConstraintException: UNIQUE constraint failed`.

**Decision.** `CatDao` became an abstract class with a
`@Transaction toggleFavorite`, so the read and the write execute as one database
transaction and another transaction cannot interleave a conflicting write
between them. That isolation
is the property being relied on; how Room schedules concurrent transactions
underneath is an implementation detail.

**Alternative rejected.** `@Insert(onConflict = REPLACE)`. It stops the crash,
which is why it is tempting, and it leaves the actual defect in place: two taps
that should cancel each other out would still end with the cat favorited. It
hides the race rather than removing it.

**Consequences.** The DAO is an abstract class rather than an interface, which
is ordinary Room usage but slightly less familiar. The guarantee is Room's, so
the test for it has to be instrumented — 50 concurrent toggles against an
in-memory database, asserting an even count cancels out. Removing only the
`@Transaction` annotation reproduces the original crash, which is what makes the
test worth having.

---

## ADR-0015

### Dedupe a page on write, not with an in-flight guard

**Accepted** · 2026-09-14

**Context.** `fetchNextBatch` read the set of ids already in the feed *before*
the network request and filtered against it *after*. Two overlapping loads — a
double-tapped Retry, or a retry while an auto-load is in flight — each snapshot
the feed before the other writes, so both append the same cats. Duplicate ids
reach `items(cats, key = { it.id })`, and `LazyColumn` throws on a duplicate
key. Reproduced with two gated concurrent calls: `[1, 2, 1, 2]`.

**Decision.** Do the whole read-filter-write inside `MutableStateFlow.update`,
computing the existing ids from the `current` value the lambda receives:

```kotlin
fetched.update { current ->
    val existingIds = current.mapTo(hashSetOf()) { it.id }
    current + newCats.filterNot { it.id in existingIds }
}
```

**The invariant.** *Network requests may overlap; each state update is applied
atomically against the current `StateFlow` value.* `update` is a compare-and-set
loop, so the ids are read from the same value that gets written. Concurrent
`update` calls are not prevented from running — the loser of the race simply
re-runs its lambda against the new value, which is what makes the result
correct either way.

An earlier version of this fix simply moved the read after the request, leaving
`fetched.value = fetched.value + ...`. That removes the long window around the
network call, but a separate read and write is still not atomic — two callers
can both read, then both write, and the second silently drops the first one's
page. It was safe only for as long as every caller happened to resume on the
same thread, which is not something a repository method can promise. `update`
makes the guarantee structural instead of circumstantial.

**Alternative rejected.** Tracking the in-flight job and ignoring overlapping
loads. It saves the redundant request, but it makes correctness depend on the
guard being present at every future call site. Making the write itself correct
is a stronger guarantee; the guard can still be added later as an efficiency
measure, and if it is ever removed nothing breaks.

**Consequences.** `update`'s lambda re-runs on contention, so it has to stay
free of side effects — which it is, since `newCats` is computed before it.

Two shapes worth naming, because the same pair caused ADR-0014:

- **A read-modify-write that spans a suspension point is not atomic unless the
  synchronization mechanism explicitly covers the whole operation** — a `Mutex`
  held across it, a database transaction, an actor. The suspension is not what
  breaks atomicity; the absence of synchronization across it is. In Kotlin the
  suspension is also invisible at the call site, so the window is easy to miss.
- **A read-modify-write with no suspension point is not automatically atomic
  either.** It depends on the synchronization guarantees of the state being
  modified. Use the primitive that provides them — `update` here, a transaction
  in ADR-0014 — rather than relying on a dispatcher the function does not
  control.

Honest limit on the test: the regression test pins the deduplication, not the
lost update. Reproducing a lost write needs genuine parallelism, and a test that
depends on losing a race is flaky by construction. The compare-and-set is a
structural guarantee rather than a tested one.

---

## ADR-0016

### Feed errors are not retryable

**Superseded** by [ADR-0019](#adr-0019) · 2026-09-14

**Context.** `getCatFeed().onEach(...).launchIn(viewModelScope)` had nothing
catching an exception from the flow, so a throwing Room query killed the
process. Adding `.catch` fixed that, but a `Flow` that has thrown is terminated
and will not emit again.

**Decision.** Split the error handler: `onLoadFailure` (a page failed,
retryable) and `onFeedFailure` (the stream died, not retryable). The error
screen said "Please restart the app".

**Reasoning at the time.** Offering Retry would have been a dead button —
fetching another page updates a feed nothing is collecting any more. A button
that silently does nothing is worse than no button.

**Why it was superseded.** The premise was that a terminated flow is
unrecoverable. It is not — the *subscription* can be restarted, which ADR-0019
does. The reasoning was sound given the design; the design was the thing to
change.

---

## ADR-0017

### Version-scoped destructive fallback

**Accepted** · 2026-09-14

**Context.** v1 of the database used a different table name and column spelling,
and the original app wiped it via `fallbackToDestructiveMigration()`. Replacing
that with `addMigrations(MIGRATION_2_3)` alone (ADR-0005) turned the silent wipe
into `IllegalStateException: A migration from 1 to 3 was required but not found`
— a crash on launch. Reproduced on-device.

**Decision.** `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)`.

**Reasoning.** These are not the same call. The blanket
`fallbackToDestructiveMigration()` permits Room to drop and recreate the
database whenever *any* migration path is missing, rather than failing the
upgrade — including for a migration nobody has written yet. It is permission
granted in advance, for cases not yet known. The version-scoped form names one
obsolete version, and every other missing migration still fails loudly. That
difference is the whole point.

**`dropAllTables = true`, deliberately.** v1's table was `favoriteCats`, which is
not an entity in v3, so Room would not know to drop it: the default would leave
an orphaned table sitting in the file indefinitely. Passing `true` clears
everything and rebuilds from the current schema. It is the more destructive of
the two options, chosen because the data being destroyed is v1 data this decision
has already written off — not because it is the safer default. The argument is
named at the call site rather than passed as a bare `true`, so a reader sees what
is being asked for.

**Consequences.** Database configuration moved into `catDatabaseBuilder()` so
the test exercises the builder the app ships rather than a copy that can drift.
Honest caveat recorded with it: v1 existed only between two commits on the same
day in December 2022, before any release, so it almost certainly never reached a
device. This is a safety net making an implicit decision explicit, not a
response to a known field crash.

Retiring a migration path is now a stated policy decision rather than
maintenance — age alone is not a reason ([`ARCHITECTURE.md`](ARCHITECTURE.md) §11d).

**Review when:** someone decides v2 should also be retired — which is the same
policy decision, made again, and needs the same explicit justification.

---

## ADR-0018

### Two-tier verification

**Accepted** · 2026-09-14

**Context.** The instrumented tests only ran when someone remembered to point
Gradle at a device — and they are the only coverage of the three failures that
destroy user data: the toggle's transaction, `MIGRATION_2_3`, and the v1 upgrade
path.

**Decision.** Two tasks in the root build. `verify` (assemble + every unit test,
no device) for the inner loop; `verifyOnDevice` (`verify` +
`connectedDebugAndroidTest`) before a PR.

**Alternative rejected.** One gate including the instrumented tests. It would
fail every time no emulator happened to be running, which teaches people to skip
it — the exact failure being fixed.

**Consequences.** `verify` names both `testDebugUnitTest` and `test`: an Android
module has only the first, a pure-Kotlin module only the second, so naming one
would silently skip the other's tests the day a second module appears.

CI is the enforcement point; the local command is deliberately identical to the
one CI runs, so a failed gate can be reproduced locally without translating a
YAML step back into Gradle tasks.

`verify` is a required status check on `dev`, so the gate is enforced rather
than remembered. `verifyOnDevice` is not: the instrumented tests still depend on
someone attaching a device, which is the remaining hole and the reason the tests
that guard the data-loss paths are the least-run tests in the project.

**Review when:** the instrumented tests run in CI — via a Gradle Managed Device
or an emulator action — at which point the two tiers may collapse into one and
this decision stops being needed.

---

## ADR-0019

### Make the feed resubscribable instead

**Accepted** · 2026-09-14 · supersedes [ADR-0016](#adr-0016)

**Context.** ADR-0016 left both screens telling the user to restart the app,
which is an admission that the app cannot recover from its own error state.

**Decision.** Each ViewModel holds a trigger `StateFlow` and flat-maps the feed
over it, so a retry resubscribes. One `Retry` event covers both failure kinds:
it resubscribes *and* loads a page, so the screen keeps no record of which
failure it hit.

**The load-bearing detail.** `catch` goes on the **inner** flow, inside
`flatMapLatest`. Downstream of `flatMapLatest` it would terminate the whole
chain *including the trigger*, and Retry would be a dead button — precisely the
bug being replaced. Moving it to the naive position fails exactly one test.

**Consequences.** `retryable` came off `CatsListUiStatus.Error`: once every
error was recoverable, the flag was always `true`, and a boolean that only ever
takes one value is noise. Easy to reintroduce if a genuinely unrecoverable error
appears.

Resubscribing is safe because the source of truth is the repository, not the
subscription: the fetched cats and the favorites both outlive any collector, so
a new subscription re-emits what the old one had. That invariant is what lets a
single event handle both failure kinds, and it is pinned by a test — if the feed
ever became subscription-scoped, that test is what would fail.

---

## ADR-0020

### One serialization library

**Accepted** · 2026-09-14

**Context.** The app shipped two: Gson for Retrofit (`CatDto`, `NetworkModule`)
and kotlinx.serialization for the Navigation 3 keys. Nobody chose that — Gson
came from 2022, kotlinx.serialization arrived with ADR-0009, and they were never
reconciled.

**Decision.** Gson is gone. Retrofit uses
`retrofit2-kotlinx-serialization-converter`, and `CatDto` is `@Serializable`
with `@SerialName` in place of `@SerializedName`.

**Reasoning.** Gson resolves models reflectively, which makes the shrinker
configuration more dependent on keep rules than generated serializers are — and
R8 is next on the list. kotlinx.serialization generates its serializers at
compile time, reducing that reflective surface. Two libraries doing one job is
also two ways to spell the same thing.

**The behavioural difference that matters.** Gson silently ignores a JSON key
the model does not declare; kotlinx.serialization rejects it. Swapping one for
the other therefore changes how the app reacts to an upstream field being added:
from ignoring it to failing every response. `Json { ignoreUnknownKeys = true }`
restores the tolerant behaviour deliberately rather than by default.

Worth being precise, because the first version of that comment was wrong: the
search endpoint currently returns exactly the four fields `CatDto` declares, so
nothing was broken without the setting. It is forward-compatibility for a wire
model this project does not own, not a fix for a present failure.

**Consequences.** One serialization library, no reflective model lookup in the
release build, and `kotlinx-serialization-core` was replaced by `-json`, which
includes it — so the Navigation 3 keys are unaffected. Gson is off the runtime
classpath entirely, confirmed against the resolved dependency graph rather than
assumed.

The unit tests could not have caught a failure here — they use fakes, and the
converter only runs against real JSON. Verified by installing on a device and
confirming the feed loads.

**Review when:** the wire models grow enough that polymorphic or custom
serialization is needed, or a dependency forces a different JSON library back
into the graph.

---

## ADR-0021

### Derive versionCode from the version name

**Accepted** · 2026-09-15

**Context.** The app declared `versionCode = 1` and `versionName = "1.0.2"`, and the
repository carries tags `v1.0.1` and `v1.0.2`. Both of those releases shipped
`versionCode` 1, because nobody remembered to bump a number that no developer ever
looks at. Play rejects an upload whose `versionCode` has not increased, so the
second release could not have shipped, and the 2.0.0 release could not either.

**Decision.** Declare the version once, in parts, and compute both values:

```kotlin
val versionMajor = 2
val versionMinor = 0
val versionPatch = 0

versionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch
versionName = "$versionMajor.$versionMinor.$versionPatch"
```

**Alternatives rejected.** Bumping `versionCode` by hand is the status quo, and it
already failed twice — a rule that depends on remembering is the thing being
removed. Deriving it from the git commit count or a CI build number makes the
number monotonic too, but couples the app's identity to the build environment: the
same commit built locally and on CI would produce different versions, and the value
is not reproducible from the source alone.

**Consequences.** Bumping the name necessarily bumps the code, so the class of bug
is gone rather than fixed once. Minor and patch are limited to 0-99 each, which is
wider than this project will use. The new code is 20000, comfortably above the 1
that history left behind, so nothing is blocked by the old mistake.

**Review when:** the app is published somewhere with its own versioning expectations,
or CI needs a distinct build number per build rather than per version — at which
point the build number belongs beside this scheme, not instead of it.

---

## ADR-0022

### Split the app into Gradle modules

**Accepted** · 2026-09-17

**Context.** The app is one `:app` module of roughly 40 source files across the
presentation/domain/data packages. Three features are next: a second animal type
(dogs, behind a `GET v1/images/search` endpoint on a sibling API that returns the
same `id`/`url`/`width`/`height` shape as the one already in use), a favorites
screen unified across both animal types with filtering and sorting, and a shared
image card (shimmer while loading, a placeholder on download failure instead of
the failure covering the whole screen). All three touch the same seam: a domain
model and a card component that today live inside the cats screen's own package
and would otherwise get copy-pasted into a second one, the way the retry and
download-feedback logic already had to be de-duplicated once in this project's
history. Module boundaries turn "don't reach into the other screen's package" from
a convention into a compile error.

**Decision.** Eight modules:

```
:app                — Hilt app, MainActivity, nav graph, DI wiring only
:core:model         — domain model, plain Kotlin/JVM: no Android, Compose, or
                      Hilt dependency
:core:data          — repository, API services, Room
:core:designsystem  — theme, the shared image card, buttons
:core:ui            — UiText, launchCatching, RetryableFlow, StateOwner,
                      SnackbarNotifier
:core:testing       — MainDispatcherRule, fakes
:feature:feed       — one paginated list screen
:feature:favorites  — the unified favorites screen
```

Each feature module exposes exactly two things — its `NavKey` and one entry
`@Composable` — everything else (ViewModel, StateHolder, contract, screen
internals) stays `internal`, enforced by the compiler rather than by convention.

Two convention plugins in a `build-logic` included build cover the two shapes
that exist: `catslist.android.library` (Compose + Hilt + the standard Android
library defaults, used by everything except `:app` and `:core:model`) and
`catslist.jvm.library` (plain Kotlin, used only by `:core:model`).

The domain model itself is not generalized from `Cat` to a species-agnostic
`Animal` in this change. That rename belongs to the dogs feature, not to moving
existing files into new module boundaries — doing both at once would make a
large, mechanical diff (file moves, package renames) hard to tell apart from a
small, meaningful one (the domain model actually changing shape).

**Alternatives rejected.** An `-api`/`-impl` split per feature module (the
pattern a large multi-team codebase uses to keep incremental builds fast and
enforce that one team can't reach into another's internals) has nothing on the
other side of the boundary to protect yet — no feature module is consumed by
another feature module, only by `:app`. It would double the feature module
count for a guarantee `internal` visibility already gives for free at this
scale. Splitting each feature further into its own `domain`/`data`/`presentation`
modules was also rejected: with one shared domain model and two feature
screens, that multiplies module count without a matching payoff, and repeats
the same mistake a pre-release review already found in this codebase — six
use cases that only forward to a single repository method — a layer added
because it is a known-good pattern, not because something today needs it.

**Consequences.** Eight modules instead of one; every existing file's package
and imports move. `:core:model` is checked dependency-free by construction (the
`jvm-library` convention plugin declares no Android/Compose/Hilt dependencies
for anything using it, so adding one is a build-file change, not a silent
accretion). Gradle can skip recompiling modules whose public surface did not
change, so a `:feature:favorites`-only edit no longer triggers a
`:feature:feed` recompile. The migration itself lands as a sequence of
individually reviewable PRs — convention plugins and empty module scaffolding
first, then one module's worth of code moved at a time — rather than one
sweeping change.

**Review when:** a feature module needs to be consumed by another feature
module rather than only by `:app` — that is the trigger to reconsider the
`-api`/`-impl` split, not size or file count on their own. Or when `:core:data`
grows enough (e.g. a download manager needing its own persistence) that
"everything data-related in one module" stops being one responsibility —
split by what it does then, not ahead of time.

