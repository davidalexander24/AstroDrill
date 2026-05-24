# AstroDrill Documentation

AstroDrill is a 2D mining-to-spaceflight game built with LibGDX, backed by a Spring Boot REST API for cloud saves and a global leaderboard. The gameplay loop runs in three phases: mine ores from a 100×300 procedural grid, refine and assemble them through a power-gated factory chain at the lander hub, then burn the resulting cargo into orbit to win. This document is the stable developer-facing reference.

## Contents

- [Repository layout](#repository-layout)
- [Getting started](#getting-started)
- [Architecture overview](#architecture-overview)
- [Diagrams](#diagrams)
- [Mining phase (`PlayScreen`)](#mining-phase-playscreen)
- [Crafting and upgrades](#crafting-and-upgrades)
- [Factory and power grid](#factory-and-power-grid)
- [Flight phase (`FlightScreen`)](#flight-phase-flightscreen)
- [Backend API](#backend-api)
- [Testing](#testing)
- [Contributor recipes](#contributor-recipes)
- [Where to look first](#where-to-look-first)

## Repository layout

The repo is two independently-buildable applications, orchestrated by a small root `package.json`:

- **`Game/`**: LibGDX game (Java 17, Gradle). Subprojects:
  - `core`: shared game logic (the bulk of the code)
  - `lwjgl3`: desktop launcher (primary target)
  - `html`: GWT/WebGL build
- **`Backend/`**: Spring Boot 4 REST API (Java 17, Maven). Persists state to PostgreSQL.
- **Root `package.json`**: launcher only, uses `concurrently` to run both apps in dev.

## Getting started

### Prerequisites

- Java 17
- PostgreSQL reachable at `localhost:5432/astrodrill_db` (credentials in `Backend/src/main/resources/application.properties`, default `postgres`/`123`). JPA runs with `ddl-auto=update` so the schema auto-migrates on boot.
- Node only if you want the root `npm` launcher (it has no JS code, just `concurrently`).

### Running from the repo root

```bash
npm run backend   # Spring Boot via Maven wrapper (port 8080)
npm run game      # LWJGL3 desktop launcher
npm run dev       # both, color-tagged BACKEND/GAME
```

### Lower-level commands

Backend (from `Backend/`):

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw test -Dtest=BackendApplicationTests#methodName
./mvnw package
```

Game (from `Game/`):

```bash
./gradlew lwjgl3:run                   # run desktop client
./gradlew lwjgl3:jar                   # cross-platform jar -> lwjgl3/build/libs/
./gradlew lwjgl3:jarWin                # Windows-only jar (smaller)
./gradlew html:superDev                # GWT dev server at localhost:8080/html
./gradlew html:dist                    # GWT production build -> html/build/dist
./gradlew test
./gradlew core:clean                   # per-subproject tasks via `name:` prefix
```

## Architecture overview

### Design patterns

| Pattern | Where | Role |
| --- | --- | --- |
| Singleton | `GameManager` | Holds the LibGDX `Game` reference, the global vault, vault observers, and the current player id. |
| Object Pool | `Pool<Block>` in `PlayScreen` | Reuses `Block` instances across the 100×300 grid; resets `isPlayerPlaced` on return. |
| Observer | `InventoryObserver`, `VaultObserver` | `Hud` listens for player-inventory and global-vault updates. |
| Factory Method | `MachineFactory` | Instantiates `Machine` subclasses by string key. |
| Strategy | `EngineStrategy` (`ChemicalEngine`, `IonEngine`) | Rocket swaps engines at runtime; each strategy supplies its own thrust and fuel burn rate. |
| State | LibGDX `Screen` driven by `GameManager` | Phase machine: `LOADING`, `MAIN_MENU`, `PLAY`, `FLIGHT` (plus `LOGIN`, `LEADERBOARD`). |

### Phase machine

`GameManager` is the only legal route between screens. Call `gm.changeScreen(ScreenType.PLAY)`, never `game.setScreen()` directly. This keeps screen lifecycle hooks (resource consumption on entry, music cleanup on exit) in one place.

### The two inventories

Two inventories coexist; they are not interchangeable.

- **`Player.inventory`**: personal, carried in the field, lost on death or respawn.
- **`GameManager.globalVault`**: colony-wide, persistent, fed by auto-banking when the player enters the LanderHub safe zone, and consumed by crafting and upgrades.

`Player.bankInventoryToVault()` maps mined `BlockType` to raw `ItemType` (for example `IRON_ORE` to `RAW_IRON`) at the moment of transfer. When adding a new ore, this mapping is the easiest place to forget.

There is also a third bucket, `Player.machineInventory`, for crafted machine items (separate from blocks). `Player.rebuildHotbar()` composes the 9-slot hotbar each frame as: blocks, then machine items, then `DECONSTRUCT_TOOL` (always slot 9).

## Diagrams

The full diagram set lives in [`docs/diagrams.md`](./diagrams.md) (renders natively in GitHub) and as pre-rendered images in [`docs/img/`](./img/). The set covers:

- **ERD** of the three PostgreSQL tables (`Player`, `SaveState`, `Leaderboard`).
- **Sequence diagrams** for the five REST endpoints (`/register`, `/login`, `/save`, `/load/{playerId}`, `/leaderboard`).
- **Gameplay flowchart** tracing the mining to craft to launch loop, plus win and crash branches.
- **Screen state diagram** for the six `ScreenType` phases driven by `GameManager.changeScreen()`, with the nested `FlightState` machine.
- **Six class diagrams**, one per implemented design pattern (Singleton, Object Pool, Observer, Factory Method, Strategy, State).

## Mining phase (`PlayScreen`)

### World shape

- 100 columns × 300 rows. Camera viewport shows roughly 30 blocks across; X is clamped to world bounds with a zoom-aware margin.
- **Strata** are probability-based (no Perlin):
  - Row 0 (surface): 100% `DIRT`
  - Crust (rows 1 to 49): mostly Dirt with Stone, Coal, Copper, Iron
  - Mantle (50 to 149): mostly Basalt with Stone, Silicon, Gold
  - Core (150 to 298): mostly Obsidian with Uranium
  - Row 299: 100% `BEDROCK` (impenetrable floor)
- The **LanderHub** sits on indestructible `BEDROCK` at columns 47 to 53, row 0. The player always spawns and respawns at `(x=53, y=1)`.
- World generation uses `RandomXS128(seed)` so it is deterministic per seed; save and restore relies on this.

Exact probabilities and ore yields drift; see `memory.md` for current numbers.

### Block invariants

`Block` carries two flags that gate every block interaction: `isDestructible` (false for `BEDROCK`) and `isPlayerPlaced` (true for blocks the player placed, false for natural terrain). The following three methods share these guarantees. Audit all three together if you change any rule.

- `mineBlockAt` (player drill and AutoMiner) refuses any block with `isPlayerPlaced=true`.
- `deconstructAt` refuses any block with `isPlayerPlaced=false`. It never breaks natural terrain.
- `placeBlockAt` rejects placement that overlaps the player's AABB, sits on bedrock, or fills an occupied cell.
- All player interactions are gated by `Player.INTERACT_RADIUS = 6f`.

### Player controller

- Custom AABB collision and custom gravity (not Box2D; Box2D is reserved for flight).
- Horizontal base speed scales with `wheelTier`.
- **Jetpack:** `W` or `UP` applies thrust, drains battery. Vertical velocity clamped between `MAX_FALL_SPEED` and `MAX_RISE_SPEED`.
- **Battery:** depletion triggers `respawn()`. Real-time HUD percentage with a low-battery warning. Capacity scales with `batteryTier`.
- **Mining cadence:** `digTimer` scales inversely with movement speed so the drill always stays slightly ahead of walking. Strength upgrades unlock layers but do not change mining speed.
- **Drill strength gating:** `Player.drillStrength` (1 to 3) gates which strata can be mined. Mining a too-hard block silently fails and shows a throttled HUD popup.

### LanderHub and hub terminal

- Entering `LanderHub.SAFE_ZONE_RADIUS` recharges the battery and auto-banks the player's inventory into the global vault.
- The hub terminal is a tabbed Scene2D UI (Upgrades, Crafting, Launch) wired through an `InputMultiplexer` so keyboard movement keeps working while the menu is open. Do not refactor this into a modal dialog.
- The Launch tab shows live readouts of the rocket cargo requirements; pressing the in-tab `LAUNCH` button or the `L` key transitions to the flight screen when the gate is satisfied.

## Crafting and upgrades

### Data shape

- **`MachineRecipe`** (`com.david.astrodrill.crafting`) bundles the crafting cost (`Map<ItemType, Integer>`), the `MachineFactory` key, the resulting `ItemType`, the `requiredHubTier`, and a human-readable description shown in the `?` popup.
- **`UpgradeDefinition.ALL`** is a static array; each entry has a per-tier cost map of the same shape.
- Crafting deducts from `globalVault` (via `GameManager.consumeItems`) and adds to `Player.machineInventory`. Upgrades also go through `consumeItems` against the vault.

### Upgrade tracks (5)

| Track | Effect | Max tier |
| --- | --- | --- |
| Drill Strength | Unlocks Mantle (Lv 2) and Core (Lv 3) strata | 3 |
| Battery Cap | Scales `maxBattery`, refills on upgrade | 4 |
| Jetpack | Scales `jetpackThrust` | 3 |
| Wheel Speed | Scales horizontal `speed` | 3 |
| Hub Tier | Unlocks tier-2 and tier-3 manufacturing recipes | 3 |

Per-tier material costs live in `UpgradeDefinition.ALL`. Refer to code (or `memory.md`) rather than memorising them here.

### Machines (11)

| # | Recipe | Hub Tier |
| --- | --- | --- |
| 1 | Iron Smelter | 1 |
| 2 | Copper Smelter | 1 |
| 3 | Coal Generator | 1 |
| 4 | Gear Assembler | 1 |
| 5 | Wire Assembler | 1 |
| 6 | Auto Miner | 1 |
| 7 | Gold Smelter | 1 |
| 8 | Refinery | 2 |
| 9 | Circuit Fab | 2 |
| 10 | Fuel Mixer | 3 |
| 11 | Hull Press | 3 |

The recipe order is intentional. Early recipes break the bootstrap deadlock by using only hand-minable ore. Check `memory.md` before reordering. Tier-locked recipes still render in the Manufacturing tab as locked so progression is visible.

## Factory and power grid

### Power model

Power is adjacency-based, not BFS. `Machine.hasAdjacentPower()` checks the four cardinal neighbours for an active `CoalGenerator`. The generator self-powers while burning.

### Demand-based burn

Each non-generator machine sets `wantsPower` per frame whenever it has input materials or is mid-process. `CoalGenerator` counts adjacent `wantsPower` machines that frame and burns coal proportionally: two active neighbours burn coal twice as fast as one. With zero demand the generator does not burn coal and renders as unlit.

To make this work, `PlayScreen` updates machines in two phases per frame: non-generators first (so they populate `wantsPower`), generators second.

### Other machine controls

- **Right-click toggle:** Right-clicking a placed machine toggles `Machine.userDisabled`. Disabled machines early-return from `update()` (no power draw, no production) and render with a red border.
- **AutoMiner** mines the block directly below it through `PlayScreen.mineBlockForMachine`, bypassing player drill-strength gating but respecting block invariants.

### Adding a new machine

1. Extend `Machine` with input/output behaviour and a 2-letter symbol.
2. Register the class in `MachineFactory.createMachine`.
3. Add a new `ItemType` for the machine item.
4. Add a `MachineRecipe` entry (cost, hub tier, description).
5. Add the 2-letter symbol to both the hotbar render and the in-world block render so it is recognisable in both places.

## Flight phase (`FlightScreen`)

- Physics is Box2D-only. Mining uses custom AABB; flight uses Box2D. Do not mix the two.
- A static launchpad floor and a dynamic rocket body. The rocket sprite is scaled up to align with the landing platform.
- **Cargo gate on entry:** `FlightScreen` consumes `20 ROCKET_FUEL + 10 HULL_PLATING + 5 CIRCUIT_BOARD` from the global vault and sets `rocket.fuel = 100f`. Cargo stays consumed even if the player aborts back to mining.
- **Engine swap (Strategy):** Press `E` to swap between `ChemicalEngine` (high thrust, high burn) and `IonEngine` (low thrust, low burn). Disabled after WIN or CRASH.
- **Thrust ramp:** Holding thrust spools up over roughly 1 second on an ease-in curve; releasing decays at 2× speed. HUD shows live `Thrust: N%`.
- **Audio:** `sounds/Rocket.wav` is streamed via `Gdx.audio.Music`, loops while thrusting, fades out 0.5s after release, and is paused (not stopped) so resumption picks up seamlessly. Screen transitions dispose the music to prevent leaks.
- **State machine:** `FlightState.ACTIVE` transitions to `WIN` (altitude ≥ 1000) or `CRASH` (fuel = 0, falling, altitude < 1000).
- **Controls:** `W` or `UP` thrust, `A` or `D` (or `LEFT`/`RIGHT`) torque, `E` engine swap, `R` abort to mining, `ENTER` to main menu after a win.
- A fresh `PlayScreen` is created on re-entry from flight. The global vault, upgrade tiers, and player id all survive via `GameManager`.

## Backend API

Base path: `/api/game`. CORS is wide open (`*`) via `CorsFilter`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/register` | Create an account. Returns 201 or 409 on conflict. |
| `POST` | `/login` | Authenticate. Returns 200 with player id or 401. |
| `POST` | `/save` | Full save blob; player id in body. |
| `GET` | `/load/{playerId}` | Retrieve save blob, 404 if none. |
| `GET` | `/leaderboard` | Returns `LeaderboardEntryDto` rows (no `Player` entity leaks). |

All errors flow through `GlobalExceptionHandler` and return `{error, message}` JSON. Request DTOs use `@Valid` plus Jakarta bean validation.

### Auth model

Passwords are BCrypt-hashed via `spring-security-crypto` (standalone, no `SecurityFilterChain`). The game client holds `currentPlayerId` in `GameManager` after login and sends it as a path parameter or body field. There is no JWT or session, so a malicious client could spoof another user's id. This is an accepted limitation for the hobby/single-player scope.

### Save format

A save is a single JSON blob in `SaveState.data` (`TEXT` column) plus `updatedAt` and denormalised `credits` and `currentPlanet` for cheap listing. `LeaderboardEntry` records `maxDepthMined` and `fastestLaunchTime`; these are preserved on save (never overwritten with a worse value).

`SaveStateSerializer.GameSaveDto` (schema version 1) contains:

- **Player:** position, batteries, tier levels, active hotbar slot, block inventory, machine inventory, `playtimeMs`, `hasLaunchedSuccessfully`.
- **`globalVault`:** `Map<ItemType, Integer>` (enum keys serialised as strings).
- **Hub:** position, size, tier.
- **World:** a delta against the deterministic seed. `seed` (long), plus `minedCells: [[col, row], ...]`, plus `placedBlocks: [{col, row, type}, ...]`, plus `machines: [{type, x, y, processTimer, userDisabled}, ...]`. Typical payload is under 5 KB.

`PlayScreen.generateWorld(long seed)` and `SaveStateSerializer.rollOriginalType()` must stay in lockstep; they encode the same strata probability table. Drift between them is the most likely source of save-corruption bugs.

### Save triggers and restoration

- Manual save: `F5` in `PlayScreen`, with a HUD toast for success or failure.
- Save & Exit: the HUD button fires `requestSave(silent, onSuccess)` with a callback that defers the screen transition until the save completes, preventing stale-save races.
- On `FlightScreen` WIN: `hasLaunchedSuccessfully` is set on the player and a save is fired (once, guarded by `winReported`) so `fastestLaunchTime` reaches the leaderboard.
- Restoration order in `PlayScreen.show()`: regen world from seed, remove mined cells, add placed blocks (`isPlayerPlaced=true`), recreate machines via `MachineFactory`, restore player (including `playtimeMs` and `hasLaunchedSuccessfully`), hub, and global vault. All mutations go through the normal codepaths so block invariants hold.

## Testing

- **Game:** `./gradlew test` runs the JUnit suite.
- **Backend:** `./mvnw test` runs the full suite. Test profile uses H2 in PostgreSQL-compatibility mode (`Backend/src/test/resources/application.properties`).
- Backend coverage spans authentication (register/login happy paths, 401 and 409 conflicts, validation), save/load roundtrips with 404 paths, high-score preservation, and leaderboard ordering and DTO shape.

## Contributor recipes

### Adding a new ore

1. Add a `BlockType` constant and decide which strata it appears in (update the probability table in `PlayScreen.generateWorld`, and the matching roll in `SaveStateSerializer.rollOriginalType()`).
2. Add a colour or render entry so it shows up in-world.
3. Decide its required drill-strength tier in `Block.requiredStrength`.
4. Add the matching raw `ItemType` and extend `Player.bankInventoryToVault()` so the ore banks into the right slot.
5. If anything refines it, add the smelter or processor as a new machine (see below).

### Adding a new machine

1. Extend `Machine` with input/output behaviour, processing time, and a 2-letter symbol.
2. Register the class in `MachineFactory.createMachine`.
3. Add a new `ItemType` for the machine item.
4. Add a `MachineRecipe` (cost, result item type, `requiredHubTier`, description).
5. Add the 2-letter symbol to both the hotbar render and the in-world block render.
6. If the machine consumes power, it does so automatically by extending `Machine`, no extra wiring required. If it generates power, model it on `CoalGenerator`.

## Deployment

### Docker Compose (recommended)

`Backend/Dockerfile` is a multi-stage build: Maven 3.9 + JDK 17 for compilation, JRE 17 Alpine for the runtime image. `Backend/docker-compose.yml` defines two services:

- `postgres` (PostgreSQL 16 Alpine, persistent volume, no host port)
- `backend` (Spring Boot, bound to `127.0.0.1:8081`)

Steps:

1. Create `Backend/.env` (gitignored) with `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `POSTGRES_DB`.
2. `cd Backend && docker compose up -d --build`
3. Expose port 8081 over HTTPS using a reverse proxy, Tailscale Funnel, or similar.
4. Update `BackendClient.baseUrl` in `Game/core/.../network/BackendClient.java` to point at your public URL.
5. Rebuild the GWT client: `cd Game && ./gradlew html:dist`

### Railway (alternative)

`Backend/nixpacks.toml` configures a Railway deploy. Set `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, and `PORT` as Railway env vars. Update `BackendClient.baseUrl` to the Railway-provided URL and rebuild the GWT dist.

### Serialization note

The game client uses libGDX `JsonReader`/`JsonValue` for parsing and manual `StringBuilder` writers for serialization. This avoids all reflection, which is required for GWT compatibility (GWT's reflection emulation does not support wrapper types like `java.lang.Long`). Gson was removed from the client for this reason.

## Where to look first

- `Game/core/src/main/java/com/david/astrodrill/GameManager.java`: singleton, phase machine, vault, observers.
- `.../screen/PlayScreen.java`: world generation, mining, placing, deconstructing, machine update loop.
- `.../screen/FlightScreen.java`: Box2D, rocket, engine strategy swap, win/crash state machine.
- `.../machine/Machine.java` and `.../machine/MachineFactory.java`: base class and the dispatch.
- `.../crafting/MachineRecipe.java` and `.../crafting/UpgradeDefinition.java`: crafting data.
- `.../network/BackendClient.java` and `.../network/SaveStateSerializer.java`: REST client and save schema.
- `Backend/src/main/java/...`: Spring Boot controllers, services, and JPA entities.
