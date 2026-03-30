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

        Currency currency = resolveCurrency(entry);
        String[] lines = entry.isCommandShop()
                ? buildCommandLines(level, entry, currency)
                : buildItemShopLines(level, entry, currency);

        sign.setText(
                sign.getFrontText()
                        .setMessage(0, Component.literal(lines[0]))
                        .setMessage(1, Component.literal(lines[1]))
                        .setMessage(2, Component.literal(lines[2]))
                        .setMessage(3, Component.literal(lines[3])),
                true
        );

        sign.setChanged();
        level.sendBlockUpdated(signPos, sign.getBlockState(), sign.getBlockState(), SignBlock.UPDATE_ALL);
    }

    private static String[] buildItemShopLines(ServerLevel level, ShopEntry entry, Currency currency) {
        String header = entry.mode() == ShopEntry.Mode.BUY ? "[Buy From]" : "[Sell To]";
        String ownerDisplay = getOwnerDisplay(level, entry);
        String itemDisplay = entry.amountPerTrade() + "x " + entry.templateItem().getHoverName().getString();
        String priceDisplay = formatPrice(entry, currency);

        return new String[] {
                header,
                ownerDisplay,
                itemDisplay,
                priceDisplay
        };
    }

    private static String[] buildCommandLines(ServerLevel level, ShopEntry entry, Currency currency) {
        return new String[] {
                "[COMMAND]",
                "Second Click to",
                getCommandDisplay(entry),
                formatPrice(entry, currency)
        };
    }

    private static Currency resolveCurrency(ShopEntry entry) {
        try {
            return CommandUtils.resolveCurrency(entry.currencyId());
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatPrice(ShopEntry entry, Currency currency) {
        if (entry.priceMinor() <= 0) {
            return "Free";
        }
        if (currency == null) {
            return entry.priceMinor() + " " + entry.currencyId();
        }
        return CommandUtils.formatAmount(entry.priceMinor(), currency);
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
    private static String getCommandDisplay(ShopEntry entry) {
        String command = entry.commandTemplate();
        if (command == null || command.isBlank()) {
            return "Unknown Command";
        }

        String stripped = command.startsWith("/") ? command.substring(1) : command;
        String condensed = stripped.trim().replaceAll("\\s+", " ");

        if (condensed.length() <= 15) {
            return condensed;
        }

        return condensed.substring(0, 15);
    }
}