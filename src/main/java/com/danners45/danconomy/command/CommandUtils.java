package com.danners45.danconomy.command;

import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.currency.CurrencyRegistry;

public final class CommandUtils {

    private CommandUtils() {
    }

    public static Currency resolveCurrency(String explicitCurrencyId) {
        if (explicitCurrencyId != null && !explicitCurrencyId.isBlank()) {
            Currency currency = CurrencyRegistry.get(explicitCurrencyId);

            if (currency == null) {
                throw new IllegalArgumentException("Unknown currency: " + explicitCurrencyId);
            }

            return currency;
        }

        int currencyCount = CurrencyRegistry.getAll().size();

        if (currencyCount == 1) {
            return CurrencyRegistry.getAll().values().iterator().next();
        }

        String defaultCurrencyId = CurrencyRegistry.getDefaultCurrencyId();

        if (defaultCurrencyId != null && !defaultCurrencyId.isBlank()) {
            Currency currency = CurrencyRegistry.get(defaultCurrencyId);

            if (currency == null) {
                throw new IllegalStateException("Configured default currency does not exist: " + defaultCurrencyId);
            }

            return currency;
        }

        throw new IllegalArgumentException("Multiple currencies exist. Please specify a currency.");
    }

    public static long parseAmountToMinorUnits(String input, Currency currency) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Amount cannot be blank.");
        }

        String trimmed = input.trim();

        if (!trimmed.matches("\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("Invalid amount: " + input);
        }

        String[] parts = trimmed.split("\\.", 2);

        String wholePart = parts[0];
        String fractionalPart = parts.length > 1 ? parts[1] : "";

        int decimalPlaces = currency.getDecimalPlaces();

        if (decimalPlaces == 0) {
            if (!fractionalPart.isEmpty()) {
                throw new IllegalArgumentException(
                        "Currency '" + currency.getId() + "' does not support decimals."
                );
            }

            return Long.parseLong(wholePart);
        }

        if (fractionalPart.length() > decimalPlaces) {
            throw new IllegalArgumentException(
                    "Too many decimal places for currency '" + currency.getId() + "'. Max: " + decimalPlaces
            );
        }

        fractionalPart = String.format("%-" + decimalPlaces + "s", fractionalPart).replace(' ', '0');

        long whole = Long.parseLong(wholePart);
        long fractional = fractionalPart.isEmpty() ? 0L : Long.parseLong(fractionalPart);

        long multiplier = (long) Math.pow(10, decimalPlaces);

        return (whole * multiplier) + fractional;
    }

    public static String formatAmount(long minorUnits, Currency currency) {
        int decimalPlaces = currency.getDecimalPlaces();
        long divisor = (long) Math.pow(10, decimalPlaces);

        long whole = decimalPlaces <= 0 ? minorUnits : minorUnits / divisor;
        long fractional = decimalPlaces <= 0 ? 0 : Math.abs(minorUnits % divisor);

        boolean hasFractional = decimalPlaces > 0 && fractional != 0;

        String unitName = (!hasFractional && whole == 1)
                ? currency.getDisplayNameSingular()
                : currency.getDisplayNamePlural();

        return switch (currency.getFormatStyle()) {
            case "WORD" ->
                    (decimalPlaces == 0 || fractional == 0)
                            ? whole + " " + unitName
                            : whole + " " + unitName + " " + String.format("%0" + decimalPlaces + "d", fractional);

            case "SYMBOL_PREFIX" ->
                    currency.getSymbol() + formatDecimalString(whole, fractional, decimalPlaces);

            case "SYMBOL_SUFFIX" ->
                    formatDecimalString(whole, fractional, decimalPlaces) + currency.getSymbol();

            case "NAME_SUFFIX" ->
                    formatDecimalString(whole, fractional, decimalPlaces) + " " + unitName;

            case "SYMBOL_PREFIX_NAME_SUFFIX" ->
                    currency.getSymbol() + formatDecimalString(whole, fractional, decimalPlaces) + " " + unitName;

            default ->
                    throw new IllegalArgumentException("Unknown format style: " + currency.getFormatStyle());
        };
    }

    private static String formatDecimalString(long whole, long fractional, int decimalPlaces) {
        if (decimalPlaces <= 0) {
            return String.valueOf(whole);
        }

        String fractionalString = String.format("%0" + decimalPlaces + "d", fractional);
        return whole + "." + fractionalString;
    }
}