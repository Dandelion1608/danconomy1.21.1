package com.danners45.danconomy.command;

import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.data.LedgerData;
import com.danners45.danconomy.permission.PermissionNodes;
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PayCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("pay")
                        .requires(source -> PermissionService.has(source, PermissionNodes.PAY))
                        .then(
                                Commands.argument("player", EntityArgument.player())
                                        .then(
                                                Commands.argument("amount", StringArgumentType.word())
                                                        .executes(ctx -> execute(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "amount"),
                                                                null))
                                                        .then(
                                                                Commands.argument("currency", StringArgumentType.word())
                                                                        .executes(ctx -> execute(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                                StringArgumentType.getString(ctx, "amount"),
                                                                                StringArgumentType.getString(ctx, "currency")))
                                                        )
                                        )
                        )
        );
    }

    private static int execute(CommandSourceStack source, ServerPlayer target, String amountInput, String currencyId) {
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }

        if (sender.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("You cannot pay yourself."));
            return 0;
        }

        try {
            Currency currency = CommandUtils.resolveCurrency(currencyId);

            if (!currency.isPayable()) {
                source.sendFailure(Component.literal("That currency cannot be paid."));
                return 0;
            }

            long amount = CommandUtils.parseAmountToMinorUnits(amountInput, currency);

            if (amount <= 0) {
                source.sendFailure(Component.literal("Amount must be greater than 0."));
                return 0;
            }

            LedgerData ledger = LedgerData.get(sender.serverLevel());
            Account senderAccount = ledger.getOrCreateAccount(sender.getUUID());
            Account targetAccount = ledger.getOrCreateAccount(target.getUUID());

            if (!senderAccount.withdraw(currency.getId(), amount)) {
                source.sendFailure(Component.literal("You do not have enough balance."));
                return 0;
            }

            targetAccount.deposit(currency.getId(), amount);
            ledger.markDirty();

            sender.sendSystemMessage(
                    Component.literal(
                            "You paid " + target.getName().getString() + " " + CommandUtils.formatAmount(amount, currency) + "."
                    )
            );

            target.sendSystemMessage(
                    Component.literal(
                            sender.getName().getString() + " has paid you " + CommandUtils.formatAmount(amount, currency) + "."
                    )
            );

            return 1;
        } catch (IllegalArgumentException | IllegalStateException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static boolean hasPermission(CommandSourceStack source, String node) {
        return source.hasPermission(0);
    }
}