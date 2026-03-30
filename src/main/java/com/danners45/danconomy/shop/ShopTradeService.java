package com.danners45.danconomy.shop;

import com.danners45.danconomy.DanConomy;
import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.account.AccountManager;
import com.danners45.danconomy.command.CommandUtils;
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.data.LedgerData;
import com.danners45.danconomy.economy.EconomyAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.UUID;

public final class ShopTradeService {
    private ShopTradeService() {
    }

    public static boolean interact(ServerPlayer player, ShopEntry entry) {
        if (entry.isCommandShop()) {
            return tryCommandShop(player, entry);
        }
        if (entry.adminShop()) {
            return switch (entry.mode()) {
                case BUY -> tryAdminBuy(player, entry);
                case SELL -> tryAdminSell(player, entry);
            };
        }
        return switch (entry.mode()) {
            case BUY -> tryBuy(player, entry);
            case SELL -> trySell(player, entry);
        };
    }

    public static boolean tryBuy(ServerPlayer buyer, ShopEntry entry) {
        if (isOwnPlayerShop(buyer, entry)) {
            return fail(buyer, "You cannot buy from your own shop.");
        }

        StorageAccess storage = requireStorageHandler(buyer, entry);
        if (storage == null) {
            return false;
        }

        if (entry.amountPerTrade() <= 0) {
            return fail(buyer, "This shop is misconfigured.");
        }
        if (entry.priceMinor() < 0) {
            return fail(buyer, "This shop has an invalid price.");
        }

        Currency currency = resolveCurrencyOrFail(buyer, entry, "This shop has an invalid currency.");
        if (currency == null) {
            return false;
        }

        IItemHandler handler = storage.handler();
        if (ShopInventoryHelper.countMatchingItems(handler, entry.templateItem()) < entry.amountPerTrade()) {
            return fail(buyer, "This shop is out of stock.");
        }

        if (!withdrawPlayerOrFail(buyer, currency, entry.priceMinor())) {
            return false;
        }

        ItemStack simulatedExtract = ShopInventoryHelper.extractMatchingItems(
                handler,
                entry.templateItem(),
                entry.amountPerTrade(),
                true
        );
        if (simulatedExtract.getCount() < entry.amountPerTrade()) {
            return refundAndFail(buyer, currency, entry.priceMinor(), "This shop is out of stock.");
        }

        ItemStack extracted = ShopInventoryHelper.extractMatchingItems(
                handler,
                entry.templateItem(),
                entry.amountPerTrade(),
                false
        );
        if (extracted.getCount() < entry.amountPerTrade()) {
            return refundAndFail(buyer, currency, entry.priceMinor(), "Trade failed; you have been refunded.");
        }

        ShopInventoryHelper.giveToPlayerOrDrop(buyer, extracted);

        if (entry.owner() != null) {
            ServerLevel level = buyer.serverLevel();
            EconomyAccess.deposit(level, entry.owner(), currency, entry.priceMinor());
            recordOfflineEarnings(level, entry.owner(), entry.currencyId(), entry.priceMinor());
        }

        sendBuySuccess(buyer, entry, currency);
        logBuy(buyer, entry, getShopOwnerName(buyer.serverLevel(), entry));
        return true;
    }

    public static boolean trySell(ServerPlayer seller, ShopEntry entry) {
        if (isOwnPlayerShop(seller, entry)) {
            return fail(seller, "You cannot sell to your own shop.");
        }
        if (entry.owner() == null) {
            return fail(seller, "This shop has no valid owner account.");
        }

        StorageAccess storage = requireStorageHandler(seller, entry);
        if (storage == null) {
            return false;
        }

        if (entry.amountPerTrade() <= 0) {
            return fail(seller, "This shop is misconfigured.");
        }
        if (entry.priceMinor() < 0) {
            return fail(seller, "This shop has an invalid price.");
        }

        Currency currency = resolveCurrencyOrFail(seller, entry, "This shop has an invalid currency.");
        if (currency == null) {
            return false;
        }

        if (ShopInventoryHelper.countMatchingItemsInPlayer(seller, entry.templateItem()) < entry.amountPerTrade()) {
            return fail(seller, "You do not have enough of that item to sell.");
        }

        IItemHandler handler = storage.handler();
        ItemStack requiredStack = entry.templateItem().copy();
        requiredStack.setCount(entry.amountPerTrade());

        ItemStack insertRemainder = ShopInventoryHelper.insertItems(handler, requiredStack, true);
        if (!insertRemainder.isEmpty()) {
            return fail(seller, "This shop storage is full.");
        }

        ServerLevel level = seller.serverLevel();
        if (!EconomyAccess.hasFunds(level, entry.owner(), currency, entry.priceMinor())) {
            return fail(seller, "This shop owner cannot afford that trade right now.");
        }

        ItemStack simulatedRemoval = ShopInventoryHelper.removeMatchingItemsFromPlayer(
                seller,
                entry.templateItem(),
                entry.amountPerTrade(),
                true
        );
        if (simulatedRemoval.getCount() < entry.amountPerTrade()) {
            return fail(seller, "You do not have enough of that item to sell.");
        }

        if (!EconomyAccess.withdraw(level, entry.owner(), currency, entry.priceMinor())) {
            return fail(seller, "This shop owner cannot afford that trade right now.");
        }

        recordOfflineSpending(level, entry.owner(), entry.currencyId(), entry.priceMinor());

        ItemStack removed = removePlayerItemsOrFail(
                seller,
                entry,
                "You do not have enough of that item to sell.",
                "Trade failed; your items were returned."
        );
        if (removed.isEmpty()) {
            EconomyAccess.deposit(level, entry.owner(), currency, entry.priceMinor());
            undoOfflineSpending(level, entry.owner(), entry.currencyId(), entry.priceMinor());
            return false;
        }

        ItemStack leftover = ShopInventoryHelper.insertItems(handler, removed, false);
        if (!leftover.isEmpty()) {
            ShopInventoryHelper.giveToPlayerOrDrop(seller, removed);
            EconomyAccess.deposit(level, entry.owner(), currency, entry.priceMinor());
            undoOfflineSpending(level, entry.owner(), entry.currencyId(), entry.priceMinor());
            return fail(seller, "Trade failed; storage could not accept the items.");
        }

        EconomyAccess.deposit(seller, currency, entry.priceMinor());
        sendSellSuccess(seller, entry, currency);
        logSell(seller, entry, getShopOwnerName(level, entry));
        return true;
    }

    private static boolean tryAdminBuy(ServerPlayer buyer, ShopEntry entry) {
        Currency currency = resolveCurrencyOrFail(buyer, entry, "This shop has an invalid currency.");
        if (currency == null) {
            return false;
        }

        if (!withdrawPlayerOrFail(buyer, currency, entry.priceMinor())) {
            return false;
        }

        ItemStack stack = entry.templateItem().copy();
        stack.setCount(entry.amountPerTrade());
        ShopInventoryHelper.giveToPlayerOrDrop(buyer, stack);

        sendBuySuccess(buyer, entry, currency);
        logBuy(buyer, entry, "Admin Store");
        return true;
    }

    private static boolean tryAdminSell(ServerPlayer seller, ShopEntry entry) {
        Currency currency = resolveCurrencyOrFail(seller, entry, "This shop has an invalid currency.");
        if (currency == null) {
            return false;
        }

        ItemStack removed = removePlayerItemsOrFail(
                seller,
                entry,
                "You do not have enough of that item to sell.",
                "Trade failed; your items were returned."
        );
        if (removed.isEmpty()) {
            return false;
        }

        EconomyAccess.deposit(seller, currency, entry.priceMinor());
        sendSellSuccess(seller, entry, currency);
        logSell(seller, entry, "Admin Store");
        return true;
    }

    private static boolean tryCommandShop(ServerPlayer player, ShopEntry entry) {
        Currency currency = resolveCurrencyOrFail(player, entry, "This command shop has an invalid currency.");
        if (currency == null) {
            return false;
        }
        if (entry.priceMinor() < 0) {
            return fail(player, "This command shop has an invalid price.");
        }

        String template = entry.commandTemplate();
        if (template == null || template.isBlank()) {
            return fail(player, "This command shop is misconfigured.");
        }

        if (entry.priceMinor() > 0 && !withdrawPlayerOrFail(player, currency, entry.priceMinor())) {
            return false;
        }

        String command = template
                .replace("{player}", player.getGameProfile().getName())
                .replace("{uuid}", player.getUUID().toString());

        try {
            String stripped = command.startsWith("/") ? command.substring(1) : command;

            player.serverLevel().getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack()
                            .withPermission(4)
                            .withSuppressedOutput(),
                    stripped
            );

            if (entry.priceMinor() > 0) {
                player.sendSystemMessage(Component.literal(
                        "Purchase successful for " + CommandUtils.formatAmount(entry.priceMinor(), currency) + "."
                ));
            } else {
                player.sendSystemMessage(Component.literal("Command executed."));
            }

            DanConomy.LOGGER.info(
                    "[DanConomy/Shop] {} executed command shop: {}",
                    player.getGameProfile().getName(),
                    stripped
            );
            return true;
        } catch (Exception e) {
            return refundAndFail(player, currency, entry.priceMinor(), "Command failed; you have been refunded.");
        }
    }

    private static boolean isOwnPlayerShop(ServerPlayer player, ShopEntry entry) {
        return !entry.adminShop()
                && entry.owner() != null
                && entry.owner().equals(player.getUUID());
    }

    private static String getShopOwnerName(ServerLevel level, ShopEntry entry) {
        if (entry.adminShop() || entry.owner() == null) {
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

    private static void recordOfflineEarnings(ServerLevel level, UUID ownerId, String currencyId, long amount) {
        if (ownerId == null || level.getServer().getPlayerList().getPlayer(ownerId) != null) {
            return;
        }

        LedgerData ledger = LedgerData.get(level);
        Account account = ledger.getOrCreateAccount(ownerId);
        account.addOfflineShopEarnings(currencyId, amount);
        ledger.markDirty();
    }

    private static void recordOfflineSpending(ServerLevel level, UUID ownerId, String currencyId, long amount) {
        if (ownerId == null || level.getServer().getPlayerList().getPlayer(ownerId) != null) {
            return;
        }

        LedgerData ledger = LedgerData.get(level);
        Account account = ledger.getOrCreateAccount(ownerId);
        account.addOfflineShopSpending(currencyId, amount);
        ledger.markDirty();
    }

    private static void undoOfflineSpending(ServerLevel level, UUID ownerId, String currencyId, long amount) {
        if (ownerId == null || level.getServer().getPlayerList().getPlayer(ownerId) != null) {
            return;
        }

        LedgerData ledger = LedgerData.get(level);
        Account account = ledger.getOrCreateAccount(ownerId);
        account.removeOfflineShopSpending(currencyId, amount);
        ledger.markDirty();
    }

    private record StorageAccess(IItemHandler handler) {
    }

    private static StorageAccess requireStorageHandler(ServerPlayer player, ShopEntry entry) {
        if (entry.storagePos() == null) {
            fail(player, "This shop is misconfigured.");
            return null;
        }

        ServerLevel level = player.serverLevel();
        ShopStorageValidator.QualificationResult result =
                ShopStorageValidator.qualifyLiveBlock(level, entry.storagePos());

        if (!result.qualified()) {
            ShopStorageValidator.logLiveQualificationFailure(level, entry.storagePos(), result);
            fail(player, "This shop is currently unavailable: " + result.summaryReason());
            return null;
        }

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, entry.storagePos(), null);
        if (handler == null) {
            fail(player, "This shop is currently unavailable.");
            return null;
        }

        return new StorageAccess(handler);
    }

    private static Currency resolveCurrencyOrFail(ServerPlayer player, ShopEntry entry, String invalidMessage) {
        try {
            return CommandUtils.resolveCurrency(entry.currencyId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            fail(player, invalidMessage);
            return null;
        }
    }

    private static boolean withdrawPlayerOrFail(ServerPlayer player, Currency currency, long amount) {
        if (!EconomyAccess.withdraw(player, currency, amount)) {
            fail(player, "You do not have enough " + currency.getDisplayNamePlural() + ".");
            return false;
        }
        return true;
    }

    private static boolean refundAndFail(ServerPlayer player, Currency currency, long amount, String message) {
        if (amount > 0) {
            EconomyAccess.deposit(player, currency, amount);
        }
        return fail(player, message);
    }

    private static ItemStack removePlayerItemsOrFail(
            ServerPlayer player,
            ShopEntry entry,
            String notEnoughMessage,
            String failedMessage
    ) {
        if (ShopInventoryHelper.countMatchingItemsInPlayer(player, entry.templateItem()) < entry.amountPerTrade()) {
            fail(player, notEnoughMessage);
            return ItemStack.EMPTY;
        }

        ItemStack removed = ShopInventoryHelper.removeMatchingItemsFromPlayer(
                player,
                entry.templateItem(),
                entry.amountPerTrade(),
                false
        );

        if (removed.getCount() < entry.amountPerTrade()) {
            ShopInventoryHelper.giveToPlayerOrDrop(player, removed);
            fail(player, failedMessage);
            return ItemStack.EMPTY;
        }

        return removed;
    }

    private static void sendBuySuccess(ServerPlayer player, ShopEntry entry, Currency currency) {
        player.sendSystemMessage(Component.literal(
                "Purchased "
                        + entry.amountPerTrade()
                        + "x "
                        + entry.templateItem().getHoverName().getString()
                        + " for "
                        + CommandUtils.formatAmount(entry.priceMinor(), currency)
                        + "."
        ));
    }

    private static void sendSellSuccess(ServerPlayer player, ShopEntry entry, Currency currency) {
        player.sendSystemMessage(Component.literal(
                "Sold "
                        + entry.amountPerTrade()
                        + "x "
                        + entry.templateItem().getHoverName().getString()
                        + " for "
                        + CommandUtils.formatAmount(entry.priceMinor(), currency)
                        + "."
        ));
    }

    private static void logBuy(ServerPlayer player, ShopEntry entry, String ownerName) {
        DanConomy.LOGGER.info(
                "[DanConomy/Shop] {} bought {} of {} from {}",
                player.getGameProfile().getName(),
                entry.amountPerTrade(),
                entry.templateItem().getHoverName().getString(),
                ownerName
        );
    }

    private static void logSell(ServerPlayer player, ShopEntry entry, String ownerName) {
        DanConomy.LOGGER.info(
                "[DanConomy/Shop] {} sold {} of {} to {}",
                player.getGameProfile().getName(),
                entry.amountPerTrade(),
                entry.templateItem().getHoverName().getString(),
                ownerName
        );
    }

    private static boolean fail(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
        return false;
    }
}