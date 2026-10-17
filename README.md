# Per Player Hardcore
A lightweight **server-side only** Fabric mod for Minecraft that lets you put specific players into a hardcore-like mode.

## Features
- Track individual players with a limited number of lives
- On final death:
  - `spectator` mode: force the player into Spectator (near their last location / hardcore spawn)
  - `worldspawn` mode: clear personal spawnpoint and send to world spawn
- Optional custom hardcore spawn point (used when the player still has lives)
- Fully configurable messages (including translations) via `config/perplayerhardcore.json`
- Persistent data via Fabric Attachment API
- Non-invasive mixins (does not cancel or replace vanilla death)

## Commands
All commands require permission level 2 (gamemaster / op).

| Command | Description |
|---------|-------------|
| `/hardcore start <player> <worldspawn\|spectator> [lives]` | Start tracking a player (default lives = 1) |
| `/hardcore stop <player>` | Stop tracking a player |
| `/hardcore setspawn <player> <x> <y> <z>` | Set the custom hardcore spawn |
| `/hardcore lives add <player> <amount>` | Add lives |
| `/hardcore lives remove <player> <amount>` | Remove lives |
| `/hardcore get <player>` | Show detailed info about a tracked player |
| `/hardcore list` | List all currently tracked online players |

## Behavior
1. When a tracked player dies:
   - Lives are decremented
   - If lives remain: message + respawn at hardcore spawn (if set) or normal vanilla spawn
   - If lives reach 0: `GAME OVER!` + final consequence

2. After respawn on final death:
   - Player is removed from tracking
   - Applied consequence (`spectator` or worldspawn)

## Configuration
File: `config/perplayerhardcore.json`

All messages can be customized / translated. Defaults are in English. Example for French:

```json
{
  "prefix": "[HARDCORE] ",
  "startPlayer": "Mode hardcore activé ! %d %s. %s.",
  "life": "vie",
  "lives": "vies",
  "modeSpectator": "Spectateur à la fin",
  "modeWorldspawn": "Spawn du monde à la fin",
  "gameOver": "GAME OVER !",
  "...": "..."
}
```

The config file is created automatically on first launch with all default English strings.

## Building
```bash
./gradlew build
```

The resulting jar will be in `build/libs/`.

## Notes
- Server-side only (clients do not need the mod)
- Compatible with other death/respawn mods thanks to non-cancelling mixins
- Data is stored per-player via Fabric Data Attachment API and survives restarts
