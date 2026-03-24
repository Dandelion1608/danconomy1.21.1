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
        ServerLevel level = buyer.serverLevel();

        if (isOwnPlayerShop(buyer, entry)) {
            buyer.sendSystemMessage(Component.literal("You cannot buy from your own shop."));
            return false;
        }

        ShopStorageValidator.QualificationResult storageResult =
                ShopStorageValidator.qualifyLiveBlock(level, entry.storagePos());

        if (!storageResult.qualified()) {
            ShopStorageValidator.logLiveQualificationFailure(level, entry.storagePos(), storageResult);
            buyer.sendSystemMessage(Component.literal(
                    "This shop is currently unavailable: " + storageResult.summaryReason()
            ));
            return false;
        }

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, entry.storagePos(), null);
        if (handler == null) {
            buyer.sendSystemMessage(Component.literal("This shop is currently unavailable."));
            return false;
        }

        if (entry.amountPerTrade() <= 0) {
            buyer.sendSystemMessage(Component.literal("This shop is misconfigured."));
            return false;
        }

        if (entry.priceMinor() < 0) {
            buyer.sendSystemMessage(Component.literal("This shop has an invalid price."));
            return false;
        }

        final Currency currency;
        try {
            currency = CommandUtils.resolveCurrency(entry.currencyId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            buyer.sendSystemMessage(Component.literal("This shop has an invalid currency."));
            return false;
        }

        int stock = ShopInventoryHelper.countMatchingItems(handler, entry.templateItem());
        if (stock < entry.amountPerTrade()) {
            buyer.sendSystemMessage(Component.literal("This shop is out of stock."));
            return false;
        }

        if (!EconomyAccess.withdraw(buyer, currency, entry.priceMinor())) {
            buyer.sendSystemMessage(Component.literal(
                    "You do not have enough " + currency.getDisplayNamePlural() + "."
            ));
            return false;
        }

        ItemStack simulatedExtract = ShopInventoryHelper.extractMatchingItems(
                handler,
                entry.templateItem(),
                entry.amountPerTrade(),
                true
        );

        if (simulatedExtract.getCount() < entry.amountPerTrade()) {
            EconomyAccess.deposit(buyer, currency, entry.priceMinor());
            buyer.sendSystemMessage(Component.literal("This shop is out of stock."));
            return false;
        }

        ItemStack extracted = ShopInventoryHelper.extractMatchingItems(
                handler,
                entry.templateItem(),
                entry.amountPerTrade(),
                false
        );

        if (extracted.getCount() < entry.amountPerTrade()) {
            EconomyAccess.deposit(buyer, currency, entry.priceMinor());
            buyer.sendSystemMessage(Component.literal("Trade failed; you have been refunded."));
            return false;
        }

        ShopInventoryHelper.giveToPlayerOrDrop(buyer, extracted);

        if (entry.owner() != null) {
            EconomyAccess.deposit(level, entry.owner(), currency, entry.priceMinor());
            recordOfflineEarnings(level, entry.owner(), entry.currencyId(), entry.priceMinor());
        }

        buyer.sendSystemMessage(Component.literal(
                "Purchased "
                        + entry.amountPerTrade()
                        + "x "
                        + entry.templateItem().getHoverName().getString()
                        + " for "
                        + CommandUtils.formatAmount(entry.priceMinor(), currency)
                        + "."
        ));

        DanConomy.LOGGER.info(
                "[DanConomy/Shop] {} bought {} of {} from {}",
                buyer.getGameProfile().getName(),
                entry.amountPerTrade(),
                entry.templateItem().getHoverName().getString(),
                getShopOwnerName(level, entry)
        );

        return true;
    }

    public static boolean trySell(ServerPlayer seller, ShopEntry entry) {
        ServerLevel level = seller.serverLevel();

        if (isOwnPlayerShop(seller, entry)) {
            seller.sendSystemMessage(Component.literal("You cannot sell to your own shop."));
            return false;
        }

        if (entry.owner() == null) {
            seller.sendSystemMessage(Component.literal("This shop has no valid owner account."));
            return false;
        }

        ShopStorageValidator.QualificationResult storageResult =
                ShopStorageValidator.qualifyLiveBlock(level, entry.storagePos());

        if (!storageResult.qualified()) {
            ShopStorageValidator.logLiveQualificationFailure(level, entry.storagePos(), storageResult);
            seller.sendSystemMessage(Component.literal(
                    "This shop is currently unavailable: " + storageResult.summaryReason()
            ));
            return false;
        }

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, entry.storagePos(), null);
        if (handler == null) {
            seller.sendSystemMessage(Component.literal("This shop is currently unavailable."));
            return false;
        }

        if (entry.amountPerTrade() <= 0) {
            seller.sendSystemMessage(Component.literal("This shop is misconfigured."));
            return false;
        }

        if (entry.priceMinor() < 0) {
            seller.sendSystemMessage(Component.literal("This shop has an invalid price."));
            return false;
        }

        final Currency currency;
        try {
            currency = CommandUtils.resolveCurrency(entry.currencyId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            seller.sendSystemMessage(Component.literal("This shop has an invalid currency."));
            return false;
        }

        ItemStack requiredStack = entry.templateItem().copy();
        requiredStack.setCount(entry.amountPerTrade());

        int playerAmount = ShopInventoryHelper.countMatchingItemsInPlayer(seller, entry.templateItem());
        if (playerAmount < entry.amountPerTrade()) {
            seller.sendSystemMessage(Component.literal(
                    "You do not have enough of that item to sell."
            ));
            return false;
        }

        ItemStack insertRemainder = ShopInventoryHelper.insertItems(handler, requiredStack, true);
        if (!insertRemainder.isEmpty()) {
            seller.sendSystemMessage(Component.literal("This shop storage is full."));
            return false;
        }

        if (!EconomyAccess.hasFunds(level, entry.owner(), currency, entry.priceMinor())) {
            seller.sendSystemMessage(Component.literal("This shop owner cannot afford that trade right now."));
            return false;
        }

        ItemStack simulatedRemoval = ShopInventoryHelper.removeMatchingItemsFromPlayer(
                seller,
                entry.templateItem(),
                entry.amountPerTrade(),
                true
        );

        if (simulatedRemoval.getCount() < entry.amountPerTrade()) {
            seller.sendSystemMessage(Component.literal(
                    "You do not have enough of that item to sell."
            ));
            return false;
        }

        if (!EconomyAccess.withdraw(level, entry.owner(), currency, entry.priceMinor())) {
            seller.sendSystemMessage(Component.literal("This shop owner cannot afford that trade right now."));
            return false;
        }

        recordOfflineSpending(level, entry.owner(), entry.currencyId(), entry.priceMinor());

        ItemStack removed = ShopInventoryHelper.removeMatchingItemsFromPlayer(
                seller,
                entry.templateItem(),
                entry.amountPerTrade(),
                false
        );

        if (removed.getCount() < entry.amountPerTrade()) {
            ShopInventoryHelper.giveToPlayerOrDrop(seller, removed);
            EconomyAccess.deposit(level, entry.owner(), currency, entry.priceMinor());
            undoOfflineSpending(level, entry.owner(), entry.currencyId(), entry.priceMinor());
            seller.sendSystemMessage(Component.literal("Trade failed; your items were returned."));
            return false;
        }

        ItemStack leftover = ShopInventoryHelper.insertItems(handler, removed, false);
        if (!leftover.isEmpty()) {
            ShopInventoryHelper.giveToPlayerOrDrop(seller, removed);
            EconomyAccess.deposit(level, entry.owner(), currency, entry.priceMinor());
            undoOfflineSpending(level, entry.owner(), entry.currencyId(), entry.priceMinor());
            seller.sendSystemMessage(Component.literal("Trade failed; storage could not accept the items."));
            return false;
        }

        EconomyAccess.deposit(seller, currency, entry.priceMinor());

        seller.sendSystemMessage(Component.literal(
                "Sold "
                        + entry.amountPerTrade()
                        + "x "
                        + entry.templateItem().getHoverName().getString()
                        + " for "
                        + CommandUtils.formatAmount(entry.priceMinor(), currency)
                        + "."
        ));

        DanConomy.LOGGER.info(
                "[DanConomy/Shop] {} sold {} of {} to {}",
                seller.getGameProfile().getName(),
                entry.amountPerTrade(),
                entry.templateItem().getHoverName().getString(),
                getShopOwnerName(level, entry)
        );

        return true;
    }

    private static boolean tryAdminBuy(ServerPlayer buyer, ShopEntry entry) {
        final Currency currency;
        try {
            currency = CommandUtils.resolveCurrency(entry.currencyId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            buyer.sendSystemMessage(Component.literal("This shop has an invalid currency."));
            return false;
        }

        if (!EconomyAccess.withdraw(buyer, currency, entry.priceMinor())) {
            buyer.sendSystemMessage(Component.literal(
                    "You do not have enough " + currency.getDisplayNamePlural() + "."
            ));
            return false;
        }

        ItemStack stack = entry.templateItem().copy();
        stack.setCount(entry.amountPerTrade());
        ShopInventoryHelper.giveToPlayerOrDrop(buyer, stack);

        buyer.sendSystemMessage(Component.literal(
                "Purchased "
                        + entry.amountPerTrade()
                        + "x "
                        + entry.templateItem().getHoverName().getString()
                        + " for "
                        + CommandUtils.formatAmount(entry.priceMinor(), currency)
                        + "."
        ));

        DanConomy.LOGGER.info(
                "[DanConomy/Shop] {} bought {} of {} from {}",
                buyer.getGameProfile().getName(),
                entry.amountPerTrade(),
                entry.templateItem().getHoverName().getString(),
                "Admin Store"
        );

        return true;
    }

    private static boolean tryAdminSell(ServerPlayer seller, ShopEntry entry) {
        final Currency currency;
        try {
            currency = CommandUtils.resolveCurrency(entry.currencyId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            seller.sendSystemMessage(Component.literal("This shop has an invalid currency."));
            return false;
        }

        int playerAmount = ShopInventoryHelper.countMatchingItemsInPlayer(seller, entry.templateItem());
        if (playerAmount < entry.amountPerTrade()) {
            seller.sendSystemMessage(Component.literal("You do not have enough of that item to sell."));
            return false;
        }

        ItemStack removed = ShopInventoryHelper.removeMatchingItemsFromPlayer(
                seller,
                entry.templateItem(),
                entry.amountPerTrade(),
                false
        );

        if (removed.getCount() < entry.amountPerTrade()) {
            ShopInventoryHelper.giveToPlayerOrDrop(seller, removed);
            seller.sendSystemMessage(Component.literal("Trade failed; your items were returned."));
            return false;
        }

        EconomyAccess.deposit(seller, currency, entry.priceMinor());

        seller.sendSystemMessage(Component.literal(
                "Sold "
                        + entry.amountPerTrade()
                        + "x "
                        + entry.templateItem().getHoverName().getString()
                        + " for "
                        + CommandUtils.formatAmount(entry.priceMinor(), currency)
                        + "."
        ));

        DanConomy.LOGGER.info(
                "[DanConomy/Shop] {} sold {} of {} to {}",
                seller.getGameProfile().getName(),
                entry.amountPerTrade(),
                entry.templateItem().getHoverName().getString(),
                "Admin Store"
        );

        return true;
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
        if (ownerId == null) {
            return;
        }

        if (level.getServer().getPlayerList().getPlayer(ownerId) != null) {
            return;
        }

        LedgerData ledger = LedgerData.get(level);
        Account account = ledger.getOrCreateAccount(ownerId);
        account.addOfflineShopEarnings(currencyId, amount);
        ledger.markDirty();
    }

    private static void recordOfflineSpending(ServerLevel level, UUID ownerId, String currencyId, long amount) {
        if (ownerId == null) {
            return;
        }

        if (level.getServer().getPlayerList().getPlayer(ownerId) != null) {
            return;
        }

        LedgerData ledger = LedgerData.get(level);
        Account account = ledger.getOrCreateAccount(ownerId);
        account.addOfflineShopSpending(currencyId, amount);
        ledger.markDirty();
    }

    private static void undoOfflineSpending(ServerLevel level, UUID ownerId, String currencyId, long amount) {
        if (ownerId == null) {
            return;
        }

        if (level.getServer().getPlayerList().getPlayer(ownerId) != null) {
            return;
        }

        LedgerData ledger = LedgerData.get(level);
        Account account = ledger.getOrCreateAccount(ownerId);
        account.removeOfflineShopSpending(currencyId, amount);
        ledger.markDirty();
    }
}