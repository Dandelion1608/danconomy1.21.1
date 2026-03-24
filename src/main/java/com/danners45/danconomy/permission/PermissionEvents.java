package com.danners45.danconomy.permission;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

public class PermissionEvents {

    @SubscribeEvent
    public static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                PermissionNodes.BALANCE,
                PermissionNodes.BALTOP,
                PermissionNodes.PAY,
                PermissionNodes.SHOP,
                PermissionNodes.ADMIN_SET,
                PermissionNodes.ADMIN_GIVE,
                PermissionNodes.ADMIN_TAKE,
                PermissionNodes.ADMIN_SHOP
        );
    }
}