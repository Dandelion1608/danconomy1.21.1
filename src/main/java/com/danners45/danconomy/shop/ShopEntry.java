package com.danners45.danconomy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ShopEntry(
        @Nullable UUID owner,
        BlockPos signPos,
        @Nullable BlockPos storagePos,
        boolean adminShop,
        Mode mode,
        String currencyId,
        long priceMinor,
        ItemStack templateItem,
        int amountPerTrade,
        BackendType backendType,
        @Nullable String commandTemplate
) {
    public enum Mode {
        BUY,
        SELL
    }

    public enum BackendType {
        STORAGE,
        COMMAND
    }

    public boolean isCommandShop() {
        return backendType == BackendType.COMMAND;
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        if (owner != null) {
            tag.putUUID("owner", owner);
        }

        tag.putLong("signPos", signPos.asLong());

        if (storagePos != null) {
            tag.putLong("storagePos", storagePos.asLong());
        }

        tag.putBoolean("adminShop", adminShop);
        tag.putString("mode", mode.name());
        tag.putString("currencyId", currencyId);
        tag.putLong("priceMinor", priceMinor);
        tag.putInt("amountPerTrade", amountPerTrade);
        if (!templateItem.isEmpty()) {
            tag.put("templateItem", templateItem.save(provider));
        }

        tag.putString("backendType", backendType.name());

        if (commandTemplate != null && !commandTemplate.isBlank()) {
            tag.putString("commandTemplate", commandTemplate);
        }

        return tag;
    }

    public static ShopEntry load(CompoundTag tag, HolderLookup.Provider provider) {
        UUID owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;

        BackendType backendType;
        try {
            backendType = BackendType.valueOf(tag.getString("backendType"));
        } catch (IllegalArgumentException e) {
            backendType = BackendType.STORAGE;
        }

        BlockPos storagePos = tag.contains("storagePos")
                ? BlockPos.of(tag.getLong("storagePos"))
                : null;

        String commandTemplate = tag.contains("commandTemplate")
                ? tag.getString("commandTemplate")
                : null;

        return new ShopEntry(
                owner,
                BlockPos.of(tag.getLong("signPos")),
                storagePos,
                tag.getBoolean("adminShop"),
                loadMode(tag),
                tag.getString("currencyId"),
                tag.getLong("priceMinor"),
                tag.contains("templateItem")
                        ? ItemStack.parse(provider, tag.getCompound("templateItem")).orElse(ItemStack.EMPTY)
                        : ItemStack.EMPTY,
                tag.getInt("amountPerTrade"),
                backendType,
                commandTemplate
        );
    }

    private static Mode loadMode(CompoundTag tag) {
        try {
            return Mode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException e) {
            return Mode.BUY;
        }
    }
}