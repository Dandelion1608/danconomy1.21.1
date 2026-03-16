package com.danners45.danconomy.account;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccountManager {

    private static final Map<UUID, Account> accounts = new HashMap<>();

    public static Account getOrCreateAccount(UUID playerId){
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID cannot be null.");
        }
        return accounts.computeIfAbsent(playerId, id -> new Account());
    }
    public static Account getAccount(UUID playerId){
        if (playerId == null){
            return null;
        }
        return accounts.get(playerId);
    }
    public static boolean hasAccount(UUID playerId) {
        if (playerId == null) {
            return false;
        }
    return accounts.containsKey(playerId);
    }
    public static Map<UUID, Account> getAllAccounts() {
        return Map.copyOf(accounts);
    }
}
