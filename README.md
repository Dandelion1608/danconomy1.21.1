# DanConomy

**DanConomy** is an economy mod for **Minecraft 1.21.1 on NeoForge**.

It adds the core economy features most servers need: player balances, payments, balance leaderboards, configurable currencies, and player shops. Pixelmon integration is also supported, but completely optional.

## Features

- Player balances
- Player-to-player payments
- Balance leaderboards
- Admin balance management commands
- Multi-currency support
- Optional Pixelmon integration
- Sign-based shops linked to storage blocks
- Sign-based commands/shops


## Commands

### Player commands
- `/bal`
- `/pay <player> <amount> [currency]`
- `/baltop`
- `/shop create buy <amount> <price> <currency>`
- `/shop create sell <amount> <price> <currency>`
- `/shop info`
- `/shop remove`
- `/shop update`
- `/shop cancel`

### Admin commands
- `/givebalance <player> <amount> [currency]`
- `/takebalance <player> <amount> [currency]`
- `/setbalance <player> <amount> [currency]`
- `/shop create admin buy <amount> <price> <currency>`
- `/shop create admin sell <amount> <price> <currency>`
- /shop create command "<command...>" [price] [currency]

## Permissions

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

## Shop support

Shops can be created as storage-backed item shops, Admin Stores with infinite wallet/items, or command shops.

Item shops use a storage block and a nearby sign.

By default, vanilla barrels are supported for item shop creation. Other blocks can be added through the config as long as they expose an inventory the shop system can use.

Command shops are sign-based admin shops that run a configured command when interacted with rather than giving or taking items. They use the existing admin shop permission node and support optional pricing with any configured currency.

## Pixelmon support

Pixelmon integration is optional.

When enabled, Pokédollars can be used as a default currency option. Without Pixelmon installed, the rest of the mod still works normally.

## Data storage

Account data is stored in the world save at:

`data/danconomy_ledger.dat`
`data/danconomy_shops.dat`