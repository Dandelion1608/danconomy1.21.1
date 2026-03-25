# DanConomy

**DanConomy** is an economy mod for **Minecraft 1.21.1 on NeoForge**.

It adds the usual economy basics for servers that want something simple: player balances, payments, balance leaderboards, configurable currencies, and player shops. Pixelmon integration is supported too, but it is completely optional.

---

## What it includes

- Player balances
- Player-to-player payments
- Balance leaderboards
- Admin balance management commands
- Multi-currency support
- Optional Pixelmon integration
- Sign-based shops linked to storage blocks

---

## Commands

### Player commands

- `/bal` — View your balance
- `/pay <player> <amount> [currency]` — Pay another player
- `/baltop` — View the balance leaderboard
- `/shop create buy <amount> <price> <currency>` — Start creating a buy shop
- `/shop create sell <amount> <price> <currency>` — Start creating a sell shop
- `/shop info` — View information about the shop sign you are looking at
- `/shop remove` — Remove the shop sign you are looking at
- `/shop update` — Refresh the shop sign you are looking at
- `/shop cancel` — Cancel the current shop creation process

### Admin commands

- `/givebalance <player> <amount> [currency]` — Add balance to a player
- `/takebalance <player> <amount> [currency]` — Remove balance from a player
- `/setbalance <player> <amount> [currency]` — Set a player’s balance directly
- `/shop create admin buy <amount> <price> <currency>` — Start creating an admin buy shop
- `/shop create admin sell <amount> <price> <currency>` — Start creating an admin sell shop

---

## Permissions

The mod can be used with either a permissions handler or a basic OP-based setup.

### Player permission nodes

- `danconomy.command.balance`
- `danconomy.command.pay`
- `danconomy.command.baltop`
- `danconomy.command.shop`

### Admin permission nodes

- `danconomy.admin.give`
- `danconomy.admin.take`
- `danconomy.admin.set`
- `danconomy.admin.shop`

If permissions are disabled in the config, command access can fall back to OP levels depending on how your server is set up.

---

## Currencies

Multiple currencies are supported.

Names, symbols, formatting, and related settings can all be changed in the config, so you can keep things simple or tailor the economy to your server.

If Pixelmon support is enabled, Pokédollars can be used as the default currency. Without Pixelmon, configured currencies continue to work normally.

---

## Pixelmon integration

Pixelmon support is optional.

When enabled, Pokédollars can be used as a default currency option. If Pixelmon is not installed, the rest of the mod still works normally for balances, payments, leaderboards, and shops.

---

## Shops

DanConomy includes player shops built around a storage block and a nearby sign.

**By default, vanilla barrels are supported for shop creation.** Other blocks can also be added through the config, as long as they expose an inventory that the shop system can interact with.

### Basic setup

1. Place a supported storage block.
2. Place a sign nearby.
3. Hold the item you want the shop to trade.
4. Use `/shop create buy <amount> <price> <currency>` or `/shop create sell <amount> <price> <currency>`.
5. Follow the prompts in-game.

Each sign handles one type of shop action, so buy and sell shops use separate signs.

### Managing shops

- Look at a shop sign and use `/shop info` to inspect it
- Look at a shop sign and use `/shop remove` to remove it
- Use `/shop update` to refresh the sign display
- Use `/shop cancel` to stop creating a shop before finishing

For player-run shops, make sure your pricing makes sense. Setting sell prices above your buy prices is an easy way to lose money.

---

## Data storage

Player account data is stored in the world save.

Current ledger location:

`data/economy_ledger.dat`

Because account data is saved with the world, regular backups are a good idea for active servers.

---

## Configuration

Most parts of the mod are configurable.

Depending on your setup, config options may include:

- default currency behavior
- permission handling
- currency names and symbols
- formatting options
- integration behavior
- supported shop storage blocks

Check the config files in your server or instance for the exact options available in your current version.

---

## Notes

- Designed for **NeoForge 1.21.1**
- Works as a standalone economy mod
- Pixelmon integration is optional
- Shop support uses configurable inventory-backed storage blocks

---

## Planned improvements

Possible future improvements may include:

- expanded shop features
- additional integrations
- ongoing codebase refactors
- further polish for configuration and documentation

---

## Documentation

This README covers the basics. More detailed setup information would probably fit better in a small wiki.

Possible wiki pages could include:

- shop setup guide
- permissions reference
- configuration reference
- currency setup examples
- Pixelmon integration notes
