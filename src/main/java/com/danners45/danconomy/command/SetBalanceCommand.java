package com.danners45.danconomy.command;

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

public class SetBalanceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setbalance")
                        .requires(source -> PermissionService.has(source, PermissionNodes.ADMIN_SET))
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
        try {
            Currency currency = CommandUtils.resolveCurrency(currencyId);
            long amount = CommandUtils.parseAmountToMinorUnits(amountInput, currency);

            LedgerData ledger = LedgerData.get(target.serverLevel());
            ledger.getOrCreateAccount(target.getUUID()).setBalance(currency.getId(), amount);
            ledger.markDirty();

            source.sendSuccess(
                    () -> Component.literal(
                            "Set " + target.getName().getString() + "'s balance to " + CommandUtils.formatAmount(amount, currency)
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