package com.danners45.danconomy.shop;

import com.danners45.danconomy.permission.PermissionNodes;
import com.danners45.danconomy.permission.PermissionService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ShopService {
    private ShopService() {
    }

    public static boolean tryCreateShop(
            ServerPlayer player,
            BlockPos signPos,
            BlockPos storagePos,
            ShopEntry.Mode mode,
            String currencyId,
            long priceMinor,
            int amountPerTrade,
            boolean adminShop
    ) {
        ServerLevel level = player.serverLevel();
        ShopData shopData = ShopData.get(level);

        if (shopData.hasShopAt(signPos)) {
            player.sendSystemMessage(Component.literal("A shop already exists at this sign."));
            return false;
        }

        if (!adminShop) {
            ShopStorageValidator.QualificationResult storageResult =
                    ShopStorageValidator.qualifyLiveBlock(level, storagePos);

            if (!storageResult.qualified()) {
                ShopStorageValidator.logLiveQualificationFailure(level, storagePos, storageResult);
                player.sendSystemMessage(Component.literal(
                        "This block cannot be used as shop storage: " + storageResult.summaryReason()
                ));
                return false;
            }
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Hold the item you want this shop to trade when creating the shop."
            ));
            return false;
        }

        if (amountPerTrade <= 0) {
            player.sendSystemMessage(Component.literal("Amount per trade must be greater than zero."));
            return false;
        }

        if (priceMinor < 0) {
            player.sendSystemMessage(Component.literal("Price cannot be negative."));
            return false;
        }

        ItemStack templateItem = heldItem.copy();
        templateItem.setCount(1);

        ShopEntry entry = new ShopEntry(
                adminShop ? null : player.getUUID(),
                signPos,
                storagePos,
                adminShop,
                mode,
                currencyId,
                priceMinor,
                templateItem,
                amountPerTrade
        );

        shopData.putShop(entry);

        player.sendSystemMessage(Component.literal(
                "Shop created successfully as a " + (adminShop ? "ADMIN " : "") + mode.name() + " shop."
        ));
        return true;
    }

    public static boolean tryRemoveShop(ServerPlayer player, BlockPos signPos) {
        ServerLevel level = player.serverLevel();
        ShopData shopData = ShopData.get(level);

        ShopEntry entry = shopData.getShopBySignPos(signPos);
        if (entry == null) {
            return false;
        }

        if (!canManageShop(player, entry)) {
            player.sendSystemMessage(Component.literal("Vandalism isn't nice."));
            return false;
        }

        shopData.removeShopBySignPos(signPos);
        player.sendSystemMessage(Component.literal("Shop removed."));
        return true;
    }

    public static boolean canManageShop(ServerPlayer player, ShopEntry entry) {
        if (PermissionService.has(player.createCommandSourceStack(), PermissionNodes.ADMIN_SHOP)) {
            return true;
        }

        return entry.owner() != null && entry.owner().equals(player.getUUID());
    }

    public static ShopEntry getShop(ServerLevel level, BlockPos signPos) {
        return ShopData.get(level).getShopBySignPos(signPos);
    }
}