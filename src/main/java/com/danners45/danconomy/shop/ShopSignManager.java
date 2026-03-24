package com.danners45.danconomy.shop;

import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.account.AccountManager;
import com.danners45.danconomy.command.CommandUtils;
import com.danners45.danconomy.currency.Currency;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public final class ShopSignManager {
    private ShopSignManager() {
    }

    public static void writeFinalDisplay(ServerLevel level, BlockPos signPos, ShopEntry entry) {
        BlockEntity blockEntity = level.getBlockEntity(signPos);
        if (!(blockEntity instanceof SignBlockEntity sign)) {
            return;
        }

        final Currency currency;
        try {
            currency = CommandUtils.resolveCurrency(entry.currencyId());
        } catch (Exception e) {
            return;
        }

        String header = entry.mode() == ShopEntry.Mode.BUY ? "[Buy From]" : "[Sell To]";
        String ownerDisplay = getOwnerDisplay(level, entry);
        String itemDisplay = entry.amountPerTrade() + "x " + entry.templateItem().getHoverName().getString();
        String priceDisplay = CommandUtils.formatAmount(entry.priceMinor(), currency);

        sign.setText(
                sign.getFrontText()
                        .setMessage(0, Component.literal(header))
                        .setMessage(1, Component.literal(ownerDisplay))
                        .setMessage(2, Component.literal(itemDisplay))
                        .setMessage(3, Component.literal(priceDisplay)),
                true
        );

        sign.setChanged();
        level.sendBlockUpdated(signPos, sign.getBlockState(), sign.getBlockState(), SignBlock.UPDATE_ALL);
    }

    private static String getOwnerDisplay(ServerLevel level, ShopEntry entry) {
        if (entry.owner() == null) {
            return "Admin Store";
        }

        ServerPlayer onlinePlayer = level.getServer().getPlayerList().getPlayer(entry.owner());
        if (onlinePlayer != null) {
            return onlinePlayer.getGameProfile().getName();
        }

        try {
            Account account = AccountManager.getAccount(entry.owner());
            if (account != null) {
                String alias = account.getAlias();
                if (alias != null && !alias.isBlank()) {
                    return alias;
                }
            }
        } catch (Exception ignored) {
        }

        return entry.owner().toString();
    }
}