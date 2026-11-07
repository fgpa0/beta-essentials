# Beta Essentials

Single-player commands, game rules and game modes for Minecraft Beta 1.7.3 on Babric (Fabric). Client only.

## Features

- **Chat commands** with TAB completion and history. `/` or T opens the chat in single player.
- **Cheats switch**: chosen on Create World, changeable in the game menu. Without cheats only `/help` and `/seed`
  work.
- **Game modes**: survival, creative (flight, item picker, middle-click pick block) and spectator.
- **Game rules** per world: Keep Inventory, Mob Griefing, Fire Spread, Nightmares, Sprinting, Tree Felling,
  Daylight Cycle, Weather Cycle, Mob Spawning, Fall Damage, Break Delay.
- **Player skins and capes** load again (Beta's own skin server is gone).

| Command | |
|---|---|
| `/help [command]` | List commands |
| `/seed` | Show the world seed |
| `/time <set\|add\|query> [value]` | Set, advance or show the time (`day`, `noon`, `night`... or ticks) |
| `/weather <clear\|rain\|thunder> [seconds]` | Set the weather |
| `/gamemode <mode>` | Survival, creative or spectator |
| `/gamerule [rule] [value]` | List, show or change game rules |
| `/give <player> <item> [amount]` | Give an item by name or id (TAB completes names) |
| `/tp <x> <y> <z>` | Teleport (`~` for relative) |
| `/sethome`, `/home`, `/delhome`, `/renamehome` | Named homes |
| `/back` | Return to where you were before the last teleport or death |
| `/spawn`, `/top` | World spawn; highest block above you |
| `/heal`, `/kill`, `/flyspeed <1-10>` | Full health; die; creative flying speed |

## Installation

Put `beta-essentials-<version>.jar` from the [releases](https://github.com/fgpa0/beta-essentials/releases) in a
Babric b1.7.3 instance's `mods/` folder (Fabric Loader 0.14 or newer). No other mods are needed.

## Settings

Options > Beta Essentials... (game rules of the open world and client settings), and Game Rules... on the Create
World screen. Game rules are stored in the world's `level.dat`, client settings in
`config/beta-essentials.properties`.

## Adding commands from another mod

Declare an entrypoint `"beta-essentials:commands"` implementing `dev.betaessentials.command.CommandProvider`, and
call `Commands.register(new Command(name, usage, description, aliases...) { ... })` from `registerCommands()`.
Registered commands get `/help` and TAB completion. Override `needsCheats()` to allow a command without cheats.

## Building

```bash
python3 build.py    # JDK 17+ and Python 3.8+ -> build/libs/beta-essentials-<version>.jar
```

Dependencies are downloaded into `build/cache/` and checked by SHA-256. Pushing a tag `v<version>` builds a GitHub
release.

## License

MIT, see [LICENSE](LICENSE). `mappings/named.tiny` is based on the
[BIN mappings](https://github.com/calmilamsy/BIN-Mappings).
