package com.danners45.danconomy.command;

import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.account.AccountManager;
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.currency.CurrencyRegistry;
import com.danners45.danconomy.permission.PermissionNodes;
import com.danners45.danconomy.permission.PermissionService;
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
                        )
                        .then(
                                Commands.literal("remove")
                                        .executes(ctx -> remove(ctx.getSource()))
                        )
                        .then(
                                Commands.literal("info")
                                        .executes(ctx -> info(ctx.getSource()))
                        )
                        .then(
                                Commands.literal("update")
                                        .executes(ctx -> update(ctx.getSource()))
                        )
                        .then(
                                Commands.literal("cancel")
                                        .executes(ctx -> cancel(ctx.getSource()))
                        )
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
            source.sendFailure(Component.literal("Only players can create shops."));
            return 0;
        }

        final Currency currency;
        try {
            currency = CommandUtils.resolveCurrency(currencyText);
        } catch (IllegalArgumentException | IllegalStateException e) {
            source.sendFailure(Component.literal("Unknown currency: " + currencyText));
            return 0;
        }

        final long priceMinor;
        try {
            priceMinor = CommandUtils.parseAmountToMinorUnits(priceText, currency);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid price: " + priceText));
            return 0;
        }

        if (priceMinor < 0) {
            source.sendFailure(Component.literal("Price cannot be negative."));
            return 0;
        }

        if (player.getMainHandItem().isEmpty()) {
            source.sendFailure(Component.literal("Hold the item you want this shop to trade."));
            return 0;
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
            player.sendSystemMessage(Component.literal(
                    "Admin shop creation started for a " + mode.name() + " shop. Right-click a sign."
            ));
        } else {
            player.sendSystemMessage(Component.literal(
                    "Shop creation started for a " + mode.name()
                            + " shop. Right-click storage first, then right-click a sign."
            ));
        }

        return 1;
    }

    private static int remove(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can remove shops."));
            return 0;
        }

        BlockPos signPos = getLookedAtSign(player);
        if (signPos == null) {
            source.sendFailure(Component.literal("Look at a shop sign first."));
            return 0;
        }

        ShopEntry entry = ShopService.getShop(player.serverLevel(), signPos);
        if (entry == null) {
            source.sendFailure(Component.literal("That sign is not a shop."));
            return 0;
        }

        return ShopService.tryRemoveShop(player, signPos) ? 1 : 0;
    }

    private static int info(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can inspect shops."));
            return 0;
        }

        BlockPos signPos = getLookedAtSign(player);
        if (signPos == null) {
            source.sendFailure(Component.literal("Look at a shop sign first."));
            return 0;
        }

        ShopEntry entry = ShopService.getShop(player.serverLevel(), signPos);
        if (entry == null) {
            source.sendFailure(Component.literal("That sign is not a shop."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Shop Info"), false);
        source.sendSuccess(() -> Component.literal("Mode: " + entry.mode().name()), false);
        source.sendSuccess(() -> Component.literal("Type: " + (entry.adminShop() ? "Admin Shop" : "Player Shop")), false);
        source.sendSuccess(() -> Component.literal("Item: " + entry.templateItem().getHoverName().getString()), false);
        source.sendSuccess(() -> Component.literal("Amount: " + entry.amountPerTrade()), false);

        try {
            Currency currency = CommandUtils.resolveCurrency(entry.currencyId());
            source.sendSuccess(() -> Component.literal("Price: " + CommandUtils.formatAmount(entry.priceMinor(), currency)), false);
        } catch (Exception e) {
            source.sendSuccess(() -> Component.literal("Price: " + entry.priceMinor() + " (" + entry.currencyId() + ")"), false);
        }

        if (!entry.adminShop()) {
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
            source.sendFailure(Component.literal("Only players can update shops."));
            return 0;
        }

        BlockPos signPos = getLookedAtSign(player);
        if (signPos == null) {
            source.sendFailure(Component.literal("Look at a shop sign first."));
            return 0;
        }

        ShopEntry entry = ShopService.getShop(player.serverLevel(), signPos);
        if (entry == null) {
            source.sendFailure(Component.literal("That sign is not a shop."));
            return 0;
        }

        if (!ShopService.canManageShop(player, entry)) {
            source.sendFailure(Component.literal("Vandalism isn't nice."));
            return 0;
        }

        ShopSignManager.writeFinalDisplay(player.serverLevel(), signPos, entry);
        player.sendSystemMessage(Component.literal("Shop updated."));
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
            source.sendFailure(Component.literal("Only players can cancel shop creation."));
            return 0;
        }

        ShopCreationManager.clearCreation(player);
        player.sendSystemMessage(Component.literal("Shop creation cancelled."));
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
}