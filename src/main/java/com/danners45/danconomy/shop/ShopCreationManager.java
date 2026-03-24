package com.danners45.danconomy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShopCreationManager {
    private static final Map<UUID, PendingShopCreation> PENDING = new HashMap<>();

    private ShopCreationManager() {
    }

    public record PendingShopCreation(
            ShopEntry.Mode mode,
            int amountPerTrade,
            long priceMinor,
            String currencyId,
            boolean adminShop,
            @Nullable BlockPos storagePos
    ) {
        public PendingShopCreation withStorage(BlockPos storagePos) {
            return new PendingShopCreation(mode, amountPerTrade, priceMinor, currencyId, adminShop, storagePos);
        }

        public boolean hasStorage() {
            return storagePos != null;
        }
    }

    public static void startCreation(
            ServerPlayer player,
            ShopEntry.Mode mode,
            int amountPerTrade,
            long priceMinor,
            String currencyId,
            boolean adminShop
    ) {
        PENDING.put(player.getUUID(), new PendingShopCreation(
                mode,
                amountPerTrade,
                priceMinor,
                currencyId,
                adminShop,
                null
        ));
    }

    public static void clearCreation(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static boolean isCreating(ServerPlayer player) {
        return PENDING.containsKey(player.getUUID());
    }

    public static @Nullable PendingShopCreation getCreation(ServerPlayer player) {
        return PENDING.get(player.getUUID());
    }

    public static void setStorage(ServerPlayer player, BlockPos storagePos) {
        PendingShopCreation current = PENDING.get(player.getUUID());
        if (current != null) {
            PENDING.put(player.getUUID(), current.withStorage(storagePos));
        }
    }
}