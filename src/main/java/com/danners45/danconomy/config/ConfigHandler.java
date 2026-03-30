package com.danners45.danconomy.config;

import com.danners45.danconomy.DanConomy;
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.currency.CurrencyRegistry;
import com.danners45.danconomy.shop.ShopStorageValidator;
import com.moandjiezana.toml.Toml;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConfigHandler {
    private static final Logger LOGGER = DanConomy.LOGGER;
    private static final Path CONFIG_PATH = Paths.get("config", "danconomy.toml");

    private static boolean showShopDebugDetails = false;
    private static List<String> allowedShopStorage = List.of("minecraft:barrel");
    private static Set<String> commandShopBlacklist = Set.of(
            "op", "deop", "stop", "reload", "ban", "pardon", "whitelist"
    );
    private static boolean strictCommandShopValidation = true;

    private ConfigHandler() {
    }

    public static void load() {
        LOGGER.info("=== CONFIG LOADER STARTED ===");

        try {
            if (!Files.exists(CONFIG_PATH)) {
                LOGGER.warn("DanConomy config not found. Creating default danconomy.toml");
                createDefaultConfig();
            }

            Toml toml = new Toml().read(CONFIG_PATH.toFile());
            validateAndRegister(toml);

            ShopStorageValidator.rebuildAllowedBlockCache();

            LOGGER.info("DanConomy config loaded successfully.");
        } catch (Exception e) {
            LOGGER.error("DanConomy config is invalid. Backing up and recreating...", e);

            backupBrokenConfig();

            try {
                createDefaultConfig();
                Toml toml = new Toml().read(CONFIG_PATH.toFile());
                validateAndRegister(toml);

                ShopStorageValidator.rebuildAllowedBlockCache();

                LOGGER.info("DanConomy config recovered successfully.");
            } catch (Exception ex) {
                LOGGER.error("Failed to recover config. Mod may not function correctly.", ex);
            }
        }
    }

    private static void validateAndRegister(Toml toml) {
        CurrencyRegistry.clear();

        Long startingBalance = toml.getLong("startingBalance", 0L);
        CurrencyRegistry.setStartingBalance(startingBalance);

        Boolean requireExplicit = toml.getBoolean("requireExplicitCurrencyIfAmbiguous", true);
        CurrencyRegistry.setRequireExplicitCurrencyIfAmbiguous(requireExplicit);

        String defaultCurrency = toml.getString("defaultCurrency", "");
        CurrencyRegistry.setDefaultCurrencyId(defaultCurrency);

        Boolean debugDetails = toml.getBoolean("shop.showDebugDetails", false);
        showShopDebugDetails = debugDetails != null && debugDetails;

        List<Object> allowedBlocksRaw = toml.getList("shop.allowedShopStorage");
        if (allowedBlocksRaw == null || allowedBlocksRaw.isEmpty()) {
            allowedShopStorage = List.of("minecraft:barrel");
        } else {
            allowedShopStorage = allowedBlocksRaw.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();

            if (allowedShopStorage.isEmpty()) {
                allowedShopStorage = List.of("minecraft:barrel");
            }
        }
        List<Object> blacklistRaw = toml.getList("shop.commandShopBlacklist");
        if (blacklistRaw == null || blacklistRaw.isEmpty()) {
            commandShopBlacklist = Set.of(
                    "op", "deop", "stop", "reload", "ban", "pardon", "whitelist"
            );
        } else {
            commandShopBlacklist = blacklistRaw.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            if (commandShopBlacklist.isEmpty()) {
                commandShopBlacklist = Set.of(
                        "op", "deop", "stop", "reload", "ban", "pardon", "whitelist"
                );
            }
        }

        Boolean strictValidation = toml.getBoolean("shop.strictCommandShopValidation", true);
        strictCommandShopValidation = strictValidation != null && strictValidation;

        List<Toml> currencies = toml.getTables("currencies");
        if (currencies == null || currencies.isEmpty()) {
            throw new IllegalStateException("No currencies defined in config.");
        }

        for (Toml currencyTable : currencies) {
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

        if (!defaultCurrency.isEmpty() && !CurrencyRegistry.exists(defaultCurrency)) {
            throw new IllegalStateException("Default currency does not exist: " + defaultCurrency);
        }

        LOGGER.info("Loaded {} currencies.", CurrencyRegistry.getAll().size());
        LOGGER.info("Loaded {} allowed shop storage entries.", allowedShopStorage.size());
    }

    public static boolean showShopDebugDetails() {
        return showShopDebugDetails;
    }

    public static List<String> allowedShopStorage() {
        return allowedShopStorage;
    }

    public static Set<String> commandShopBlacklist() {
        return commandShopBlacklist;
    }

    public static boolean strictCommandShopValidation() {
        return strictCommandShopValidation;
    }

    private static void createDefaultConfig() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());

        String defaultConfig = """
# DanConomy Configuration
#
# defaultCurrency:
#   The currency id used when no specific currency is provided and a default is needed.
#   Leave blank to avoid forcing a default.
#
# requireExplicitCurrencyIfAmbiguous:
#   If true, commands must specify a currency when multiple valid currencies could apply.
#
# startingBalance:
#   The amount new accounts start with, in minor units.
#   Example: with decimalPlaces = 2, 10000 = 100.00

defaultCurrency = ""
requireExplicitCurrencyIfAmbiguous = true
startingBalance = 10000

[shop]
# showDebugDetails:
#   If true, extra technical detail is shown in shop validation logs.
showDebugDetails = false

# allowedShopStorage:
#   List of block ids that are allowed to be used as shop storage.
#   These still must qualify as valid item storage in-world.
#   Examples:
#   - "minecraft:barrel"
#   - "minecraft:chest"
#   - "ironchest:copper_chest"
allowedShopStorage = ["minecraft:barrel"]

# commandShopBlacklist:
#   Root command names that cannot be used for command shops.
#   These are checked against the first word of the command only.
commandShopBlacklist = ["op", "deop", "stop", "reload", "ban", "pardon", "whitelist"]

# strictCommandShopValidation:
#   If true, command shops also attempt a Brigadier parse check using probe values.
strictCommandShopValidation = true

[[currencies]]
# id:
#   Internal currency id used in commands and references.
id = "dollar"

# displayNameSingular:
#   Name shown when the amount is singular.
displayNameSingular = "dollar"

# displayNamePlural:
#   Name shown when the amount is plural.
displayNamePlural = "dollars"

# symbol:
#   Symbol used for symbol-based display styles.
symbol = "$"

# decimalPlaces:
#   Number of decimal places used by this currency.
#   Example: 2 means 100 = 1.00
decimalPlaces = 2

# formatStyle:
#   Controls how the currency is displayed.
#   Supported examples:
#   - "WORD"
#   - "SYMBOL_PREFIX"
#   - "SYMBOL_SUFFIX"
formatStyle = "SYMBOL_PREFIX"

# payable:
#   If true, players can transfer this currency with pay/shop-style flows.
payable = true
""";

        Files.writeString(
                CONFIG_PATH,
                defaultConfig,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static void backupBrokenConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                return;
            }

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