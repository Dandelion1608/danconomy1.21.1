package com.danners45.danconomy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public final class ShopEvents {
    private static final Component VANDALISM_MESSAGE = Component.literal("Vandalism isn't nice.");
    private static final Component STORAGE_LOCKED_MESSAGE = Component.literal("Hands off. This storage belongs to a shop.");

    private ShopEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (ShopCreationManager.isCreating(player)) {
            ShopCreationManager.PendingShopCreation pending = ShopCreationManager.getCreation(player);
            if (pending == null) {
                return;
            }

            event.setCanceled(true);

            if (pending.adminShop()) {
                if (!(state.getBlock() instanceof SignBlock)) {
                    player.sendSystemMessage(Component.literal("Right-click a sign to create the admin shop."));
                    return;
                }

                boolean created = ShopService.tryCreateShop(
                        player,
                        pos,
                        BlockPos.ZERO,
                        pending.mode(),
                        pending.currencyId(),
                        pending.priceMinor(),
                        pending.amountPerTrade(),
                        true
                );

                if (!created) {
                    return;
                }

                ShopEntry entry = ShopService.getShop(player.serverLevel(), pos);
                if (entry != null) {
                    ShopSignManager.writeFinalDisplay(player.serverLevel(), pos, entry);
                }

                ShopCreationManager.clearCreation(player);
                return;
            }

            if (!pending.hasStorage()) {
                ShopStorageValidator.QualificationResult result =
                        ShopStorageValidator.qualifyLiveBlock(player.serverLevel(), pos);

                if (!result.qualified()) {
                    ShopStorageValidator.logLiveQualificationFailure(player.serverLevel(), pos, result);
                    player.sendSystemMessage(Component.literal(
                            "That block cannot be used as shop storage: " + result.summaryReason()
                    ));
                    return;
                }

                ShopCreationManager.setStorage(player, pos);
                player.sendSystemMessage(Component.literal("Storage selected. Now right-click a sign."));
                return;
            }

            if (!(state.getBlock() instanceof SignBlock)) {
                player.sendSystemMessage(Component.literal("That is not a sign. Right-click a sign to finish setup."));
                return;
            }

            boolean created = ShopService.tryCreateShop(
                    player,
                    pos,
                    pending.storagePos(),
                    pending.mode(),
                    pending.currencyId(),
                    pending.priceMinor(),
                    pending.amountPerTrade(),
                    false
            );

            if (!created) {
                return;
            }

            ShopEntry entry = ShopService.getShop(player.serverLevel(), pos);
            if (entry != null) {
                ShopSignManager.writeFinalDisplay(player.serverLevel(), pos, entry);
            }

            ShopCreationManager.clearCreation(player);
            return;
        }

        if (state.getBlock() instanceof SignBlock) {
            ShopEntry entry = ShopService.getShop(player.serverLevel(), pos);
            if (entry == null) {
                return;
            }

            event.setCanceled(true);
            ShopTradeService.interact(player, entry);
            return;
        }

        ShopEntry storageShop = ShopData.get(player.serverLevel()).getFirstShopByStoragePos(pos);
        if (storageShop != null && !ShopService.canManageShop(player, storageShop)) {
            event.setCanceled(true);
            player.sendSystemMessage(STORAGE_LOCKED_MESSAGE);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof SignBlock) {
            ShopEntry signShop = ShopService.getShop(player.serverLevel(), pos);
            if (signShop == null) {
                return;
            }

            if (!ShopService.canManageShop(player, signShop)) {
                event.setCanceled(true);
                player.sendSystemMessage(VANDALISM_MESSAGE);
                return;
            }

            ShopData.get(player.serverLevel()).removeShopBySignPos(pos);
            player.sendSystemMessage(Component.literal("Shop removed."));
            return;
        }

        ShopEntry storageShop = ShopData.get(player.serverLevel()).getFirstShopByStoragePos(pos);
        if (storageShop != null && !ShopService.canManageShop(player, storageShop)) {
            event.setCanceled(true);
            player.sendSystemMessage(VANDALISM_MESSAGE);
        }
    }
}