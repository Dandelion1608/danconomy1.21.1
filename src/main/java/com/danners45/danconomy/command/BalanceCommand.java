package com.danners45.danconomy.command;

import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.data.LedgerData;
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.currency.CurrencyRegistry;
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
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                ctx.getSource().sendFailure(Component.literal("Players only."));
                                return 0;
                            }

                            LedgerData ledger = LedgerData.get(player.serverLevel());
                            Account account = ledger.getOrCreateAccount(player.getUUID());

                            if (CurrencyRegistry.getAll().size() == 1) {
                                Currency currency = CurrencyRegistry.getAll().values().iterator().next();
                                long balance = account.getBalance(currency.getId());

                                player.sendSystemMessage(
                                        Component.literal("Balance: " + CommandUtils.formatAmount(balance, currency))
                                );
                                return 1;
                            }

                            player.sendSystemMessage(Component.literal("=== Balances ==="));

                            for (Currency currency : CurrencyRegistry.getAll().values()) {
                                long balance = account.getBalance(currency.getId());
                                player.sendSystemMessage(
                                        Component.literal(
                                                currency.getId() + ": " + CommandUtils.formatAmount(balance, currency)
                                        )
                                );
                            }

                            return 1;
                        })
                        .then(
                                Commands.argument("currency", StringArgumentType.word())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                                ctx.getSource().sendFailure(Component.literal("Players only."));
                                                return 0;
                                            }

                                            String currencyId = StringArgumentType.getString(ctx, "currency");
                                            Currency currency = CurrencyRegistry.get(currencyId);

                                            if (currency == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown currency."));
                                                return 0;
                                            }

                                            LedgerData ledger = LedgerData.get(player.serverLevel());
                                            Account account = ledger.getOrCreateAccount(player.getUUID());
                                            long balance = account.getBalance(currency.getId());

                                            player.sendSystemMessage(
                                                    Component.literal("Balance: " + CommandUtils.formatAmount(balance, currency))
                                            );
                                            return 1;
                                        })
                        )
        );
    }

    private static boolean hasPermission(CommandSourceStack source) {
        return source.hasPermission(0);
    }
}