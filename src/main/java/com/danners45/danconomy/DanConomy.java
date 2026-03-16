package com.danners45.danconomy;

// Minecraft Imports
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
// Neo Imports
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
// Mod Imports
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.currency.CurrencyRegistry;

@Mod(DanConomy.MODID)
public class DanConomy {
    public static final String MODID = "danconomy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DanConomy(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("=====Danconomy Initialised, Beginning Banking Sequence=====");

        Currency pokedollar = new Currency("pokedollar", "PokeDollar", "P");
        CurrencyRegistry.register(pokedollar);

        LOGGER.info("Registered Currency: " + pokedollar.getId());
    }
}