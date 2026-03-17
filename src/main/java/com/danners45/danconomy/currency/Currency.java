package com.danners45.danconomy.currency;

public class Currency {

    private final String id;
    private final String displayNameSingular;
    private final String displayNamePlural;
    private final String symbol;
    private final int decimalPlaces;
    private final String formatStyle;
    private final boolean payable;

    public Currency(
            String id,
            String displayNameSingular,
            String displayNamePlural,
            String symbol,
            int decimalPlaces,
            String formatStyle,
            boolean payable
    ) {
        this.id = normalizeId(id);
        this.displayNameSingular = displayNameSingular;
        this.displayNamePlural = displayNamePlural;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
        this.formatStyle = formatStyle;
        this.payable = payable;
    }

    public String getId() {
        return id;
    }

    public String getDisplayNameSingular() {
        return displayNameSingular;
    }

    public String getDisplayNamePlural() {
        return displayNamePlural;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public String getFormatStyle() {
        return formatStyle;
    }

    public boolean isPayable() {
        return payable;
    }

    private String normalizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Currency id cannot be null.");
        }

        return id.trim().toLowerCase();
    }
}