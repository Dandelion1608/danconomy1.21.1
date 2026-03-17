package com.danners45.danconomy.currency;

public class Currency {

    public enum BackingType {
        LEDGER,
        PIXELMON_MIRRORED
    }

    private final String id;
    private final String displayNameSingular;
    private final String displayNamePlural;
    private final String symbol;
    private final int decimalPlaces;
    private final String formatStyle;
    private final boolean payable;
    private final BackingType backingType;

    public Currency(
            String id,
            String displayNameSingular,
            String displayNamePlural,
            String symbol,
            int decimalPlaces,
            String formatStyle,
            boolean payable
    ) {
        this(
                id,
                displayNameSingular,
                displayNamePlural,
                symbol,
                decimalPlaces,
                formatStyle,
                payable,
                BackingType.LEDGER
        );
    }

    public Currency(
            String id,
            String displayNameSingular,
            String displayNamePlural,
            String symbol,
            int decimalPlaces,
            String formatStyle,
            boolean payable,
            BackingType backingType
    ) {
        this.id = normalizeId(id);
        this.displayNameSingular = displayNameSingular;
        this.displayNamePlural = displayNamePlural;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
        this.formatStyle = formatStyle;
        this.payable = payable;
        this.backingType = backingType == null ? BackingType.LEDGER : backingType;
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

    public BackingType getBackingType() {
        return backingType;
    }

    public boolean isLedgerBacked() {
        return backingType == BackingType.LEDGER;
    }

    public boolean isPixelmonMirrored() {
        return backingType == BackingType.PIXELMON_MIRRORED;
    }

    private String normalizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Currency id cannot be null.");
        }

        return id.trim().toLowerCase();
    }
}