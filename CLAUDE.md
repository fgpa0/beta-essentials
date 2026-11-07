# Beta Essentials

Single-player commands, game rules, game modes and a cheats switch for Minecraft Beta 1.7.3 on Babric (Fabric).
Client only. Standalone: no dependencies on or references to other mods.

## Build and release

```bash
python3 build.py   # JDK 17+ and Python 3 -> build/libs/beta-essentials-<version>.jar
```

- `build.py` downloads the b1.7.3 client and pinned libraries (SHA-256 checked) into `build/cache/`, remaps the
  client to the names in `mappings/named.tiny` (BIN mappings), compiles, and remaps the jar back to intermediary
  with tiny-remapper `--mixin`. The `Cannot remap field_xxx` warnings are normal.
- When a mixin targets something new, check the built jar with `javap -v` that the annotation targets ended up as
  intermediary names; unremapped targets crash the game at launch.
- Release: bump `version` in `src/main/resources/fabric.mod.json`, add a `CHANGELOG.md` entry
  (`## x.y.z (YYYY-MM-DD)`, newest on top, a few plain bullets for players), commit, push a tag `v<version>`.
  `.github/workflows/build.yml` builds it and publishes a GitHub release.

## Rules

- Scope: commands, game rules, game modes, cheats, homes, player skins and the command API. Gameplay tweaks, bug
  fixes, recipes, interface options and world generators belong in other mods.
- No references to other mods (no compatibility checks by mod id, no integrations). Other mods use the command API.
- No personal names, emails or local paths in files, commits or release notes.
- Single player only: `Source.current()` returns null for the local player; keep the API shaped so multiplayer
  could be added without changing mods that use it.

## Architecture

- `Ess`: game access, messages, teleport (also across dimensions), `topY`. `PlayerState`: flight, sprinting, game
  mode effects, keep inventory. `TreeFeller`, `PickBlock`, `MobExplosions`: game rule and creative helpers.
- `settings/`: every option is a `Setting` (switch, number or choice, held as an int) registered in `Settings` with
  a scope (WORLD: `EssData.rules` in level.dat, also used by /gamerule; CLIENT: `config/beta-essentials.properties`,
  only changed values written as active lines), a category (tab: Game Rules, Client) and an optional group.
  `gui/SettingsScreen` shows them (Options > Beta Essentials..., Game Rules... on Create World).
- World data: `EssData` (cheats, game mode, rules, homes, /back position) in level.dat under `BetaEssentials`;
  worlds saved before the rename use `Essentials`, which is still read. The client settings file falls back to
  `config/essentials.properties` the same way.
- `command/`: `Commands` (registry, entrypoint `beta-essentials:commands`, parsing, TAB completion), `Command`
  (`needsCheats()`, `listed()`), `All` (the built-in commands), `Source` (who runs a command), `Players`.
- `gui/`: `EssChatScreen` (chat with commands, TAB, history), `CreativeScreen` (item picker), settings screen.
- `tweaks/Skins` + `mixin/ui/ImageDownloadMixin`: vanilla's image download thread gets the current Mojang texture
  address instead of the dead skin/cape URLs.
- Settings are read through a volatile field (`Setting.on()`), so code running on other threads may check them.
