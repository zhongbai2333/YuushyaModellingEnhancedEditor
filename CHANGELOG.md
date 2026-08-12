# Changelog

All notable changes to this project will be documented in this file.

## Unreleased

### Added

- NCPB-derived block, item, and text scene editing for Yuushya Modelling 2.4.2.
- Six-direction move, rotation, and overall-scale Gizmos with modifier-aware snapping.
- Exit autosave, deletion, undo/redo, collision presets, and save-time Z-fighting detection.
- Search-synchronized block and item picker previews, including real 3D item-model rendering.
- Diameter-25 environment preview with edge dithering, persistent section meshes, and event-driven invalidation.
- Real integrated-client ModBench acceptance coverage and environment capture performance gates.
- Clean release, production Jar integrity, runtime SHA-256, translation-key, and GitHub CI verification.
- User, development, release, contribution, security, and project-handoff documentation.
- NCPB-style CI artifacts with SHA-256 files, tag releases, and integrated-client release gating.

### Changed

- BenchMod is now pinned to the immutable JitPack `0.1.1` release; local builds and CI no longer clone and
  publish a separate BenchMod checkout to Maven Local.
- CI now runs once per pull request or `master` update; the heavier integrated-client Bench is a `v*` release
  gate and must pass alongside the production build before GitHub publishes the release.
- Environment capture now yields every 32 cells instead of after a complete 20×20 neighborhood plane.
- Minecraft and NeoForge metadata ranges are pinned to the audited 26.1.2 / 26.1.2.76 versions.
- Non-essential environment invalidation Mixins are optional; editor binding also falls back on linkage errors.
- Yuushya 2.4.2's empty `CUSTOM` collision placeholder remains readable but is no longer created by cycling the
  editable collision presets.
- Z-fighting save choices now share a host-neutral, unit-tested decision coordinator; ModBench drives the real
  warning, optimize-and-save button, and server-visible epsilon offset.

### Fixed

- CI IDE verification now materializes the patched Minecraft Jar before checking the generated classpath.
- The integrated-client PIP cache gate now measures a dedicated unchanged warmup window instead of comparing
  cache reuse against legitimate redraws accumulated across the entire multi-screen scenario.
- VS Code now starts JDT LS and Gradle import with Java 25, uses the stable Buildship importer, and verifies that
  its classpath points to the current `build.nosync` Minecraft Jar containing `Screen.class` instead of the
  retired `build/moddev` path.
