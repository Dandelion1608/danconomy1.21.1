package com.danners45.danconomy.shop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class ShopInventoryHelper {
    private ShopInventoryHelper() {
    }

    public static boolean matchesTemplate(ItemStack candidate, ItemStack template) {
        if (candidate.isEmpty() || template.isEmpty()) {
            return false;
        }

        return ItemStack.isSameItemSameComponents(candidate, template);
    }

    public static int countMatchingItems(IItemHandler handler, ItemStack template) {
        int total = 0;

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stackInSlot = handler.getStackInSlot(slot);

            if (matchesTemplate(stackInSlot, template)) {
                total += stackInSlot.getCount();
            }
        }

        return total;
    }

    public static ItemStack extractMatchingItems(IItemHandler handler, ItemStack template, int amountToExtract, boolean simulate) {
        if (amountToExtract <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack extractedTotal = ItemStack.EMPTY;
        int remaining = amountToExtract;

        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack stackInSlot = handler.getStackInSlot(slot);

            if (!matchesTemplate(stackInSlot, template)) {
                continue;
            }

            int toTake = Math.min(remaining, stackInSlot.getCount());
            ItemStack extracted = handler.extractItem(slot, toTake, simulate);

            if (extracted.isEmpty()) {
                continue;
            }

            if (extractedTotal.isEmpty()) {
                extractedTotal = extracted.copy();
            } else {
                extractedTotal.grow(extracted.getCount());
            }

            remaining -= extracted.getCount();
        }

        if (!extractedTotal.isEmpty()) {
            extractedTotal.setCount(amountToExtract - remaining);
        }

        return extractedTotal;
    }

    public static ItemStack insertItems(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();

        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }

        return remaining;
    }

    public static int countMatchingItemsInPlayer(ServerPlayer player, ItemStack template) {
        int total = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (matchesTemplate(stack, template)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    public static ItemStack removeMatchingItemsFromPlayer(ServerPlayer player, ItemStack template, int amountToRemove, boolean simulate) {
        if (amountToRemove <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack removedTotal = ItemStack.EMPTY;
        int remaining = amountToRemove;

        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!matchesTemplate(stack, template)) {
                continue;
            }

            int toTake = Math.min(remaining, stack.getCount());

            if (simulate) {
                ItemStack simulated = stack.copy();
                simulated.setCount(toTake);

                if (removedTotal.isEmpty()) {
                    removedTotal = simulated;
                } else {
                    removedTotal.grow(simulated.getCount());
                }

                remaining -= toTake;
                continue;
            }

            ItemStack split = stack.split(toTake);

            if (removedTotal.isEmpty()) {
                removedTotal = split.copy();
            } else {
                removedTotal.grow(split.getCount());
            }

            remaining -= split.getCount();
        }

        if (!removedTotal.isEmpty()) {
            removedTotal.setCount(amountToRemove - remaining);
        }

        return removedTotal;
    }

    public static void giveToPlayerOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        boolean added = player.getInventory().add(remaining);

        if (!added && !remaining.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    remaining.copy()
            );
            player.level().addFreshEntity(itemEntity);
        }
    }
}