# InvChest

[简体中文](https://github.com/OutHimic/InvChest/blob/main/README.md)  |  [ENGLISH](https://github.com/OutHimic/InvChest/blob/main/README_EN.md)

A Bukkit plugin that automatically transfers items from barrels directly into players' pockets!
**Not recommended for use yet** as there are still a *few small bugs*: Transferred items remain in the barrel, and I don't know how to remove them qwq, resulting in infinite item duplication for players...
I don't know Java, I need technical help!!!!

## Features

- 🔗 **Barrel Binding Mechanism**: Bind barrels to player inventories via commands
- 🔄 **Automatic Item Transfer**: Items in bound barrels automatically transfer to player inventories
- 🌍 **Server-Wide Support**: Supports server-wide automatic transfer
- 🛡️ **Permission System**: Complete permission control to protect player data security
- 🌐 **Multi-Language Support**: Supports Chinese and English, easily extendable to other languages
- ⚙️ **Highly Configurable**: Customize various parameters through configuration files

## Requirements

- **Minecraft Version**: 1.21+ (Paper server recommended)
- **Java Version**: Java 17+
- **Backward Compatibility**: Java 8+ (Minecraft 1.12+), Java 17+ (Minecraft 1.18+)

## Installation

1. Download the latest `InvChest.jar` file
2. Place the file into your server's `plugins` folder
3. Restart the server
4. The plugin will automatically generate configuration and language files (Chinese & English)

## Commands

### Basic Commands
- `/invchest` - Bind the targeted barrel to your own inventory

### Advanced Commands
- `/invchest [PlayerID]` - Bind the targeted barrel to the specified player's inventory
- `/invchest reload` - Reload plugin configuration

## Permissions

- `invchest.bind` - Basic binding permission (default: true)
- `invchest.bind.others` - Bind to others permission (default: op)
- `invchest.reload` - Reload plugin permission (default: op)

## Configuration

The plugin will generate the following files in the `plugins/InvChest/` directory:

### config.yml
```yaml
# InvChest Configuration File
# Language settings, supports zh_cn, en_us, etc.
language: zh_cn

# Debug mode
debug: false

# Item transfer settings
transfer:
  # Automatic transfer interval (seconds)
  interval: 5
  # Whether to enable server-wide automatic transfer
  global-transfer: true
  # Whether to show notification messages during transfer (disabled by default to prevent server spam)
  show-messages: false

# Barrel binding settings
binding:
  # Maximum number of bound barrels (per player)
  max-bindings: 10
  # Whether to allow other players to open bound barrels
  allow-others-open: false
  # Whether to allow breaking bound barrels (enabled by default, automatically unbinds when broken)
  allow-break: true
