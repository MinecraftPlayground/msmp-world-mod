<img src="assets/icon.png" width="64" align="right">

# MSMP World

A server-side Fabric mod that extends the [Minecraft Server Management Protocol](https://minecraft.wiki/w/Minecraft_Server_Management_Protocol) (MSMP) by providing additional functions for getting and setting world data.

This mod is designed for tooling, dashboards, automation systems, map generators, external monitoring tools, and integrations that need structured access to world data without relying on command parsing or RCON hacks.


## Installation

1. Download the mod `.jar` and place it in your server's `mods/` folder.
2. Enable the Management Server in `server.properties`:
   ```properties
   management-server-enabled=true
   ```
3. Start the server. The Management Server will listen on `localhost:25576` by default.


## RPC Methods

The mod currently provides the following MSMP RPC methods. All of these methods are also automatically discoverable through the standard `rpc.discover` MSMP endpoint.

| Method                    | Description                                                                            |
| ------------------------- | -------------------------------------------------------------------------------------- |
| `world:block`             | Returns the block at a given position in a given dimension                             |
| `world:block/set`         | Sets the block at a given position in a given dimension                                |
| `world:chunk/surface`     | Returns the surface blocks of a chunk (256 columns) using a compact palette format     |
| `world:chunk/surface/set` | Bulk-places blocks across all 256 columns of a chunk using a compact palette format    |
| `world:path_find`         | Computes a path between two positions, simulating a generic player-like ground walker  |

> If you want more methods or notifications for other purposes, please [open an issue](https://github.com/MinecraftPlayground/msmp-world-mod/issues/new?template=new_method_or_notification_suggesetion.yml)


## Method Reference

### `world:block`

Returns the block (id, states, and block-entity data) at a given position.

```json
// Request
{ "dimension": "minecraft:overworld", "position": [100, 64, 200] }

// Response
{
  "dimension": "minecraft:overworld",
  "position": [100, 64, 200],
  "block": {
    "id": "minecraft:chest",
    "states": { "facing": "north" },
    "components": { "Items": [ { "Slot": 0, "id": "minecraft:diamond", "count": 5 } ] }
  }
}
```

`states` is omitted for blocks with no state properties. `components` is omitted if the block has no block entity (Chest, Sign, Furnace, ...).

---

### `world:block/set`

Sets the block at a given position. Returns the confirmed state after placement.

```json
// Request
{
  "dimension": "minecraft:overworld",
  "position": [100, 64, 200],
  "block": {
    "id": "minecraft:chest",
    "states": { "facing": "north" },
    "components": { "Items": [ { "Slot": 0, "id": "minecraft:diamond", "count": 5 } ] }
  }
}

// Response - same shape as world:block
```

`states` and `components` are both optional. Missing states fall back to the block's default values (matching Vanilla `/setblock` behavior).

---

### `world:chunk/surface`

Returns the surface block of each of the 256 columns (16×16) of a chunk, using a compact palette format to avoid repeating identical block descriptions.

```json
// Request
{ "dimension": "minecraft:overworld", "chunk": [6, -2], "heightmap": "MOTION_BLOCKING_NO_LEAVES" }

// Response (excerpt - heights/blocks always have exactly 256 entries)
{
  "dimension": "minecraft:overworld",
  "chunk": [6, -2],
  "heightmap": "MOTION_BLOCKING_NO_LEAVES",
  "palette": [
    { "id": "minecraft:grass_block" },
    { "id": "minecraft:water" },
    { "id": "minecraft:chest", "states": { "facing": "north" } }
  ],
  "heights": [71, 68, 77, /* ... */],
  "blocks":  [0,  1,  2,  /* ... */],
  "blockEntities": [
    { "index": 2, "components": { "Items": [ /* ... */ ] } }
  ]
}
```

**Reading the response:** `blocks[i]` is an index into `palette`; `heights[i]` is the Y-coordinate of the surface block in that column. The flat index maps to world coordinates as:

```
worldX = chunkX * 16 + (index / 16)
worldZ = chunkZ * 16 + (index % 16)
```

**Sentinel values:** `heights[i] == -2147483648` (`Integer.MIN_VALUE`) and `blocks[i] == -1` mean "no block found in this column" (e.g. an empty column at the world edge). The array length is always exactly 256.

**`blockEntities`** is a sparse list - only columns that have a block entity at their surface appear here, referenced by the same flat `index`. Kept separate from `palette` so that e.g. two chests with different inventories but the same orientation can still share one palette entry.

**`heightMap` values** (optional, defaults to `MOTION_BLOCKING_NO_LEAVES`):

| Value                       | Meaning                                                       |
|-----------------------------|---------------------------------------------------------------|
| `WORLD_SURFACE`             | Topmost non-air block (includes snow, leaves, flowers)        |
| `MOTION_BLOCKING`           | Topmost blocking block, water surface counts                  |
| `MOTION_BLOCKING_NO_LEAVES` | Like above, but leaves are ignored (shows ground under trees) |
| `OCEAN_FLOOR`               | Ocean floor, ignores water                                    |

---

### `world:chunk/surface/set`

Bulk-places blocks across all 256 surface columns of a chunk. Uses the same compact palette format as `world:chunk/surface`, so a GET response can be modified and sent straight back.

```json
// Request (excerpt - heights/blocks always have exactly 256 entries)
{
  "dimension": "minecraft:overworld",
  "chunk": [6, -2],
  "palette": [
    { "id": "minecraft:grass_block" },
    { "id": "minecraft:chest", "states": { "facing": "north" } }
  ],
  "heights": [71, 77, /* ... */],
  "blocks":  [0,  1,  /* ... */],
  "blockEntities": [
    { "index": 1, "components": { "Items": [ { "Slot": 0, "id": "minecraft:diamond", "count": 5 } ] } }
  ]
}

// Response - mirrors the validated request (no heightMap field)
```

**Sentinel values:** `heights[i] == -2147483648` or `blocks[i] == -1` means "leave this column untouched". Both should always be set together.

Validation runs in full before any world mutation - a bad palette entry or invalid `blockEntities` reference fails the entire request rather than leaving a partially-applied chunk.

---

### `world:path_find`

Computes a navigation path between two positions, simulating a generic player-like ground walker (no flying, no swimming). Returns `found: false` when no path exists or the target is unreachable - this is a normal outcome, not an error.

**Hard distance limit:** requests with a straight-line distance greater than 256 blocks between `start` and `end` are rejected immediately.

```json
// Request
{
  "dimension": "minecraft:overworld",
  "start": [100, 64, 200],
  "end":   [150, 70, 230]
}

// Response - path found
{
  "dimension": "minecraft:overworld",
  "found": true,
  "path": [
    [100, 64, 200],
    [101, 64, 201],
    /* ... */
    [150, 70, 230]
  ]
}

// Response - no path found
{ "dimension": "minecraft:overworld", "found": false, "path": [] }
```


## License

[LGPL-3.0](LICENSE)
