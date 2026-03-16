package com.danners45.danconomy.currency;

public class CurrencyBootstrap {
    public static void registerDefaults() {
        registerDefaultCurrency("dollar", "Dollar", "$");
    }
    private static void registerDefaultCurrency(String id, String displayName, String symbol) {
        Currency currency = new Currency(id, displayName, symbol);
        CurrencyRegistry.register(currency);
    }
}
