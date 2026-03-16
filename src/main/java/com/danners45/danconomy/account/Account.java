package com.danners45.danconomy.account;

import java.util.HashMap;
import java.util.Map;


public class Account {

    private String alias = "";
    private final Map<String, Long> balances = new HashMap<>();


    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias){
        if (alias == null){
            this.alias = "";
            return;
        }
        this.alias = alias.trim();
    }
    public long getBalance(String currencyId){
        if (currencyId == null){
            return 0L;
        }
        return balances.getOrDefault(normalizeCurrencyId(currencyId), 0L);
    }
    public void setBalance(String currencyId, long amount){
        if (currencyId == null){
            throw new IllegalArgumentException("Currency ID Cannot be null.");
        }
        if (amount < 0){
            throw new IllegalArgumentException("Balance cannot be Negative.");
        }
        balances.put(normalizeCurrencyId(currencyId), amount);
    }

    public void deposit(String currencyId, long amount){
        if (currencyId == null){
            throw new IllegalArgumentException("Currency ID Cannot be NULL.");
        }
        if (amount < 0){
            throw new IllegalArgumentException("Deposit Amount cannot be Negative.");
        }
        String normalizedId = normalizeCurrencyId(currencyId);
        long currentBalance = getBalance(normalizedId);

        balances.put(normalizedId, currentBalance + amount);
    }
    public boolean withdraw(String currencyId, long amount){
        if (currencyId == null){
            throw new IllegalArgumentException("Currency ID Cannot be NULL.");
        }
        if (amount < 0){
            throw new IllegalArgumentException("Withdraw amount cannot be Negative.");
        }
        String normalizedId = normalizeCurrencyId(currencyId);
        long currentBalance = getBalance(normalizedId);

        if (currentBalance < amount) {
            return false;
        }
        balances.put(normalizedId, currentBalance - amount);
        return true;
    }

    public Map<String, Long> getAllBalances() {
        return Map.copyOf(balances);
    }
    private String normalizeCurrencyId(String currencyId){
        return currencyId.trim().toLowerCase();
    }
}

