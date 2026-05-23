# AstroDrill System Diagrams

This file collects every diagram: ERD, sequence diagrams, gameplay flowchart, screen state diagram, and one class diagram per design pattern. GitHub renders the diagram natively.

Pre-rendered images live in [`docs/img/`](./img/).

## Contents

- [1. Entity Relationship Diagram (ERD)](#1-entity-relationship-diagram-erd)
- [2. Sequence diagrams](#2-sequence-diagrams)
  - [2.1 Register](#21-register)
  - [2.2 Login](#22-login)
  - [2.3 Save (F5 in PlayScreen)](#23-save-f5-in-playscreen)
  - [2.4 Load (Continue from main menu)](#24-load-continue-from-main-menu)
  - [2.5 Leaderboard](#25-leaderboard)
- [3. Gameplay flowchart](#3-gameplay-flowchart)
- [4. Screen state diagram](#4-screen-state-diagram)
- [5. Design pattern class diagrams](#5-design-pattern-class-diagrams)
  - [5.1 Singleton (`GameManager`)](#51-singleton-gamemanager)
  - [5.2 Object Pool (`Pool<Block>`)](#52-object-pool-poolblock)
  - [5.3 Observer (`VaultObserver`, `InventoryObserver`)](#53-observer-vaultobserver-inventoryobserver)
  - [5.4 Factory Method (`MachineFactory`)](#54-factory-method-machinefactory)
  - [5.5 Strategy (`EngineStrategy`)](#55-strategy-enginestrategy)
  - [5.6 State (`FlightState`)](#56-state-flightstate)
- [Rendering images](#rendering-images)

## 1. Entity Relationship Diagram (ERD)

[`./img/erd.png`](./img/erd.png)

Three normalised tables backing the Spring Boot persistence layer. `Player` is the parent; each player owns many `SaveState` rows (one per save slot, currently one) and a single `Leaderboard` row recording their best run.

```mermaid
erDiagram
    PLAYER ||--o{ SAVE_STATE : "has"
    PLAYER ||--o| LEADERBOARD : "ranked in"

    PLAYER {
        bigint id PK
        varchar username UK "NOT NULL, UNIQUE"
        varchar password_hash "NOT NULL, BCrypt"
    }
    SAVE_STATE {
        bigint id PK
        bigint player_id FK
        int credits
        varchar current_planet
        text data "JSON blob, world delta"
        timestamp updated_at
    }
    LEADERBOARD {
        bigint id PK
        bigint player_id FK "OneToOne"
        int max_depth_mined "high-water mark"
        bigint fastest_launch_time "low-water mark"
    }
```

Key points to call out on the slide:

- `Player.username` carries a `UNIQUE` constraint, enforced both at the JDBC level and by `AuthService.register()`.
- `SaveState.data` is a `TEXT` column storing the full world delta as JSON (`SaveStateSerializer.GameSaveDto`, schema version 1).
- `Leaderboard` rows are never inserted with worse values; `GameService.saveProgress()` takes the max of `maxDepthMined` and the min non-zero of `fastestLaunchTime` on every save.

## 2. Sequence diagrams

Common participants across every sequence diagram:

- `Game` (the LibGDX client, usually triggered by a screen or HUD event)
- `BackendClient` (`network/BackendClient.java`, the HTTP wrapper)
- `GameController` (`@RestController` at `/api/game`)
- `AuthService` or `GameService` (the orchestration layer)
- `Repository` (Spring Data JPA, one per entity)
- `PostgreSQL` (the database)

All HTTP calls from the game are asynchronous: `BackendClient` issues the request via `Gdx.net.sendHttpRequest`, and the callback is marshalled back onto the LibGDX render thread with `Gdx.app.postRunnable()`.

### 2.1 Register

[`./img/sequence-register.png`](./img/sequence-register.png)

```mermaid
sequenceDiagram
    actor User
    participant LoginScreen
    participant BackendClient
    participant GameController
    participant AuthService
    participant PlayerRepo as PlayerRepository
    participant DB as PostgreSQL

    User->>LoginScreen: click Register
    LoginScreen->>BackendClient: register(RegisterRequest, cb)
    BackendClient->>GameController: POST /api/game/register
    GameController->>AuthService: register(request)
    AuthService->>PlayerRepo: findByUsername(username)
    PlayerRepo->>DB: SELECT
    DB-->>PlayerRepo: Optional<Player>
    PlayerRepo-->>AuthService: result

    alt username taken
        AuthService-->>GameController: throw UsernameTakenException
        GameController-->>BackendClient: 409 Conflict
    else available
        AuthService->>AuthService: bcrypt(password)
        AuthService->>PlayerRepo: save(new Player)
        PlayerRepo->>DB: INSERT
        DB-->>PlayerRepo: generated id
        PlayerRepo-->>AuthService: Player
        AuthService-->>GameController: LoginResponse(id, username)
        GameController-->>BackendClient: 201 Created
    end

    BackendClient->>BackendClient: postRunnable(cb)
    BackendClient-->>LoginScreen: cb.onResult(response)
```

### 2.2 Login

[`./img/sequence-login.png`](./img/sequence-login.png)

```mermaid
sequenceDiagram
    actor User
    participant LoginScreen
    participant BackendClient
    participant GameController
    participant AuthService
    participant PlayerRepo as PlayerRepository
    participant DB as PostgreSQL

    User->>LoginScreen: click Login
    LoginScreen->>BackendClient: login(LoginRequest, cb)
    BackendClient->>GameController: POST /api/game/login
    GameController->>AuthService: login(request)
    AuthService->>PlayerRepo: findByUsername(username)
    PlayerRepo->>DB: SELECT
    DB-->>PlayerRepo: Optional<Player>
    PlayerRepo-->>AuthService: result

    alt missing or wrong password
        AuthService-->>GameController: throw InvalidCredentialsException
        GameController-->>BackendClient: 401 Unauthorized
    else match
        AuthService->>AuthService: bcrypt.matches(input, hash)
        AuthService-->>GameController: LoginResponse(id, username)
        GameController-->>BackendClient: 200 OK
    end

    BackendClient->>BackendClient: postRunnable(cb)
    BackendClient-->>LoginScreen: cb.onResult(response)
    LoginScreen->>GameManager: store currentPlayerId, changeScreen(MAIN_MENU)
```

### 2.3 Save (F5 in PlayScreen)

[`./img/sequence-save.png`](./img/sequence-save.png)

```mermaid
sequenceDiagram
    actor Player
    participant PlayScreen
    participant Serializer as SaveStateSerializer
    participant BackendClient
    participant GameController
    participant GameService
    participant SaveRepo as SaveStateRepository
    participant LbRepo as LeaderboardRepository
    participant DB as PostgreSQL

    Player->>PlayScreen: press F5
    PlayScreen->>PlayScreen: requestSave(silent=false)
    PlayScreen->>Serializer: snapshot(this, player, hub)
    Serializer-->>PlayScreen: JSON string
    PlayScreen->>BackendClient: save(SaveRequest, cb)
    BackendClient->>GameController: POST /api/game/save
    GameController->>GameService: saveProgress(req)

    GameService->>SaveRepo: findByPlayerId or new
    SaveRepo->>DB: SELECT
    DB-->>SaveRepo: Optional<SaveState>
    GameService->>SaveRepo: save(updated SaveState)
    SaveRepo->>DB: INSERT or UPDATE

    GameService->>LbRepo: findByPlayerId or new
    LbRepo->>DB: SELECT
    GameService->>LbRepo: save(max depth, min launch time)
    LbRepo->>DB: INSERT or UPDATE

    GameService-->>GameController: SaveResponse(updatedAt)
    GameController-->>BackendClient: 200 OK
    BackendClient->>BackendClient: postRunnable(cb)
    BackendClient-->>PlayScreen: cb.onSuccess
    PlayScreen->>PlayScreen: show "Saved." HUD toast
```

### 2.4 Load (Continue from main menu)

[`./img/sequence-load.png`](./img/sequence-load.png)

```mermaid
sequenceDiagram
    actor User
    participant MainMenu as MainMenuScreen
    participant BackendClient
    participant GameController
    participant GameService
    participant SaveRepo as SaveStateRepository
    participant DB as PostgreSQL
    participant GameManager
    participant PlayScreen

    User->>MainMenu: click Continue
    MainMenu->>BackendClient: load(playerId, cb)
    BackendClient->>GameController: GET /api/game/load/{playerId}
    GameController->>GameService: loadProgress(playerId)
    GameService->>SaveRepo: findByPlayerId(playerId)
    SaveRepo->>DB: SELECT
    DB-->>SaveRepo: Optional<SaveState>

    alt no save row
        GameService-->>GameController: throw SaveNotFoundException
        GameController-->>BackendClient: 404 Not Found
    else found
        GameService-->>GameController: LoadResponse(data, updatedAt)
        GameController-->>BackendClient: 200 OK
    end

    BackendClient->>BackendClient: postRunnable(cb)
    BackendClient-->>MainMenu: cb.onResult(response)
    MainMenu->>GameManager: pendingSaveBlob = data
    MainMenu->>GameManager: changeScreen(PLAY)
    GameManager->>PlayScreen: show()
    PlayScreen->>PlayScreen: regen world from seed, apply mined/placed deltas, restore vault and hub
```

### 2.5 Leaderboard

[`./img/sequence-leaderboard.png`](./img/sequence-leaderboard.png)

```mermaid
sequenceDiagram
    actor User
    participant LbScreen as LeaderboardScreen
    participant BackendClient
    participant GameController
    participant GameService
    participant LbRepo as LeaderboardRepository
    participant DB as PostgreSQL

    User->>LbScreen: open Leaderboard
    LbScreen->>BackendClient: getLeaderboard(cb)
    BackendClient->>GameController: GET /api/game/leaderboard
    GameController->>GameService: getTopLeaderboard()
    GameService->>LbRepo: findTop10ByOrderByMaxDepthMinedDesc()
    LbRepo->>DB: SELECT ... ORDER BY max_depth_mined DESC LIMIT 10
    DB-->>LbRepo: rows
    LbRepo-->>GameService: List<Leaderboard>
    GameService->>GameService: map to LeaderboardEntryDto
    GameService-->>GameController: List<DTO>
    GameController-->>BackendClient: 200 OK
    BackendClient->>BackendClient: postRunnable(cb)
    BackendClient-->>LbScreen: cb.onResult(entries)
    LbScreen->>LbScreen: render table
```

## 3. Gameplay flowchart

[`./img/gameplay-flowchart.png`](./img/gameplay-flowchart.png)

This flowchart traces a single player from app start to either a successful escape or a crash retry. It overlays both phases (mining and flight) so the cargo gate is visible as the bridge between them.

```mermaid
flowchart TD
    Start([Launch app]) --> Loading[LOADING screen]
    Loading --> Login[LOGIN screen]
    Login -->|success| Menu[MAIN_MENU]
    Menu -->|Continue| Load[Load save from backend]
    Menu -->|New Game| Spawn
    Menu -->|Leaderboard| Lb[LEADERBOARD]
    Lb --> Menu
    Load --> Spawn

    subgraph Mining ["PLAY screen (mining and factory)"]
      direction TB
      Spawn([Spawn at LanderHub]) --> Mine[Mine ore with drill]
      Mine --> Battery{Battery low?}
      Battery -->|yes| Return[Return to hub]
      Battery -->|no| Choice
      Return --> Recharge[Recharge and auto-bank vault]
      Recharge --> Choice
      Choice{Have machines<br/>to craft?}
      Choice -->|no| Mine
      Choice -->|yes| Craft[Craft machine in hub terminal]
      Craft --> Place[Place machine, wire power]
      Place --> Produce[Factory produces cargo:<br/>RocketFuel, HullPlating, CircuitBoard]
      Produce --> Gate{Cargo gate met?<br/>20 fuel + 10 plating + 5 board}
      Gate -->|no| Mine
      Gate -->|yes| Launch[Press L to launch]
    end

    Launch --> Flight

    subgraph FlightP ["FLIGHT screen"]
      direction TB
      Flight([Cargo consumed, rocket spawned]) --> Thrust[Apply thrust, steer]
      Thrust --> CheckAlt{Altitude >= 1000?}
      CheckAlt -->|yes| Win([WIN])
      CheckAlt -->|no| Fuel{Fuel = 0 and falling?}
      Fuel -->|yes| Crash([CRASH])
      Fuel -->|no| Thrust
    end

    Win -->|ENTER| Menu
    Crash -->|R| Spawn
```

## 4. Screen state diagram

[`./img/screen-state.png`](./img/screen-state.png)

`GameManager.changeScreen(ScreenType)` is the only legal way to move between screens. Every transition below corresponds to a single `changeScreen()` call somewhere in the codebase.

```mermaid
stateDiagram-v2
    [*] --> LOADING
    LOADING --> LOGIN : assets ready
    LOGIN --> MAIN_MENU : login success
    MAIN_MENU --> LEADERBOARD : view leaderboard
    LEADERBOARD --> MAIN_MENU : back
    MAIN_MENU --> PLAY : New Game or Continue
    PLAY --> MAIN_MENU : Save and Exit
    PLAY --> FLIGHT : press L (cargo gate met)
    FLIGHT --> PLAY : press R (abort or crash retry)
    FLIGHT --> MAIN_MENU : press ENTER after WIN

    state FLIGHT {
        [*] --> ACTIVE
        ACTIVE --> WIN : altitude >= 1000
        ACTIVE --> CRASH : fuel=0 AND velocity.y<=0 AND altitude<1000
        WIN --> [*]
        CRASH --> [*]
    }
```

## 5. Design pattern class diagrams

Six diagrams, one per pattern. Each one shows only the participating classes plus the methods that demonstrate the pattern so they read well in slides.

### 5.1 Singleton (`GameManager`)

[`./img/pattern-singleton.png`](./img/pattern-singleton.png)

`GameManager` owns the game-wide singletons (the LibGDX `Game` reference, the `globalVault`, the current player id) and exposes a private constructor with `getInstance()`. Every screen calls `GameManager.getInstance()` rather than holding its own copy.

```mermaid
classDiagram
    class GameManager {
        -static GameManager instance
        -Game game
        -Map~ItemType, Integer~ globalVault
        -List~VaultObserver~ vaultObservers
        -Long currentPlayerId
        -String currentUsername
        -String pendingSaveBlob
        -GameManager()
        +static getInstance() GameManager
        +changeScreen(ScreenType) void
        +addItem(ItemType, int) void
        +hasItems(Map) boolean
        +consumeItems(Map) void
        +addVaultObserver(VaultObserver) void
        +notifyVaultObservers() void
    }
```

### 5.2 Object Pool (`Pool<Block>`)

[`./img/pattern-object-pool.png`](./img/pattern-object-pool.png)

`PlayScreen` allocates `Block` instances through a LibGDX `Pool<Block>` so mining and refilling tiles never trigger fresh allocations. `Block.reset()` is the hook that scrubs per-instance state (including `isPlayerPlaced`) when the pool reclaims an object.

```mermaid
classDiagram
    class Pool~T~ {
        <<library>>
        +obtain() T
        +free(T) void
    }
    class BlockPool {
        +newObject() Block
    }
    class Block {
        +BlockType type
        +boolean isDestructible
        +boolean isPlayerPlaced
        +int col
        +int row
        +reset() void
    }
    class PlayScreen {
        -Pool~Block~ blockPool
        +mineBlockAt(col, row) Block
        +placeBlockAt(col, row, type) void
    }

    Pool <|-- BlockPool
    BlockPool ..> Block : creates
    PlayScreen --> BlockPool : uses
```

### 5.3 Observer (`VaultObserver`, `InventoryObserver`)

[`./img/pattern-observer.png`](./img/pattern-observer.png)

`Hud` registers as a `VaultObserver` against `GameManager` and as an `InventoryObserver` against `Player`. Whenever the vault or the player's inventory changes, the subjects iterate their listener list and call `onVaultUpdated()` or `onInventoryUpdated()` so the HUD repaints without polling.

```mermaid
classDiagram
    class VaultObserver {
        <<interface>>
        +onVaultUpdated(Map vault) void
    }
    class InventoryObserver {
        <<interface>>
        +onInventoryUpdated(Map inv) void
    }
    class GameManager {
        -List~VaultObserver~ vaultObservers
        +addVaultObserver(VaultObserver) void
        +notifyVaultObservers() void
    }
    class Player {
        -List~InventoryObserver~ inventoryObservers
        +addInventoryObserver(InventoryObserver) void
        +notifyInventoryObservers() void
    }
    class Hud {
        +onVaultUpdated(Map) void
        +onInventoryUpdated(Map) void
    }

    VaultObserver <|.. Hud
    InventoryObserver <|.. Hud
    GameManager --> VaultObserver : notifies
    Player --> InventoryObserver : notifies
```

### 5.4 Factory Method (`MachineFactory`)

[`./img/pattern-factory.png`](./img/pattern-factory.png)

`MachineFactory.createMachine(String type, ...)` returns a concrete `Machine` subclass based on a string key. Save restoration and player placement both go through this single entry point so the rest of the codebase never instantiates machine subclasses directly.

```mermaid
classDiagram
    class Machine {
        <<abstract>>
        #float x
        #float y
        #boolean userDisabled
        +update(delta) void
        +hasAdjacentPower() boolean
        +getMachineType() String
    }
    class MachineFactory {
        +static createMachine(String type, float x, float y) Machine
    }
    class AutoMiner
    class CoalGenerator
    class IronSmelter
    class CopperSmelter
    class GoldSmelter
    class GearAssembler
    class WireAssembler
    class Refinery
    class CircuitFab
    class FuelMixer
    class HullPress

    Machine <|-- AutoMiner
    Machine <|-- CoalGenerator
    Machine <|-- IronSmelter
    Machine <|-- CopperSmelter
    Machine <|-- GoldSmelter
    Machine <|-- GearAssembler
    Machine <|-- WireAssembler
    Machine <|-- Refinery
    Machine <|-- CircuitFab
    Machine <|-- FuelMixer
    Machine <|-- HullPress
    MachineFactory ..> Machine : returns
```

### 5.5 Strategy (`EngineStrategy`)

[`./img/pattern-strategy.png`](./img/pattern-strategy.png)

`Rocket` holds a reference to an `EngineStrategy` and delegates thrust and fuel-burn questions to whichever implementation is currently plugged in. Pressing `E` in `FlightScreen` swaps between `ChemicalEngine` (high thrust, high burn) and `IonEngine` (low thrust, low burn) without altering `Rocket` itself.

```mermaid
classDiagram
    class EngineStrategy {
        <<interface>>
        +getThrust() float
        +getFuelBurnRate() float
        +getName() String
    }
    class ChemicalEngine {
        +getThrust() float
        +getFuelBurnRate() float
        +getName() String
    }
    class IonEngine {
        +getThrust() float
        +getFuelBurnRate() float
        +getName() String
    }
    class Rocket {
        -EngineStrategy engine
        -float fuel
        +setEngine(EngineStrategy) void
        +applyThrust(delta) void
    }

    EngineStrategy <|.. ChemicalEngine
    EngineStrategy <|.. IonEngine
    Rocket o-- EngineStrategy : current strategy
```

### 5.6 State (`FlightState`)

[`./img/pattern-state.png`](./img/pattern-state.png)

`FlightScreen` runs a three-state machine (`ACTIVE`, `WIN`, `CRASH`). Each update tick checks transition conditions; input handling branches on the current state so thrust and steering only apply in `ACTIVE`, and `ENTER` only works in `WIN`.

```mermaid
classDiagram
    class FlightScreen {
        -FlightState state
        +update(delta) void
        +handleInput() void
        -checkWin() void
        -checkCrash() void
    }
    class FlightState {
        <<enumeration>>
        ACTIVE
        WIN
        CRASH
    }
    class Rocket {
        +float maxAltitude
        +float fuel
        +Vector2 velocity
    }

    FlightScreen --> FlightState : current state
    FlightScreen --> Rocket : observes
```
