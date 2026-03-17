package com.danners45.danconomy.economy;

import com.danners45.danconomy.DanConomy;
import com.danners45.danconomy.account.Account;
import com.danners45.danconomy.currency.Currency;
import com.danners45.danconomy.data.LedgerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class EconomyAccess {

    private static final String PIXELMON_MODID = "pixelmon";

    private EconomyAccess() {
    }

    public static long getBalance(ServerPlayer player, Currency currency) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null.");
        }

        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null.");
        }

        return switch (currency.getBackingType()) {
            case LEDGER -> getLedgerBalance(player, currency);
            case PIXELMON_MIRRORED -> getPixelmonMirroredBalance(player, currency);
        };
    }

    public static boolean withdraw(ServerPlayer player, Currency currency, long amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null.");
        }

        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null.");
        }

        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }

        return switch (currency.getBackingType()) {
            case LEDGER -> withdrawLedger(player, currency, amount);
            case PIXELMON_MIRRORED -> withdrawPixelmonMirrored(player, currency, amount);
        };
    }

    public static void deposit(ServerPlayer player, Currency currency, long amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null.");
        }

        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null.");
        }

        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }

        switch (currency.getBackingType()) {
            case LEDGER -> depositLedger(player, currency, amount);
            case PIXELMON_MIRRORED -> depositPixelmonMirrored(player, currency, amount);
        }
    }

    public static void setBalance(ServerPlayer player, Currency currency, long amount) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null.");
        }

        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null.");
        }

        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }

        switch (currency.getBackingType()) {
            case LEDGER -> setLedgerBalance(player, currency, amount);
            case PIXELMON_MIRRORED -> setPixelmonMirroredBalance(player, currency, amount);
        }
    }

    private static long getLedgerBalance(ServerPlayer player, Currency currency) {
        LedgerData ledger = LedgerData.get(player.serverLevel());
        Account account = ledger.getOrCreateAccount(player.getUUID());
        return account.getBalance(currency.getId());
    }

    private static boolean withdrawLedger(ServerPlayer player, Currency currency, long amount) {
        LedgerData ledger = LedgerData.get(player.serverLevel());
        Account account = ledger.getOrCreateAccount(player.getUUID());

        boolean success = account.withdraw(currency.getId(), amount);
        if (success) {
            ledger.markDirty();
        }

        return success;
    }

    private static void depositLedger(ServerPlayer player, Currency currency, long amount) {
        LedgerData ledger = LedgerData.get(player.serverLevel());
        Account account = ledger.getOrCreateAccount(player.getUUID());
        account.deposit(currency.getId(), amount);
        ledger.markDirty();
    }

    private static void setLedgerBalance(ServerPlayer player, Currency currency, long amount) {
        LedgerData ledger = LedgerData.get(player.serverLevel());
        Account account = ledger.getOrCreateAccount(player.getUUID());
        account.setBalance(currency.getId(), amount);
        ledger.markDirty();
    }

    private static long getPixelmonMirroredBalance(ServerPlayer player, Currency currency) {
        Long balance = getPixelmonBalance(player, currency);

        if (balance == null) {
            DanConomy.LOGGER.warn("Pixelmon balance read failed for {}. Falling back to ledger mirror.", player.getName().getString());
            return getLedgerBalance(player, currency);
        }

        mirrorToLedger(player, currency, balance);
        return balance;
    }

    private static boolean withdrawPixelmonMirrored(ServerPlayer player, Currency currency, long amount) {
        boolean success = pixelmonTake(player, currency, amount);

        if (!success) {
            DanConomy.LOGGER.warn("Pixelmon withdraw failed for {}. Ledger mirror was not changed.", player.getName().getString());
            return false;
        }

        Long updatedBalance = getPixelmonBalance(player, currency);
        if (updatedBalance != null) {
            mirrorToLedger(player, currency, updatedBalance);
        }

        return true;
    }

    private static void depositPixelmonMirrored(ServerPlayer player, Currency currency, long amount) {
        boolean success = pixelmonAdd(player, currency, amount);

        if (!success) {
            DanConomy.LOGGER.warn("Pixelmon deposit failed for {}. Falling back to ledger mirror only.", player.getName().getString());
            depositLedger(player, currency, amount);
            return;
        }

        Long updatedBalance = getPixelmonBalance(player, currency);
        if (updatedBalance != null) {
            mirrorToLedger(player, currency, updatedBalance);
        }
    }

    private static void setPixelmonMirroredBalance(ServerPlayer player, Currency currency, long amount) {
        boolean success = pixelmonSetBalance(player, currency, amount);

        if (!success) {
            DanConomy.LOGGER.warn("Pixelmon set balance failed for {}. Falling back to ledger mirror only.", player.getName().getString());
            setLedgerBalance(player, currency, amount);
            return;
        }

        Long updatedBalance = getPixelmonBalance(player, currency);
        if (updatedBalance != null) {
            mirrorToLedger(player, currency, updatedBalance);
        } else {
            mirrorToLedger(player, currency, amount);
        }
    }

    private static void mirrorToLedger(ServerPlayer player, Currency currency, long balanceMinorUnits) {
        LedgerData ledger = LedgerData.get(player.serverLevel());
        Account account = ledger.getOrCreateAccount(player.getUUID());
        account.setBalance(currency.getId(), balanceMinorUnits);
        ledger.markDirty();
    }

    private static Long getPixelmonBalance(ServerPlayer player, Currency currency) {
        try {
            Object bankAccount = getPixelmonBankAccount(player);
            if (bankAccount == null) {
                return null;
            }

            Method getBalance = bankAccount.getClass().getMethod("getBalance");
            Object result = getBalance.invoke(bankAccount);

            if (result instanceof BigDecimal decimal) {
                return toMinorUnits(decimal, currency);
            }

            DanConomy.LOGGER.warn("Pixelmon getBalance returned unexpected type: {}", result == null ? "null" : result.getClass().getName());
            return null;
        } catch (Exception e) {
            DanConomy.LOGGER.warn("Failed to read Pixelmon balance for {}.", player.getName().getString(), e);
            return null;
        }
    }

    private static boolean pixelmonSetBalance(ServerPlayer player, Currency currency, long amountMinorUnits) {
        try {
            Object bankAccount = getPixelmonBankAccount(player);
            if (bankAccount == null) {
                return false;
            }

            Method setBalance = bankAccount.getClass().getMethod("setBalance", BigDecimal.class);
            setBalance.invoke(bankAccount, toMajorAmount(amountMinorUnits, currency));
            invokeUpdatePlayerIfPresent(bankAccount);
            return true;
        } catch (Exception e) {
            DanConomy.LOGGER.warn("Failed to set Pixelmon balance for {}.", player.getName().getString(), e);
            return false;
        }
    }

    private static boolean pixelmonAdd(ServerPlayer player, Currency currency, long amountMinorUnits) {
        try {
            Object bankAccount = getPixelmonBankAccount(player);
            if (bankAccount == null) {
                return false;
            }

            Method add = bankAccount.getClass().getMethod("add", BigDecimal.class);
            Object result = add.invoke(bankAccount, toMajorAmount(amountMinorUnits, currency));
            invokeUpdatePlayerIfPresent(bankAccount);

            if (result instanceof Boolean b) {
                return b;
            }

            DanConomy.LOGGER.warn("Pixelmon add returned unexpected type: {}", result == null ? "null" : result.getClass().getName());
            return false;
        } catch (Exception e) {
            DanConomy.LOGGER.warn("Failed to add Pixelmon balance for {}.", player.getName().getString(), e);
            return false;
        }
    }

    private static boolean pixelmonTake(ServerPlayer player, Currency currency, long amountMinorUnits) {
        try {
            Object bankAccount = getPixelmonBankAccount(player);
            if (bankAccount == null) {
                return false;
            }

            Method take = bankAccount.getClass().getMethod("take", BigDecimal.class);
            Object result = take.invoke(bankAccount, toMajorAmount(amountMinorUnits, currency));
            invokeUpdatePlayerIfPresent(bankAccount);

            if (result instanceof Boolean b) {
                return b;
            }

            DanConomy.LOGGER.warn("Pixelmon take returned unexpected type: {}", result == null ? "null" : result.getClass().getName());
            return false;
        } catch (Exception e) {
            DanConomy.LOGGER.warn("Failed to take Pixelmon balance for {}.", player.getName().getString(), e);
            return false;
        }
    }

    private static Object getPixelmonBankAccount(ServerPlayer player) throws Exception {
        if (!ModList.get().isLoaded(PIXELMON_MODID)) {
            return null;
        }

        try {
            Method playerMethod = player.getClass().getMethod("getBankAccount");
            Object result = playerMethod.invoke(player);
            Object resolved = resolvePixelmonAccountResult(result);

            if (resolved != null) {
                return resolved;
            }
        } catch (NoSuchMethodException ignored) {
        }

        Class<?> proxyClass = Class.forName("com.pixelmonmod.pixelmon.api.economy.BankAccountProxy");

        try {
            Method getBankAccount = proxyClass.getMethod("getBankAccount", ServerPlayer.class);
            Object result = getBankAccount.invoke(null, player);
            Object resolved = resolvePixelmonAccountResult(result);

            if (resolved != null) {
                return resolved;
            }
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Method getBankAccount = proxyClass.getMethod("getBankAccount", UUID.class);
            Object result = getBankAccount.invoke(null, player.getUUID());
            Object resolved = resolvePixelmonAccountResult(result);

            if (resolved != null) {
                return resolved;
            }
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Method getBankAccountUnsafe = proxyClass.getMethod("getBankAccountUnsafe", ServerPlayer.class);
            Object result = getBankAccountUnsafe.invoke(null, player);
            Object resolved = resolvePixelmonAccountResult(result);

            if (resolved != null) {
                return resolved;
            }
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Method getBankAccountUnsafe = proxyClass.getMethod("getBankAccountUnsafe", UUID.class);
            Object result = getBankAccountUnsafe.invoke(null, player.getUUID());
            Object resolved = resolvePixelmonAccountResult(result);

            if (resolved != null) {
                return resolved;
            }
        } catch (NoSuchMethodException ignored) {
        }

        DanConomy.LOGGER.warn("Could not resolve a Pixelmon bank account for {}.", player.getName().getString());
        return null;
    }

    private static Object resolvePixelmonAccountResult(Object result) throws Exception {
        if (result == null) {
            return null;
        }

        if (result instanceof Optional<?> optional) {
            return resolvePixelmonAccountResult(optional.orElse(null));
        }

        if (result instanceof CompletableFuture<?> future) {
            return resolvePixelmonAccountResult(future.get());
        }

        return result;
    }

    private static void invokeUpdatePlayerIfPresent(Object bankAccount) {
        try {
            Method updatePlayer = bankAccount.getClass().getMethod("updatePlayer");
            updatePlayer.invoke(bankAccount);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            DanConomy.LOGGER.warn("Failed to send Pixelmon balance update packet.", e);
        }
    }

    private static BigDecimal toMajorAmount(long minorUnits, Currency currency) {
        return BigDecimal.valueOf(minorUnits)
                .movePointLeft(currency.getDecimalPlaces())
                .setScale(currency.getDecimalPlaces(), RoundingMode.DOWN);
    }

    private static long toMinorUnits(BigDecimal majorAmount, Currency currency) {
        return majorAmount
                .movePointRight(currency.getDecimalPlaces())
                .setScale(0, RoundingMode.DOWN)
                .longValue();
    }
}