package com.danners45.danconomy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ShopData extends SavedData {
    private static final String DATA_NAME = "danconomy_shops";

    private final Map<Long, ShopEntry> shopsBySignPos = new HashMap<>();

    public static ShopData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(
                        ShopData::new,
                        ShopData::load
                ),
                DATA_NAME
        );
    }

    public ShopData() {
    }

    public static ShopData load(CompoundTag tag, HolderLookup.Provider provider) {
        ShopData data = new ShopData();

        ListTag shops = tag.getList("shops", Tag.TAG_COMPOUND);
        for (int i = 0; i < shops.size(); i++) {
            CompoundTag entryTag = shops.getCompound(i);
            ShopEntry entry = ShopEntry.load(entryTag, provider);
            data.shopsBySignPos.put(entry.signPos().asLong(), entry);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag shops = new ListTag();

        for (ShopEntry entry : shopsBySignPos.values()) {
            shops.add(entry.save(provider));
        }

        tag.put("shops", shops);
        return tag;
    }

    public void putShop(ShopEntry entry) {
        shopsBySignPos.put(entry.signPos().asLong(), entry);
        setDirty();
    }

    public ShopEntry getShopBySignPos(long signPos) {
        return shopsBySignPos.get(signPos);
    }

    public ShopEntry getShopBySignPos(BlockPos signPos) {
        return shopsBySignPos.get(signPos.asLong());
    }

    public ShopEntry removeShopBySignPos(BlockPos signPos) {
        ShopEntry removed = shopsBySignPos.remove(signPos.asLong());
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    public boolean hasShopAt(BlockPos signPos) {
        return shopsBySignPos.containsKey(signPos.asLong());
    }

    public Collection<ShopEntry> getAllShops() {
        return shopsBySignPos.values();
    }

    public ShopEntry getFirstShopByStoragePos(BlockPos storagePos) {
        for (ShopEntry entry : shopsBySignPos.values()) {
            if (entry.isCommandShop()) {
                continue;
            }

            BlockPos entryStoragePos = entry.storagePos();
            if (entryStoragePos != null && entryStoragePos.equals(storagePos)) {
                return entry;
            }
        }
        return null;
    }
}