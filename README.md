# ⚔️ KalkiSMP — Custom Discord & Server Integration Plugin

> **Kalki SMP S2** custom Minecraft Paper plugin built for bidirectional Discord chat synchronization, event notifications, console log forwarding, and server management.

---

## 🚀 Features

- **💬 Minecraft ↔ Discord Chat Sync:**
  - In-game chat messages are forwarded to Discord in a clean format.
  - Discord messages appear in-game with customizable player formatting (`[Discord] User: Message`).
  - Ignores bot messages and webhooks.
  - Prevents ping exploitation (`@everyone` / `@here` / role pings are sanitized).
- **🔔 Player Event Notifications:**
  - ⚔ **Join:** Custom notification when a player joins the server.
  - 👋 **Quit:** Custom notification when a player leaves the server.
  - ☠ **Death:** Custom notification displaying the exact in-game death message.
- **🟢🔴 Server Status Notifications:**
  - Automated **Server Online** message on startup.
  - Automated **Server Offline** message on shutdown.
- **🖥️ Console → Discord Logging:**
  - Forwards console output to a designated Discord channel.
  - Rate-limited and batched to prevent hitting Discord API rate limits.
  - Automatic filtering of JDA internal spam, credentials, and token secrets.
- **⚡ Thread-Safe & Performant:**
  - All Discord network requests are non-blocking and run off the main server thread.
  - Minecraft chat broadcasts are safely scheduled on Bukkit's main server thread.
- **🛠️ Administrative Commands:**
  - `/kalkismp` — Displays plugin information.
  - `/kalkismp status` — Shows real-time connection status and feature toggles.
  - `/kalkismp reload` — Safely reloads configurations and reconnects Discord bot if needed.

---

## 📦 Requirements

* **Minecraft Server:** Paper 1.21.x
* **Java:** OpenJDK 21 or higher
* **Build Tool:** Apache Maven 3.8+

---

## 🔧 Building from Source

To compile and package the plugin JAR with shaded dependencies:

```bash
mvn clean package
```

The final shaded plugin JAR will be generated in:
```
target/kalkismp-1.0.0.jar
```

---

## 📥 Installation & Setup

1. Copy `target/kalkismp-1.0.0.jar` to your server's `plugins/` directory.
2. Start the server once to generate default configuration files in `plugins/KalkiSMP/`.
3. Open `plugins/KalkiSMP/config.yml` and configure your **Discord Bot Token** and **Channel IDs**:
   ```yaml
   discord:
     enabled: true
     token: "YOUR_DISCORD_BOT_TOKEN" # Or set KALKI_DISCORD_TOKEN environment variable

     channels:
       chat: "123456789012345678"     # In-game & Discord chat channel ID
       events: "123456789012345678"   # Join/Leave/Death/Status channel ID
       console: "123456789012345678"  # Console log channel ID
   ```
4. Restart the server or run `/kalkismp reload`.

---

## 🤖 Discord Bot Permissions & Privileged Intents

To ensure full functionality, set up your bot in the [Discord Developer Portal](https://discord.com/developers/applications):

1. **Privileged Gateway Intents:**
   - Enable **Message Content Intent** (`MESSAGE_CONTENT`) under **Bot → Privileged Gateway Intents**.
2. **Channel Permissions:**
   - View Channels
   - Send Messages
   - Embed Links
   - Read Message History

---

## 📜 Commands & Permissions

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/kalkismp` | Show basic plugin information | `kalkismp.admin` | OP |
| `/kalkismp status` | View connection status and channel IDs | `kalkismp.admin` | OP |
| `/kalkismp reload` | Reload configuration files safely | `kalkismp.admin` | OP |

---

## 🏗️ Project Architecture

```
src/main/java/live/shotdevs/kalkismp/
├── KalkiSMP.java              # Main plugin entrypoint
├── commands/
│   └── KalkiCommand.java      # Command executor & tab completer
├── config/
│   └── ConfigManager.java     # Config & environment variable loader
├── discord/
│   ├── DiscordListener.java   # Discord message listener
│   ├── DiscordManager.java    # JDA lifecycle & connection manager
│   └── DiscordMessageSender.java # Messaging helper
├── listeners/
│   ├── ChatListener.java      # Minecraft chat listener
│   ├── DeathListener.java     # Player death listener
│   └── JoinLeaveListener.java # Join & quit listener
├── logging/
│   └── DiscordLogHandler.java # Java logger handler for console output
└── util/
    └── MessageFormatter.java  # Color & mention formatting utility
```

---

## 📄 License

Developed specifically for **Kalki SMP S2** by `shotdevs`.
