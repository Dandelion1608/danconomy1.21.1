# DanConomy

A lightweight multi-currency economy mod featuring basic commands and an optional pixelmon compatibility layer while supporting Permissions handlers or OP based environments

## Available Commands
### Playerfacing commands(permissions enabled by default):
* /Pay danconomy.command.pay
* /Bal danconomy.command.bal
* /Baltop danconomy.command.baltop
### Admin Commands (permissions false by default):
* /givebalance danconomy.admin.give
* /takebalance danconomy.admin.take
* /setbalance danconomy.admin.set

## Admin Accessible NBT Storage
all account data is held in a ledger located in the world folder for your save: /<world>/data/economy_ledger.dat

## Optional Pixelmon Integration
Runs With or Without Pixelmon - running with pixelmon will integrate the Pokedollars internal cash system with the virtual /bal system as the default currency - without pixelmon this mod will pull the default from the config file with space to run asmany additional currencies in tandem as you may need

Configurable starting balance, Currency Name, Symbol and Singular/Plural references as well as text format for command output results (IE: Alex paid you $100.50 Dollars or You paid Steve 100 Dollars 50 with alternative solutions available

##Now with Barrel/sign Shops

Create a Sign Shop by first placing a Storage container (barrel currently only vanilla barrels are supported) and a sign near by - Please Note: 1 sign, 1 Action (Buy/Sell) - Multiple Signs to a storage are possible

Use the command /Shop create buy <Quantity> <Price> <Currency> to start the process of creating a Sale Sign, Right click the Barrel, and with the item you want to sell in hand, Right Click the sign - the sign will then populate with the details:

[Buy From]
<playername>
<quantity>x <item>
<Currencysymbol><Price>
To create a Purchasing sign, do the same again but with the command /shop create sell <Quantity> <Price> <Currency> once complete this will populate the sign with the details:

[Sell To]
<playername>
<quantity>x <item>
<Currencysymbol><Price>
To buy or sell goods through these signs, simply right click,

Beware of setting the "Sell To" Price Higher than your "Buy From" Price, thats how you lose money

## Planned Features:

Additional Resources: 
==========
Community Documentation: https://docs.neoforged.net/  
NeoForged Discord: https://discord.neoforged.net/
