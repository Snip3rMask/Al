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

## Part 0 — Baseline Freeze [~]

**Goal:** Preserve a known-good rollback point.

- [x] Confirm `main` is clean and synced with origin at `57b5777f`.
- [x] Confirm latest CI run is green.
- [!] Confirm latest release installs on a real device.
- [ ] Record baseline screenshots for Home, Lists, Notifications, Profile, Search, Seasonal, Explore, Calendar, Media Details, and Settings.
- [x] Record baseline APK size.
- [!] Record baseline cold-start time.
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
- Device automation was unavailable from the current Termux session (`adb`, Shizuku, and UI automation wrappers were absent), so actual installation, screenshots, and cold-start timing remain pending user/device validation.

**Exit gate:** Release `21` or later is confirmed stable, rollback point documented, and real-device installation/screenshots/cold-start validation completed.

## Part 1 — Domain Contracts Only

**Goal:** Define playback models and repository interfaces without touching UI.

- [x] Create `msr.atsulab.app.player.domain.model`.
- [~] Port the concepts behind Anifux `AnimeItem`, `EpisodeItem`, `VideoSource`, `SkipInterval`, and `SourceCandidate`.
  - [x] Port `AnimeItem` concept to `PlaybackAnime`.
- [ ] Define `PlaybackAnime`, `PlaybackEpisode`, `PlaybackProgress`.
- [ ] Define `EpisodeRepository`, `VideoSourceRepository`, `SkipTimeRepository`, and `PlaybackProgressRepository`.
- [ ] Define `SourceProvider`.
- [ ] Add model/mapping unit tests where practical.
- [ ] Keep Android UI dependencies out of domain models.

**Exit gate:** Build succeeds, existing UI remains unchanged, no Hilt or ExoPlayer dependency is needed yet.

## Part 2 — Source/API Layer

**Goal:** Port source discovery and video resolution behind clean interfaces.

- [ ] Add shared playback `OkHttpClient` through Koin.
- [ ] Port `DakiApi` to `DakiSourceProvider`.
- [ ] Port `MkissaApi` to `MkissaSourceProvider`.
- [ ] Port `AnifuxApi` to `AnifuxSourceProvider`.
- [ ] Port `AniSkipService` to `SkipTimeRepository`.
- [ ] Implement `DefaultEpisodeRepository` and `DefaultVideoSourceRepository`.
- [ ] Implement source fallback ordering and SUB/DUB preference handling.
- [ ] Port `SourceMappingStore` behavior.
- [ ] Register playback repositories in Koin.
- [ ] Add debug-only diagnostics/logging where useful.

**Exit gate:** A known anime resolves episodes and playable sources; failed sources trigger typed fallbacks; main AtsuLab UI remains unchanged.

## Part 3 — Headless ExoPlayer Engine

**Goal:** Prove playback before adding heavy custom UI.

- [ ] Add `media3-exoplayer`, `media3-exoplayer-hls`, and `media3-ui`.
- [ ] Create `PlaybackEngine`.
- [ ] Support HLS playback, play/pause, seek, speed, and resume position.
- [ ] Add playback error mapping and subtitle attachment.
- [ ] Handle audio focus and foreground/background lifecycle safely.
- [ ] Release the player cleanly.

**Exit gate:** A temporary debug activity plays HLS video; resume/background transitions work without leaking or crashing.

## Part 4 — AtsuLab Entry Point

**Goal:** Connect Media Details to real playback.

- [ ] Extend navigation contract with player navigation.
- [ ] Implement navigation in `DefaultNavigationManager`.
- [ ] Map AniList `Media` to `PlaybackAnime`.
- [ ] Pass media ID, title, type, cover image, and initial episode.
- [ ] Replace the Play placeholder in `MediaFragment` with real navigation.
- [ ] Keep Download as placeholder until the offline phase.
- [ ] Handle guest mode and unavailable episodes gracefully.

**Exit gate:** Details page opens the player, episodes load, a working source plays, and back navigation returns correctly.

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

- [ ] Remove temporary debug screens and dead Anifux-derived code.
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

## Next Action

1. User installs/tests Release `21`, confirms it opens, and captures baseline screenshots if possible.
2. Record cold-start time manually or enable Shizuku/wireless ADB for automation.
3. Complete **Part 0 — Baseline Freeze**.
4. Start **Part 1 — Domain Contracts Only**.
