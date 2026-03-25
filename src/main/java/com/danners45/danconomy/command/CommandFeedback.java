package com.danners45.danconomy.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class CommandFeedback {

    private CommandFeedback() {
    }

    public static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}