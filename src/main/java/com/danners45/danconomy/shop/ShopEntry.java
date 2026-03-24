package com.danners45.danconomy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record ShopEntry(
        UUID owner,
        BlockPos signPos,
        BlockPos storagePos,
        boolean adminShop,
        Mode mode,
        String currencyId,
        long priceMinor,
        ItemStack templateItem,
        int amountPerTrade
) {
    public enum Mode {
        BUY,
        SELL
    }

    public CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        if (owner != null) {
            tag.putUUID("owner", owner);
        }

        tag.putLong("signPos", signPos.asLong());
        tag.putLong("storagePos", storagePos.asLong());
        tag.putBoolean("adminShop", adminShop);
        tag.putString("mode", mode.name());
        tag.putString("currencyId", currencyId);
        tag.putLong("priceMinor", priceMinor);
        tag.put("templateItem", templateItem.save(provider));
        tag.putInt("amountPerTrade", amountPerTrade);

        return tag;
    }

    public static ShopEntry load(CompoundTag tag, HolderLookup.Provider provider) {
        UUID owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;

        return new ShopEntry(
                owner,
                BlockPos.of(tag.getLong("signPos")),
                BlockPos.of(tag.getLong("storagePos")),
                tag.getBoolean("adminShop"),
                loadMode(tag),
                tag.getString("currencyId"),
                tag.getLong("priceMinor"),
                ItemStack.parse(provider, tag.getCompound("templateItem")).orElse(ItemStack.EMPTY),
                tag.getInt("amountPerTrade")
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