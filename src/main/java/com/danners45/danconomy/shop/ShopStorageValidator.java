package com.danners45.danconomy.shop;

import com.danners45.danconomy.DanConomy;
import com.danners45.danconomy.config.ConfigHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ShopStorageValidator {
    private static final Set<Block> VALIDATED_ALLOWED_BLOCKS = new HashSet<>();

    private static final String INVALID_BLOCK_ID = "Invalid Block ID";
    private static final String BLOCK_NOT_ALLOWED = "Block Not Allowed";
    private static final String BLOCK_CANNOT_STORE_ITEMS = "Block Cannot Store Items";

    private ShopStorageValidator() {
    }

    public record QualificationResult(boolean qualified, String summaryReason, String debugReason) {
        public static QualificationResult pass(String summaryReason, String debugReason) {
            return new QualificationResult(true, summaryReason, debugReason);
        }

        public static QualificationResult fail(String summaryReason, String debugReason) {
            return new QualificationResult(false, summaryReason, debugReason);
        }

        public String formattedReason() {
            boolean showDebug = false;

            try {
                showDebug = ConfigHandler.showShopDebugDetails();
            } catch (Exception ignored) {
            }

            if (showDebug && debugReason != null && !debugReason.isBlank()) {
                return summaryReason + " (" + debugReason + ")";
            }

            return summaryReason;
        }
    }

    public static void rebuildAllowedBlockCache() {
        VALIDATED_ALLOWED_BLOCKS.clear();

        List<String> configuredIds = ConfigHandler.allowedShopStorage();

        for (String rawId : configuredIds) {
            ResourceLocation id = ResourceLocation.tryParse(rawId);

            if (id == null) {
                logConfigQualificationFailure(rawId, QualificationResult.fail(
                        INVALID_BLOCK_ID,
                        "invalid format"
                ));
                continue;
            }

            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                logConfigQualificationFailure(rawId, QualificationResult.fail(
                        INVALID_BLOCK_ID,
                        "block not found in registry"
                ));
                continue;
            }

            Block block = BuiltInRegistries.BLOCK.get(id);
            VALIDATED_ALLOWED_BLOCKS.add(block);

            DanConomy.LOGGER.info(
                    "[DanConomy/Shop] Configured block entry '{}' passed qualification.",
                    rawId
            );
        }
    }

    public static Set<Block> getValidatedAllowedBlocks() {
        return Collections.unmodifiableSet(VALIDATED_ALLOWED_BLOCKS);
    }

    public static boolean isAllowedConfiguredBlock(Block block) {
        return VALIDATED_ALLOWED_BLOCKS.contains(block);
    }

    public static QualificationResult qualifyLiveBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        String blockId = String.valueOf(BuiltInRegistries.BLOCK.getKey(block));

        if (!isAllowedConfiguredBlock(block)) {
            return QualificationResult.fail(
                    BLOCK_NOT_ALLOWED,
                    "block '" + blockId + "' is not on the allowed shop storage list"
            );
        }

        IItemHandler handler = getStorage(level, pos);
        if (handler == null) {
            return QualificationResult.fail(
                    BLOCK_CANNOT_STORE_ITEMS,
                    "no item handler capability"
            );
        }

        if (handler.getSlots() <= 0) {
            return QualificationResult.fail(
                    BLOCK_CANNOT_STORE_ITEMS,
                    "handler has zero usable slots"
            );
        }

        return QualificationResult.pass(
                "Valid Shop Storage",
                "block '" + blockId + "' is valid shop storage"
        );
    }

    public static boolean tryValidateForShop(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        QualificationResult result = qualifyLiveBlock(level, pos);

        if (!result.qualified()) {
            logLiveQualificationFailure(level, pos, result);
            player.sendSystemMessage(Component.literal(
                    "This block cannot be used as shop storage: " + result.summaryReason()
            ));
            return false;
        }

        logLiveQualificationSuccess(level, pos, result);
        player.sendSystemMessage(Component.literal("Shop storage accepted."));
        return true;
    }

    public static void logConfigQualificationFailure(String rawId, QualificationResult result) {
        DanConomy.LOGGER.warn(
                "[DanConomy/Shop] Configured block entry '{}' failed qualification: {}",
                rawId,
                result.formattedReason()
        );
    }

    public static void logLiveQualificationFailure(ServerLevel level, BlockPos pos, QualificationResult result) {
        Block block = level.getBlockState(pos).getBlock();
        String blockId = String.valueOf(BuiltInRegistries.BLOCK.getKey(block));

        DanConomy.LOGGER.warn(
                "[DanConomy/Shop] Block '{}' failed qualification: {}",
                blockId,
                result.formattedReason()
        );
    }

    public static void logLiveQualificationSuccess(ServerLevel level, BlockPos pos, QualificationResult result) {
        Block block = level.getBlockState(pos).getBlock();
        String blockId = String.valueOf(BuiltInRegistries.BLOCK.getKey(block));

        DanConomy.LOGGER.info(
                "[DanConomy/Shop] Block '{}' passed qualification: {}",
                blockId,
                result.formattedReason()
        );
    }

    private static @Nullable IItemHandler getStorage(ServerLevel level, BlockPos pos) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler != null && handler.getSlots() > 0) {
            return handler;
        }

        for (Direction direction : Direction.values()) {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
            if (handler != null && handler.getSlots() > 0) {
                return handler;
            }
        }

        return null;
    }
}