# AtsuLab × Anifux Player Migration Plan

## Purpose

This is the master execution plan for merging Anifux playback features into AtsuLab without breaking the existing AniList tracking app.

Check this document before starting migration work. Update it after every completed task, failed attempt, architecture decision, or scope change.

## Ground Rules

1. Work in small phases. Never merge the whole Anifux player at once.
2. Convert Java to Kotlin gradually, preserving behavior exactly.
3. Do not redesign UI during a conversion commit.
4. Do not mix refactor, feature logic, UI redesign, and dependency upgrades in one commit.
5. AtsuLab conventions win: Kotlin, Rx3, Koin, Coil, ViewBinding.
6. Keep third-party source APIs behind interfaces.
7. Every phase must pass local review, GitHub Actions build, and real-device smoke test.
8. If a phase fails, stop and fix or roll back before starting the next phase.
9. Existing AtsuLab features must remain functional after every merged phase.
10. No secrets or private API keys may be hardcoded.

## Current Baseline

- Repository: `Snip3rMask/Al`
- Branch: `main`
- Latest pre-plan migration commit: `57b5777f`
- Latest successful release: `21`
- App ID: `msr.atsulab.app`
- Debug App ID: `msr.atsulab.app.debug`
- Min SDK: `23`
- Target SDK: `35`
- Existing stack: Kotlin, Apollo3, OkHttp, Retrofit, Rx3, Koin, Coil, ViewBinding
- Anifux source stack: Java, Hilt, LiveData, Media3/ExoPlayer, Glide

### Already Completed Before This Plan

- [x] AtsuLab rebrand and package rename.
- [x] Stable debug signing keystore.
- [x] Direct GitHub Actions release workflow.
- [x] Numbered GitHub releases.
- [x] AniList implicit OAuth login fixed.
- [x] Media details header changed to `Score` / `Favorites` text above title.
- [x] Media details action row added: `Download | Add/List bookmark | Play`.
- [x] Add/List icon shows status badge.
- [x] Added-state outline increased to `2dp`.

### System Work — 2026-08-24

- [x] Added an app-wide local crash reporter during `Application.onCreate`.
  - Captures timestamp, app uptime, current screen, last tap/action, thread, stack trace, memory state, package/version, device, Android API level, security patch, and process start time.
  - Records lifecycle breadcrumbs from `BaseActivity`, `BaseFragment`, and `BaseDialogFragment`, plus quick-tap descriptions from activity touch dispatch.
  - Persists latest report, rolling trace, and crash history to both app-private internal storage and external app-specific storage.
  - Caps trace/history size and keeps the last useful bytes without unbounded file growth.
- [x] Removed the temporary HLS Test launcher, debug provider, debug application override, and player-specific crash recorder after Media3/HLS verification.
  - The production playback engine and its anonymous diagnostics seam remain intact.
  - Verified together with the app-wide reporter in Release 65.

## Migration Principles

### Conversion Policy

Anifux Java will be ported to Kotlin gradually.

For every converted class:

1. Preserve original behavior first.
2. Preserve constants, dimensions, animation timing, callback order, and view hierarchy.
3. Replace Hilt injection with Koin-compatible constructors.
4. Replace LiveData/UiState with Rx-friendly state only where AtsuLab integration requires it.
5. Replace Glide-specific calls with AtsuLab/Coil equivalents when needed.
6. Use only AtsuLab-owned packages under `msr.atsulab.app.player.*`.

No production class should remain under `com.anifux.*`.

### UI Preservation Policy

The goal is pixel-equivalent Anifux player UI, not an immediate redesign.

During porting, preserve:

- View creation order and layout params.
- Padding, margin, width, height, colors, theme attributes.
- Drawables, icons, gesture thresholds, animation durations.
- Portrait/landscape behavior and controls visibility logic.
- Seekbar behavior and lock/unlock flow.

Any intentional visual change must be a separate UI-polish commit after the corresponding behavior works.

## Target Architecture

```text
msr.atsulab.app.player.domain
├─ model: PlaybackAnime, PlaybackEpisode, VideoSource,
│          SkipInterval, SourceCandidate, PlaybackProgress
├─ repository: EpisodeRepository, VideoSourceRepository,
│               SkipTimeRepository, PlaybackProgressRepository
└─ provider: SourceProvider, DakiSourceProvider,
              MkissaSourceProvider, AnifuxSourceProvider

msr.atsulab.app.player.engine
├─ PlaybackEngine
├─ PlaybackError
├─ QualityController
└─ SubtitleController

msr.atsulab.app.player.ui
├─ PlayerActivity
├─ PlayerVideoView
├─ PlayerControllerView
├─ EpisodePanelController
├─ ServerSelectorController
├─ QualitySelectorController
├─ SubtitleController
├─ GestureController
└─ SkipController

msr.atsulab.app.player.storage
├─ PlaybackProgressStore
├─ SourceMappingStore
└─ DownloadQueueStore
```

AtsuLab integration seams:

```text
MediaFragment / MediaViewModel
└─ Play button → player navigation → PlayerActivity

HomeFragment
└─ Continue Watching row → PlayerActivity

MediaList / Editor
└─ Watching status sync → playback progress hooks
```

## Execution Phases

Status meanings: `[ ]` not started, `[~]` in progress, `[x]` complete, `[!]` blocked/needs decision.

## Part 0 — Baseline Freeze [x]

**Goal:** Preserve a known-good rollback point.

- [x] Confirm `main` is clean and synced with origin at `57b5777f`.
- [x] Confirm latest CI run is green.
- [x] Confirm latest release installs on a real device.
- [x] Record baseline screenshots for Home, Lists, Notifications, Profile, Search, Seasonal, Explore, Calendar, Media Details, and Settings.
- [x] Record baseline APK size.
- [x] Record baseline cold-start time.
- [x] Confirm stable debug signing prerequisites for install-over-update.

### Part 0 Findings — 2026-08-23

- Local `main` HEAD and remote `origin/main`: `57b5777ff91a0581f1a87c57d91dc10e84f96c30`.
- Latest successful CI run for this commit: `32616250281`.
- Release `21` asset: `app-debug.apk`.
- Asset size: `14,321,280` bytes (`13.66 MiB` / `14.32 MB`).
- SHA-256: `4579c5a00dba8a3b3d95bb07cc9d0deecf1b8009f13c6205b0997aced497e077`.
- Package: `msr.atsulab.app.debug`, versionCode `200008`, versionName `2.1.3-debug`.
- SDK range: min `23`, target `35`.
- ABIs: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`.
- APK verifies with v1 and v2 signature schemes.
- Signing certificate SHA-256: `2e7ce6d8fe5e8fed6b3a0b7f907ef899153e5f4666feec4124a034ccb75667dc`.
- Release `20` and Release `21` have identical signing certificates, so signature-based replacement/update is allowed.
- At the initial audit, device automation was unavailable from the current Termux session (`adb`, Shizuku, and UI automation wrappers were absent), so installation, screenshots, and cold-start timing were queued for manual device validation.

### Part 0 Closure — 2026-08-24

- The user completed real-device validation and confirmed the baseline phase.
- Baseline screenshots were captured on-device for Home, Lists, Notifications, Profile, Search, Seasonal, Explore, Calendar, Media Details, and Settings; they are local validation artifacts and are intentionally not committed.
- Cold-start behavior was reviewed on-device as part of the same baseline pass.
- HLS playback was additionally confirmed on Symphony Z35 running Android 11 through the staged Media3/HLS diagnostic flow.

**Exit gate:** Release `21` or later is confirmed stable, rollback point documented, and real-device installation/screenshots/cold-start validation completed.

## Part 1 — Domain Contracts Only [x]

**Goal:** Define playback models and repository interfaces without touching UI.

- [x] Create `msr.atsulab.app.player.domain.model`.
- [x] Port the concepts behind Anifux `AnimeItem`, `EpisodeItem`, `VideoSource`, `SkipInterval`, and `SourceCandidate`.
  - [x] Port `AnimeItem` concept to `PlaybackAnime`.
  - [x] Port `EpisodeItem` concept to `PlaybackEpisode`.
  - [x] Port `VideoSource` concept to `VideoSource`.
  - [x] Port `SkipInterval` concept to `SkipInterval`.
  - [x] Port `SourceCandidate` concept to `SourceCandidate`.
- [x] Define `PlaybackAnime`, `PlaybackEpisode`, `PlaybackProgress`.
- [x] Define `EpisodeRepository`, `VideoSourceRepository`, `SkipTimeRepository`, and `PlaybackProgressRepository`.
- [x] Define `SourceProvider`.
- [x] Add model/mapping unit tests where practical.
- [x] Keep Android UI dependencies out of domain models.

### Part 1 Decisions

- AniList identifiers use `Int?`; provider-specific slugs remain strings.
- The old `VideoSource.source` compatibility field is named `legacySourceId`.
- Embedded intro/outro sentinel values are represented by a shared `SkipInterval` list instead of four `-1` fields.
- Playback progress includes enough anime/episode/source metadata for Continue Watching without coupling to ExoPlayer or storage JSON.
- Repository contracts use Rx3: one-shot reads return `Single`, progress observation returns `Observable`, and writes return `Completable`.
- CI now executes local unit tests before producing the debug release artifact.

**Exit gate:** Build succeeds, existing UI remains unchanged, no Hilt or ExoPlayer dependency is needed yet.

## Part 2 — Source/API Layer [x]

**Goal:** Port source discovery and video resolution behind clean interfaces.

- [x] Add shared playback `OkHttpClient` through Koin.
- [x] Port `DakiApi` to `DakiSourceProvider`.
- [x] Port `MkissaApi` to `MkissaSourceProvider`.
- [x] Port `AnifuxApi` to `AnifuxSourceProvider`.
- [x] Port `AniSkipService` to `SkipTimeRepository`.
- [x] Implement `DefaultEpisodeRepository` and `DefaultVideoSourceRepository`.
- [x] Implement source fallback ordering and SUB/DUB preference handling.
- [x] Port `SourceMappingStore` behavior.
- [x] Register playback repositories in Koin.
- [x] Add debug-only diagnostics/logging where useful.

**Exit gate:** A known anime resolves episodes and playable sources; failed sources trigger typed fallbacks; main AtsuLab UI remains unchanged.

### Part 2 Progress

- [x] Added a dedicated, qualified playback `OkHttpClient` under `player.di`.
- [x] Preserved Anifux API client behavior: 20s connect timeout, 35s read timeout, connection retry, and HTTP/HTTPS redirects.
- [x] Kept the client isolated from AtsuLab Apollo/Retrofit clients so player headers cannot leak into tracking APIs.
- [x] Registered it in Koin as `playbackHttpClient`.
- [x] Added factory unit tests for timeout, redirect, retry, and instance independence.
- [x] Added a Koin module test proving the qualified client resolves as a singleton.
- Completed the Daki provider port:
  - Added `DakiSourceProvider` behind the shared Part 1 `SourceProvider` contract.
  - Preserved direct AniList-to-AniDB resolution with ranked search fallback.
  - Preserved Daki HTML search parsing, episode selection, language discovery, HLS extraction, headers, referer, and Cloudflare challenge detection.
  - Mapped provider results into pure AtsuLab playback domain models without exposing Anifux types.
  - Registered the provider in Koin under a dedicated qualifier.
  - Added parser, provider, and Koin-resolution tests, including fallback and HLS source resolution paths.
- Hardened the Daki port during behavior audit:
  - Restored Java `Math.round` semantics for ranked search scores.
  - Rejected valid-position episodes with blank IDs exactly like Anifux source resolution did.
  - Preserved AniList-ID fallback when a title is unavailable.
  - Moved candidate validation into Rx callables so failures remain deferred and observable.
- Completed the Mkissa provider port:
  - Preserved direct AniList-ID episode lookup and the exact GraphQL query shape.
  - Preserved Mkissa browser headers, SUB-first selection, raw episode labels/URLs, show-ID fallback, and numeric sorting.
  - Mapped results to pure AtsuLab playback episode models without exposing Anifux types.
  - Returned no direct playback sources because Anifux used backend source resolution for Mkissa episodes; this lets the later repository fallback chain continue safely.
  - Registered the provider in Koin under a dedicated qualifier and added parser, network, and DI tests.
  - Corrected test-only wire assertions for Kotlin GraphQL escaping and OkHttp's charset-bearing Content-Type.
- Completed the Anifux backend provider port:
  - Preserved grouped source-candidate discovery, provider display order, candidate filtering, and backend request headers.
  - Preserved AniDB episode lookup, Java-compatible fallback numbering, AniList resolution, title-search fallback, and primary source resolution.
  - Preserved source metadata extraction for subtitles, nested captions, referer, display names, intro/outro fields, and skip times.
  - Added an optional `SourceCandidate.backendProvider` key so flat AtsuLab contracts can retain the backend's incompatible provider IDs safely.
  - Registered the provider in Koin and added parser, network, fallback, and DI tests.
- Completed the AniSkip repository port:
  - Preserved AniList-to-MAL title resolution, cleaned-title cache behavior, and the exact GraphQL query shape.
  - Preserved AniSkip v2 request headers, OP/ED type filters, episode length conversion, result validation, and millisecond rounding.
  - Replaced the legacy mutable HashMap with a thread-safe concurrent MAL-ID cache.
  - Extended `SkipTimeRepository` with explicit playback duration so runtime media duration is used instead of being inferred later.
  - Registered `DefaultSkipTimeRepository` through a dedicated playback repository Koin module.
  - Added parser, network/cache, graceful-failure, invalid-input, and Koin-resolution tests.
- Completed the default episode and video-source repositories:
  - Added an ordered Mkissa → Anifux → Daki episode fallback chain that skips failed, candidate-less, and empty providers.
  - Tagged resolved episodes with their originating provider so source resolution returns to the correct provider first.
  - Added generic video-source fallback after the tagged provider while preserving provider identity on every result.
  - Implemented SUB/DUB and server preference ordering without removing alternate choices from the player UI.
  - Normalized episode metadata with anime title, cover image, AniList ID, and provider ID.
  - Registered both repositories in Koin using the explicit playback provider order.
  - Added fake-provider tests for deferred failures, empty chains, compatible AniDB candidates, provider routing, fallback, preference sorting, and DI resolution.
- Completed the source mapping persistence port:
  - Added immutable provider picks, skipped providers, AniList identity, and confirmation timestamp models.
  - Preserved merge-on-save behavior while replacing an existing pick for the same provider key.
  - Stored mappings in app-private SharedPreferences under a dedicated AtsuLab preference file.
  - Sanitized incomplete or malformed JSON and removed unreadable legacy entries on read.
  - Registered the storage contract in Koin and added codec, merge, corruption, blank-input, and round-trip tests.
- Completed Part 2 debug diagnostics and code-level exit review:
  - Added a semantic playback diagnostics contract for provider skips, provider failures, fallback indexes, and invalid mapping cleanup.
  - Logged only anonymous provider/anime/playback identifiers and error details; titles, episode names, URLs, and headers are never logged.
  - Kept debug logging active for debug builds and replaced the implementation with a no-op for release builds.
  - Wired diagnostics through Koin into episode/source repositories and source mapping storage.
  - Added repository tests proving failed and empty providers emit useful fallback diagnostics.
  - Confirmed no `com.anifux.*` package or Anifux source-path dependency leaked into AtsuLab production code.
  - Confirmed this phase changed player data/diagnostic seams only; existing AtsuLab screens and resources remain untouched.

### Part 2 Closure — 2026-08-24

- Completed a live exit-gate check for AniList ID `21` (`One Piece`):
  - Mkissa resolved `1176` SUB episodes.
  - Episode `1` produced `10` sources through the primary backend resolver; `5` were HLS.
- Found and fixed a compatibility regression where Mkissa episodes incorrectly supplied `confirmedSourceSlug = mkissa`.
  - The field is reserved for a confirmed Nora/anineko slug and was intentionally absent in the original Anifux flow.
  - Added parser and wire-path regression assertions so the backend resolver receives the exact original request shape.
- Confirmed failed and empty provider results continue through the typed repository fallback chain with anonymous diagnostics.
- Existing AtsuLab tracking UI remained unchanged during this data-layer phase.

## Part 3 — Headless ExoPlayer Engine [x]

**Goal:** Prove playback before adding heavy custom UI.

- [x] Add `media3-exoplayer`, `media3-exoplayer-hls`, and `media3-ui`.
- [x] Create `PlaybackEngine`.
- [x] Support HLS playback, play/pause, seek, speed, and resume position.
- [x] Add playback error mapping and subtitle attachment.
- [x] Handle audio focus and foreground/background lifecycle safely.
- [x] Release the player cleanly.

**Exit gate:** A temporary debug activity plays HLS video; resume/background transitions work without leaking or crashing.

### Part 3 Progress — 2026-08-23

- Added Media3 ExoPlayer, HLS, and UI dependencies in isolation behind an AtsuLab-owned playback engine contract.
- Implemented HLS preparation with explicit resume position, autoplay, play/pause, seek, speed changes, and surface attachment.
- Merged optional external VTT/SRT subtitles into the primary HLS source while preserving provider referer headers.
- Configured network wake mode, audio-focus handling, and noisy-device pause behavior through Media3.
- Mapped fatal playback errors into network, content, decoding, audio-track, DRM, and unknown categories without leaking URLs into diagnostics.
- Made foreground/background transitions restore prior play intent exactly once and made release idempotent.
- Added a debug-only temporary HLS activity using a public test stream; it was non-exported and did not alter production navigation.
- Used that staged activity to confirm real-device HLS playback, background/resume breadcrumbs, and crash reporting behavior.
- Removed the temporary HLS launcher/provider/recorder after verification while retaining the reusable Media3 engine contract for Part 4 onward.

## Part 4 — AtsuLab Entry Point

**Goal:** Connect Media Details to real playback.

- [x] Extend navigation contract with player navigation.
- [x] Implement navigation in `DefaultNavigationManager`.
- [x] Map AniList `Media` to `PlaybackAnime`.
- [x] Pass media ID, title, type, cover image, and initial episode.
- [x] Replace the Play placeholder in `MediaFragment` with real navigation.
- [x] Keep Download as placeholder until the offline phase.
- [ ] Handle guest mode and unavailable episodes gracefully.

**Exit gate:** Details page opens the player, episodes load, a working source plays, and back navigation returns correctly.

### Part 4 Progress — 2026-08-24

- Added `navigateToPlayer(anime: PlaybackAnime, initialEpisode: Int = 1)` as the player navigation seam.
- Kept the contract independent from AniList response types so a dedicated mapper can translate `Media` into `PlaybackAnime` next.
- Implemented the navigation override in `DefaultNavigationManager`.
- Added a minimal registered `PlayerActivity` destination with primitive intent extras and invalid-input finishing.
- Deferred full player shell UI, playback loading, Play-button wiring, and runtime handling to their own focused slices.
- Added a pure `Media.toPlaybackAnime(appSetting)` mapper for anime only.
  - Maps IDs, preferred title, alternative titles, cover quality preference, banner, episode count/duration, year, release status, adult flag, country, and MAL ID.
  - Rejects manga input before any player navigation can begin.
- Added JUnit coverage for naming/image preferences, normal-quality cover fallback, manga rejection, unknown status, and safe defaults.
- Added a pure playback start-policy helper that allows guests and rejects manga, invalid AniList IDs, blank titles, and not-yet-released anime.
- Replaced the Media Details Play placeholder with navigation to the minimal player destination using episode 1 by default.
- Added a user-facing playback-unavailable fallback for invalid or unsupported entries.

## Part 5 — Continue Watching

**Goal:** Save and restore playback progress reliably.

- [ ] Port `ContinueWatchingStore` behavior to Kotlin storage.
- [ ] Save progress on pause, stop, and background.
- [ ] Save episode duration and source label/ID.
- [ ] Restore last position on reopen.
- [ ] Mark completed episodes appropriately.
- [ ] Allow removing an entry.
- [ ] Reject invalid/negative progress.

**Exit gate:** Partial progress survives app kill/process death and resumes exactly once per episode.

## Part 6 — Player Shell UI

**Goal:** Introduce the Anifux-style player shell without advanced controls.

- [ ] Create AtsuLab-owned `PlayerActivity`.
- [ ] Port basic portrait and landscape/fullscreen layout builders.
- [ ] Port `PlayerVideoView` and `PlayerControllerView` skeletons.
- [ ] Show video surface, loading state, and fatal playback error state.
- [ ] Add back/close, orientation, and fullscreen system-bar behavior.
- [ ] Preserve Anifux dimensions and colors.

**Exit gate:** Portrait/landscape shells match Anifux structure; rotation does not restart playback unexpectedly; system bars and back press behave correctly.

## Part 7 — Advanced Controls

**Goal:** Port controls incrementally. Each group is an independent commit and test cycle.

### 7.1 Basic Transport

- [ ] Play/pause, previous/next, seekbar drag.
- [ ] Elapsed time, total/remaining time, buffered indicator.

### 7.2 Lock Controls

- [ ] Lock button and unlock overlay.
- [ ] Block gestures while locked.

### 7.3 Gestures

- [ ] Horizontal seek, volume, brightness.
- [ ] Double-tap seek and single-tap controls toggle.
- [ ] Gesture HUD feedback.

### 7.4 Speed Control

- [ ] Speed menu.
- [ ] Persist selected speed.
- [ ] Apply speed without resetting stream.

### 7.5 Subtitles

- [ ] Embedded subtitle detection.
- [ ] External subtitle attachment.
- [ ] Language selection, styling, toggle, and missing-subtitle fallback.

### 7.6 Quality Selector

- [ ] Detect HLS quality variants.
- [ ] Provide AUTO and manual options.
- [ ] Preserve position on quality switch.

### 7.7 Server Selector

- [ ] SUB/DUB tabs and server list.
- [ ] Active server indicator and loading-more state.
- [ ] Switch server with minimal progress loss.
- [ ] Clear all-servers-failed state.

### 7.8 Skip Intro/Outro

- [ ] Resolve MAL ID through AniList.
- [ ] Fetch AniSkip intervals.
- [ ] Show/hide skip buttons by interval.
- [ ] Add seekbar marker.
- [ ] Cache skip data per episode.

### 7.9 Frame Capture

- [ ] Capture current frame.
- [ ] Save/share captured frame.
- [ ] Respect settings toggle and API-level differences.

**Exit gate:** Every control works independently, survives rotation, does not conflict with lock/gesture state, and causes no leak after repeated open/close.

## Part 8 — Episode Panel & Navigation

**Goal:** Provide full episode browsing inside the player.

- [ ] Port episode grid and range selector.
- [ ] Highlight current episode and select episodes manually.
- [ ] Load full episode list efficiently.
- [ ] Implement previous, next, and auto-play next behavior.
- [ ] Preserve expected progress behavior on episode switch.

**Exit gate:** Long lists scroll smoothly, ranges work, current episode is obvious, and boundaries/auto-play behave correctly.

## Part 9 — Source Selection Screen

**Goal:** Let users fix wrong automatic source matching.

- [ ] Port source selection skeleton.
- [ ] Show candidates and support manual mapping.
- [ ] Remember mapping by AniList ID.
- [ ] Reset mapping safely.
- [ ] Support single-server mode.
- [ ] Show loading/skeleton, empty, and error/retry states.

**Exit gate:** Wrong auto-match can be corrected manually, mappings persist across restarts, and cancel changes nothing.

## Part 10 — Downloads / Offline

**Goal:** Add reliable offline HLS downloads. Start only after online playback is stable.

- [ ] Port download models and HLS downloader.
- [ ] Create foreground download service.
- [ ] Add queue and downloaded-entry stores.
- [ ] Support queue, pause, resume, cancel, retry, and delete.
- [ ] Show accurate notification progress.
- [ ] Enforce concurrent-download limits.
- [ ] Detect already-downloaded episodes.
- [ ] Enable offline playback.
- [ ] Respect storage settings.

**Exit gate:** Airplane-mode playback works; app kill does not corrupt the queue; notifications and deletion behave correctly.

## Part 11 — AtsuLab Integration Polish

**Goal:** Blend playback naturally into the tracking app.

- [ ] Add Continue Watching section to Home.
- [ ] Add recent search history to Search.
- [ ] Optionally add home row customization.
- [ ] Show resume CTA and episode progress on Media Details.
- [ ] Sync watching progress with AniList editor where sensible.
- [ ] Improve playback loading/error states.
- [ ] Update About page and remove obsolete AL-chan references.
- [ ] Verify deep links still work after player navigation.

**Exit gate:** Player feels integrated; tracking features, Home speed, navigation predictability, and existing flows remain intact.

## Part 12 — Hardening & Release

**Goal:** Prepare the merged app for stable use.

- [x] Remove temporary debug screens and dead Anifux-derived code.
- [ ] Remove unused dependencies.
- [ ] Review ProGuard/R8 rules.
- [ ] Test debug and release builds.
- [ ] Run LeakCanary checks.
- [ ] Test Android 6/min SDK, low-RAM devices, modern Android, background audio/audio focus, PiP if enabled, rotation, process death, and install-over-update.
- [ ] Verify no `com.anifux` package or embedded secret remains.
- [ ] Update user-facing documentation and release notes.

**Exit gate:** Full regression passes, CI is green, release artifact is generated directly in GitHub Releases, rollback path is known.

## Testing Checklist

### Existing AtsuLab Regression

- [ ] Launch, login, guest mode, Home, anime list, manga list, notifications, profile, search, seasonal, explore, calendar, media details, editor, and settings all work.

### Playback Smoke Test

- [ ] Details opens.
- [ ] Episode list loads.
- [ ] Source resolves.
- [ ] Video plays.
- [ ] Pause/resume works.
- [ ] Seek works.
- [ ] Background/foreground works.
- [ ] Rotation works.
- [ ] Back navigation works.
- [ ] Progress saves.

### Failure Cases

- [ ] No internet.
- [ ] Slow internet.
- [ ] Source returns HTTP error.
- [ ] Source returns malformed response.
- [ ] All sources fail.
- [ ] Unsupported stream format.
- [ ] App backgrounded during load/playback/download.
- [ ] Process killed during playback/download.

### Device Matrix

- [ ] Android 6 / min SDK device or emulator.
- [ ] Mid-range Android 10–13 device.
- [ ] Modern Android 14+ device.
- [ ] Low-RAM device/emulator.
- [ ] Portrait phone.
- [ ] Landscape phone.
- [ ] Large screen/tablet if available.

## Git And CI Workflow

Use focused branches such as:

```text
feature/player-part-1-domain-contracts
feature/player-part-2-source-layer
feature/player-part-3-playback-engine
feature/player-part-4-details-entry
feature/player-controls-quality
```

Keep commits isolated. Examples:

```text
feat: add playback domain contracts
feat: add Daki source provider
feat: add headless playback engine
refactor: port player video view to kotlin
fix: preserve seekbar state after rotation
style: match Anifux player spacing
chore: add media3 dependencies
```

Before considering any part complete:

- [ ] Push branch or `main`.
- [ ] GitHub Actions debug build succeeds.
- [ ] Generated APK installs.
- [ ] Smoke test passes.
- [ ] This document is updated.

## Risk Register

| Risk | Impact | Mitigation |
|---|---|---|
| Third-party source APIs break | Playback stops | Provider interface, fallback chain, manual source selection |
| Java-to-Kotlin conversion changes behavior | Runtime bugs | Small conversions, behavior tests, no redesign in same commit |
| ExoPlayer lifecycle leak | Crashes/high memory | Central `PlaybackEngine`, strict release tests |
| Orientation rebuild breaks state | Bad UX | Preserve state in ViewModel/engine, rotation tests |
| Continue watching writes bad data | Wrong resume | Validate time/range, schema versioning |
| Download queue corruption | Offline failures | Persistent queue, recovery, atomic updates |
| UI regression in old AtsuLab screens | Existing users affected | Baseline screenshots and regression checklist |
| Package/name leakage | Maintenance confusion | No `com.anifux` production code, final grep gate |

## Progress Log

Append dated entries here. Do not delete history.

### 2026-08-23

- Created master migration plan.
- Confirmed latest successful release: `21`.
- Confirmed latest pre-plan migration commit: `57b5777f`.
- Set next executable phase: **Part 0 — Baseline Freeze**, followed by **Part 1 — Domain Contracts Only**.
- Completed repository-side Part 0 checks: branch sync, green CI, Release `21` integrity, package metadata, APK size, and signing compatibility.
- Marked real-device installation, screenshots, and cold-start timing as blocked because device automation was unavailable.
- Created `msr.atsulab.app.player.domain.model` package marker as the first Part 1 task.
- Added `PlaybackAnime` as the first real domain model. Provider-specific IDs use `externalIds`; list/tracking and continue-watching fields are deferred to their own domains.
- Replaced the temporary package marker with `PlaybackAnime`.
- Completed Part 1 repository-side work:
  - Added `PlaybackEpisode`, `VideoSource`, `SkipInterval`, `SourceCandidate`, and `PlaybackProgress`.
  - Added episode, video-source, skip-time, playback-progress, and source-provider contracts using Rx3.
  - Enabled JUnit Platform and added pure-domain model tests.
  - Updated GitHub Actions to run unit tests before building and releasing the debug APK.
- Started Part 2 with the shared playback HTTP foundation:
  - Added `playbackHttpClient` as a singleton qualifier in Koin.
  - Used a dedicated factory so provider APIs share one connection core without touching existing AtsuLab network clients.
  - Preserved Anifux timeouts and redirect behavior while keeping logging disabled until debug diagnostics are intentionally introduced.

### 2026-08-24

- Closed Part 0 after user-confirmed real-device installation, baseline screen review, screenshots, and cold-start validation.
- Confirmed staged HLS playback on Symphony Z35 running Android 11.
- Added the app-wide local crash reporter with dual-path report/trace/history persistence, lifecycle breadcrumbs, tap tracking, memory/device metadata, and size caps.
- Removed the temporary HLS Test launcher, debug provider/application override, and player-specific crash recorder after successful Media3/HLS verification.
- Published Release `65` from green CI at commit `0104ef44`.
- Set next executable phase: **Part 4 — AtsuLab Entry Point**.
- Started Part 4 by adding the playback navigation contract without touching the current Play placeholder.
- Implemented manager navigation through a temporary minimal `PlayerActivity` shell.
- Added the AniList-to-playback-domain anime mapper with unit tests.

## Next Action

1. Implement episode loading inside `PlayerActivity` with loading, empty, error, and retry states.
2. Resolve a playable source through `VideoSourceRepository` and attach it safely to Media3.
3. Complete runtime guest/unavailable-episode validation before moving to Continue Watching.
