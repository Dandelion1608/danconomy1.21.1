package com.danners45.danconomy;

// Minecraft Imports
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.UUID;
// Neo Imports
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
// Inhouse Imports
import com.danners45.danconomy.currency.CurrencyBootstrap;
import com.danners45.danconomy.currency.CurrencyRegistry;
import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.account.AccountManager;

@Mod(DanConomy.MODID)
public class DanConomy {
    public static final String MODID = "danconomy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DanConomy(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("=====Danconomy Initialised, Beginning Banking Sequence=====");

        CurrencyBootstrap.registerDefaults();

        LOGGER.info("Loaded {} Currencies.", CurrencyRegistry.getAll().size());

        UUID testId = UUID.randomUUID();
        Account testAccount = AccountManager.getOrCreateAccount(testId);

        testAccount.setAlias("TestPlayer");
        testAccount.deposit("dollar", 500);

        LOGGER.info("Test account '{}' balance: {}", testAccount.getAlias(), testAccount.getBalance("dollar"));
    }
}