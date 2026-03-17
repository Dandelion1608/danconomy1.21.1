package com.danners45.danconomy;

// Minecraft Imports

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
// Neo Imports
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
// Inhouse Imports
import com.danners45.danconomy.currency.CurrencyConfigLoader;
import com.danners45.danconomy.currency.CurrencyRegistry;
import com.danners45.danconomy.account.AccountEvents;
import com.danners45.danconomy.command.CommandRegistry;
import com.danners45.danconomy.permission.PermissionEvents;


@Mod(DanConomy.MODID)
public class DanConomy {
    public static final String MODID = "danconomy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DanConomy(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("=====Danconomy Initialised, Beginning Banking Sequence=====");

        CurrencyConfigLoader.load();

        NeoForge.EVENT_BUS.register(AccountEvents.class);
        NeoForge.EVENT_BUS.register(CommandRegistry.class);
        NeoForge.EVENT_BUS.register(PermissionEvents.class);

    }
}