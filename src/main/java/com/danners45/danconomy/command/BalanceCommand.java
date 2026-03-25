package com.danners45.danconomy.command;

import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.currency.CurrencyRegistry;
import com.danners45.danconomy.economy.EconomyAccess;
import com.danners45.danconomy.permission.PermissionNodes;
import com.danners45.danconomy.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BalanceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bal")
                        .requires(source -> PermissionService.has(source, PermissionNodes.BALANCE))
                        .executes(ctx -> showAllBalances(ctx.getSource()))
                        .then(
                                Commands.argument("currency", StringArgumentType.word())
                                        .executes(ctx -> showSpecificBalance(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "currency")
                                        ))
                        )
        );
    }

    private static int showAllBalances(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return CommandFeedback.fail(source, "Players only.");
        }

        if (CurrencyRegistry.getAll().size() == 1) {
            Currency currency = CurrencyRegistry.getAll().values().iterator().next();
            long balance = EconomyAccess.getBalance(player, currency);
            sendBalance(player, currency, balance);
            return 1;
        }

        player.sendSystemMessage(Component.literal("Balances:"));

        for (Currency currency : CurrencyRegistry.getAll().values()) {
            long balance = EconomyAccess.getBalance(player, currency);

            player.sendSystemMessage(
                    Component.literal("- " + currency.getDisplayNamePlural() + ": " + CommandUtils.formatAmount(balance, currency))
            );
        }

        return 1;
    }

    private static int showSpecificBalance(CommandSourceStack source, String currencyId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return CommandFeedback.fail(source, "Players only.");
        }

        Currency currency = CurrencyRegistry.get(currencyId);
        if (currency == null) {
            return CommandFeedback.fail(source, "Unknown currency: " + currencyId);
        }

        long balance = EconomyAccess.getBalance(player, currency);
        sendBalance(player, currency, balance);
        return 1;
    }

    private static void sendBalance(ServerPlayer player, Currency currency, long balance) {
        player.sendSystemMessage(
                Component.literal("Balance: " + CommandUtils.formatAmount(balance, currency))
        );
    }
}