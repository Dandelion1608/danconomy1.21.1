//Economy account checker/creation utility
package com.danners45.danconomy.account;


import com.danners45.danconomy.DanConomy;
import com.danners45.danconomy.data.LedgerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;

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
}