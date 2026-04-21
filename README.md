# MailBox

[![Release](https://github.com/ikafly144/MailBox/actions/workflows/build.yml/badge.svg)](https://github.com/ikafly144/MailBox/actions/workflows/build.yml)
[![Build Status](https://ci.codemc.io/job/ikafly144/job/MailBox/badge/icon?subject=dev+build)](https://ci.codemc.io/job/ikafly144/job/MailBox/)
[![GitHub License](https://img.shields.io/github/license/ikafly144/MailBox)](https://github.com/ikafly144/MailBox/blob/master/COPYING.LESSER)

A powerful mail system plugin for Minecraft PaperMC servers that enables players to send and receive messages and items
through an intuitive GUI interface with economy integration.

## Version Support Policy

| Minecraft Version | Support Status         | Notes                  |
|-------------------|------------------------|------------------------|
| 26.1.x            | ✅ Main Target          | Current target version |
| 1.21.6 ~ 1.21.11  | ✅ Supported            | -                      |
| 1.21 ~ 1.21.5     | 🚧 No Longer Supported | Use legacy version     |
| < 1.20            | ❌ Not Supported        | Not compatible         |

**Support Policy:**

- Active support for current Minecraft version and recent minor versions
- No support for versions older than 1.20
- Legacy versions available for older servers

## Features

- **Intuitive GUI**: User-friendly interface for managing mail and items.
- **Economy Integration**: Seamless integration with popular economy plugins for item transactions.
- **Customisable Settings**: Flexible configuration options to tailor the plugin to your server's needs

## Installation

1. Download the latest version of MailBox from the [Releases](https://modrinth.com/plugin/mailbox) page.
2. Place the downloaded JAR file into your server's `plugins` directory.
3. Restart your server to generate the configuration files.
4. Customise the settings in the generated `config.yml` file as needed.
5. Use the `/mail` command to access the plugin's features and manage your mail.

## Quick Action

Press `G` to open the mail dialogue GUI, allowing you to quickly access your inbox and creating new mails without typing commands.

## Commands

- `/mail inbox` - Open the mail inbox GUI.
- `/sendmail <player>` - Send a mail to another player.
- `/mail reload` - Reload the plugin configuration.
- `/mail template` - Manage mail templates (admin only).
- `/mail template create <name>` - Create a new mail template (admin only).
- `/mail template delete <name>` - Delete an existing mail template (admin only).
- `/mail template edit <name> <field> <value>` - Edit a mail template field (admin only).
- `/mail template edit <name> attachment add <item>` - Add an item attachment to a mail template (admin only).
- `/mail template edit <name> attachment delete <item>` - Remove an item attachment from
- `/mail template edit <name> attachment list` - List all item attachments in a mail template (admin only).
- `/mail template send <player> <template>` - Send a mail using a template (admin only).

## APIs

- **MailBox API**: Provides methods for managing mails, templates, and attachments programmatically.

### Gradle

```gradle
repositories {
    maven {
        url 'https://repo.codemc.io/repository/ikafly144/'
    }
}

dependencies {
    compileOnly 'net.sabafly:mailbox-api:{version}'
}
```
