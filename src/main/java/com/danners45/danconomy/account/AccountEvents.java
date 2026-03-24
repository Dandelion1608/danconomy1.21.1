package com.danners45.danconomy.account;

import com.danners45.danconomy.DanConomy;
import com.danners45.danconomy.command.CommandUtils;
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.data.LedgerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;

public class AccountEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        LedgerData ledger = LedgerData.get(level);

        UUID playerId = player.getUUID();
        Account account = ledger.getOrCreateAccount(playerId);

        account.setAlias(player.getGameProfile().getName());

        sendOfflineShopSummary(player, account);

        ledger.markDirty();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        LedgerData ledger = LedgerData.get(level);

        ledger.markDirty();

        level.getServer().saveEverything(false, false, true);

        DanConomy.LOGGER.info(
                "Ledger saved on logout for player '{}'",
                player.getGameProfile().getName()
        );
    }

    private static void sendOfflineShopSummary(ServerPlayer player, Account account) {
        Map<String, Long> earnings = account.consumeOfflineShopEarnings();
        Map<String, Long> spending = account.consumeOfflineShopSpending();

        for (Map.Entry<String, Long> entry : earnings.entrySet()) {
            if (entry.getValue() <= 0L) {
                continue;
            }

            String formattedAmount = formatCurrency(entry.getKey(), entry.getValue());
            player.sendSystemMessage(Component.literal(
                    "You earned " + formattedAmount + " from store trades while you were offline."
            ));
        }

        for (Map.Entry<String, Long> entry : spending.entrySet()) {
            if (entry.getValue() <= 0L) {
                continue;
            }

            String formattedAmount = formatCurrency(entry.getKey(), entry.getValue());
            player.sendSystemMessage(Component.literal(
                    "You spent " + formattedAmount + " through store trades while you were offline."
            ));
        }
    }

    private static String formatCurrency(String currencyId, long amountMinor) {
        try {
            Currency currency = CommandUtils.resolveCurrency(currencyId);
            return CommandUtils.formatAmount(amountMinor, currency);
        } catch (Exception ignored) {
            return amountMinor + " (" + currencyId + ")";
        }
    }
}