package com.danners45.danconomy.currency;

import java.util.HashMap;
import java.util.Map;

public class CurrencyRegistry {

    private static final Map<String, Currency> currencies = new HashMap<>();
    private static String defaultCurrencyId = "";
    private static long startingBalance = 0;
    private static boolean requireExplicitCurrencyIfAmbiguous = true;

    private CurrencyRegistry() {
    }

    public static void register(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Cannot register null currency.");
        }

        String id = normalizeId(currency.getId());

        if (id.isBlank()) {
            throw new IllegalArgumentException("Currency id cannot be blank.");
        }

        if (currencies.containsKey(id)) {
            throw new IllegalArgumentException("Currency already registered: " + id);
        }

        currencies.put(id, currency);
    }

    public static void registerOrReplace(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Cannot register null currency.");
        }

        String id = normalizeId(currency.getId());

        if (id.isBlank()) {
            throw new IllegalArgumentException("Currency id cannot be blank.");
        }

        currencies.put(id, currency);
    }

    public static Currency get(String id) {
        if (id == null) {
            return null;
        }

        return currencies.get(normalizeId(id));
    }

    public static boolean exists(String id) {
        if (id == null) {
            return false;
        }

        return currencies.containsKey(normalizeId(id));
    }

    public static Map<String, Currency> getAll() {
        return Map.copyOf(currencies);
    }

    public static int size() {
        return currencies.size();
    }

    public static void clear() {
        currencies.clear();
        defaultCurrencyId = "";
        startingBalance = 0;
        requireExplicitCurrencyIfAmbiguous = true;
    }

    public static void setDefaultCurrencyId(String id) {
        if (id == null || id.isBlank()) {
            defaultCurrencyId = "";
            return;
        }

        defaultCurrencyId = normalizeId(id);
    }

    public static String getDefaultCurrencyId() {
        return defaultCurrencyId;
    }

    public static void setStartingBalance(long amount) {
        startingBalance = amount;
    }

    public static long getStartingBalance() {
        return startingBalance;
    }

    public static void setRequireExplicitCurrencyIfAmbiguous(boolean value) {
        requireExplicitCurrencyIfAmbiguous = value;
    }

    public static boolean isRequireExplicitCurrencyIfAmbiguous() {
        return requireExplicitCurrencyIfAmbiguous;
    }

    private static String normalizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Currency id cannot be null.");
        }

        return id.trim().toLowerCase();
    }
}