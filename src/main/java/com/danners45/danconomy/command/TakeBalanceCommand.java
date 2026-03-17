package com.danners45.danconomy.command;

import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.economy.EconomyAccess;
import com.danners45.danconomy.permission.PermissionNodes;
import com.danners45.danconomy.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TakeBalanceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("takebalance")
                        .requires(source -> PermissionService.has(source, PermissionNodes.ADMIN_TAKE))
                        .then(
                                Commands.argument("player", EntityArgument.player())
                                        .then(
                                                Commands.argument("amount", StringArgumentType.word())
                                                        .executes(ctx -> execute(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "amount"),
                                                                null
                                                        ))
                                                        .then(
                                                                Commands.argument("currency", StringArgumentType.word())
                                                                        .executes(ctx -> execute(
                                                                                ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "amount"),
                                                                                StringArgumentType.getString(ctx, "currency")
                                                                        ))
                                                        )
                                        )
                        )
        );
    }

    private static int execute(CommandSourceStack source, ServerPlayer target, String amountInput, String currencyId) {
        try {
            Currency currency = CommandUtils.resolveCurrency(currencyId);
            long amount = CommandUtils.parseAmountToMinorUnits(amountInput, currency);

            if (amount <= 0) {
                source.sendFailure(Component.literal("Amount must be greater than 0."));
                return 0;
            }

            if (!EconomyAccess.withdraw(target, currency, amount)) {
                source.sendFailure(Component.literal(
                        target.getName().getString() + " does not have enough " + currency.getDisplayNamePlural() + "."
                ));
                return 0;
            }

            long newBalance = EconomyAccess.getBalance(target, currency);
            String formattedAmount = CommandUtils.formatAmount(amount, currency);
            String formattedNewBalance = CommandUtils.formatAmount(newBalance, currency);

            source.sendSuccess(
                    () -> Component.literal(
                            "Took " + formattedAmount + " from " + target.getName().getString() + ". New balance: " + formattedNewBalance
                    ),
                    true
            );

            return 1;
        } catch (IllegalArgumentException | IllegalStateException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }
}