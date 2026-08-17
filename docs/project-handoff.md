# Project handoff

## Purpose

Yuushya Modelling Enhanced Editor is a standalone, client-only NeoForge mod for Minecraft 26.1.2. It replaces
the editing experience for Yuushya Modelling entity blocks without taking ownership of their world data,
persistence, validation, or client-to-server protocol.

The first integration target is the Yuushya Modelling show-block editor. The enhanced editor must always fail
closed: if its compatibility binding cannot be established, the original Yuushya screen opens unchanged.

## Audited upstreams

| Project | Repository | Audited branch/revision | Role |
| --- | --- | --- | --- |
| NetMusicCanPlayBili | <https://github.com/zhongbai2333/NetMusicCanPlayBili> | `master`; editor core introduced by `be964ac` | Source of the reusable scene-editor interaction model |
| SceneEditor | <https://github.com/zhongbai2333/SceneEditor> | Version and compatible range are declared in `gradle.properties` | JiJ camera, projection, selection, Gizmo, and history core |
| Yuushya Modelling | <https://github.com/Crystal1921/Yuushya-Modelling> | `26.1` at `57f4d86407510f76191e35c280d8e1c8d347bf42` | Runtime host, entity-block data, original screens, and network protocol |
| Yuushya Townscape | <https://gitee.com/yuushyatownscape/yuushya-townscape/> | `26.1` at `3478bb99b3a1b2bb7df7dd8a974364c12e49f410` | Separate content upstream and optional development content |

The revisions above are audit anchors, not permanent dependency locks. Re-audit the named integration classes
before changing the supported Yuushya version range.

## SceneEditor and NCPB provenance

The host-neutral camera matrices, projection, picking, selection policies, Gizmo interaction, bounded command
history, and drag transaction now live in the independent SceneEditor repository and are embedded through
NeoForge JiJ. The published compatibility range begins with the first SceneEditor version containing the
Yuushya-proven multi-selection, modifier snapping, cursor wrapping, large-model rotation sensitivity, and
pole-clamped camera behavior.

The Yuushya-specific document, model-unit transform, collision shape, coplanar checks, rendering, black-gold
widgets, networking, and save coordination remain local. The Screen is therefore a focused host adapter rather
than a copy of NCPB's media-console document and networking code, while reusable fixes have one upstream source.

Both NCPB and this project use the MIT license and currently share the same copyright holder. Keep this
provenance section when moving or substantially adapting NCPB editor code.

## Yuushya 26.1 integration map

The verified opening path is:

```text
GuiItem.inMainHandRightClickOnBlock
  -> ClientMethod.openGuiScreen
  -> new ShowBlockScreen(showBlockEntity, newBlockState)
  -> Minecraft.setScreen
```

The enhanced mod observes the resulting `ShowBlockScreen` through NeoForge's client `ScreenEvent.Opening`.
This avoids patching Yuushya source and allows the unmodified screen to remain the fallback.

| Editor concept | Yuushya 26.1 source |
| --- | --- |
| Scene/document | `ShowBlockEntity#getTransformData()` |
| Layer | `TransformBlockData` list entry / slot |
| Position | `TransformBlockData.pos` (`Vector3d`, Yuushya model units) |
| Rotation | `TransformBlockData.rot` (`Vector3f`, X/Y/Z degrees, applied Z then Y then X) |
| Scale | `TransformBlockData.scales` (`Vector3f`); its screen exposes one overall control |
| Payload | `TransformBlockData.blockState` |
| Visibility | `TransformBlockData.isShown` |
| Incremental submission | `TransformDataOncePacket.sendToServerSide` |
| Field selector | `TransformType` |
| Commit/save signal | `TransformDataOncePacket.sendToServerSideSuccess` |
| Custom collision payload | `AbstractTransformBlockEntity#getCustomShape()` / Shape Tool `SHAPE_DATA` |
| Original fallback | `ShowBlockScreen` |

The same adapter pattern is used for `ItemBlockScreen`/`ItemBlockEntity` and `TextBlockScreen`/`TextBlockEntity`.
Item layers preserve `ItemStack`, color, and `enableBlock`; text layers preserve JSON line fragments, culling, and
mirroring. Their native `REMOVE` operation resets a slot, so the enhanced host compacts retained layers into the
front of the list and clears trailing slots before sending the existing success packet.

Yuushya is a required host dependency and the adapter compiles directly against this audited contract. Public
entity/data/network APIs are called with strong types; Mixin Accessors expose only the private constructor state
needed to replace the original screens. API drift must fail during compilation or compatibility verification,
not as a late reflective error. Do not submit through a replacement packet or write block-entity NBT directly.

## Scope decisions

The committed targets are `ShowBlockScreen`, `ItemBlockScreen`, and `TextBlockScreen`. They share the same editor
workspace and use explicit strongly typed data/packet adapters for each Yuushya host type; an adapter or Accessor
failure leaves that type's original screen in place.

The target UI is the NCPB-style scene editor: layer list, central scene viewport, transform inspector, shared
camera matrices for rendering/projection/picking, Gizmo manipulation, and bounded undo/redo. Editing remains in
an immutable draft; normal exit validates and commits it, while the explicit discard action is the no-submit path.

## Safety invariants

1. This mod is client-only; Yuushya Modelling remains required on client and server.
2. Opening or binding failure always falls back to the original Yuushya screen.
3. Editing occurs in an immutable local draft until normal exit/save.
4. Escape and close validate and submit the draft; only the explicitly labelled Discard action sends no packets.
5. Exit autosave validates original layer identities, new-layer uniqueness, and finite transform values before
   submission; existing layers may be deleted but retained layers may not reorder.
6. Submission uses Yuushya's existing packet types and finishes with its success/save signal; deletion is represented
   by native `REMOVE` plus front-compaction because Yuushya does not physically shrink transform lists.
7. Visible block layers are checked for coplanar faces before save; the user may apply a reversible `1e-4` offset,
   keep the overlap, or return to editing.
8. A supported-version update requires compatibility tests and an in-game smoke test with both mods installed.
9. Custom collision editing remains client-only: it requires Creative mode, synchronizes a selected Shape Tool
   with vanilla creative-inventory packets, and applies it through Yuushya's normal `ShapeItem` interaction.
   It must not add a custom server packet or write block-entity NBT directly.
10. Auto-generated block collision keeps each outline-shape AABB before transformation. Out-of-cell boxes remain
    editable for native-data compatibility, but the UI must warn that vanilla collision broadphase can stop querying
    their source block and offer lossless-to-the-cell clipping; do not imply a client-only fix changes server physics.
11. Custom collision is edited in canonical model space. Shape Tool payloads and block-entity `customShape` are in
    world-local space, so block `HORIZONTAL_FACING` must be applied on write and inverted on read. Yuushya rotates
    rendered ShowBlock geometry around the cell center but returns `customShape` without that rotation.
12. A visible concave cavity is not necessarily traversable. A standing player's horizontal AABB is about 0.6 blocks
    wide, so the vanilla inner-stair 0.5-by-0.5 upper cavity cannot contain it. Do not "fix" this by silently eroding
    generated collision; document the clearance requirement and test concave multi-box client/server round trips.
13. The audited Yuushya Modelling 2.4.2 registers ShowBlock, ItemBlock, and TextBlock without `dynamicShape`, although
    CUSTOM collision reads block-entity `customShape`. Minecraft can therefore cache the fallback collision for
    context-free state queries while contextual queries read the live shape. Vanilla stairs are state-only and do
    not have this split. The complete fix belongs upstream and must run on both sides; do not add a client-only Mixin
    that changes block shape caching. While 2.4.2 remains supported, Bench records the known mismatch without
    blocking this client-only add-on's release; once the host declares `dynamicShape`, the same Bench hard-asserts
    equality between context-free and contextual queries on both the integrated client and server.

## Delivery sequence

1. Correct the runtime probe to the real 26.1 class names.
2. Implement and unit-test Euler/quaternion and document mapping.
3. Replace the audited ShowBlock, ItemBlock, and TextBlock screens through `ScreenEvent.Opening` with a one-shot
   original-screen bypass.
4. Deliver layer selection, numeric transform editing, visibility, exit autosave/explicit discard, undo/redo,
   and original fallback.
5. Port NCPB orbit/pan/dolly, standard views, selection, and Gizmo math with their tests.
6. Add block-state preview rendering and depth-correct picking using one shared camera frame.
7. Add a Yuushya 26.1 development fixture and unattended integrated-client acceptance test.

All seven steps are implemented for the three committed editor targets.

## Current implementation status

- Steps 1–3 are implemented for ShowBlock, ItemBlock, and TextBlock through strongly typed hosts,
  read-only Mixin Accessors, and `ScreenEvent.Opening`; production code does not use Java reflection.
- The Screen now uses NCPB's black-gold workspace shell: element hierarchy on the left, a central PIP scene
  viewport, compact three-column transform inspector on the right, top-right move/rotate/scale tool strip, bottom
  status/camera HUD, and the six-axis orientation widget. It retains Yuushya's immutable draft, bounded
  undo/redo, normal-exit autosave, explicit discard, and original-screen fallback semantics.
- The inspector exposes nine values: position X/Y/Z, rotation X/Y/Z, and independent scale X/Y/Z.
  Scale changes write Yuushya's existing `SCALE_X/Y/Z` fields independently and compensate each raw position
  component by `oldScale / newScale`, preserving the visible pivot without private data or protocol changes.
- NCPB's host-neutral camera navigation subset, mouse policy, selection policy, Gizmo constraint math, and drag
  transaction are present. `W` exposes world-space X/Y/Z move handles, `E` exposes world-space rotation rings,
  and `R` exposes colored X/Y/Z scale handles. All use a captured camera frame and commit one history entry per
  pointer drag; scale preserves the visible pivot through per-axis position compensation.
- The viewport submits visible Minecraft `BlockState` values to a minimal PIP renderer using the current resource
  pack's baked models. It reproduces Yuushya's center-scale, translated-position, center-rotation composition;
  rendering, layer points, projection, and Gizmo picking share the resulting scaled pivot and `CameraFrame`.
  Equal immutable render states reuse the previous PIP texture instead of redrawing every Screen frame. The
  renderer caches resolved `BlockModelRenderState` values by `BlockState` until the resource-pack model set
  changes and frustum-culls transformed unit cubes before submission whenever a redraw is required.
- Environment rendering follows NCPB's terrain route rather than submitting each surrounding block as a GUI
  feature node. A client-thread capture manager progressively snapshots 16³ sections inside a fixed diameter-25
  sphere (`R=12.5`, solid through `R=9.5`, three-block dither shell), including the 20³ AO/light neighborhood.
  A single low-priority compiler thread then uses vanilla `ModelBlockRenderer`/`FluidRenderer` to produce
  opaque, cutout, and translucent `MeshData`; the render thread uploads each completed section into persistent
  shared `UberGpuBuffer` heaps. Camera motion only rebuilds a small section render plan, uploads dynamic
  `ChunkSection` uniforms, and performs section-level frustum culling. Unchanged sections never re-tessellate.
- Layer selection inverse-transforms the picking ray into each rendered unit cube and chooses the nearest
  non-negative world-ray hit. Rotated/scaled overlapping blocks therefore no longer depend on pivot proximity.
- The hierarchy has a searchable registry-backed block picker with a live candidate-model PIP on its left.
  Choosing an entry appends it at model origin `(0,0,0)`, selects it, records one undoable command, and the 26.1
  host allocates the corresponding new Yuushya slot before sending `BLOCK_STATE`, transform, visibility, and
  final success packets.
- Item and text layers have dedicated pick/edit panels. Item previews use the real `ItemStack`, including Yuushya's
  optional block-state component; text previews retain line JSON until edited and expose culling/mirroring. The
  same hierarchy supports Delete/Backspace with undo and native-slot compaction on submit.
- Gizmo deltas use relative, modifier-aware snap levels (`0.1`/`0.05`/`0.001` for move and scale; `15°`/`5°`/
  `0.001°` for rotation), preventing an existing transform from jumping to a global grid when a drag starts.
- Save-time Z-fighting detection compares transformed block faces in world space, ignores edge/point contact, and
  offers a reversible `1e-4` offset optimization before packet submission.
- The host exposes the ShowBlock world position to a read-only environment sampler. The editor captures only the
  fixed diameter-25 sphere around that position; stable coordinate-hash dithering scatters the outer three-block
  shell, while the origin ShowBlock is masked from the environment mesh. Environment sections cannot be selected,
  undone, or submitted as Yuushya layers. Client block-update packets and chunk load/unload events invalidate only
  affected sections, and each 20³ capture yields between fixed 32-cell batches so the client-thread budget is
  enforceable rather than being checked only after a complete 20×20 Y plane.
- Gizmo sizing derives a world-space bounding radius from the selected scale and a screen-space radius from all
  eight projected block corners. Rendering and hit testing consume the same adaptive axis/ring/scale dimensions,
  and the scale handle chooses the viewport diagonal with the most remaining space.
- Selected blocks have both a distance-scaled gold world outline and short screen-space gold corner markers. The
  latter remain visible when a transparent/new block overlaps an opaque layer, so hierarchy, inspector, and scene
  selection cannot disagree visually.
- Grid and Gizmo lines use NCPB's physical-pixel compensation path. Minor and four-unit major grid lines use
  separate luminance/alpha levels; Bench verifies that close and distant camera positions retain usable line
  weights without tying the documentation to one machine's measured scale.
- The PIP renderer flushes its opaque, cutout, and sorted translucent sheets before restoring its temporary 3D
  projection. This is required for translucent blocks such as honey to reach the PIP framebuffer.
- The Screen uses responsive NCPB-style dual-panel geometry at small logical resolutions. Bench verifies that the
  hierarchy, inspector, commit buttons, and tool strip remain usable without clipping or overlap.
- Screen-side render inputs also preserve immutable snapshot identity: camera matrices are rebuilt only when the
  `CameraState` reference or viewport changes, editor-layer snapshots only when the immutable `SceneDocument`
  changes, environment snapshots only when sampled content changes, and Gizmo sizes only when their layer/camera
  inputs change. `CameraMatrices` has value equality so independently reconstructed equivalent frames still
  participate correctly in PIP render-state equality.
- NeoForge 26.1 exposes public `GuiGraphicsExtractor.submitPictureInPictureRenderState` and
  `peekScissorStack` methods, so this implementation does not require NCPB's `GuiGraphicsExtractor` accessor
  Mixin.
- The audited Yuushya Modelling 2.4.2 Jar is a `compileOnly` and development `runtimeOnly` dependency, so the
  adapter compiles against its API and ordinary `runClient` loads both mods. Its version, required classes, and
  SHA-256 are checked before client/Bench runs. Overriding the path also requires an explicitly audited
  `-Pyuushya_runtime_sha256=<sha256>` value.
- The generated metadata requires the audited `yuushya_modelling` `[2.4.2]` on the client. It must not require only
  Townscape's `yuushya` modId: the latest Townscape `26.1` source and 2.3.0 Jar still do not contain the
  modelling classes and explicitly detect `yuushya_modelling` as a separate mod.
- Host-neutral tests cover transforms, selection, history, clipboard behavior, camera policy, collision handling,
  and Z-fighting geometry. The ordinary dual-Mod development client reaches resource loading without enabling
  the Bench plugin.
- ModBench scenario `yuushya-editor.axis-scale-apply` runs against a real integrated server and real
  `ShowBlockEntity`. It checks Screen interception, the NCPB black-gold button shell, responsive GUI geometry,
  independent-axis scale compensation, isolated X/Y/Z mouse scale drags with unchanged sibling axes, stable pivots
  and exact undo, `onClose()` autosave packet
  results, unchanged sibling layers, fixture cleanup, visible stone/oak-leaves/honey rendering, a dense 25×25
  platform plus gold environment sampling, registry preview/search, block and item-picker PIP previews, appending
  duplicate `minecraft:glass` layers at `(0,0,0)`, the real Z-fighting optimize-and-save decision and its
  server-side epsilon offset, selected-layer visibility, near/far line-width compensation, `NONE → CUSTOM`
  collision editing through a Creative Shape Tool plus exact server-side custom-box bounds,
  a live environment block update, real ItemBlock deletion, and real
  TextBlock multiline content/culling/mirroring updates, creation of a second text layer, and server reload. It
  hard-asserts in a dedicated unchanged warmup window that PIP
  textures are reused more often than rendered, that model resolutions are a minority of edited instances, the
  shell is partly scattered, and
  persistent environment sections are rendered repeatedly after being compiled once. Environment capture also
  has hard gates of 10 ms per bounded slice and 12 ms per client Tick. Per-run measurements are intentionally
  kept out of this document; the authoritative report is
  `build.nosync/modBench/raw-results/default/client/summary.json`, while capture and invalidation counters are
  written to `artifacts/custom/yuushya-editor-performance.txt`.

## Development and Bench dependencies

- `libs/yuushya_modelling-26.1.2-2.4.2.jar` is the default `compileOnly`, development `runtimeOnly`, and Bench host.
  It is validated by version, required classes, and SHA-256 in `verifyYuushyaRuntime`, never shaded into the
  production Jar, and may be overridden only together with the audited `yuushya_runtime_sha256` property.
- BenchMod is pinned to the immutable JitPack `0.1.2` release. The plugin injects API and Runtime modules from
  the same release, so no sibling checkout or Maven Local publication is required.
- Run `./gradlew verifyYuushyaEditorBench -PenableModBench=true` for the complete unattended client flow. Raw results live under
  `build.nosync/modBench/raw-results/default/client`; a portable collection is written to
  `build.nosync/modBench/bundles/default/client`.
- Run `./gradlew releaseBuild --no-build-cache --no-configuration-cache` for a clean distributable Jar. The task rejects filesystem
  conflict copies such as `Screen 2.class`, Bench classes, and shaded Yuushya host classes; CI performs the same
  flow with the pinned JitPack BenchMod release.

## Yuushya main-project merge check

The audited `26.1` Townscape branch and artifact do not contain the modelling Screen/classes used here. Townscape
uses mod id `yuushya` and detects `yuushya_modelling` as a separate mod; the latter uses mod id
`yuushya_modelling` and owns the ShowBlock, ItemBlock, and TextBlock screens/entities, transform data, and packets. The
Minecraft 26.1.2 verification client likewise loads Yuushya Modelling 2.4.2 as a separate mod. Keep the explicit
runtime dependency and metadata dependency unless a future upstream artifact is re-audited and proves an actual
merge.

## Follow-up scope

- If exact baked-quad surface picking becomes necessary, it can refine the current depth-correct transformed-cube
  picking without changing the Screen selection contract.
- Re-audit upstream class names, packet semantics, dependency range, and Bench fixtures before supporting a
  Yuushya Modelling release other than the currently pinned `2.4.2`.
