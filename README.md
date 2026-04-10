# AIO Bingo — RuneLite Plugin

RuneLite external plugin that tracks in-game OSRS events and submits them to the AIO Bingo API. Displays live board state, team progress, and leaderboard in a sidebar panel.

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java JDK | 11+ | `javac -version` to check (needs JDK, not just JRE) |
| Docker + Compose | Latest | For running the API + DB locally |

> **Note:** Gradle wrapper is included — no separate Gradle install needed. RuneLite client is downloaded automatically by `./gradlew run`.

### Installing JDK 11 (if needed)

```bash
# Ubuntu/Debian (WSL)
sudo apt install openjdk-11-jdk-headless

# Verify
javac -version   # should show "javac 11.x.x"
```

## Project Structure

```
aio-bingo-plugin/
├── build.gradle                     Gradle build config (RuneLite plugin hub format)
├── gradlew / gradlew.bat            Gradle wrapper (no install needed)
├── dev.sh                           Build + deploy script
├── runelite-plugin.properties       Plugin manifest
└── src/
    ├── main/java/com/bajansdk/aiobingo/
    │   ├── AioBingoPlugin.java      Main plugin (event hooks, queue, scheduling)
    │   ├── AioBingoConfig.java      Config interface (tokens, URL, toggles)
    │   ├── AioBingoPluginPanel.java Sidebar panel (board view, leaderboard)
    │   ├── BingoApiClient.java      HTTP client (OkHttp + Gson)
    │   ├── model/                   Data models (GameEvent, BingoBoard, etc.)
    │   └── ui/                      Panel UI components
    └── test/java/com/bajansdk/aiobingo/
        └── AioBingoPluginTest.java  Dev launcher (starts RuneLite with plugin)
```

---

## Step-by-Step: Running the Plugin with RuneLite

### Step 1: Build and launch RuneLite with the plugin

```bash
./gradlew run
```

**What this does:**
- Downloads the RuneLite client (first run takes 1-2 minutes)
- Compiles the plugin
- Launches RuneLite in developer mode with the plugin pre-loaded
- No need to install RuneLite separately — this IS RuneLite

> **First run:** Gradle downloads dependencies (~200MB). Subsequent runs are fast (~5s).

> **WSL users:** This works out of the box if you have WSLg (Windows 11). The RuneLite window opens on your Windows desktop. You can verify WSLg works by running `xclock` or `xeyes` first.

### Step 2: Configure the plugin in RuneLite

Once RuneLite opens:

1. Click the **wrench icon** (⚙) on the right sidebar to open Settings
2. Search for **"AIO Bingo"** in the plugin list
3. Make sure the plugin is **enabled** (toggle ON)
4. Click into the plugin settings and fill in:

| Setting | Value | Where to find it |
|---------|-------|-------------------|
| **Board Token** | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` | Web UI → Board → Settings tab |
| **Team Token** | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` | Web UI → Board → Teams tab |
| **API Base URL** | `http://localhost:8000` | Default, no change needed |

### Step 5: Log in to OSRS and test

1. Log into OSRS through the RuneLite client
2. Look for the **bingo panel** in the right sidebar (it should show your board)
3. Perform actions that match your tiles:
   - Pick up a drop (ITEM_DROP)
4. Events are batched and sent every **30 seconds**
5. The panel refreshes every **30 seconds**

---

## Architecture

```
Player action → RuneLite event hook → Plugin queues GameEvent
    → Batch POST /api/events/{teamToken}/batch (every 30s or 50 events)
    → API matches events against tile definitions → updates TileProgress

Plugin polls GET /api/board/{boardToken}/progress/{teamToken} every 30s
    → Updates sidebar panel UI
```

### Tracked Event Types

| Type | Hook | Detection |
|------|------|-----------|
| ITEM_DROP | `NpcLootReceived` | One event per loot item stack |
| PVP_KILL | `onPlayerLootReceived()` | One event per player loot item |

### Config Options

All config is in `AioBingoConfig.java`:

- **Connection section:** Board token, team token, API base URL
- **Tracking section:** Toggle each event type on/off

---

## Troubleshooting

### Using Jagex Accounts with the Dev Client

If you login via the Jagex Launcher, the dev client (`./gradlew run`) won't have your credentials by default. To fix this:

1. Open your **existing** RuneLite launcher (the Windows one launched via Jagex Launcher)
2. Run **RuneLite (configure)** from the Start Menu, or pass `--configure` to the launcher
3. In the **Client arguments** box, add: `--insecure-write-credentials`
4. Click **Save**
5. Launch RuneLite **once** through the Jagex Launcher — this writes your credentials to `~/.runelite/credentials.properties`
6. Now `./gradlew run` will automatically use those saved credentials to log in

> **Security note:** `credentials.properties` can be used to log into your account directly. Don't share it. Delete it when done with development. You can invalidate it via "End sessions" in runescape.com account settings.

### `./gradlew run` shows display errors
You need WSLg (Windows 11) or X11 forwarding. Test with `xeyes` first.

### Build fails with "does not provide JAVA_COMPILER"
You have JRE but not JDK. Install the full JDK:
```bash
sudo apt install openjdk-11-jdk-headless
```

### Plugin doesn't appear in RuneLite sidebar
- Ensure the plugin is enabled in Settings → search "AIO Bingo"
- If sideloading, restart RuneLite after copying the JAR

### Events aren't reaching the API
- Check API is running: `curl http://localhost:8000/health`
- Check API logs: `docker compose logs -f api`
- Verify tokens are correct in plugin settings
- Events batch every 30s — wait at least that long

---

## Data Usage & Privacy

### What data the plugin collects

The plugin tracks in-game OSRS events **only when enabled** and sends them to the configured API endpoint:

| Data Collected | Purpose |
|---------------|---------|
| **Player display name** | Identify which team member triggered an event |
| **Item drops** (item name + quantity) | Match drop-based bingo tiles |
| **Player Kills** (Player killed name) | Match count to bingo tiles |

### What data is NOT collected

- **No account credentials** (username, password, login tokens)
- **No personal information** (email, IP address, real name)
- **No financial information** (payment details, bank contents)
- **No chat history** — only specific regex-matched game messages are processed
- **No location data** (tile coordinates, world number)

### Data transmission

- Events are batched locally and sent via HTTPS POST every 30 seconds (or when 50 events accumulate)
- Data is sent only to the API endpoint configured in plugin settings (default: your self-hosted instance)
- No data is sent to any third-party service — all communication is between the plugin and your API server
- The plugin can be toggled off at any time; no events are tracked while disabled

### Data retention

- Event data is stored in the PostgreSQL database for scoring and leaderboard purposes
- Board owners can delete boards, which cascades to all associated events, progress, and team data
- No data is shared with Jagex, RuneLite, or any other party

### Third-party disclaimer

This plugin is a **third-party application**. It is not affiliated with, endorsed by, or associated with Jagex Ltd, Old School RuneScape, or RuneLite. Use of this plugin is at your own discretion and in accordance with Jagex's third-party client guidelines.

