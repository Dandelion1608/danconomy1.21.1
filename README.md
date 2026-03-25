# DanConomy

**DanConomy** is an economy mod for **Minecraft 1.21.1 on NeoForge**.

It adds player balances, payments, balance leaderboards, configurable currencies, optional Pixelmon integration, and sign-based shop support for servers that want a straightforward economy system without a large plugin stack.

---

## Features

- Player balances
- Player-to-player payments
- Balance leaderboard
- Admin balance management commands
- Multi-currency support
- Optional Pixelmon integration
- Sign/shop support with linked storage

---

## Commands

### Player commands

- `/bal` — View your balance
- `/pay <player> <amount> [currency]` — Pay another player
- `/baltop` — View the balance leaderboard

### Admin commands

- `/givebalance <player> <amount> [currency]` — Add balance to a player
- `/takebalance <player> <amount> [currency]` — Remove balance from a player
- `/setbalance <player> <amount> [currency]` — Set a player’s balance directly

### Shop commands

- `/shop create buy` — Create a buy shop
- `/shop create sell` — Create a sell shop

---

## Permissions

DanConomy supports both permission-handler setups and OP-based environments.

### Player permission nodes

- `danconomy.command.bal`
- `danconomy.command.pay`
- `danconomy.command.baltop`

### Admin permission nodes

- `danconomy.admin.give`
- `danconomy.admin.take`
- `danconomy.admin.set`

### Shop permission nodes

- `danconomy.shop.create.buy`
- `danconomy.shop.create.sell`

If permissions are disabled in config, command access can fall back to OP levels depending on your setup.

---

## Currencies

DanConomy supports multiple currencies.

Currency names, symbols, formatting, and related settings can be configured. This allows the mod to fit a variety of server styles, whether you want a simple default economy or a more customized setup.

If Pixelmon support is available and enabled, Pokédollars can be used as the default currency. If Pixelmon is not present, DanConomy can still run normally with your configured currencies.

---

## Pixelmon Integration

DanConomy can be used with or without Pixelmon.

When Pixelmon integration is available, the mod can use Pokédollars as a default currency option. Without Pixelmon, the mod continues to function as a standard configurable economy mod.

This integration is optional and is not required for the core balance, payment, leaderboard, or shop features.

---

## Shops

DanConomy includes sign/shop support using a linked storage block.

**By default, vanilla barrels are supported for shop creation.** Additional blocks can be added through the config, provided they register and expose an inventory that the shop system can use.

### Basic setup

1. Place a supported storage block.
2. Place a sign near the storage block.
3. Use `/shop create buy` or `/shop create sell`.
4. Follow the setup prompts in-game.

Shops are intended to provide a simple player-run trading system on top of the base economy features.

A more detailed setup guide will likely be better placed in a small wiki.

---

## Data Storage

Player account data is stored in the world save.

Current ledger location:

`data/economy_ledger.dat`

Because account data is saved with the world, you should back up your world data regularly if you are running the mod on an active server.

---

## Configuration

DanConomy is designed to be configurable.

Depending on your setup, configuration may include:
- default currency behavior
- permission handling
- currency names and symbols
- formatting options
- integration behavior
- supported shop storage blocks

Check the config files in your server or instance for the exact options available in your current version.

---

## Current Focus

DanConomy currently provides the core features needed for a server economy:
- balances
- payments
- leaderboards
- admin controls
- shop support

Additional features and improvements may be added over time.

---

## Notes

- Designed for **NeoForge 1.21.1**
- Works as a standalone economy mod
- Pixelmon integration is optional
- Shop support uses configurable inventory-backed storage blocks

---

## Planned Improvements

- Ongoing Codebase refactors
