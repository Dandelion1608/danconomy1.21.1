package com.danners45.danconomy.account;

import java.util.HashMap;
import java.util.Map;

public class Account {

    private String alias = "";
    private final Map<String, Long> balances = new HashMap<>();
    private final Map<String, Long> offlineShopEarnings = new HashMap<>();
    private final Map<String, Long> offlineShopSpending = new HashMap<>();

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        if (alias == null) {
            this.alias = "";
            return;
        }
        this.alias = alias.trim();
    }

    public long getBalance(String currencyId) {
        if (currencyId == null) {
            return 0L;
        }
        return balances.getOrDefault(normalizeCurrencyId(currencyId), 0L);
    }

    public void setBalance(String currencyId, long amount) {
        if (currencyId == null) {
            throw new IllegalArgumentException("Currency ID cannot be null.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        balances.put(normalizeCurrencyId(currencyId), amount);
    }

    public void deposit(String currencyId, long amount) {
        if (currencyId == null) {
            throw new IllegalArgumentException("Currency ID cannot be null.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Deposit amount cannot be negative.");
        }

        String normalizedId = normalizeCurrencyId(currencyId);
        long currentBalance = getBalance(normalizedId);
        balances.put(normalizedId, currentBalance + amount);
    }

    public boolean withdraw(String currencyId, long amount) {
        if (currencyId == null) {
            throw new IllegalArgumentException("Currency ID cannot be null.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Withdraw amount cannot be negative.");
        }

        String normalizedId = normalizeCurrencyId(currencyId);
        long currentBalance = getBalance(normalizedId);

        if (currentBalance < amount) {
            return false;
        }

        balances.put(normalizedId, currentBalance - amount);
        return true;
    }

    public void addOfflineShopEarnings(String currencyId, long amount) {
        if (currencyId == null) {
            throw new IllegalArgumentException("Currency ID cannot be null.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Offline earnings amount cannot be negative.");
        }

        String normalizedId = normalizeCurrencyId(currencyId);
        offlineShopEarnings.put(
                normalizedId,
                offlineShopEarnings.getOrDefault(normalizedId, 0L) + amount
        );
    }

    public void removeOfflineShopEarnings(String currencyId, long amount) {
        if (currencyId == null) {
            throw new IllegalArgumentException("Currency ID cannot be null.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Offline earnings removal amount cannot be negative.");
        }

        String normalizedId = normalizeCurrencyId(currencyId);
        long current = offlineShopEarnings.getOrDefault(normalizedId, 0L);
        long updated = current - amount;

        if (updated > 0L) {
            offlineShopEarnings.put(normalizedId, updated);
        } else {
            offlineShopEarnings.remove(normalizedId);
        }
    }

    public void addOfflineShopSpending(String currencyId, long amount) {
        if (currencyId == null) {
            throw new IllegalArgumentException("Currency ID cannot be null.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Offline spending amount cannot be negative.");
        }

        String normalizedId = normalizeCurrencyId(currencyId);
        offlineShopSpending.put(
                normalizedId,
                offlineShopSpending.getOrDefault(normalizedId, 0L) + amount
        );
    }

    public void removeOfflineShopSpending(String currencyId, long amount) {
        if (currencyId == null) {
            throw new IllegalArgumentException("Currency ID cannot be null.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Offline spending removal amount cannot be negative.");
        }

        String normalizedId = normalizeCurrencyId(currencyId);
        long current = offlineShopSpending.getOrDefault(normalizedId, 0L);
        long updated = current - amount;

        if (updated > 0L) {
            offlineShopSpending.put(normalizedId, updated);
        } else {
            offlineShopSpending.remove(normalizedId);
        }
    }

    public Map<String, Long> consumeOfflineShopEarnings() {
        Map<String, Long> snapshot = new HashMap<>(offlineShopEarnings);
        offlineShopEarnings.clear();
        return snapshot;
    }

    public Map<String, Long> consumeOfflineShopSpending() {
        Map<String, Long> snapshot = new HashMap<>(offlineShopSpending);
        offlineShopSpending.clear();
        return snapshot;
    }

    public Map<String, Long> getOfflineShopEarnings() {
        return Map.copyOf(offlineShopEarnings);
    }

    public Map<String, Long> getOfflineShopSpending() {
        return Map.copyOf(offlineShopSpending);
    }

    public Map<String, Long> getAllBalances() {
        return Map.copyOf(balances);
    }

    private String normalizeCurrencyId(String currencyId) {
        return currencyId.trim().toLowerCase();
    }
}