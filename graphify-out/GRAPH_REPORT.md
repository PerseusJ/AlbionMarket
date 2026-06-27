# Graph Report - .  (2026-06-26)

## Corpus Check
- Corpus is ~304 words - fits in a single context window. You may not need a graph.

## Summary
- 48 nodes · 48 edges · 11 communities (5 shown, 6 thin omitted)
- Extraction: 79% EXTRACTED · 21% INFERRED · 0% AMBIGUOUS · INFERRED: 10 edges (avg confidence: 0.78)
- Token cost: 180 input · 420 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Plugin Manager & Lifecycle|Plugin Manager & Lifecycle]]
- [[_COMMUNITY_Player Event Listener|Player Event Listener]]
- [[_COMMUNITY_Plugin Dependencies|Plugin Dependencies]]
- [[_COMMUNITY_OpenCode Graphify Plugin|OpenCode Graphify Plugin]]
- [[_COMMUNITY_String Utilities|String Utilities]]
- [[_COMMUNITY_Plugin Initialization|Plugin Initialization]]
- [[_COMMUNITY_OpenCode Schema Config|OpenCode Schema Config]]
- [[_COMMUNITY_NPM Dependencies|NPM Dependencies]]
- [[_COMMUNITY_Hex Chat Colors|Hex Chat Colors]]
- [[_COMMUNITY_VS Code Settings|VS Code Settings]]
- [[_COMMUNITY_Plugin Shutdown|Plugin Shutdown]]

## God Nodes (most connected - your core abstractions)
1. `GraphifyPlugin()` - 5 edges
2. `AlbionMarket` - 5 edges
3. `PlayerListener` - 5 edges
4. `PluginManager` - 5 edges
5. `AlbionMarket plugin descriptor` - 4 edges
6. `AlbionMarket.onEnable` - 3 edges
7. `Override` - 2 edges
8. `PlayerJoinEvent` - 2 edges
9. `EventHandler` - 2 edges
10. `Utils` - 2 edges

## Surprising Connections (you probably didn't know these)
- `AlbionMarket plugin descriptor` --references--> `AlbionMarket`  [EXTRACTED]
  C:/Projects/AlbionMarket/src/main/resources/plugin.yml → src/main/java/com/perseus/albionmarket/AlbionMarket.java
- `PlayerListener` --conceptually_related_to--> `Bukkit event listener pattern`  [INFERRED]
  src/main/java/com/perseus/albionmarket/listeners/PlayerListener.java → C:/Projects/AlbionMarket/src/main/java/com/perseus/albionmarket/listeners/PlayerListener.java
- `PluginManager` --conceptually_related_to--> `Singleton pattern`  [INFERRED]
  src/main/java/com/perseus/albionmarket/managers/PluginManager.java → C:/Projects/AlbionMarket/src/main/java/com/perseus/albionmarket/managers/PluginManager.java
- `PluginManager` --references--> `PluginManager.getInstance`  [EXTRACTED]
  src/main/java/com/perseus/albionmarket/managers/PluginManager.java → C:/Projects/AlbionMarket/src/main/java/com/perseus/albionmarket/managers/PluginManager.java
- `AlbionMarket.onEnable` --references--> `PlayerListener`  [EXTRACTED]
  C:/Projects/AlbionMarket/src/main/java/com/perseus/albionmarket/AlbionMarket.java → src/main/java/com/perseus/albionmarket/listeners/PlayerListener.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Plugin Bootstrap Flow** — albionmarket_albionmarket_onEnable, managers_pluginmanager_getInstance, managers_pluginmanager_initialize, listeners_playerlistener_PlayerListener [INFERRED 0.85]
- **Bukkit Plugin Lifecycle Hooks** — albionmarket_albionmarket_AlbionMarket, albionmarket_albionmarket_onEnable, albionmarket_albionmarket_onDisable [INFERRED 0.85]
- **Graphify Injection Reminder Mechanism** — opencode_opencode_config, plugins_graphify_graphifyplugin, plugins_graphify_knowledge_graph [INFERRED 0.95]

## Communities (11 total, 6 thin omitted)

### Community 0 - "Plugin Manager & Lifecycle"
Cohesion: 0.29
Nodes (3): Singleton pattern, PluginManager, Override

### Community 1 - "Player Event Listener"
Cohesion: 0.36
Nodes (6): Bukkit event listener pattern, EventHandler, Listener, PlayerListener.onPlayerJoin, PlayerListener, PlayerJoinEvent

### Community 2 - "Plugin Dependencies"
Cohesion: 0.33
Nodes (6): AlbionMarket, JavaPlugin, AlbionMarket plugin descriptor, Citizens (soft dependency), PlaceholderAPI (soft dependency), Vault (hard dependency)

### Community 3 - "OpenCode Graphify Plugin"
Cohesion: 0.38
Nodes (6): OpenCode Configuration, @opencode-ai/plugin v1.17.11, Package Dependencies, GraphifyPlugin(), Knowledge Graph at graphify-out/, tool.execute.before

### Community 5 - "Plugin Initialization"
Cohesion: 0.67
Nodes (3): AlbionMarket.onEnable, PluginManager.getInstance, PluginManager.initialize

## Knowledge Gaps
- **14 isolated node(s):** `$schema`, `plugin`, `@opencode-ai/plugin`, `java.compile.nullAnalysis.mode`, `String` (+9 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PluginManager` connect `Plugin Manager & Lifecycle` to `Plugin Initialization`?**
  _High betweenness centrality (0.166) - this node is a cross-community bridge._
- **Why does `AlbionMarket.onEnable` connect `Plugin Initialization` to `Player Event Listener`?**
  _High betweenness centrality (0.141) - this node is a cross-community bridge._
- **Why does `PluginManager.getInstance` connect `Plugin Initialization` to `Plugin Manager & Lifecycle`?**
  _High betweenness centrality (0.139) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GraphifyPlugin()` (e.g. with `@opencode-ai/plugin v1.17.11` and `Knowledge Graph at graphify-out/`) actually correct?**
  _`GraphifyPlugin()` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `$schema`, `plugin`, `@opencode-ai/plugin` to the rest of the system?**
  _14 weakly-connected nodes found - possible documentation gaps or missing edges._