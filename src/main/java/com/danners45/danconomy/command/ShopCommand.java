package com.danners45.danconomy.command;

import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.account.AccountManager;
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.currency.CurrencyRegistry;
import com.danners45.danconomy.permission.PermissionNodes;
import com.danners45.danconomy.permission.PermissionService;
import com.danners45.danconomy.shop.CommandShopValidator;
import com.danners45.danconomy.shop.ShopCreationManager;
import com.danners45.danconomy.shop.ShopEntry;
import com.danners45.danconomy.shop.ShopService;
import com.danners45.danconomy.shop.ShopSignManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.CompletableFuture;

public final class ShopCommand {
    private ShopCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("shop")
                        .requires(source -> PermissionService.has(source, PermissionNodes.SHOP))
                        .then(
                                Commands.literal("create")
                                        .then(
                                                Commands.literal("buy")
                                                        .then(
                                                                Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                        .then(
                                                                                Commands.argument("price", StringArgumentType.word())
                                                                                        .then(
                                                                                                Commands.argument("currency", StringArgumentType.word())
                                                                                                        .suggests((ctx, builder) -> suggestCurrencies(ctx.getSource(), builder))
                                                                                                        .executes(ctx -> start(
                                                                                                                ctx.getSource(),
                                                                                                                ShopEntry.Mode.BUY,
                                                                                                                IntegerArgumentType.getInteger(ctx, "amount"),
                                                                                                                StringArgumentType.getString(ctx, "price"),
                                                                                                                StringArgumentType.getString(ctx, "currency"),
                                                                                                                false
                                                                                                        ))
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("sell")
                                                        .then(
                                                                Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                        .then(
                                                                                Commands.argument("price", StringArgumentType.word())
                                                                                        .then(
                                                                                                Commands.argument("currency", StringArgumentType.word())
                                                                                                        .suggests((ctx, builder) -> suggestCurrencies(ctx.getSource(), builder))
                                                                                                        .executes(ctx -> start(
                                                                                                                ctx.getSource(),
                                                                                                                ShopEntry.Mode.SELL,
                                                                                                                IntegerArgumentType.getInteger(ctx, "amount"),
                                                                                                                StringArgumentType.getString(ctx, "price"),
                                                                                                                StringArgumentType.getString(ctx, "currency"),
                                                                                                                false
                                                                                                        ))
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("admin")
                                                        .requires(source -> PermissionService.has(source, PermissionNodes.ADMIN_SHOP))
                                                        .then(
                                                                Commands.literal("buy")
                                                                        .then(
                                                                                Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                                        .then(
                                                                                                Commands.argument("price", StringArgumentType.word())
                                                                                                        .then(
                                                                                                                Commands.argument("currency", StringArgumentType.word())
                                                                                                                        .suggests((ctx, builder) -> suggestCurrencies(ctx.getSource(), builder))
                                                                                                                        .executes(ctx -> start(
                                                                                                                                ctx.getSource(),
                                                                                                                                ShopEntry.Mode.BUY,
                                                                                                                                IntegerArgumentType.getInteger(ctx, "amount"),
                                                                                                                                StringArgumentType.getString(ctx, "price"),
                                                                                                                                StringArgumentType.getString(ctx, "currency"),
                                                                                                                                true
                                                                                                                        ))
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("sell")
                                                                        .then(
                                                                                Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                                        .then(
                                                                                                Commands.argument("price", StringArgumentType.word())
                                                                                                        .then(
                                                                                                                Commands.argument("currency", StringArgumentType.word())
                                                                                                                        .suggests((ctx, builder) -> suggestCurrencies(ctx.getSource(), builder))
                                                                                                                        .executes(ctx -> start(
                                                                                                                                ctx.getSource(),
                                                                                                                                ShopEntry.Mode.SELL,
                                                                                                                                IntegerArgumentType.getInteger(ctx, "amount"),
                                                                                                                                StringArgumentType.getString(ctx, "price"),
                                                                                                                                StringArgumentType.getString(ctx, "currency"),
                                                                                                                                true
                                                                                                                        ))
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("command")
                                                        .requires(source -> PermissionService.has(source, PermissionNodes.ADMIN_SHOP))
                                                        .then(
                                                                Commands.argument("raw", StringArgumentType.greedyString())
                                                                        .executes(ctx -> startCommandShop(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(ctx, "raw")
                                                                        ))
                                                        )
                                        )
                        )
                        .then(Commands.literal("remove").executes(ctx -> remove(ctx.getSource())))
                        .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                        .then(Commands.literal("update").executes(ctx -> update(ctx.getSource())))
                        .then(Commands.literal("cancel").executes(ctx -> cancel(ctx.getSource())))
        );
    }

    private static CompletableFuture<Suggestions> suggestCurrencies(
            CommandSourceStack source,
            SuggestionsBuilder builder
    ) {
        for (Currency currency : CurrencyRegistry.getAll().values()) {
            builder.suggest(currency.getId());
        }
        return builder.buildFuture();
    }

    private static int start(
            CommandSourceStack source,
            ShopEntry.Mode mode,
            int amount,
            String priceText,
            String currencyText,
            boolean adminShop
    ) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return CommandFeedback.fail(source, "Only players can create shops.");
        }

        Currency currency = tryResolveCurrency(source, currencyText);
        if (currency == null) {
            return 0;
        }

        Long priceMinor = tryParsePrice(source, priceText, currency);
        if (priceMinor == null) {
            return 0;
        }

        if (priceMinor < 0) {
            return CommandFeedback.fail(source, "Price cannot be negative.");
        }

        if (player.getMainHandItem().isEmpty()) {
            return CommandFeedback.fail(source, "Hold the item you want this shop to trade.");
        }

        ShopCreationManager.startCreation(
                player,
                mode,
                amount,
                priceMinor,
                currency.getId(),
                adminShop
        );

        if (adminShop) {
            source.sendSuccess(() -> Component.literal(
                    "Admin shop creation started for a " + mode.name() + " shop.\nInteract with a sign."
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Shop creation started for a " + mode.name() + " shop.\nInteract With storage first, then Interact With a sign."
            ), false);
        }

        return 1;
    }

    private static int startCommandShop(CommandSourceStack source, String raw) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return CommandFeedback.fail(source, "Only players can create command shops.");
        }

        ParsedCommandShopInput parsed = parseCommandShopInput(source, raw);
        if (parsed == null) {
            return 0;
        }

        Currency currency = tryResolveCurrency(source, parsed.currencyId());
        if (currency == null) {
            return 0;
        }

        CommandShopValidator.Result validation =
                CommandShopValidator.validate(player.serverLevel().getServer(), player, parsed.commandTemplate());

        if (!validation.valid()) {
            return CommandFeedback.fail(source, "Invalid command: " + validation.reason());
        }

        ShopCreationManager.startCommandCreation(
                player,
                parsed.commandTemplate(),
                parsed.priceMinor(),
                currency.getId()
        );

        if (parsed.priceMinor() > 0L) {
            source.sendSuccess(() -> Component.literal(
                    "Command shop creation started.\nCommand: " + parsed.commandTemplate()
                            + "\nPrice: " + CommandUtils.formatAmount(parsed.priceMinor(), currency)
                            + "\nInteract with a sign."
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Command shop creation started.\nCommand: " + parsed.commandTemplate()
                            + "\nPrice: Free\nInteract with a sign."
            ), false);
        }

        return 1;
    }

    private record ParsedCommandShopInput(
            String commandTemplate,
            long priceMinor,
            String currencyId
    ) {
    }

    private static ParsedCommandShopInput parseCommandShopInput(CommandSourceStack source, String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isBlank()) {
            CommandFeedback.fail(source, "Command cannot be empty.");
            return null;
        }

        if (!trimmed.startsWith("\"")) {
            CommandFeedback.fail(
                    source,
                    "Wrap the command in quotes. Example: /shop create command \"/warp @p Majestic Meadows\" 10 dollar"
            );
            return null;
        }

        int closingQuote = trimmed.indexOf('"', 1);
        if (closingQuote <= 1) {
            CommandFeedback.fail(source, "Command must be wrapped in quotes.");
            return null;
        }

        String command = trimmed.substring(1, closingQuote).trim();
        if (command.isBlank()) {
            CommandFeedback.fail(source, "Command cannot be empty.");
            return null;
        }

        String remainder = trimmed.substring(closingQuote + 1).trim();

        Currency fallbackCurrency = getFirstAvailableCurrency();
        if (fallbackCurrency == null) {
            CommandFeedback.fail(source, "No currency is configured.");
            return null;
        }

        if (remainder.isEmpty()) {
            return new ParsedCommandShopInput(command, 0L, fallbackCurrency.getId());
        }

        String[] tail = remainder.split("\\s+");
        if (tail.length == 1) {
            CommandFeedback.fail(source, "If price is specified, currency is mandatory.");
            return null;
        }

        if (tail.length != 2) {
            CommandFeedback.fail(source, "After the quoted command, use either nothing or: <price> <currency>");
            return null;
        }

        String priceText = tail[0];
        String currencyText = tail[1];

        Currency currency = tryResolveCurrency(source, currencyText);
        if (currency == null) {
            return null;
        }

        Long priceMinor = tryParsePrice(source, priceText, currency);
        if (priceMinor == null) {
            return null;
        }

        if (priceMinor < 0L) {
            CommandFeedback.fail(source, "Price cannot be negative.");
            return null;
        }

        return new ParsedCommandShopInput(command, priceMinor, currency.getId());
    }

    private static Currency tryResolveCurrency(CommandSourceStack source, String currencyText) {
        try {
            return CommandUtils.resolveCurrency(currencyText);
        } catch (IllegalArgumentException | IllegalStateException e) {
            CommandFeedback.fail(source, e.getMessage());
            return null;
        }
    }

    private static Long tryParsePrice(CommandSourceStack source, String priceText, Currency currency) {
        try {
            return CommandUtils.parseAmountToMinorUnits(priceText, currency);
        } catch (IllegalArgumentException e) {
            CommandFeedback.fail(source, e.getMessage());
            return null;
        }
    }

    private static Currency getCurrencyIfExists(String currencyId) {
        try {
            return CommandUtils.resolveCurrency(currencyId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return null;
        }
    }

    private static Currency getFirstAvailableCurrency() {
        for (Currency currency : CurrencyRegistry.getAll().values()) {
            return currency;
        }
        return null;
    }

    private static int remove(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return CommandFeedback.fail(source, "Only players can remove shops.");
        }

        ShopEntry entry = getLookedAtShopOrFail(source, player);
        if (entry == null) {
            return 0;
        }

        return ShopService.tryRemoveShop(player, entry.signPos()) ? 1 : 0;
    }

    private static int info(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return CommandFeedback.fail(source, "Only players can inspect shops.");
        }

        ShopEntry entry = getLookedAtShopOrFail(source, player);
        if (entry == null) {
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Shop Info"), false);
        source.sendSuccess(() -> Component.literal("Mode: " + entry.mode().name()), false);

        if (entry.isCommandShop()) {
            source.sendSuccess(() -> Component.literal("Type: Admin Command Shop"), false);
            source.sendSuccess(() -> Component.literal("Command: " + entry.commandTemplate()), false);

            Currency currency = getCurrencyIfExists(entry.currencyId());
            if (currency != null) {
                String priceText = entry.priceMinor() <= 0L
                        ? "Free"
                        : CommandUtils.formatAmount(entry.priceMinor(), currency);
                source.sendSuccess(() -> Component.literal("Price: " + priceText), false);
            } else {
                source.sendSuccess(() -> Component.literal(
                        "Price: " + entry.priceMinor() + " (" + entry.currencyId() + ")"
                ), false);
            }

            source.sendSuccess(() -> Component.literal("Sign: " + entry.signPos().toShortString()), false);
            source.sendSuccess(() -> Component.literal("Owner: Admin Store"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Type: " + (entry.adminShop() ? "Admin Shop" : "Player Shop")), false);
        source.sendSuccess(() -> Component.literal("Item: " + entry.templateItem().getHoverName().getString()), false);
        source.sendSuccess(() -> Component.literal("Amount: " + entry.amountPerTrade()), false);

        Currency currency = getCurrencyIfExists(entry.currencyId());
        if (currency != null) {
            source.sendSuccess(() -> Component.literal(
                    "Price: " + CommandUtils.formatAmount(entry.priceMinor(), currency)
            ), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Price: " + entry.priceMinor() + " (" + entry.currencyId() + ")"
            ), false);
        }

        if (!entry.adminShop() && entry.storagePos() != null) {
            source.sendSuccess(() -> Component.literal("Storage: " + entry.storagePos().toShortString()), false);
        }

        source.sendSuccess(() -> Component.literal("Sign: " + entry.signPos().toShortString()), false);

        if (entry.adminShop()) {
            source.sendSuccess(() -> Component.literal("Owner: Admin Store"), false);
        } else if (ShopService.canManageShop(player, entry)) {
            String ownerDisplay = getOwnerDisplay(entry);
            source.sendSuccess(() -> Component.literal("Owner: " + ownerDisplay), false);
        }

        return 1;
    }

    private static int update(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return CommandFeedback.fail(source, "Only players can update shops.");
        }

        ShopEntry entry = getLookedAtShopOrFail(source, player);
        if (entry == null) {
            return 0;
        }

        if (!ShopService.canManageShop(player, entry)) {
            return CommandFeedback.fail(source, "Vandalism isn't nice.");
        }

        ShopSignManager.writeFinalDisplay(player.serverLevel(), entry.signPos(), entry);
        source.sendSuccess(() -> Component.literal("Shop updated."), false);
        return 1;
    }

    private static String getOwnerDisplay(ShopEntry entry) {
        if (entry.owner() == null) {
            return "Admin Store";
        }

        try {
            Account account = AccountManager.getAccount(entry.owner());
            if (account != null) {
                String alias = account.getAlias();
                if (alias != null && !alias.isBlank()) {
                    return alias;
                }
            }
        } catch (Exception ignored) {
        }

        return entry.owner().toString();
    }

    private static int cancel(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return CommandFeedback.fail(source, "Only players can cancel shop creation.");
        }

        ShopCreationManager.clearCreation(player);
        source.sendSuccess(() -> Component.literal("Shop creation cancelled."), false);
        return 1;
    }

    private static BlockPos getLookedAtSign(ServerPlayer player) {
        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return null;
        }

        BlockPos pos = blockHit.getBlockPos();
        if (!(player.serverLevel().getBlockState(pos).getBlock() instanceof SignBlock)) {
            return null;
        }

        return pos;
    }

    private static ShopEntry getLookedAtShopOrFail(CommandSourceStack source, ServerPlayer player) {
        BlockPos signPos = getLookedAtSign(player);
        if (signPos == null) {
            CommandFeedback.fail(source, "Look at a shop sign first.");
            return null;
        }

        ShopEntry entry = ShopService.getShop(player.serverLevel(), signPos);
        if (entry == null) {
            CommandFeedback.fail(source, "That sign is not a shop.");
            return null;
        }

        return entry;
    }
}