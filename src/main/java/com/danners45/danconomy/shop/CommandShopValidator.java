package com.danners45.danconomy.shop;

import com.danners45.danconomy.config.ConfigHandler;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Set;

public final class CommandShopValidator {
    private CommandShopValidator() {
    }

    public record Result(boolean valid, String reason) {
    }

    public static Result validate(MinecraftServer server, ServerPlayer creator, String command) {
        if (command == null || command.isBlank()) {
            return new Result(false, "Command cannot be empty.");
        }

        String trimmed = command.startsWith("/") ? command.substring(1) : command;
        String root = trimmed.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);

        Set<String> blacklist = ConfigHandler.commandShopBlacklist();
        if (blacklist.contains(root)) {
            return new Result(false, "That command is blacklisted.");
        }

        if (server.getCommands().getDispatcher().getRoot().getChild(root) == null) {
            return new Result(false, "That command is not registered on this server.");
        }

        if (ConfigHandler.strictCommandShopValidation()) {
            String probe = trimmed
                    .replace("{player}", creator.getGameProfile().getName())
                    .replace("{uuid}", creator.getUUID().toString());

            CommandSourceStack source = server.createCommandSourceStack().withPermission(4);
            ParseResults<CommandSourceStack> parsed =
                    server.getCommands().getDispatcher().parse(probe, source);

            if (parsed.getReader().canRead() || !parsed.getExceptions().isEmpty()) {
                return new Result(false, "Command syntax is not valid in this environment.");
            }
        }

        return new Result(true, "");
    }
}