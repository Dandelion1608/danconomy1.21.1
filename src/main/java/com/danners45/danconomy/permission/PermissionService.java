package com.danners45.danconomy.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

public final class PermissionService {

    private PermissionService() {
    }

    public static boolean has(CommandSourceStack source, PermissionNode<Boolean> node) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return source.hasPermission(2);
        }

        return PermissionAPI.getPermission(player, node);
    }
}