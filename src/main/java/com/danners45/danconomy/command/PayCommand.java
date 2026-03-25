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

public class PayCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("pay")
                        .requires(source -> PermissionService.has(source, PermissionNodes.PAY))
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
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            return CommandFeedback.fail(source, "Players only.");
        }

        if (sender.getUUID().equals(target.getUUID())) {
            return CommandFeedback.fail(source, "You cannot pay yourself.");
        }

        try {
            Currency currency = CommandUtils.resolveCurrency(currencyId);

            if (!currency.isPayable()) {
                return CommandFeedback.fail(source, "That currency cannot be paid.");
            }

            long amount = CommandUtils.parseAmountToMinorUnits(amountInput, currency);

            if (amount <= 0) {
                return CommandFeedback.fail(source, "Amount must be greater than 0.");
            }

            if (!EconomyAccess.withdraw(sender, currency, amount)) {
                return CommandFeedback.fail(source, "You do not have enough " + currency.getDisplayNamePlural() + ".");
            }

            EconomyAccess.deposit(target, currency, amount);

            String formattedAmount = CommandUtils.formatAmount(amount, currency);
            notifyPayment(sender, target, formattedAmount);

				return 1;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return CommandFeedback.fail(source, e.getMessage());
        }
    }
    private static void notifyPayment(ServerPlayer sender, ServerPlayer target, String formattedAmount) {
        sender.sendSystemMessage(
                Component.literal("You paid " + target.getName().getString() + " " + formattedAmount + ".")
        );

        target.sendSystemMessage(
                Component.literal(sender.getName().getString() + " paid you " + formattedAmount + ".")
        );
    }
}