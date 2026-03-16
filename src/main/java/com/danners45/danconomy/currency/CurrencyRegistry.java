package com.danners45.danconomy.currency;

import java.util.HashMap;
import java.util.Map;

public class CurrencyRegistry {

    private static final Map<String, Currency> currencies = new HashMap<>();

    public static void register (Currency currency){
        String id = currency.getId();

        if (currencies.containsKey(id)) {
            throw new IllegalArgumentException("Currency already registered: " + id);
        }

        currencies.put(id, currency);
    }

    public static Currency get(String id) {
        return currencies.get(id);
    }

    public static Map<String, Currency> getAll(){
        return Map.copyOf(currencies);
    }
}
