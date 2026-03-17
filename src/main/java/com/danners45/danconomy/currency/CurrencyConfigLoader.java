package com.danners45.danconomy.currency;

import com.danners45.danconomy.DanConomy;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.moandjiezana.toml.Toml;

public class CurrencyConfigLoader {

    private static final Logger LOGGER = DanConomy.LOGGER;

    private static final Path CONFIG_PATH = Paths.get("config", "danconomy.toml");

    public static void load() {
        DanConomy.LOGGER.info("=== CONFIG LOADER STARTED ===");
        try {
            if (!Files.exists(CONFIG_PATH)) {
                LOGGER.warn("DanConomy config not found. Creating default danconomy.toml");
                createDefaultConfig();
            }

            Toml toml = new Toml().read(CONFIG_PATH.toFile());

            validateAndRegister(toml);

            LOGGER.info("DanConomy config loaded successfully.");

        } catch (Exception e) {
            LOGGER.error("DanConomy config is invalid. Backing up and recreating...", e);

            backupBrokenConfig();

            try {
                createDefaultConfig();
                Toml toml = new Toml().read(CONFIG_PATH.toFile());
                validateAndRegister(toml);
            } catch (Exception ex) {
                LOGGER.error("Failed to recover config. Mod may not function correctly.", ex);
            }
        }
    }

    private static void validateAndRegister(Toml toml) {

        CurrencyRegistry.clear();

        // ✅ Load global settings
        Long startingBalance = toml.getLong("startingBalance", 0L);
        CurrencyRegistry.setStartingBalance(startingBalance);

        Boolean requireExplicit = toml.getBoolean("requireExplicitCurrencyIfAmbiguous", true);
        CurrencyRegistry.setRequireExplicitCurrencyIfAmbiguous(requireExplicit);

        String defaultCurrency = toml.getString("defaultCurrency", "");
        CurrencyRegistry.setDefaultCurrencyId(defaultCurrency);

        // ✅ Load currencies
        var currencies = toml.getTables("currencies");

        if (currencies == null || currencies.isEmpty()) {
            throw new IllegalStateException("No currencies defined in config.");
        }

        for (var currencyTable : currencies) {

            String id = currencyTable.getString("id");
            String singular = currencyTable.getString("displayNameSingular");
            String plural = currencyTable.getString("displayNamePlural");
            String symbol = currencyTable.getString("symbol");
            Long decimalPlaces = currencyTable.getLong("decimalPlaces");
            String formatStyle = currencyTable.getString("formatStyle");
            Boolean payable = currencyTable.getBoolean("payable");

            if (id == null || singular == null || plural == null || symbol == null
                    || decimalPlaces == null || formatStyle == null || payable == null) {
                throw new IllegalStateException("Currency entry is missing required fields.");
            }

            if (decimalPlaces < 0) {
                throw new IllegalStateException("decimalPlaces cannot be negative for currency: " + id);
            }

            Currency currency = new Currency(
                    id,
                    singular,
                    plural,
                    symbol,
                    decimalPlaces.intValue(),
                    formatStyle,
                    payable
            );

            CurrencyRegistry.register(currency);
        }

        // ✅ Validate default currency exists
        if (!defaultCurrency.isEmpty() && !CurrencyRegistry.exists(defaultCurrency)) {
            throw new IllegalStateException("Default currency does not exist: " + defaultCurrency);
        }

        LOGGER.info("Loaded {} currencies.", CurrencyRegistry.getAll().size());
    }

    private static void createDefaultConfig() throws IOException {

        Files.createDirectories(CONFIG_PATH.getParent());

        String defaultConfig = """
# DanConomy Configuration

defaultCurrency = ""
requireExplicitCurrencyIfAmbiguous = true
startingBalance = 10000

[[currencies]]
id = "dollar"
displayNameSingular = "dollar"
displayNamePlural = "dollars"
symbol = "$"
decimalPlaces = 2
formatStyle = "WORD"
payable = true
""";

        Files.writeString(CONFIG_PATH, defaultConfig,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void backupBrokenConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) return;

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            Path backupPath = CONFIG_PATH.resolveSibling("danconomy.toml." + timestamp + ".bak");

            Files.move(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.warn("Backed up invalid config to: {}", backupPath);

        } catch (IOException e) {
            LOGGER.error("Failed to backup broken config.", e);
        }
    }
}