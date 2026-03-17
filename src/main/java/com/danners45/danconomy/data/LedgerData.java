//Saves economy NBT to /world/data/economy_ledger.dat
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

    private static final String DATA_NAME = "economy_ledger";

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

            Account account = new Account();
            account.setAlias(accountTag.getString("alias"));

            CompoundTag balancesTag = accountTag.getCompound("balances");

            for (String currencyId : balancesTag.getAllKeys()) {
                long amount = balancesTag.getLong(currencyId);
                account.setBalance(currencyId, amount);
            }

            data.accounts.put(uuid, account);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag accountsTag = new CompoundTag();

        for (Map.Entry<UUID, Account> entry : accounts.entrySet()) {
            UUID uuid = entry.getKey();
            Account account = entry.getValue();

            CompoundTag accountTag = new CompoundTag();
            accountTag.putString("alias", account.getAlias());

            CompoundTag balancesTag = new CompoundTag();

            for (Map.Entry<String, Long> balanceEntry : account.getAllBalances().entrySet()) {
                balancesTag.putLong(balanceEntry.getKey(), balanceEntry.getValue());
            }

            accountTag.put("balances", balancesTag);
            accountsTag.put(uuid.toString(), accountTag);
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

        Account account = accounts.computeIfAbsent(playerId, id -> {
            Account newAccount = new Account();

            String defaultCurrencyId = CurrencyRegistry.getDefaultCurrencyId();
            long startingBalance = CurrencyRegistry.getStartingBalance();

            if (defaultCurrencyId != null
                    && !defaultCurrencyId.isBlank()
                    && startingBalance > 0) {
                newAccount.setBalance(defaultCurrencyId, startingBalance);
            }

            return newAccount;
        });
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
}