package com.danners45.danconomy;

// Minecraft Imports
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

// Neo Imports
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// Inhouse Imports
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.currency.CurrencyConfigLoader;
import com.danners45.danconomy.currency.CurrencyRegistry;
import com.danners45.danconomy.account.AccountEvents;
import com.danners45.danconomy.command.CommandRegistry;
import com.danners45.danconomy.permission.PermissionEvents;

@Mod(DanConomy.MODID)
public class DanConomy {
    public static final String MODID = "danconomy";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Change this if Pixelmon's actual mod id is different in your environment.
    private static final String PIXELMON_MODID = "pixelmon";
    private static final String PIXELMON_CURRENCY_ID = "pokedollar";

    public DanConomy(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("=====Danconomy Initialised, Beginning Banking Sequence=====");

        CurrencyConfigLoader.load();
        registerOptionalPixelmonCurrency();

        NeoForge.EVENT_BUS.register(AccountEvents.class);
        NeoForge.EVENT_BUS.register(CommandRegistry.class);
        NeoForge.EVENT_BUS.register(PermissionEvents.class);

    }

    private static void registerOptionalPixelmonCurrency() {
        if (!ModList.get().isLoaded(PIXELMON_MODID)) {
            LOGGER.info("Pixelmon not detected. Using configured DanConomy currencies only.");
            return;
        }

        LOGGER.info("Pixelmon detected. Registering Pixelmon-backed currency '{}'.", PIXELMON_CURRENCY_ID);

        CurrencyRegistry.registerOrReplace(
                new Currency(
                        PIXELMON_CURRENCY_ID,
                        "Pokedollar",
                        "Pokedollars",
                        "₽",
                        2,
                        "SYMBOL_PREFIX",
                        true,
                        Currency.BackingType.PIXELMON_MIRRORED
                )
        );

        if (CurrencyRegistry.getDefaultCurrencyId() == null || CurrencyRegistry.getDefaultCurrencyId().isBlank()) {
            CurrencyRegistry.setDefaultCurrencyId(PIXELMON_CURRENCY_ID);
            LOGGER.info("No default currency was configured. Defaulting to '{}'.", PIXELMON_CURRENCY_ID);
        }
    }
}