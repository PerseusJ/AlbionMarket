# AlbionMarket — Production Development Roadmap

> **Project**: AlbionMarket (Spigot/Paper 1.21+ Plugin)  
> **Artifact ID**: `com.perseus.albionmarket`  
> **Target API**: Spigot 1.21.10-R0.1-SNAPSHOT (Java 21)  
> **Hard Dependencies**: Vault  
> **Soft Dependencies**: Citizens, PlaceholderAPI  
> **Author**: PerseusJ  
> **Document Version**: 2.0 — June 2026 (Semantic Versioning Structure)

---

## Table of Contents

- [Executive Summary](#executive-summary)
- [Architectural Decisions Registry](#architectural-decisions-registry)
- [Versioning Cadence](#versioning-cadence)
- [Phase 1 — Playable MVP (V1.0)](#phase-1--playable-mvp-v10)
- [Phase 2 — Platform & Engine Refinement (V2.0)](#phase-2--platform--engine-refinement-v20)
- [Phase 3 — Permissions & Player QoL (V3.0)](#phase-3--permissions--player-qol-v30)
- [Phase 4 — Economic Intelligence (V4.0)](#phase-4--economic-intelligence-v40)
- [Phase 5 — Platform & Scale (V5.0)](#phase-5--platform--scale-v50)
- [Data Architecture Blueprint](#data-architecture-blueprint)
- [Configuration Schema Blueprint](#configuration-schema-blueprint)
- [Package Structure Blueprint](#package-structure-blueprint)
- [Risk Registry](#risk-registry)
- [Reference Documentation](#reference-documentation)

---

## Executive Summary

AlbionMarket is a high-performance, asynchronous Spigot/Paper Minecraft plugin that faithfully replicates the **localized Order Book Marketplace** architecture of Albion Online. The system completely abandons standard "flat-list" auction house mechanics in favor of a true **dual-sided bid/ask matching engine** where:

- Markets are **entirely localized** to specific regional NPC entities
- Taxes and fees are **dynamically configured per market node**
- Items **never teleport** across the world — players must physically visit the node to claim assets
- The matching engine supports **four distinct transaction types**: Sell Orders, Buy Orders, Quick Buy, and Quick Sell
- All monetary operations use **integer-only arithmetic** (zero floating-point drift)
- Order matching uses **optimistic concurrency control** via atomic database updates
- **Partial fills** are natively supported

### Playable-First Strategy

The roadmap is structured using strict semantic versioning. Each major version (V1.0 through V5.0) is a **fully compilable, runnable, and playable baseline release**. Five sequential patch versions build on each major baseline, incrementally introducing features in a logical progression. V1.0 delivers a complete, end-to-end marketplace experience using simplified components; every subsequent version iteratively enhances towards a full platform ecosystem.

---

## Architectural Decisions Registry

These decisions were resolved during the design interview and are **binding constraints** for all implementation phases.

| # | Decision | Resolution | Rationale |
|---|----------|-----------|-----------|
| AD-01 | Database Backend | **V1.0**: SQLite via HikariCP. **V2.0**: Adds MySQL/MariaDB toggle. | Gets V1.0 out faster; abstract DAO interface isolates backend |
| AD-02 | NPC/Entity System | **PDC Entity Tagging** via `PersistentDataContainer` with `NamespacedKey(albionmarket, node_id)` | Entity-provider agnostic; works with Citizens NPCs, vanilla Villagers, or Interaction entities |
| AD-03 | GUI Framework | **V1.0**: Chest GUI with simple chat/click inputs. **V2.0**: Virtual Anvil/Sign packet input | Avoids Anvil/Sign packet jank in V1.0 MVP while securing core flow |
| AD-04 | Item Identity | **Material + SHA-256 hash**. **V1.0**: Exact match with durability gate. **V2.0**: Advanced NBT/Data Component sanitization. | Accurate item fingerprinting; V1.0 ensures 100% durability items can trade immediately |
| AD-05 | Currency Precision | **Long integers** internally (whole silver units). Convert to `double` only at the Vault API boundary with half-up rounding | Zero floating-point drift; deterministic fee calculations |
| AD-06 | Order Expiry | **Dual-layered**: Lazy on-access expiry checks (V1.0) + configurable hourly async batch cleanup (V2.0) | Guarantees data accuracy on read; background sweep handles unvisited stale orders later |
| AD-07 | Concurrency Control | **Optimistic concurrency** via atomic `UPDATE ... WHERE status = 'ACTIVE'` with affected-row verification | Cross-backend compatible (SQLite + MySQL); no JVM lock overhead; naturally scales |
| AD-08 | Partial Fills | **Native from V1.0** with `initial_quantity` / `remaining_quantity` columns | Mirrors Albion's behavior; prevents liquidity deadlocks on low-pop servers |
| AD-09 | Matching Trigger | **Event-driven match-on-placement** — new orders immediately evaluate against counterparty queue | Real-time transaction processing; no cron overhead for matching |
| AD-10 | Price Crossing | **Maker's price** — trades execute at the resting order's price (buyer's bid for incoming sells) | Exact Albion behavior; seller receives price improvement; buyer's escrow covers it |
| AD-11 | Admin Configuration | **Hierarchical YAML** (`config.yml` globals → `markets.yml` per-node overrides) + in-game `/market admin link` PDC tagging | Separates structural definitions from world data; bulk-editable |
| AD-12 | Delivery System | **Database-backed virtual mailbox** per market node, claimable via GUI sub-tab | Localized; items stay until physically retrieved; handles offline fills |
| AD-13 | Message System | **Externalized YAML** (`messages.yml`) powered by **Kyori Adventure MiniMessage** | Full RGB/hex, hover, click events; auto-generated English template; missing-key fallback |
| AD-14 | Fee Configuration | **Hierarchical**: global defaults → per-node overrides → **permission-based multipliers** (e.g., `albionmarket.tax.premium`) | Replicates Albion's Premium tax discount system |
| AD-15 | Notifications | **Adventure API** ActionBar alerts (online) + styled login toast (offline aggregation) + **PlaceholderAPI** expansions | Non-intrusive; external scoreboard integration; respects localization |

---

## Versioning Cadence

| Version | Baseline | Cumulative Feature Scope |
|---------|----------|--------------------------|
| **V1.0** | V1.0.0 | SQLite database, config framework, basic item hashing, matching engine, chest GUI, chat input, mailbox |
| **V2.0** | V2.0.0 | MySQL support, advanced NBT sanitization, MMOItems/ItemsAdder/Oraxen hooks, expiry background task, Anvil GUI input |
| **V3.0** | V3.0.0 | Permission-based tax multipliers, PlaceholderAPI expansions, login notifications, admin commands, localization |
| **V4.0** | V4.0.0 | Transaction logging, price history, revenue analytics, CSV export, audit tools |
| **V5.0** | V5.0.0 | Embedded HTTP API, web dashboard, order modification, webhooks, production hardening |

Each major version baseline (V1.0.0–V5.0.0) is a **compilable, runnable, playable release**. Five sequential patches follow each baseline.

---

## Phase 1 — Playable MVP (V1.0)

**Goal**: Deliver a fully playable, end-to-end marketplace experience. Players can visit an NPC, open a GUI, place buy and sell orders, have those orders match asynchronously, and claim their earnings/items from a localized virtual mailbox.

### V1.0.0 — Core Plugin Foundation (Baseline)
- Plugin main class (`AlbionMarket.java`) with `onEnable` / `onDisable` lifecycle
- Async execution framework (`AsyncExecutor.java`) establishing thread-safety patterns
- `ConfigManager.java` parsing `config.yml` with hierarchical defaults
- `MarketNodeRegistry.java` and `MarketNode.java` loading `markets.yml` per-node definitions
- `MessageManager.java` loading `messages.yml` with MiniMessage support
- `DatabaseManager.java` lifecycle and `DatabaseProvider.java` interface contract
- `SQLiteProvider.java` implementing core SQLite backend via HikariCP
- `SchemaManager.java` creating all core tables (`market_orders`, `player_mailboxes`, `schema_version`)
- `EntityInteractionListener.java` listening for right-clicks on tagged entities
- PDC entity tagging via `/market admin link <node_id>` command skeleton
- `Utils.java` with formatting and color code utilities

### V1.0.1 — Economy Bridge & Item Identity
- `EconomyBridge.java` wrapping Vault API, converting internal `long` ↔ Vault `double` with half-up rounding
- `FeeCalculator.java` computing setup fees and transaction taxes from config
- `EscrowManager.java` handling escrow lock/unlock/refund logic
- `ItemHasher.java` generating SHA-256 hex hashes of item stacks
- `ItemIdentity.java` value object combining `Material` + `nbt_hash`
- `ItemSerializer.java` Base64 serialization/deserialization of ItemStacks
- Durability gate enforcement — items must be at 100% durability to be hashed and listed

### V1.0.2 — Core Matching Engine
- `MatchingEngine.java` implementing match-on-placement logic with optimistic concurrency
- `MatchResult.java` and `TradeExecution.java` result value objects
- `OrderValidator.java` pre-flight validation (funds, inventory space, max orders, price bounds)
- `OrderManager.java` for creating and cancelling orders
- `MarketOrder.java`, `OrderStatus.java`, `OrderType.java` domain objects
- Sell Order and Buy Order placement flows with correct setup fee capture and escrow locking
- Quick Buy and Quick Sell instant matching against the resting limit book
- Native partial fill support via `initial_quantity` / `remaining_quantity` columns
- `MarketNodeInteractEvent.java` custom event fired on NPC interaction

### V1.0.3 — Virtual Mailbox & Lazy Expiry
- `MailboxManager.java` and `MailboxEntry.java` for routing filled assets to player's node mailbox
- Cash and item asset types with claim logic
- `source_type` tracking (TRADE_FILL, CANCEL_REFUND, EXPIRY_REFUND, ESCROW_REFUND)
- Lazy expiry check — `expires_at < NOW()` evaluated when orders are loaded from database
- Cancel flow returning escrowed funds and items to mailbox
- `EntityInteractionListener.java` finalization — opening GUI on NPC right-click

### V1.0.4 — Simplified Chest GUI
- `GUIManager.java` central GUI registry and inventory click event handler
- `GUIPage.java` abstract base page and `PaginatedGUIPage.java` variant
- `ChatInputHandler.java` for simple chat-based numeric entry (click prompt → type in chat)
- `GUIItemBuilder.java` utility for building display items
- `MainMenuPage.java` — market NPC main menu with order/mailbox navigation
- `BrowseSellOrdersPage.java` and `BrowseBuyOrdersPage.java` — browse active orders
- `OrderBookPage.java` — detailed order book for a single item
- `CreateSellOrderPage.java` and `CreateBuyOrderPage.java` — order creation flows
- `MyOrdersPage.java` — player's active orders with cancel support
- `MailboxPage.java` — claim assets from virtual mailbox
- `ConfirmationPage.java` — trade/order confirmation dialog
- `GUIClickListener.java` routing all inventory clicks to active page

### V1.0.5 — Commands, Permissions & Polish
- `MarketCommand.java` root `/market` command with subcommand routing
- `AdminSubcommand.java` — `/market admin link`, `/market admin reload`, `/market admin info`
- `MarketTabCompleter.java` tab completion for all commands
- Permission nodes: `albionmarket.use`, `albionmarket.admin`
- Full end-to-end verification — players can list, buy, sell, and claim without command-line interaction
- Error handling hardening, edge case coverage, and performance profiling
- Bugfixes and stability improvements from integration testing

---

## Phase 2 — Platform & Engine Refinement (V2.0)

**Goal**: Upgrade the functional MVP for large-scale production servers with cross-platform database support, deep item handling, MMOItems integration, and UX polish.

### V2.0.0 — MySQL & Cross-Platform Database (Baseline)
- `MySQLProvider.java` implementing full MySQL/MariaDB backend via HikariCP
- Configuration toggle `database.type: mysql` with host/port/credentials
- `SchemaManager.java` migration tracking via `schema_version` table
- WAL mode enabled for SQLite; connection pool tuning for both backends
- `DatabaseProvider.java` interface finalized — all data access routed through abstraction
- Existing V1.0 features verified and stable on both SQLite and MySQL

### V2.0.1 — Advanced Item Sanitization
- `ItemHasher.java` expanded to strip volatile data components (custom damage, repair costs, random UUIDs, leather dye colors)
- Configurable PDC keys-to-hash list in `config.yml` (`item-identity.pdc-keys-to-hash`)
- MMOItems integration — preserve `mmoitems:type` identifier in NBT hash
- ItemsAdder integration — preserve `itemsadder:id` identifier in NBT hash
- Oraxen integration — preserve `oraxen:id` identifier in NBT hash
- Cross-plugin item matching for server networks using custom items

### V2.0.2 — Order Expiry Background Task
- `ExpiryCleanupTask.java` — configurable `BukkitRunnable` running on async thread
- Batch processing of expired orders (`status = 'ACTIVE' AND expires_at < NOW()`)
- Atomic status transition: ACTIVE → EXPIRED with mailbox entry creation
- Moved locked escrow (buy orders) and serialized items (sell orders) to mailboxes
- Configuration: `cleanup.expired-order-check-interval-minutes`, `cleanup.batch-size`

### V2.0.3 — Anvil/Sign GUI Input
- `AnvilInputHandler.java` using protocol-level virtual Anvil packets for numeric input
- Remove dependency on chat-based input — players type directly in Anvil rename field
- Sign packet input as alternative for servers without Anvil support
- Backward compatibility: fallback to `ChatInputHandler.java` if packet injection unavailable
- Validation feedback displayed on the Anvil output slot (green checkmark / red error)

### V2.0.4 — GUI Sorting, Filtering & Pagination Polish
- `PaginatedGUIPage.java` enhanced with sort options (price ascending/descending, newest first)
- Search/filter by item name or material type
- Page jump controls (first, last, numbered page buttons)
- Improved display density — more orders visible per page
- Item lore showing fill percentage, time remaining, and per-unit pricing

### V2.0.5 — Performance Optimization & Stability
- HikariCP connection pool tuning for both SQLite and MySQL profiles
- Query optimization — composite index verification and query plan analysis
- Caching layer for frequently accessed data (market node registry, item hash lookups)
- Memory leak audit — ensure all async tasks self-cancel on plugin disable
- Stress testing with 10,000+ concurrent orders and 100+ simultaneous players
- Thread safety audit of all async ↔ sync boundary crossings

---

## Phase 3 — Permissions & Player QoL (V3.0)

**Goal**: Tune the economy with permission-based modifiers, improve player retention with rich notifications, and deliver polished administrative tooling.

### V3.0.0 — Permission-Based Tax System (Baseline)
- `albionmarket.tax.premium` permission node applying configurable tax multiplier
- `albionmarket.tax.vip` permission node with separate multiplier
- `FeeCalculator.java` updated to query permission-based multipliers and compute effective tax
- `tax-permission-multipliers` section in `config.yml` with `min-effective-tax-percent` floor
- Setup fee remains non-refundable and unaffected by multipliers (per Albion design)
- Existing V1.0–V2.0 features verified and stable with permission system

### V3.0.1 — Notifications & Real-Time Alerts
- `NotificationManager.java` handling all player-facing alerts
- **Online Trade Alerts**: ActionBar message via Adventure API when an order fills while player is online
- Configurable alert sounds (`online-trade-alert-sound`)
- **Login Toasts**: On `PlayerJoinEvent`, async query of unclaimed mailbox entries
- Styled MiniMessage notification with hover/click details
- Configurable sound effect (`login-notification-sound`) and delay

### V3.0.2 — PlaceholderAPI Expansion
- `PlaceholderExpansion.java` registering `%albionmarket_*%` placeholders
- `%albionmarket_pending_items_<node>%` — unclaimed item count per node
- `%albionmarket_pending_cash_<node>%` — unclaimed silver amount per node
- `%albionmarket_active_orders_<node>%` — active order count for player
- `%albionmarket_total_active_orders_<node>%` — global active order count
- Configurable cache TTL (`placeholders.cache-ttl-seconds`) to reduce database load

### V3.0.3 — Admin Command Suite
- `/market admin reload` — hot-reload `config.yml`, `markets.yml`, `messages.yml` without server restart
- `/market admin info <node_id>` — display node configuration, active order count, revenue stats
- `/market admin purge <node_id>` — force-expire all active orders at a node, routing assets to mailboxes
- `/market admin stats` — cross-node summary of fees collected and trade volume
- Rich tab completion for all admin subcommands

### V3.0.4 — Player Trade History & In-Game Stats
- "My Trades" GUI page showing player's executed trade history
- In-game stats display: total trades, volume, taxes paid, fees paid
- `PlayerListener.java` enhanced with session tracking
- Command `/market stats [player]` for self and admin lookup of player statistics

### V3.0.5 — Localization & Message System Finalization
- Auto-generated English `messages.yml` template on first run
- Missing-key fallback — if a key is absent from the file, use the default English string
- MiniMessage parse error handling — try-catch with plain-text legacy fallback
- All user-facing strings externalized — zero hardcoded strings in business logic
- Placeholder system finalized: `{player}`, `{node_name}`, `{item_name}`, `{quantity}`, `{price}`, `{total}`, `{setup_fee}`, `{tax}`, `{net_payout}`, `{order_id}`, `{remaining}`

---

## Phase 4 — Economic Intelligence (V4.0)

**Goal**: Expose the economic data the engine has been generating to players and administrators, enabling data-driven server management.

### V4.0.0 — Transaction Logging Infrastructure (Baseline)
- `market_trade_history` table fully activated — every trade execution logged
- `TradeExecution.java` recording: buyer/seller UUID, material, hash, quantity, price, tax, trade type
- Schema index creation for `(node_id, nbt_hash, executed_at)` and `(player_uuid, executed_at)`
- Async write queue for trade logging — non-blocking, batched writes
- Existing V1.0–V3.0 features verified and stable with trade history enabled

### V4.0.1 — Price History & Trend Display
- `PriceHistoryService.java` aggregating trade data for 24h/7d/30d windows
- `PriceHistoryPage.java` in-game GUI showing price trends for a selected item
- Display: average price, volume, lowest/highest price, transaction count per period
- Sparkline-style visual representation using Minecraft lore formatting
- Lazy aggregation with configurable cache TTL for performance

### V4.0.2 — Admin Analytics Dashboard (In-Game)
- `TaxRevenueService.java` tracking total setup fees and transaction taxes collected per node
- `/market admin stats` enhanced with revenue breakdown charts
- Revenue by node, by time period (24h/7d/30d/all-time), by item category
- `/market admin revenue <node_id>` — detailed fee collection report
- `ReportExporter.java` generating CSV exports for external spreadsheet analysis

### V4.0.3 — Market Analytics GUI
- Browse-able analytics GUI accessible from main menu (for admin permission holders)
- Node selection view with revenue/volume summary cards
- Item popularity ranking — most traded items by volume and by value
- Market activity heatmap — busiest hours/days per node
- Export buttons triggering CSV download to server filesystem

### V4.0.4 — Forecasting & Trend Detection
- Moving average calculations for price trends (7-day SMA, 30-day SMA)
- Volatility indicators — standard deviation of prices per item
- "Hot items" detection — items with rapidly increasing trade volume
- GUI indicators: trend arrows (up/down/stable) next to items in browse views
- Optional Discord-style webhook alerts for unusual market activity (infrastructure prepared for V5.0)

### V4.0.5 — Audit Logging & Compliance Tools
- All administrative actions logged to `market_audit_log` table
- `/market admin audit <player>` — view all order/trade activity for a specific player
- Duplicate detection — flag potentially duplicated items by comparing serialized data
- Full trade traceability — for any trade_id, trace buyer_order_id and seller_order_id back to original orders
- Exportable audit reports for server owner compliance and investigation

---

## Phase 5 — Platform & Scale (V5.0)

**Goal**: Transform AlbionMarket from a Minecraft plugin into an ecosystem platform with external API access, web integration, and enterprise-grade hardening.

### V5.0.0 — Embedded HTTP API (Baseline)
- `HttpApiServer.java` launching lightweight async HTTP server on plugin startup
- Powered by `com.sun.net.httpserver.HttpServer` — no external dependencies
- `ApiAuthFilter.java` Bearer token authentication from `config.yml`
- Rate limiting (configurable `api.rate-limit-per-minute`)
- Endpoints:
  - `GET /api/v1/nodes` — list all market nodes
  - `GET /api/v1/nodes/{id}/orderbook/{nbt_hash}` — order book for an item
  - `GET /api/v1/nodes/{id}/history/{nbt_hash}` — trade history for an item
  - `GET /api/v1/stats` — server-wide market statistics
- All endpoints return JSON with consistent error format
- Existing V1.0–V4.0 features verified and stable with API enabled

### V5.0.1 — Web Dashboard Support
- Static HTML/JS/CSS reference dashboard consuming the HTTP API
- Node selection dropdown with live order book display
- Price history charts using Canvas/SVG rendering
- Server stats overview panel
- Responsive design for mobile and desktop
- Implementation guide for server owners to deploy alongside the plugin

### V5.0.2 — Order Modification
- GUI-based order editing — change price and/or quantity of existing active orders
- Price change: recalculate setup fee delta, charge or refund the difference
- Quantity increase: validate additional escrow for buy orders
- Quantity decrease: immediately release excess escrow (buy) or return items to mailbox (sell)
- SQL `UPDATE` on existing order row — no cancel-and-recreate race conditions
- `OrderManager.java` extended with `modifyOrder()` method

### V5.0.3 — Webhook System
- Configurable webhook URLs for external event notifications
- Events: order fill, large trade executed, new node created, admin actions
- JSON payload format documented for custom integrations
- Discord webhook compatibility — embed-formatted messages with color coding
- `WebhookService.java` with async delivery and retry logic

### V5.0.4 — API Extensions & Third-Party Integration
- `GET /api/v1/players/{uuid}/orders` — player's active orders via API
- `GET /api/v1/players/{uuid}/history` — player's trade history via API
- `GET /api/v1/players/{uuid}/mailbox` — player's pending mailbox entries via API
- Webhook subscription management — register/deregister webhooks via API
- API versioning prefix (`/api/v1/`) to maintain backward compatibility

### V5.0.5 — Full Production Hardening & Documentation
- Comprehensive documentation: setup guide, API reference, webhook guide, configuration reference
- Performance benchmark suite — baseline metrics for all operations
- Security audit — rate limiting edge cases, token rotation, SQL injection verification
- Graceful degradation — plugin operates at reduced capacity if a dependency is missing
- Final round of integration testing across SQLite, MySQL, Paper 1.21+, and all supported soft dependencies
- Archive-quality release with full changelog and migration guides from V1.0 through V5.0

---

## Data Architecture Blueprint

> **Note**: This section describes the structural layout and column purposes. No SQL syntax or DDL is included per the project constraints.

### Table: `market_orders`

| Column | Type | Purpose |
|--------|------|---------|
| `order_id` | Auto-increment long | Unique order identifier |
| `node_id` | String (64) | Foreign key to market node config |
| `player_uuid` | String (36) | Order creator's UUID |
| `order_type` | Enum string (BUY/SELL) | Order direction |
| `material` | String (128) | Bukkit Material name |
| `nbt_hash` | String (64) | SHA-256 hex hash of sanitized item identity |
| `display_label` | String (256) | Human-readable item label for display |
| `serialized_item` | Text (long) | Base64-encoded full ItemStack serialization |
| `initial_quantity` | Integer | Original order quantity |
| `remaining_quantity` | Integer | Unfilled quantity remaining |
| `price_per_unit` | Long | Price per single item (integer silver) |
| `total_escrow` | Long | Locked funds for buy orders (0 for sell orders) |
| `setup_fee_paid` | Long | Setup fee amount paid (for audit trail) |
| `status` | Enum string | ACTIVE / FILLED / CANCELLED / EXPIRED |
| `created_at` | Timestamp | Order creation time |
| `expires_at` | Timestamp | Expiration time |
| `updated_at` | Timestamp | Last modification time |

**Indexes**:
- `(node_id, nbt_hash, order_type, status)` — Primary query path for matching engine
- `(player_uuid, node_id, status)` — "My Orders" lookups
- `(status, expires_at)` — Expiry cleanup queries

### Table: `player_mailboxes`

| Column | Type | Purpose |
|--------|------|---------|
| `entry_id` | Auto-increment long | Unique mailbox entry ID |
| `player_uuid` | String (36) | Recipient player UUID |
| `node_id` | String (64) | Market node where asset is claimable |
| `asset_type` | Enum string (ITEM/CASH) | What type of asset |
| `serialized_item` | Text (long, nullable) | Base64 item data (null for CASH entries) |
| `cash_amount` | Long (nullable) | Silver amount (null for ITEM entries, 0 if not applicable) |
| `source_order_id` | Long (nullable) | Reference to originating order (for audit) |
| `source_type` | String (64) | TRADE_FILL / CANCEL_REFUND / EXPIRY_REFUND / ESCROW_REFUND |
| `claimed` | Boolean | Whether the player has collected this entry |
| `created_at` | Timestamp | When the entry was created |
| `claimed_at` | Timestamp (nullable) | When the entry was claimed |

**Indexes**:
- `(player_uuid, node_id, claimed)` — Primary mailbox query
- `(player_uuid, claimed)` — Cross-node pending count for notifications

### Table: `market_trade_history`

| Column | Type | Purpose |
|--------|------|---------|
| `trade_id` | Auto-increment long | Unique trade identifier |
| `node_id` | String (64) | Where the trade occurred |
| `buyer_uuid` | String (36) | Buyer's UUID |
| `seller_uuid` | String (36) | Seller's UUID |
| `material` | String (128) | Item material |
| `nbt_hash` | String (64) | Item identity hash |
| `quantity` | Integer | Quantity traded |
| `price_per_unit` | Long | Execution price per unit |
| `total_value` | Long | price × quantity |
| `tax_amount` | Long | Tax collected from seller |
| `seller_payout` | Long | Net amount seller received |
| `trade_type` | Enum string | LIMIT_MATCH / QUICK_BUY / QUICK_SELL |
| `buyer_order_id` | Long (nullable) | Reference to buyer's order (null for Quick Buy) |
| `seller_order_id` | Long (nullable) | Reference to seller's order (null for Quick Sell) |
| `executed_at` | Timestamp | Trade execution time |

**Indexes**:
- `(node_id, nbt_hash, executed_at)` — Price history queries
- `(node_id, executed_at)` — Node stats queries
- `(buyer_uuid, executed_at)` / `(seller_uuid, executed_at)` — Player trade history

### Table: `schema_version`

| Column | Type | Purpose |
|--------|------|---------|
| `version` | Integer | Current schema version number |
| `applied_at` | Timestamp | When this version was applied |
| `description` | String (256) | Human-readable migration description |

---

## Configuration Schema Blueprint

### `config.yml` — Global Plugin Settings

```yaml
# Database Configuration
database:
  type: sqlite  # sqlite | mysql
  sqlite:
    file: market.db
  mysql:
    host: localhost
    port: 3306
    database: albionmarket
    username: root
    password: ""
  pool:
    max-pool-size: 10
    connection-timeout-ms: 5000
    idle-timeout-ms: 600000

# Economy Defaults (overridable per-node in markets.yml)
economy:
  default-setup-fee-percent: 2.5
  default-transaction-tax-percent: 6.0
  default-max-order-duration-days: 30
  default-max-active-orders-per-player: 30
  currency-name-singular: "silver"
  currency-name-plural: "silver"
  min-price-per-unit: 1
  max-price-per-unit: 999999999
  tax-permission-multipliers:
    albionmarket.tax.premium: 0.5    # 50% tax reduction
    albionmarket.tax.vip: 0.25       # 75% tax reduction
  min-effective-tax-percent: 0.0     # Floor for stacked multipliers

# Item Identity
item-identity:
  pdc-keys-to-hash: []  # e.g., ["mmoitems:type", "itemsadder:id"]
  require-full-durability: true

# Cleanup
cleanup:
  expired-order-check-interval-minutes: 60
  batch-size: 100

# Notifications
notifications:
  login-notification-enabled: true
  login-notification-delay-ticks: 60
  login-notification-sound: ENTITY_EXPERIENCE_ORB_PICKUP
  online-trade-alert-sound: BLOCK_NOTE_BLOCK_PLING

# PlaceholderAPI
placeholders:
  cache-ttl-seconds: 30

# API Server (V5.0)
api:
  enabled: false
  port: 8780
  bind-address: "127.0.0.1"
  auth-token: "CHANGE_ME"
  rate-limit-per-minute: 60
```

### `markets.yml` — Per-Node Market Definitions

```yaml
# Each top-level key is a unique node_id
# Use /market admin link <node_id> to bind a node to an entity in-world

desert_bazaar:
  display-name: "<gradient:#FFD700:#FF8C00>Desert Bazaar</gradient>"
  setup-fee-percent: 2.5
  transaction-tax-percent: 8.0
  max-order-duration-days: 14
  max-active-orders-per-player: 20

mountain_exchange:
  display-name: "<gradient:#87CEEB:#4682B4>Mountain Exchange</gradient>"
  setup-fee-percent: 2.5
  transaction-tax-percent: 4.0
  max-order-duration-days: 30
  max-active-orders-per-player: 50

# Minimal node — inherits all defaults from config.yml
forest_market:
  display-name: "<green>Forest Market</green>"
  inherit-defaults: true
```

### `messages.yml` — Player-Facing Strings (Excerpt)

```yaml
# All values use MiniMessage format
# Available placeholders: {player}, {node_name}, {item_name}, {quantity},
# {price}, {total}, {setup_fee}, {tax}, {net_payout}, {order_id}, {remaining}

prefix: "<dark_gray>[</dark_gray><gradient:#FFD700:#FF8C00>Market</gradient><dark_gray>]</dark_gray> "

order:
  sell-created: "{prefix}<green>Sell order placed!</green> <gray>{quantity}x {item_name} @ {price}/ea</gray> <dark_gray>|</dark_gray> <red>Setup fee: {setup_fee}</red>"
  buy-created: "{prefix}<green>Buy order placed!</green> <gray>{quantity}x {item_name} @ {price}/ea</gray> <dark_gray>|</dark_gray> <red>Total locked: {total} + {setup_fee} fee</red>"
  cancelled: "{prefix}<yellow>Order #{order_id} cancelled.</yellow> <gray>Assets routed to your mailbox at {node_name}.</gray>"
  filled: "{prefix}<gold>Order #{order_id} has been filled!</gold> <gray>Visit {node_name} to collect.</gray>"
  expired: "{prefix}<gray>Order #{order_id} has expired.</gray> <gray>Assets routed to your mailbox at {node_name}.</gray>"
  insufficient-funds: "{prefix}<red>Insufficient funds.</red> <gray>You need {total} but only have {balance}.</gray>"
  inventory-full: "{prefix}<red>Your inventory is full.</red> <gray>Items remain in your mailbox.</gray>"
  damaged-item: "{prefix}<red>Items must be at full durability to list on the market.</red>"
  max-orders: "{prefix}<red>You have reached the maximum number of active orders ({max}) at this market.</red>"

trade:
  quick-buy-success: "{prefix}<green>Purchased {quantity}x {item_name} for {total}!</green>"
  quick-sell-success: "{prefix}<green>Sold {quantity}x {item_name} for {net_payout}!</green> <gray>(Tax: {tax})</gray>"
  order-unavailable: "{prefix}<red>This order is no longer available.</red>"

mailbox:
  claimed-item: "{prefix}<green>Claimed {quantity}x {item_name} from {node_name}.</green>"
  claimed-cash: "{prefix}<green>Claimed {amount} from {node_name}.</green>"
  login-notification: "{prefix}<yellow>You have unclaimed assets at <hover:show_text:'{details}'><gold>{count} market(s)</gold></hover>!</yellow>"

admin:
  linked: "{prefix}<green>Entity linked to market node: {node_name}</green>"
  unlinked: "{prefix}<yellow>Entity unlinked from market.</yellow>"
  reloaded: "{prefix}<green>Configuration reloaded.</green>"
  invalid-node: "{prefix}<red>Node ID '{node_id}' does not exist in markets.yml.</red>"
```

---

## Package Structure Blueprint

```
com.perseus.albionmarket/
├── AlbionMarket.java                    # Plugin main class
├── config/
│   ├── ConfigManager.java               # config.yml loader
│   ├── MarketNodeRegistry.java          # markets.yml loader + node lookup
│   ├── MarketNode.java                  # Value object for a market node
│   └── MessageManager.java             # messages.yml + MiniMessage
├── database/
│   ├── DatabaseManager.java             # Lifecycle manager
│   ├── DatabaseProvider.java            # Interface (DAO contract)
│   ├── SQLiteProvider.java              # SQLite implementation
│   ├── MySQLProvider.java               # MySQL/MariaDB implementation
│   └── SchemaManager.java              # Table creation + migrations
├── economy/
│   ├── EconomyBridge.java               # Vault wrapper (long ↔ double)
│   ├── FeeCalculator.java              # Setup fee + tax calculations
│   └── EscrowManager.java              # Escrow lock/unlock/refund logic
├── engine/
│   ├── MatchingEngine.java              # Core match-on-placement logic
│   ├── MatchResult.java                 # Result value object
│   ├── TradeExecution.java              # Individual trade execution record
│   └── OrderValidator.java             # Pre-flight validation checks
├── identity/
│   ├── ItemHasher.java                  # SHA-256 item fingerprinting
│   ├── ItemIdentity.java                # Value object (material + hash)
│   └── ItemSerializer.java             # Base64 serialization/deserialization
├── mailbox/
│   ├── MailboxManager.java              # Claim logic
│   └── MailboxEntry.java               # Value object
├── orders/
│   ├── OrderManager.java                # Create, cancel, modify orders
│   ├── MarketOrder.java                 # Value object
│   ├── OrderStatus.java                 # Enum: ACTIVE, FILLED, CANCELLED, EXPIRED
│   ├── OrderType.java                   # Enum: BUY, SELL
│   └── ExpiryCleanupTask.java          # Background expiry sweep
├── gui/
│   ├── GUIManager.java                  # Central GUI registry + event handler
│   ├── GUIPage.java                     # Abstract base page
│   ├── PaginatedGUIPage.java           # Paginated variant
│   ├── AnvilInputHandler.java          # Virtual anvil numeric input (V2.0)
│   ├── ChatInputHandler.java           # Chat/Command entry input (V1.0)
│   ├── pages/
│   │   ├── MainMenuPage.java           # Market NPC main menu
│   │   ├── BrowseSellOrdersPage.java   # Browse sell orders
│   │   ├── BrowseBuyOrdersPage.java    # Browse buy orders
│   │   ├── OrderBookPage.java          # Detailed order book for one item
│   │   ├── CreateSellOrderPage.java    # Sell order creation flow
│   │   ├── CreateBuyOrderPage.java     # Buy order creation flow
│   │   ├── MyOrdersPage.java           # Player's active orders
│   │   ├── MailboxPage.java            # Player's mailbox
│   │   ├── ConfirmationPage.java       # Trade/order confirmation
│   │   └── PriceHistoryPage.java       # Price history display (V4.0)
│   └── items/
│       └── GUIItemBuilder.java         # Utility for building display items
├── listeners/
│   ├── PlayerListener.java              # Join, quit events
│   ├── EntityInteractionListener.java   # Right-click entity → market open
│   └── GUIClickListener.java           # Inventory click routing
├── commands/
│   ├── MarketCommand.java               # Root /market command
│   ├── AdminSubcommand.java            # /market admin ...
│   └── MarketTabCompleter.java         # Tab completion
├── notifications/
│   ├── NotificationManager.java         # Login toasts, trade alerts
│   └── PlaceholderExpansion.java        # PAPI integration
├── analytics/                           # V4.0+
│   ├── PriceHistoryService.java        # Aggregation queries
│   ├── TaxRevenueService.java          # Revenue tracking
│   └── ReportExporter.java            # CSV export
├── api/                                 # V5.0
│   ├── HttpApiServer.java              # Embedded HTTP server
│   ├── ApiAuthFilter.java             # Bearer token auth
│   ├── WebhookService.java            # Webhook delivery
│   └── handlers/
│       ├── NodesHandler.java
│       ├── OrdersHandler.java
│       ├── HistoryHandler.java
│       └── StatsHandler.java
├── async/
│   └── AsyncExecutor.java              # Thread scheduling utilities
├── events/
│   └── MarketNodeInteractEvent.java    # Custom event for NPC interaction
└── utils/
    └── Utils.java                       # Color codes, formatting
```

---

## Risk Registry

| Risk | Severity | Mitigation |
|------|----------|-----------|
| **Item duplication via Quick Sell race condition** | CRITICAL | Optimistic concurrency on DB updates + main-thread inventory verification/removal before DB commit. If inventory removal fails post-DB-check, abort and log. |
| **Economy desync (Vault double withdrawal)** | HIGH | Single-point Vault calls via `EconomyBridge` with pre/post balance assertions in debug mode. Log all Vault transactions to `market_trade_history`. |
| **NBT hash instability across versions** | MEDIUM | Pin item serialization format. Include serialization version in hash. Migration path for hash changes: re-hash all active orders during schema migration. |
| **SQLite write contention under load** | MEDIUM | WAL mode enabled by default. Connection pool limited to 1 write connection for SQLite. Recommend MySQL for servers with 100+ concurrent players. |
| **Main thread freeze from sync inventory operations** | MEDIUM | Strict async execution framework. Inventory operations are minimal (single-tick check+remove). Performance profiling in QA phase. |
| **Citizens plugin update breaks PDC** | LOW | PDC is a Bukkit API, not Citizens-specific. Entity tagging is provider-agnostic by design. |
| **MiniMessage parsing errors in messages.yml** | LOW | Try-catch around all MiniMessage parse calls. Fallback to plain-text legacy format. Log malformed keys at WARNING level. |

---

## Reference Documentation

These resources informed the economic model, fee calculations, and UI design decisions:

| Source | Purpose | URL |
|--------|---------|-----|
| Albion Online Setup Fee PSA | Confirms 2.5% non-refundable setup fee on both buy and sell orders | [Reddit Thread](https://www.reddit.com/r/albiononline/comments/1ard0ns/psa_buy_and_sell_orders_have_a_nonrefundable_25/) |
| Market Tax Breakdown Discussion | Clarifies setup fee vs. transaction tax distinction, premium discount mechanics | [Reddit Thread](https://www.reddit.com/r/albiononline/comments/147ar37/market_tax_makes_no_sense_or_am_i_just_stupid_help/) |
| Marketplace UI Video Walkthrough | Visual reference for tab layout, order book display, and interaction flows | [YouTube](https://www.youtube.com/watch?v=vBHwpso9bHM) |
| Albion Online Data Project | Reference architecture for external market data APIs and web dashboards | [albion-online-data.com](https://www.albion-online-data.com/) |
| MiniMessage Format Reference | Official formatting documentation for Kyori Adventure MiniMessage | [Adventure Docs](https://docs.advntr.dev/minimessage/format.html) |
| PlaceholderAPI Expansion Guide | Official guide for registering custom PAPI placeholder expansions | [GitHub Wiki](https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion) |
| Spigot PersistentDataContainer | API documentation for entity PDC tagging | [Spigot Javadoc](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/persistence/PersistentDataContainer.html) |

---

> **End of Roadmap Document**
>
> This document is the authoritative planning reference for all AlbionMarket development phases.
> No functional code, SQL syntax, or method implementations are included per project constraints.
> All architectural decisions are binding unless explicitly superseded by a future design review.
