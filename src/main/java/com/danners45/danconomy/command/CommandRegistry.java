package com.danners45.danconomy.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class CommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BalanceCommand.register(event.getDispatcher());
        SetBalanceCommand.register(event.getDispatcher());
        GiveBalanceCommand.register(event.getDispatcher());
        TakeBalanceCommand.register(event.getDispatcher());
        PayCommand.register(event.getDispatcher());
        BalTopCommand.register(event.getDispatcher());
    }
}