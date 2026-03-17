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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public class BalTopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("baltop")
                        .requires(source -> PermissionService.has(source, PermissionNodes.BALTOP))
                        .executes(ctx -> execute(ctx.getSource(), null))
                        .then(
                                Commands.argument("currency", StringArgumentType.word())
                                        .executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "currency")))
                        )
        );
    }

    private static int execute(CommandSourceStack source, String currencyId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Players only."));
            return 0;
        }

        try {
            Currency currency = CommandUtils.resolveCurrency(currencyId);
            LedgerData ledger = LedgerData.get(player.serverLevel());

            player.sendSystemMessage(Component.literal("=== Balance Top (" + currency.getId() + ") ==="));

            ledger.getAllAccounts().entrySet().stream()
                    .sorted(
                            Comparator.<Map.Entry<UUID, Account>>comparingLong(
                                    entry -> entry.getValue().getBalance(currency.getId())
                            ).reversed()
                    )
                    .limit(10)
                    .forEach(entry -> {
                        String alias = entry.getValue().getAlias();
                        long balance = entry.getValue().getBalance(currency.getId());

                        player.sendSystemMessage(
                                Component.literal(alias + ": " + CommandUtils.formatAmount(balance, currency))
                        );
                    });

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