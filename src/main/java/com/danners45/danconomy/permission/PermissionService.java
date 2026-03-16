package com.danners45.danconomy.permission;

public class PermissionService {

    public static boolean hasPermissionLevel(int permissionLevel) {
        return permissionLevel >= 2;
    }
}