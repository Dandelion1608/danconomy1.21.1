package com.danners45.danconomy.currency;

public class Currency {
    private final String id;
    private final String displayName;
    private final String symbol;

    public Currency(String id, String displayName, String symbol) {
        this.id = id;
        this.displayName = displayName;
        this.symbol = symbol;
    }
    public String getId(){
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
    public String getSymbol(){
        return symbol;
    }
}
