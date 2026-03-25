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
        String normalizedId = requireCurrencyId(currencyId);
        requireNonNegative(amount, "Balance cannot be negative.");

        balances.put(normalizedId, amount);
    }

    public void deposit(String currencyId, long amount) {
        addToBasket(balances, currencyId, amount, "Deposit amount cannot be negative.");
    }

    public boolean withdraw(String currencyId, long amount) {
        String normalizedId = requireCurrencyId(currencyId);
        requireNonNegative(amount, "Withdraw amount cannot be negative.");

        long currentBalance = getBalance(normalizedId);

        if (currentBalance < amount) {
            return false;
        }

        balances.put(normalizedId, currentBalance - amount);
        return true;
    }

    public void addOfflineShopEarnings(String currencyId, long amount) {
        addToBasket(offlineShopEarnings, currencyId, amount, "Offline earnings amount cannot be negative.");
    }

    public void removeOfflineShopEarnings(String currencyId, long amount) {
        takeFromBasket(offlineShopEarnings, currencyId, amount, "Offline earnings removal amount cannot be negative.");
    }

    public void addOfflineShopSpending(String currencyId, long amount) {
        addToBasket(offlineShopSpending, currencyId, amount, "Offline spending amount cannot be negative.");
    }

    public void removeOfflineShopSpending(String currencyId, long amount) {
        takeFromBasket(offlineShopSpending, currencyId, amount, "Offline spending removal amount cannot be negative.");
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

    // helper methods
    private String requireCurrencyId(String currencyId) {
        if (currencyId == null) {
            throw new IllegalArgumentException("Currency ID cannot be null.");
        }
        return normalizeCurrencyId(currencyId);
    }
    private static void requireNonNegative(long amount, String message) {
        if (amount < 0) {
            throw new IllegalArgumentException(message);
        }
    }
    private void addToBasket(Map<String, Long> basket, String currencyId, long amount, String message) {
        String normalizedId = requireCurrencyId(currencyId);
        requireNonNegative(amount, message);
        basket.put(normalizedId, basket.getOrDefault(normalizedId, 0L) + amount);
    }
    private void takeFromBasket(Map<String, Long> basket, String currencyId, long amount, String message) {
        String normalizedId = requireCurrencyId(currencyId);
        requireNonNegative(amount, message);

        long current = basket.getOrDefault(normalizedId, 0L);
        long updated = current - amount;

        if (updated > 0L) {
            basket.put(normalizedId, updated);
        } else {
            basket.remove(normalizedId);
        }
    }

}