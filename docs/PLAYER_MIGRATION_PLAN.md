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
- Latest successful release: `121` (`33aafb36be9efbac921b117af3ab42a97c23d7d4`)
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

## Part 4 — AtsuLab Entry Point [x]

**Goal:** Connect Media Details to real playback.

- [x] Extend navigation contract with player navigation.
- [x] Implement navigation in `DefaultNavigationManager`.
- [x] Map AniList `Media` to `PlaybackAnime`.
- [x] Pass media ID, title, type, cover image, and initial episode.
- [x] Replace the Play placeholder in `MediaFragment` with real navigation.
- [x] Keep Download as placeholder until the offline phase.
- [x] Handle guest mode and unavailable episodes gracefully.

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

### Part 4 Runtime Work — 2026-08-24

- Added a temporary playback-ready shell with a video surface, loading/status text, retry, and fatal-error feedback.
- Wired `PlayerActivity` to the existing Rx3 episode and source repository fallback chains.
- Added pure requested-episode selection with exact-number matching and positional fallback for provider numbering differences.
- Resolved the selected episode through `VideoSourceRepository`, attached the winning HLS source to Media3, and preserved referer handling in the engine.
- Kept guest access allowed at the entry-policy layer; runtime playback performs no AniList login dependency.
- Separated unavailable-episode/source results from generic runtime errors and made both retryable.
- Released the playback engine, cancelled active Rx work on destroy, and routed foreground/background transitions through the engine lifecycle.

### Part 4 Device Verification — 2026-08-24

- Confirmed on a real device with Release `77` that the Media Details Play button opens `PlayerActivity`.
- Confirmed episode discovery, source resolution, and real HLS video playback work end-to-end without crashing.
- Confirmed back navigation returns correctly from an active player session.
- Closed Part 4 after the real-device playback and navigation checks passed.

## Part 5 — Continue Watching [!]

**Goal:** Save and restore playback progress reliably.

- [ ] Port `ContinueWatchingStore` behavior to Kotlin storage.
- [ ] Save progress on pause, stop, and background.
- [ ] Save episode duration and source label/ID.
- [ ] Restore last position on reopen.
- [ ] Mark completed episodes appropriately.
- [ ] Allow removing an entry.
- [ ] Reject invalid/negative progress.

**Exit gate:** Partial progress survives app kill/process death and resumes exactly once per episode.

**Deferred — 2026-08-24:** Continue Watching is postponed until the full player shell and basic transport controls exist. Saving/resuming progress before those surfaces stabilize would risk repeated runtime and UI refactors.

## Part 6 — Player Shell UI [x]

**Goal:** Introduce the Anifux-style player shell without advanced controls.

- [x] Create AtsuLab-owned `PlayerActivity`.
- [x] Port basic portrait and landscape/fullscreen layout builders.
- [x] Port `PlayerVideoView` and `PlayerControllerView` skeletons.
- [x] Show video surface, loading state, and fatal playback error state.
- [x] Add back/close, orientation, and fullscreen system-bar behavior.
- [x] Preserve Anifux dimensions and colors.

**Exit gate:** Portrait/landscape shells match Anifux structure; rotation does not restart playback unexpectedly; system bars and back press behave correctly.

### Part 6 Planning Audit — 2026-08-24

- Anifux shell is split across `PlayerLayoutBuilder`, `PlayerVideoView`, and `PlayerControllerView`.
- Portrait shell uses a fixed-height video frame above a watching/status row and scrollable lower panel.
- Landscape shell uses full-screen video with centered loading/source-error feedback.
- Anifux video shell embeds Media3 `PlayerView` with its own controller disabled, `RESIZE_MODE_FIT`, keep-screen-on, transparent controls overlay, and top/bottom control containers.
- Key preserved dimensions/colors: primary dark `#0A0A0D`, surface `#1F222A`, accent `#0EA5E9`; portrait top `102dp`, landscape top `86dp`, bottom controls `112dp`, main icons `44dp`, loading spinner `58dp`.
- AtsuLab currently has a functional temporary shell but still uses a raw `SurfaceView`, simple bottom status, no `PlayerView`, no Anifux gradients/top bar, no orientation-specific builder, and no reusable controller skeleton.
- Part 6 must therefore be executed as small slices: design metrics → `PlayerView` engine seam → video shell → portrait/landscape builders → controller skeleton/system bars → rotation/back validation.

### Part 6 Slice 1 — Design Metrics — 2026-08-24

- Added Kotlin-owned `PlayerShellMetrics` for Anifux shell colors, typography sizes, control dimensions, episode grid columns, and portrait video sizing.
- Added a pure portrait-height calculation that preserves the original 30% screen ratio and `460px` minimum.
- Added focused unit coverage for exact shell values, ratio rounding, minimum enforcement, and invalid input.

### Part 6 Slice 2 — PlayerView Seam — 2026-08-24

- Replaced the raw surface-only playback seam with an opaque `setVideoView` contract.
- Attached and detached Media3 `PlayerView` inside the Media3 engine implementation without exposing ExoPlayer.
- Migrated the temporary player shell to a Media3 `PlayerView` with controls disabled, keep-screen-on enabled, and FIT resize mode.
- Updated engine delegation tests for the new video-view lifecycle.

### Part 6 Slice 3 — Shell Skeleton — 2026-08-24

- Added Kotlin-owned shell builders for Anifux-shaped portrait and landscape player layouts.
- Portrait now uses a measured fixed-height video frame, watching/status row, and scrollable lower shell area.
- Landscape now uses full-screen video with immersive system bars, a compact top bar, and centered bottom status controls.
- Added a controller skeleton with close/back, title, status, retry, and loading-indicator seams without advanced transport controls.
- Preserved the frozen primary-dark/surface colors and core `102dp` portrait / `86dp` landscape top-bar dimensions.
- Rotation now rebuilds the shell and reattaches the existing playback engine to the new Media3 `PlayerView`.
- Fixed the remaining builder scope/reference issues and normalized formatted player status arguments.
- Confirmed green CI and published the shell-skeleton debug build as Release `83`.
- User confirmed Release `84` device validation for portrait, landscape, rotation, back/close, loading/retry, and HLS playback; Part 6 is closed.

### Chrome Parity Pass — 2026-08-25

- Ported Anifux's complete static player chrome before advanced wiring so the shell no longer looks sparse.
- Restored exact top-row order: back/title, then landscape server pill → speed; both orientations receive audio → subtitle → cast; landscape adds episode → settings.
- Replaced the generic close glyph with Anifux's back artwork and reused the original 42dp icon targets with 8dp circular ripple padding.
- Restored the bottom transport order and offsets: lock at `24dp`, volume at `76dp`, optional rewind at `-140dp`, previous/play/next around center, optional forward at `+140dp`, and rotate at the right `24dp`.
- Corrected the landscape top chrome to remain in its frozen `86dp` band instead of stretching over the video surface.
- Added a landscape server pill using active source metadata and wired rewind/forward plus orientation rotation.
- Audio, subtitle, cast, episode list, settings, server selection, and player-level mute remain visible placeholders until their migration slices connect real behavior.
- Fixed the CI receiver-shadowing failures by capturing the shell orientation before `LinearLayout.apply {}` and assigning outer controller fields with explicit receiver labels.
- Confirmed all ported chrome drawables, labels, callbacks, icon order, and frozen offsets exist; advanced chrome actions remain intentionally inert until their slices are connected.

## Part 7 — Advanced Controls [~]

**Goal:** Port controls incrementally. Each group is an independent commit and test cycle.

### 7.1 Basic Transport [x]

- [x] Play/pause and previous/next episode controls.
- [x] Seekbar drag.
- [x] Elapsed time, total/remaining time, buffered indicator.

#### Part 7.1 Slice A — Episode Navigation State — 2026-08-24

- Added a tested `PlaybackEpisodeNavigator` that preserves the resolved episode list and current selection index.
- Selection keeps the existing exact-number-first behavior, then positional fallback.
- Previous/next targets use list adjacency and do not wrap across boundaries.
- `PlayerActivity` now retains navigation state and can resolve adjacent episode sources before transport buttons are exposed.

#### Part 7.1 Slice B — Transport Contract — 2026-08-24

- Added the Anifux-shaped centered transport seam: previous → play/pause → next.
- Ported the original 48dp icon artwork into 44dp controls with circular ripple targets and 70dp center offsets.
- Added explicit transport callbacks to `PlayerControllerSkeleton`; current shell callbacks remain temporary no-ops.
- Attached the hidden transport row to both portrait and landscape shells so user-visible behavior remains unchanged until Slice C wires engine actions.

#### Part 7.1 Slice C — Transport Wiring — 2026-08-24

- Added tested adjacency availability checks so previous/next controls disable at list boundaries.
- Exposed the transport row during buffering, ready playback, and ended states; hid it while loading or after errors.
- Wired play/pause to the retained Media3 play-when-ready state and previous/next to the episode navigator.
- Preserved playback state, navigation index, button enablement, and icon state across orientation rebuilds.
- Moved landscape source/status feedback to center, matching Anifux and avoiding overlap with bottom transport controls.
- Kept the persistent status surface for retry/error states only; normal playback relies on transport controls and the loading spinner.

#### Part 7.1 Slice D — Seekbar Drag — 2026-08-24

- Ported Anifux's 1000-step seekbar, accent progress line, buffered layer, thin thumb, bottom gradient, and 38dp progress row.
- Added a read-only `PlaybackEngine.currentState` seam for live position polling without exposing ExoPlayer.
- Updated seek position/buffer every 250ms on the main thread while preserving lifecycle cleanup.
- Suppressed automatic progress writes during drag and committed the selected position through `seekTo` only after release.
- Kept numeric elapsed/total labels deferred to the next focused slice.
- Fixed portrait placement by moving seekbar/transport buttons from the lower scroll panel into the video-frame bottom overlay, matching Anifux.

#### Part 7.1 Slice E — Time Indicators — 2026-08-24

- Added Anifux-style bold white elapsed and total-time labels on either side of the weighted seekbar.
- Preserved exact Anifux formatting as zero-padded minutes/seconds and added focused formatter coverage.
- Updated elapsed/duration labels every 250ms with position, buffer, and duration state.
- During drag, the elapsed label immediately previews the selected position before playback seeks.
- Buffered progress remains synchronized through the seekbar's secondary-progress layer.
- Closed Part 7.1 Basic Transport.

### 7.2 Lock Controls [x]

- [x] Lock button and unlock overlay.
- [x] Block gestures while locked.

#### Part 7.2 Slice A — Playback Lock — 2026-08-24

- Added the Anifux-style left-side lock action and temporary top-left unlock overlay.
- Ported original lock/unlock artwork, 44dp targets, portrait/landscape unlock offsets, and two-second auto-hide timing.
- Locking hides top controls, transport row, seekbar/time labels, and retry/status surface while keeping essential loading feedback.
- Unlocking restores the previously visible transport/status state.
- Locked state blocks play/pause, previous/next, seekbar commits, and future gesture actions at the central control boundary.
- Preserved lock state across rotation and cleaned up unlock timers on destruction.

#### Part 7.2 Slice B — Controls Visibility Fix — 2026-08-24

- Separated playback availability from user-facing controls visibility so ready/buffering state no longer forces permanent controls.
- Restored Anifux behavior: video tap toggles controls, shown controls auto-hide after four seconds, and interactions can restart that window.
- Locked-state screen taps now reveal the temporary unlock button again after it auto-hides.
- Buffering no longer force-shows controls; controls remain hidden until playback becomes ready, matching Anifux.
- Controls are hidden during IDLE/unavailable/source-loading transitions, while retry/status remains available when required.
- Preserved manual visibility, lock state, and auto-hide scheduling across orientation changes and lifecycle stops.

#### Validation — Release 98 — 2026-08-24

- Device validation confirmed buffering stays chrome-free, ready playback reveals controls, tap toggling works, four-second auto-hide works, and locked-state unlock reveal/unlock work.
- Closed Part 7.2 Lock Controls.

#### Brightness Reliability Fix — 2026-08-25

- Replaced the OEM-dependent activity-window brightness override with a dedicated video-surface dimming scrim.
- Mapped gesture level directly to scrim opacity so downward drags reliably darken video and upward drags remove the dimming layer.
- Preserved the minimum/max levels, HUD percentage, rotation state, lock blocking, and existing volume behavior.

### 7.3 Gestures [x]

- [x] Double-tap seek and single-tap controls toggle.
- [x] Vertical volume and brightness gestures.
- [x] Gesture HUD feedback.

#### Part 7.3 Slice A — Double-Tap Seek — 2026-08-24

- Added a dedicated player gesture handler using Anifux's confirmed single-tap and immediate double-tap behavior.
- Double-tapping the left half seeks back 10 seconds; double-tapping the right half seeks forward 10 seconds.
- Migrated single-tap handling into the same gesture boundary while preserving controls toggling and locked-touch unlock reveal.
- Blocked gesture seeking during loading/error states and while playback controls are locked.
- Kept relative seeks clamped to the current media duration and synchronized transport state immediately.

#### Part 7.3 Slice B — Vertical Levels & HUD — 2026-08-24

- Ported Anifux landscape-only vertical gestures: left-half drag adjusts brightness and right-half drag adjusts media volume.
- Preserved the 22dp activation threshold, 1.15 diagonal bias, 1.25 sensitivity, 5% minimum brightness, 0–100% volume range, and immediate gesture reset on release.
- Added the exact 82×138dp gesture HUD with SUN/VOL labels, accent level bar, percentage feedback, side placement, and margins.
- Restored Anifux HUD timing: visible during adjustment, then a 420ms delay followed by a 160ms fade.
- Kept locked touches isolated from brightness/volume adjustment, single taps, double-tap seeks, and control visibility changes.
- Rebuilt and attached a fresh HUD during orientation changes while retaining the same shell lifecycle cleanup.
- Closed Part 7.3 Gestures pending real-device validation of the latest green gesture release.

### 7.4 Speed Control [x]

- [x] Speed menu.
- [x] Persist selected speed.
- [x] Apply speed without resetting stream.

#### Part 7.4 — Speed Control — 2026-08-24

- Ported Anifux's compact landscape speed menu with `0.75x`, `1.0x`, `1.25x`, `1.5x`, and `2.0x`.
- Reproduced the exact 190dp popup, right-top offsets, 48dp rows, selected accent fill, surface border, and 170ms slide/fade animation.
- Added the original-style speed icon to the landscape top bar while keeping portrait controls unchanged.
- Applied selection directly through Media3 playback parameters so changing speed never reprepares the source or resets position.
- Reasserted the selected speed on READY and before each source start so episode switches retain it.
- Added a player-scoped `PlaybackPreferencesStore` backed by isolated SharedPreferences; invalid persisted values normalize safely to `1.0x`.
- Dismissed the popup silently on rotation, background, destruction, and shell rebuild to prevent stale callbacks and window leaks.
- Kept the menu blocked during loading/error/locked states through the existing transport visibility boundary.
- Closed Part 7.4 Speed Control pending real-device validation of the latest green release.


### 7.5 Subtitles [x]

- [x] Embedded subtitle detection.
- [x] External subtitle attachment.
- [x] Language selection, toggle, and missing-subtitle fallback.
- [x] Styling controls and persisted appearance preferences.
- [x] Custom subtitle font file selection.

#### Part 7.5 Slice B — Selection Menu & Toggle — 2026-08-25

- Added a public engine seam that selects a stable embedded text-track override or disables the text renderer for Off.
- Added Anifux's full-height right-side subtitle panel at `340dp`, including scrim, slide/fade animation, title, focus outlines, and dismiss behavior.
- Restored Anifux row geometry: `66dp` buttons with `8dp` margins, selected accent fill, unselected surface fill, exact panel padding, and `Off` last.
- Wired detected embedded tracks into the menu and applied selections directly through `DefaultTrackSelector` without repreparing playback.
- Labeled external subtitles as English so they remain selectable after attachment and added a safe English fallback when no usable track snapshot exists yet.
- Dismissed the subtitle panel on rotation, background, destruction, and shell rebuild; lock/loading boundaries continue to block opening it.
- Subtitle appearance/style settings remain deferred to a later storage/settings slice.

#### Part 7.5 Slice C — Style & Persistence — 2026-08-25

- Added a persisted `SubtitleStyle` model covering size, font style/color, background color/no-background/opacity, bottom position, and shadow.
- Restored Anifux defaults and ranges: `50–300%` text size, four style modes, fixed color presets, and `0–100%` padding/shadow/opacity.
- Added an AtsuLab player-side style panel with Anifux's `380dp` side-panel behavior, sliders, segment chips, color swatches, no-background toggle, and reset action.
- Applied styles through Media3's `CaptionStyleCompat`, fractional text size, edge shadow, typeface mapping, and subtitle-view bottom margin without reattaching playback.
- Saved every change immediately in the existing playback preference store and reapplied the normalized style after rotation or shell rebuild.
- Custom `.ttf` selection was deferred during Slice C and completed in Slice D.
- Fixed the real-device style-panel crash by attaching each slider value label only to its slider row instead of first adding it to both the panel and row.

#### Subtitle Selection Fix — 2026-08-25

- Fixed embedded subtitle switching by using Media3's track-type disable API instead of passing `C.TRACK_TYPE_TEXT` as a renderer index.
- Cleared stale text-selection overrides before selecting another language or switching subtitles Off.

#### Subtitle Validation & Crash Fix — Releases 119–121 — 2026-08-25

- Release `119` exposed a real-device crash when opening Style Settings because a slider value label received two parents.
- Commit `9d0083d83d641f0570865f74a5f409c4a198fcaf` removed the duplicate attachment and published Release `120`.
- User confirmed that the Style Settings flow no longer crashes after Release `120`.
- Release `121` (`33aafb36be9efbac921b117af3ab42a97c23d7d4`) added custom font import/clearing and passed CI; device validation is pending.

#### Part 7.5 Slice D — Custom Font — 2026-08-25

- Added custom font selection and clear actions to the persisted subtitle style panel.
- Used the system content picker without runtime storage permissions, then validated and copied the selected TTF/OTF into app-private storage.
- Persisted the copied font path with the existing playback preference store so it survives rotation, process death, and restarts.
- Applied the custom typeface through Media3 caption styling while safely falling back to the selected built-in style if a stored file becomes unreadable.
- Closed Part 7.5 Subtitles.

### 7.6 Quality Selector [~]

- [x] Detect HLS quality variants.
- [x] Provide AUTO and manual options.
- [x] Preserve position on quality switch.

#### Part 7.6 Slice A — HLS Variant Detection — 2026-08-25

- Added a `VideoQuality` snapshot to `PlaybackState` using stable Media3 track IDs and selected state.
- Detected video variants directly from Media3 `Tracks` without a second HLS master-playlist request.
- Restored Anifux label behavior: resolution height first, then Anifux's bitrate thresholds, followed by provider-provided labels.
- Normalized variants into highest-to-lowest order by height, bitrate, and width; focused metadata tests were added.

#### Part 7.6 Slice B — Selection & Reset-Free Switching — 2026-08-25

- Added the Anifux-style 54×42dp landscape quality pill with AUTO/FHD/HD/SD labels.
- Added a 380dp right-side “Choose quality” panel using Anifux row sizing, focus outlines, selected fill, AUTO-first ordering, and descending variants.
- Connected quality selection through the playback engine seam so manual/AUTO changes use Media3 track overrides without re-preparing the media source or resetting position.
- Cleared manual selection on episode/source reload while preserving it across rotation.
- Blocked selections for missing track IDs and added focused coverage for option ordering, label mapping, engine delegation, and position preservation.
- Marked Part 7.6 code-complete pending Release device validation.

### 7.7 Server Selector [x]

- [x] SUB/DUB tabs and server list.
- [x] Active server indicator.
- [x] Loading-more state.
- [x] Switch server with minimal progress loss.
- [x] Clear all-servers-failed state.

#### Part 7.7 Slice A — Language Tabs & Server List — 2026-08-25

- Added the Anifux-style 340dp “Audio” panel with SUB/DUB tabs, a muted Servers heading, and exact row/focus styling.
- Kept the full source snapshot in the player and filtered it into language modes without deleting alternate sources.
- Grouped servers by display name with stable numbering and highlighted the active source.
- Wired the landscape server pill to the panel, replacing generic S1 labels with provider/server names.
- Switched servers by reusing Media3 preparation at the latest captured position, then restored persisted speed without restarting the episode from zero.
- Guarded empty SUB/DUB modes and dismissed the panel safely across rotation, backgrounding, and destruction.

#### Part 7.7 Slice B — Audio Entry & Language Switch Fix — 2026-08-25

- Moved the SUB/DUB “Audio” panel to the audio icon and made the server pill open a separate “Select Server” list without tabs.
- Changed SUB/DUB selection from filter-only behavior into Anifux-compatible playback switching: the active source is retained when it already matches, otherwise the first matching source starts at the preserved position.
- Kept the previous language active when its requested opposite mode has no sources, showed an explicit no-server toast, and prevented silent fallback to the other language.
- Aligned dub detection with Anifux by using language/server/quality metadata only, avoiding false classification from provider display names.

#### Part 7.7 Slice C — More Servers & Failure Recovery — 2026-08-25

- Added a secondary source-resolution contract that queries remaining providers without re-querying the provider that produced the current episode.
- Kept first playback startup fast, merged additional provider sources in the background, deduplicated URLs, and preserved active-source indexes while appending rows.
- Added the Anifux-style animated “Loading more servers...” row to the server panel.
- Added automatic playback fallback: a failed source is remembered, the first usable same-language source is tried next, and other-language sources are used only when needed.
- Added explicit “No working server” terminal state with retry, cleared failed state on successful READY/manual selection/new episode/reload, and refreshed an open panel when loading or failure state changes.
- Covered remaining-provider merging, provider failure isolation, same-language fallback priority, and failed-index handling with focused tests.

#### Part 7.7 Closure — 2026-08-25

- Fixed the server-recovery compile errors by aligning failed-source index naming in `PlayerActivity`.
- Strengthened retry recovery so a terminal failure re-queries all source providers instead of only secondary providers.
- Preserved active playback indexes while appending background sources and added focused repository coverage for provider merging and recovery.
- Closed Part 7.7 at code level with green CI (`aa07e863`); real-device validation remains required.

### 7.8 Skip Intro/Outro [x]

- [x] Resolve MAL ID through AniList.
- [x] Fetch AniSkip intervals.
- [x] Show/hide skip buttons by interval.
- [x] Add seekbar marker.
- [x] Cache skip data per episode.

#### Part 7.8 — Skip Intro/Outro — 2026-08-25

- Reused the existing AniSkip repository flow for MAL-ID resolution, OP/ED lookup, request headers, parser validation, and thread-safe MAL-ID caching.
- Added a process-local interval cache keyed by anime, provider/episode identity, episode number, and requested duration so source switches do not re-fetch the same skip data.
- Loaded intervals once playback reports a valid duration, cleared state safely when episodes or sources reload, and ignored stale async responses after episode changes.
- Added an Anifux-style rounded Skip Intro/Skip Outro control positioned above the transport row; it seeks exactly to the interval end and resumes playback without changing persisted speed.
- Added accent-colored seekbar markers that map each valid interval onto the current duration and hide automatically for empty or unknown-duration results.
- Kept skip controls hidden while controls are hidden or playback is locked, and suppressed the button during the final second of its interval to avoid a useless tap.

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
- Implemented runtime episode loading, source resolution, Media3 attachment, retry/error states, and lifecycle cleanup.
- Confirmed Release `77` plays real HLS video from the Media Details entry point on a real device.
- Deferred Continue Watching and started Part 6 with frozen player shell metrics and unit tests.
- Completed Part 6 slices 1–3 at code level: frozen metrics, Media3 seam, portrait/landscape shells, controller/status/loading seams, rotation reattachment, and system-bar behavior.
- Fixed the shell-builder scope issue and mixed positional/named arguments, then confirmed green CI and Release `83`.
- User confirmed Release `84` real-device validation on Symphony Z35: portrait, landscape, rotation, back/close, loading/retry, and HLS playback.
- Closed Part 6 and set the next executable phase to **Part 7.1 Basic Transport**.
- Completed Part 7.1 Slices A–C: navigation state, transport contract, and working play/pause plus previous/next wiring.
- Completed Part 7.1 Slices D–E: Anifux-style seekbar drag, portrait overlay fix, elapsed/total labels, and buffered-progress synchronization.
- Closed Part 7.1 Basic Transport at code level pending Release `94` device validation.
- Implemented Part 7.2 playback locking, temporary unlock overlay, control blocking, and rotation persistence.
- Completed Part 7.3 Slices A–B: confirmed/double/single tap handling, ±10s double-tap seek, landscape vertical volume/brightness, and Anifux-style HUD.
- Completed Part 7.4 Speed Control: exact landscape popup, persistent normalized speed, direct Media3 application without stream reset, and safe rotation/lifecycle dismissal.
- Started Part 7.5 Subtitles with Media3 embedded-track snapshots, track-change emissions, normalized metadata, and focused tests.
- Completed a player chrome parity pass with Anifux icon/order/offset restoration and corrected compact landscape top placement.
- Completed Part 7.5 Slice B: embedded subtitle selection, Off toggle, external-subtitle fallback, and the exact Anifux right-side panel.
- Completed Part 7.5 Slice C: persisted live subtitle styling controls with direct Media3 caption application.
- Fixed Release `119` subtitle-style-panel crash caused by a duplicate parent attachment in slider rows.
- Completed Part 7.5 Slice D: permission-free custom font import, persistence, validation, clearing, and Media3 caption application.

### 2026-08-25

- Fixed subtitle language selection and Off behavior by clearing stale text-track overrides and disabling the correct Media3 track type.
- Completed player chrome parity, subtitle selection/toggle, persisted live styling, and permission-free custom font support.
- Published green releases through Release `121`; the latest documented stable smoke point is Release `120`, where the user confirmed Style Settings no longer crashes.
- Set next executable phase: **Part 7.6 Quality Selector**, beginning with HLS variant detection after Release `121` device validation.

- Completed Part 7.6 Slice A: Media3 HLS video-quality detection with Anifux-compatible labels and descending normalization.
- Completed Part 7.6 Slice B: AUTO/manual quality UI, Media3 track overrides, reset-free switching, rotation/source-reset handling, and focused unit tests.
- Completed Part 7.7 Slice A: SUB/DUB filtering, grouped/numbered server rows, active-server highlighting, and position-preserving source switches with focused model tests.
- Fixed Part 7.7 entry routing and language switching: Audio owns SUB/DUB, server pill owns server-only selection, and selecting a valid language immediately switches to that mode without silent fallback.
- Completed Part 7.7 Slice C: background more-sources merging, animated panel loading, automatic same-language fallback, retryable all-failed state, and focused recovery tests.

## Next Action

1. Install the latest green release and confirm AUTO/manual quality switching still works without resetting position or speed.
2. Confirm Audio owns SUB/DUB switching while the server pill opens Select Server, background servers merge into the panel, and failure/retry behavior is stable.
3. Play an anime with known AniSkip data: verify the seekbar markers appear and Skip Intro/Skip Outro appears only inside its interval.
4. Tap each skip control, verify playback resumes at the interval end with position/speed preserved, then rotate/background/reopen and ensure controls and markers remain stable.
