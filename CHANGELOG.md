# Changelog

All notable changes to this project will be documented in this file.

## Unreleased

### Added

- NCPB-derived block, item, and text scene editing for Yuushya Modelling 2.4.2.
- Six-direction move and rotation Gizmos plus independent X/Y/Z scale handles with modifier-aware snapping.
- Exit autosave, deletion, undo/redo, collision presets, and save-time Z-fighting detection.
- Search-synchronized block and item picker previews, including real 3D item-model rendering.
- Complete item-layer settings for colour, item/block render mode, and per-property block-state editing.
- Diameter-25 environment preview with edge dithering, persistent section meshes, and event-driven invalidation.
- Real integrated-client ModBench acceptance coverage and environment capture performance gates.
- Real text-editor Bench coverage for multiline edits, culling/mirroring, new layers, autosave, and server reload.
- Independent-scale Bench coverage for inspector X scaling and isolated X/Y/Z Gizmo drags with pivot/undo checks.
- Clean release, production Jar integrity, runtime SHA-256, translation-key, and GitHub CI verification.
- User, development, release, contribution, security, and project-handoff documentation.
- NCPB-style CI artifacts with SHA-256 files, tag releases, and integrated-client release gating.

### Changed

- The project has entered beta testing; the current project version now has a single source of truth in
  `gradle.properties`, while documentation uses stable placeholders and release-channel wording.
- BenchMod is now pinned to the immutable JitPack `0.1.2` release; local builds and CI no longer clone and
  publish a separate BenchMod checkout to Maven Local.
- CI now runs once per pull request or `master` update; the heavier integrated-client Bench is a `v*` release
  gate and must pass alongside the production build before GitHub publishes the release.
- Automated tag publishing now marks only `-alpha` tags as prereleases; `-beta`, `-rc`, and stable tags create
  normal GitHub Releases.
- Environment capture now yields every 32 cells instead of after a complete 20×20 neighborhood plane.
- Build inputs remain pinned to the audited Minecraft 26.1.2 / NeoForge 26.1.2.76 toolchain, while published
  metadata accepts Minecraft `[26.1.2,27)` and NeoForge `[26,)`, matching NCPB's compatibility policy.
- Non-essential environment invalidation Mixins are optional; editor binding also falls back on linkage errors.
- Yuushya 2.4.2's empty `CUSTOM` collision placeholder remains readable but is no longer created by cycling the
  editable collision presets.
- Z-fighting save choices now share a host-neutral, unit-tested decision coordinator; ModBench drives the real
  warning, optimize-and-save button, and server-visible epsilon offset.
- Block-mode item layers now use Yuushya's native item origin, participate in shape-aware coplanar checks, and
  retain their `BLOCKSTATE` component through editing, native export, network save, and server reload.

### Fixed

- The thinnest preview-line tier is now two physical pixels, and deferred item/text/block batches are flushed
  before grid, outline, collision, selection, and Gizmo overlays so background modeled items cannot cover
  foreground editor lines.
- CI IDE verification now materializes the patched Minecraft Jar before checking the generated classpath.
- The integrated-client PIP cache gate now measures a dedicated unchanged warmup window instead of comparing
  cache reuse against legitimate redraws accumulated across the entire multi-screen scenario.
- Gradle now uses NCPB's project-level Java 25 Daemon criteria across IDE, wrapper and CI entry points; ordinary
  VS Code import no longer loads ModBench, while explicit `-PenableModBench=true` runs retain the full Bench
  source set and tasks without machine-specific JDK paths.
- IDE classpath verification points to the current `build.nosync` Minecraft Jar containing `Screen.class`
  instead of the retired `build/moddev` path.
- The nine-field inspector now uses a three-column compact layout so action buttons no longer overlap at small
  GUI heights.
