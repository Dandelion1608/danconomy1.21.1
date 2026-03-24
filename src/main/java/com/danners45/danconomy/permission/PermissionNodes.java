package com.danners45.danconomy.permission;

import com.danners45.danconomy.DanConomy;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

public final class PermissionNodes {

    public static final PermissionNode<Boolean> BALANCE =
            boolNode("command.balance", true);

    public static final PermissionNode<Boolean> BALTOP =
            boolNode("command.baltop", true);

    public static final PermissionNode<Boolean> PAY =
            boolNode("command.pay", true);

    public static final PermissionNode<Boolean> SHOP =
            boolNode("command.shop", true);

    public static final PermissionNode<Boolean> ADMIN_SET =
            boolNode("admin.set", false);

    public static final PermissionNode<Boolean> ADMIN_GIVE =
            boolNode("admin.give", false);

    public static final PermissionNode<Boolean> ADMIN_TAKE =
            boolNode("admin.take", false);

    public static final PermissionNode<Boolean> ADMIN_SHOP =
            boolNode("admin.shop", false);

    private PermissionNodes() {
    }

    private static PermissionNode<Boolean> boolNode(String nodeName, boolean defaultValue) {
        return new PermissionNode<>(
                DanConomy.MODID,
                nodeName,
                PermissionTypes.BOOLEAN,
                (player, playerUUID, contexts) -> defaultValue
        ).setInformation(
                Component.literal(nodeName),
                Component.literal("DanConomy permission node: " + DanConomy.MODID + "." + nodeName)
        );
    }
}