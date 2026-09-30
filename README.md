# Kalki SMP S2 - Discord Integration Plugin

A custom Minecraft Paper plugin built specifically for Kalki SMP S2 that bridges Minecraft and Discord. 

## Features

- **Bidirectional Chat Bridge:**
  - Minecraft to Discord chat routing.
  - Discord to Minecraft chat routing.
- **Server Notifications:**
  - Player Join / Leave notifications.
  - Player Death notifications.
  - Server Startup / Shutdown notifications.
- **Discord Status Panel (V2 Components):**
  - Persistent server status panel using modern Discord Components V2 (not legacy embeds).
  - Shows Server Status (Online), TPS, and Player Count.
  - Interactive "👥 Players" button: Displays an ephemeral paginated list of online players.
  - Interactive "🔄 Refresh" button: Forces an immediate status update.
  - LuckPerms Integration: Dynamically displays player ranks in the player list if LuckPerms is installed (with graceful fallback).
  - Background auto-updates for the status panel.

## Requirements

- Minecraft Paper 1.21.x
- Java 21
- [LuckPerms](https://luckperms.net/) (Optional, but recommended for rank integration)
- A Discord Bot Token with appropriate intents (Message Content, Server Members).

## Setup & Configuration

1. Place the compiled `.jar` file into your `plugins` folder.
2. Start the server to generate the default configuration files.
3. Stop the server and configure the plugin in `plugins/KalkiSMP/config.yml`.

### `config.yml`

```yaml
bot-token: "YOUR_DISCORD_BOT_TOKEN"
guild-id: "YOUR_GUILD_ID"
chat-channel-id: "YOUR_CHAT_CHANNEL_ID"

status:
  enabled: true
  channel-id: "YOUR_STATUS_CHANNEL_ID"
  update-interval-seconds: 60
```

4. Start the server. The plugin will automatically connect to Discord and start syncing chat. The status panel will be generated in the specified `status.channel-id`.

## Building from Source

To compile the plugin yourself, ensure you have Maven and Java 21 installed.

```bash
mvn clean package
```

The compiled artifact will be located in the `target/` directory.

## Architecture

- Pure Bukkit/Paper API for Minecraft operations.
- JDA (Java Discord API) for Discord interactions.
- All Discord operations are performed asynchronously to ensure the main server thread is never blocked.
- Uses Discord Components V2 (ActionRows, Buttons, MessageCreateBuilder, MessageEditBuilder).
