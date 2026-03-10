# Poke Lantern

A Forge mod for Minecraft 1.20.1 + Cobblemon 1.5.2 that suppresses all Pokemon spawns globally and only allows them within a configurable radius of a craftable Poke Lantern block.

## Behavior

- **Global suppression**: No Pokemon spawn anywhere by default.
- **Poke Lantern zone**: Spawns are allowed within `spawnRadius` blocks (default 64) of any active Poke Lantern.
- **Per-player limit**: Each player may have exactly one Poke Lantern placed at a time.
- **Shared zones**: Any player inside *any* lantern zone will see spawns.
- **Dimension-aware**: Only lanterns in the same dimension as the spawn count.

## Crafting Recipe

```
[ Ender Pearl ] [ Ghast Tear  ] [ Ender Pearl ]
[ Ender Pearl ] [   Lantern   ] [ Ender Pearl ]
[  Blaze Rod  ] [ Gold Block  ] [  Blaze Rod  ]
```

## Configuration

Located at `world/serverconfig/pokelantern-server.toml`:

| Key               | Default | Description                                      |
|-------------------|---------|--------------------------------------------------|
| `allowShinies`    | `true`  | Shiny Pokemon bypass the radius check            |
| `allowLegendaries`| `true`  | Legendary Pokemon bypass the radius check        |
| `spawnRadius`     | `64`    | Radius in blocks around lantern (range: 1–512)   |

## Requirements

- Minecraft 1.20.1
- Forge 47.4.16
- KotlinForForge 4.5.0
- Cobblemon 1.5.2+1.20.1

## Building

```bash
./gradlew build
```

Output JAR: `poke/PokeLantern-forge-1.20.1-1.0.0.jar`

## License

Apache-2.0
