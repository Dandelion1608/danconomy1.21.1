package com.danners45.danconomy.data;

import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.currency.CurrencyRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LedgerData extends SavedData {

    private static final String DATA_NAME = "danconomy_ledger";

    private final Map<UUID, Account> accounts = new HashMap<>();

    public LedgerData() {
    }

    public static LedgerData create() {
        return new LedgerData();
    }

    public static LedgerData load(CompoundTag tag, HolderLookup.Provider registries) {
        LedgerData data = new LedgerData();

        CompoundTag accountsTag = tag.getCompound("accounts");
        for (String uuidString : accountsTag.getAllKeys()) {
            UUID uuid = UUID.fromString(uuidString);
            CompoundTag accountTag = accountsTag.getCompound(uuidString);
            data.accounts.put(uuid, readAccount(accountTag));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag accountsTag = new CompoundTag();

        for (Map.Entry<UUID, Account> entry : accounts.entrySet()) {
            accountsTag.put(entry.getKey().toString(), writeAccount(entry.getValue()));
        }

        tag.put("accounts", accountsTag);
        return tag;
    }

    public static LedgerData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();

        return storage.computeIfAbsent(
                new SavedData.Factory<>(LedgerData::create, LedgerData::load),
                DATA_NAME
        );
    }

    public Account getOrCreateAccount(UUID playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player id cannot be null.");
        }

        Account account = accounts.computeIfAbsent(playerId, id -> createNewAccount());
        setDirty();
        return account;
    }

    public Account getAccount(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        return accounts.get(playerId);
    }

    public boolean hasAccount(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        return accounts.containsKey(playerId);
    }

    public Map<UUID, Account> getAllAccounts() {
        return Map.copyOf(accounts);
    }

    public void markDirty() {
        setDirty();
    }

    private static Account readAccount(CompoundTag accountTag) {
        Account account = new Account();
        account.setAlias(accountTag.getString("alias"));

        readLongMap(accountTag.getCompound("balances"), account::setBalance);

        readLongMap(accountTag.getCompound("offlineShopEarnings"), (currencyId, amount) -> {
            if (amount > 0L) {
                account.addOfflineShopEarnings(currencyId, amount);
            }
        });

        readLongMap(accountTag.getCompound("offlineShopSpending"), (currencyId, amount) -> {
            if (amount > 0L) {
                account.addOfflineShopSpending(currencyId, amount);
            }
        });

        return account;
    }

    private static CompoundTag writeAccount(Account account) {
        CompoundTag accountTag = new CompoundTag();
        accountTag.putString("alias", account.getAlias());
        accountTag.put("balances", writeLongMap(account.getAllBalances()));
        accountTag.put("offlineShopEarnings", writeLongMap(account.getOfflineShopEarnings()));
        accountTag.put("offlineShopSpending", writeLongMap(account.getOfflineShopSpending()));
        return accountTag;
    }

    private Account createNewAccount() {
        Account newAccount = new Account();

        String currencyId = CurrencyRegistry.getDefaultCurrencyId();
        long startingBalance = CurrencyRegistry.getStartingBalance();

        if ((currencyId == null || currencyId.isBlank()) && CurrencyRegistry.getAll().size() == 1) {
            currencyId = CurrencyRegistry.getAll().keySet().iterator().next();
        }

        if (currencyId != null && !currencyId.isBlank() && startingBalance > 0) {
            newAccount.setBalance(currencyId, startingBalance);
        }

        return newAccount;
    }

    private static void readLongMap(CompoundTag tag, EntryConsumer consumer) {
        for (String key : tag.getAllKeys()) {
            consumer.accept(key, tag.getLong(key));
        }
    }

    private static CompoundTag writeLongMap(Map<String, Long> values) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            tag.putLong(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    @FunctionalInterface
    private interface EntryConsumer {
        void accept(String key, long amount);
    }
}