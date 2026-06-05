# StarBridge

> A server-side [StarMade](https://www.star-made.org/) mod that bridges your game server and a Discord server — live chat relay, event and status notifications, in-game/Discord commands, and account linking.

StarBridge connects a running StarMade dedicated server to Discord. In-game chat is relayed to a Discord channel and back, server and gameplay events (joins, deaths, faction changes, crashes, restarts) are announced, and admins get a set of commands usable from both the game and Discord. It's **server-side only** — players don't need to install anything.

- **Mod version:** 3.0.0
- **Target StarMade:** 0.303.19
- **Platform:** StarLoader (`api.mod.StarMod`), Java 21
- **Discord library:** [JDA](https://github.com/discord-jda/JDA)
- **StarMadeDock:** https://starmadedock.net/content/starbridge.8253/

---

## Features

### Chat & event relay (game ↔ Discord)
- **Two-way chat** between in-game chat and a configured Discord chat channel.
- **Gameplay events** announced to Discord: player kills, deaths, and suicides; faction created; war declared, peace, alliance formed/broken; players joining/leaving factions.
- **Player session events**: first-time joins, joins, and leaves.
- **Server lifecycle**: starting, started (with boot time), stopping, scheduled stop/restart timers, restarting, and crashes.

Each message type targets the **chat** channel, the **log** channel, or **both** (see `bot/ChannelTarget` and `bot/MessageType`).

### Logging bridge
Server `INFO` / `WARNING` / `EXCEPTION` / `DEBUG` / `FATAL` log output is mirrored to a dedicated Discord **log channel**, so you can watch the server without shell access.

### Crash & error management
- `ServerCrashEvent` is caught, announced to Discord, and reported with the crash-report path and context.
- The **`ErrorManager`** fingerprints exceptions (by their top *N* stack frames), then **deduplicates, rate-limits, and lets you mute** recurring errors so Discord isn't flooded — tunable via the `errors` config.

### Commands (in-game and Discord)
| Command | Permission | What it does |
|---------|------------|--------------|
| `info` | `starbridge.view_info` | Show information about a player or faction |
| `link` | `starbridge.link_account` | Link a StarMade account to a Discord account |
| `list` | `starbridge.list_info` | List server/player information |
| `rename_system` | — | Give a star system a custom name |
| `errors` | `starbridge.error*` | Inspect / manage tracked errors |

Commands are defined once in `commands/CommandTypes` and exposed both as in-game chat commands and as Discord **slash commands** (with modal/button interactions via JDA). Access is gated by a **permission-group** system (`data/permissions`, e.g. `starbridge.*` nodes, admin role).

### Custom system names
`SystemNameGetEvent` is intercepted so star systems can be given human-readable names from the `systems` config (keyed by center-origin coordinates), via the `rename_system` command.

### Persistence
Player records and links are stored through `server/ServerDatabase` and `data/player/PlayerData` (with `JsonSerializable` data models), persisted via StarLoader's `PersistentObjectUtil`.

---

## Architecture

```
videogoose.starbridge
  StarBridge.java        # StarMod entry point: onEnable/onDisable, lifecycle, log routing
  bot/                   # Discord side — DiscordBot (JDA ListenerAdapter), MessageType,
                         #   ChannelTarget (CHAT/LOG/BOTH routing)
  commands/              # CommandTypes (the command registry), DiscordCommand,
                         #   ICommandExecutor — shared by in-game + slash commands
  manager/               # ConfigManager, EventManager (StarLoader event listeners), DataManager
  server/                # ServerDatabase — player/link persistence
  data/                  # Models: MessageData, PlayerData, config/, permissions/, other/ (Pair)
  error/                 # ErrorManager + ErrorEntry — fingerprint/dedup/rate-limit/mute
  utils/                 # DiscordUtils, PlayerUtils, DateUtils, ExceptionStreamWatcher, …
```

**Startup flow** (`StarBridge.onEnable`): initialize config → error manager → register StarLoader event listeners → connect the Discord bot → register game commands → announce "server started". A shutdown hook ensures a clean "server stopping" message and bot disconnect.

---

## Configuration

On first run StarBridge writes default config files (via StarLoader's `FileConfiguration`). Fill in your Discord details before using it.

**`config`** — main settings:

| Key | Description |
|-----|-------------|
| `debug-mode` | Extra debug output / Discord debug notices |
| `bot-name`, `bot-avatar` | Bot identity applied to the Discord account |
| `bot-token` | Your Discord bot token |
| `server-id` | Discord server (guild) ID |
| `chat-channel-id` | Channel for chat relay + event announcements |
| `log-channel-id` | Channel for server log output |
| `admin-role-id` | Discord role granted admin permissions |
| `restart-timer` | Auto-restart interval (ms) |
| `default-shutdown-timer` | Default delay for timed shutdowns (ms) |

**`systems`** — map of system center-origin coordinates → custom names.

**`errors`** — error-relay tuning: `enabled`, `min-post-interval-ms` (cooldown between repeats of the same error), `fingerprint-frames` (stack frames used to identify a recurring error).

---

## Building from source

Requires **JDK 21** and a local StarMade install (StarBridge compiles against `StarMade.jar` and the bundled `lib/`).

1. Set your StarMade path in `gradle.properties`:
   ```properties
   starmade_root=/path/to/StarMade/      # trailing slash required
   ```
   (or override per-build with `-Pstarmade_root=/path/to/StarMade/`)
2. Build:
   ```sh
   ./gradlew build
   ```

The build keeps `mod.json`'s version in sync with `mod_version` (see the `updateVersion` task).

## Installation

1. Build (above) or download the jar from [StarMadeDock](https://starmadedock.net/content/starbridge.8253/).
2. Drop the jar into your server's StarMade `mods/` folder.
3. Start the server once to generate the config files, fill in your Discord `bot-token`, `server-id`, and channel IDs, then restart.

This is a **server mod** (`server_mod: true`, `client_mod: false`) — only the server needs it.
