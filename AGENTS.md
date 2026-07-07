# Baritone Agent Guidelines

## Project Overview
Baritone is a Minecraft pathfinding bot.

- **`26.1` branch**: Minecraft **26.1/26.1.1/26.1.2** (Java 25). Cross-version via mod metadata version ranges.
- **`26.2` branch**: Minecraft **26.2** (Chaos Cubed, Java 25). Forge 65.0.0, Fabric Loader 0.19.3+.

Cross-version compatibility within a minor version is achieved via mod metadata version ranges (no build-system multi-targeting). The project is a multi-source-set Gradle project using the **Unimined** plugin (not ForgeGradle or Fabric Loom).

## Build & Test
```bash
./gradlew build                    # Full build (main + api + launch + test)
./gradlew build --no-daemon        # If daemon causes issues
```

Build output goes to `dist/`. The relevant artifact is `baritone-standalone-fabric-*-dirty.jar`.

## Source Layout
```
src/
  api/          — Public API (baritone.api.*): interfaces, events, goals, commands, etc.
  main/         — Core implementation (baritone.*): pathfinder, behaviors, processes, utils
  launch/       — Mixins & launch (baritone.launch.*): MixinMinecraft, MixinLocalPlayer, etc.
  test/         — JUnit 4 unit tests
  schematica_api/ — Optional Schematica integration
```

Subprojects: `fabric/`, `forge/`, `tweaker/` (included via settings.gradle).

## Code Style
- **4-space indentation**, no tabs. Continuation indent is 8 spaces.
- **Naming**: PascalCase classes, `I` prefix for interfaces, camelCase methods/fields, UPPER_SNAKE_CASE constants.
- **Imports**: grouped and alphabetically sorted: baritone → com.mojang → net.minecraft → org → java.
- **License header**: Every `.java` file starts with the LGPL-3.0 block comment.
- **Javadoc**: Class-level `@author`/`@since` on public API classes. Method Javadoc for complex methods.
- **Inline comments**: `//` style for tricky logic. Avoid extraneous comments.
- **Visibility**: Internal classes are package-private; only API surface is `public`.
- **No tabs, no trailing whitespace, no CRLF**.

## Key API Concepts
- **`IBaritone`**: main interface, accessed via `BaritoneAPI.getProvider()`.
- **`IPathingBehavior`**: controls pathfinding via `PathingBehavior` (extends `Behavior`).
- **`IGameEventHandler`**: event bus. MixinMinecraft fires `TickEvent`, `WorldEvent`, `PlayerUpdateEvent`.
- **`IInputOverrideHandler`**: manages forced input states (sprint, sneak, movement keys).
- **`IPlayerContext`**: wraps `Minecraft` player/level access. Has `player()`, `world()`, `playerFeet()`, etc.
- **Behaviors**: `PathingBehavior`, `LookBehavior`, `InventoryBehavior`, `ElytraBehavior` extend `Behavior` and override `onTick(TickEvent)`.

## Mixin Patterns
- **Package**: `baritone.launch.mixins`, naming: `Mixin{TargetClass}`.
- **Config**: `src/launch/resources/mixins.baritone.json` lists all mixins under `"client"`.
- **Registry**: `BaritoneMixinConnector` → `Mixins.addConfiguration("mixins.baritone.json")`.
- Access private fields with `@Shadow`, add fields with `@Unique`.
- Use `@Accessor`/`@Invoker` interfaces in `baritone.utils.accessor` package.
- Injection patterns: `@At("HEAD")`, `@At("RETURN")`, `@At(value = "FIELD", ...)`, `@At(value = "INVOKE", ...)`.
- **Mixin target verification**: Mixin `target=` / `method=` strings are NOT compile-checked. Always verify against the actual jar with `javap` after each MC minor version bump.

## Pathfinding Architecture
- **PathingBehavior**: manages path lifecycle. Fields: `current` (PathExecutor), `next` (IPath), `inProgress` (AbstractNodeCostSearch), `goal` (Goal).
- **PathExecutor**: runs a path segment, returns `onTick()` status. Calls `shouldSprintNextTick()` which reads from `IInputOverrideHandler`.
- **PathingControlManager**: coordinates `process` (e.g., MineProcess, BuilderProcess) which produce `PathingCommand`.
- **AbstractNodeCostSearch**: runs A* on a background thread (`Baritone.getExecutor()`). Has `cancelRequested` flag and `calculate()` method.
- **Locking**: `pathPlanLock` for current/next/goal, `pathCalcLock` for inProgress. Always synchronize in that order (pathPlanLock → pathCalcLock) to avoid deadlocks.

## Common Pitfalls
- **World transitions**: `PathingBehavior` has no `onWorldEvent` handler by default. If pathing breaks after world exit/re-enter, the stale `inProgress` reference blocks new path calculations. Call `secretInternalSegmentCancel()` (which now nulls `inProgress`) on `WorldEvent.PRE`.
- **Sprint**: `shouldSprintNextTick()` in `PathExecutor` must NOT call `setInputForceState(Input.SPRINT, false)` — that clears sprint before `PlayerMovementInput` reads it in the same tick. The `InputOverrideHandler` stores forced states; `clearAllKeys()` in `Movement.update()` resets them at the correct time.
- **Rendering**: MC 26.1.2+ uses Gizmos API (world coordinates), not camera-relative coordinates with PoseStack. `Gizmos.line()` ignores the PoseStack. For the PoseStack-based render path (`drawPath(PoseStack, ...)`), coordinates must still be camera-relative (subtract `renderPosX/Y/Z`).
- **Voxel shapes**: `VoxelShape.bounds()` returns a standard AABB(0,0,0,1,1,1). No centered-bounds compensation needed. Use `move(pos.getX(), pos.getY(), pos.getZ())` — no offset.
- **PlayerMovementInput**: Must extend `net.minecraft.client.player.Input` (MC 26.1.2+). The `tick()` method signature must match `ClientInput.tick()`.
- **MC 26.2 API changes** (when porting from 26.1):
  - `net.minecraft.util.Tuple` removed → use `baritone.api.utils.Pair` (add `getA()`/`getB()` alongside `first()`/`second()`)
  - `Minecraft.gui.getChat()` (MC 26.1) → `Minecraft.gui.hud.getChat()` (MC 26.2)
  - `Minecraft.getToastManager()` → `Minecraft.gui.toastManager()`
  - `Minecraft.setScreen(s)` → `Minecraft.gui.setScreen(s)`
  - `Minecraft.screen` (field, MC 26.1) → `Minecraft.gui.screen()` (method, MC 26.2)
  - `EntityType.FIREWORK_ROCKET` → `EntityTypes.FIREWORK_ROCKET`
  - `BlockPos.getCenter()` → `Vec3.atCenterOf(BlockPos)`
  - `LevelChunkSection.SECTION_SIZE` → inline `16` (constant removed)
  - `LevelRenderer.setBlocksDirty(x,y,z,x,y,z)` removed → `clearVisibleSections()` + `resetLevelRenderData()`
  - `Blocks.WHITE_SHULKER_BOX`..`BLACK` → `Blocks.DYED_SHULKER_BOX.forEach(...)`
  - `Blocks.WHITE_BED`..`BLACK` → `Blocks.BED.forEach(...)`
  - `LevelRenderer.renderLevel()` renamed to `LevelRenderer.render()`
  - `LivingEntity.getYRot()` moved to `Entity.getYRot()` (parent class)
- **Logging**: Use `RateLimitedLogger` (5s cooldown per key) for per-frame debug messages. Never `System.out.println` in render paths.

## Testing
- JUnit 4 (`@Test`, `Assert.*`).
- No mocking framework — tests are pure logic unit tests.
- Run with `./gradlew test`.
- Test classes in `src/test/java/baritone/` mirror the package structure.

## Git Workflow
- Commit messages are concise, matching repo style (e.g., "fix pathing reset on world change").
- Tag releases with `vX.Y.Z`.
- Never commit secrets or generated jars.
