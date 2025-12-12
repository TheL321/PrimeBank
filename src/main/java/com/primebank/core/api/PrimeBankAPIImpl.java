package com.primebank.core.api;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import com.primebank.api.PrimeBankAPI;
import com.primebank.api.PrimeBankException;
import com.primebank.api.PrimeBankResult;
import com.primebank.api.PrimeBankSecurityException;
import com.primebank.core.Money;
import com.primebank.core.accounts.Account;
import com.primebank.core.accounts.AccountRegistry;
import com.primebank.core.accounts.PlayerAccounts;
import com.primebank.core.locks.AccountLockManager;
import com.primebank.core.logging.TransactionLogger;
import com.primebank.core.state.PrimeBankState;

import net.minecraftforge.fml.common.FMLCommonHandler;

public class PrimeBankAPIImpl implements PrimeBankAPI {
    private static final PrimeBankAPIImpl INSTANCE = new PrimeBankAPIImpl();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PrimeBankAPIImpl() {
    }

    public static PrimeBankAPIImpl instance() {
        return INSTANCE;
    }

    private void checkServerSide() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            throw new PrimeBankSecurityException("PrimeBank API cannot be called from the client side.");
        }
    }

    private String sanitize(String s) {
        return (s == null || s.isEmpty()) ? "Unknown" : s;
    }

    private void record(Account acc, String type, String other, long amount, String desc) {
        String ts = LocalDateTime.now().format(DATE_FORMAT);
        acc.addTransaction(new Account.TransactionRecord(ts, type, other, amount, desc));
    }

    @Override
    public long getBalance(UUID player) {
        if (player == null)
            throw new IllegalArgumentException("UUID cannot be null");
        checkServerSide();

        String id = PlayerAccounts.personalAccountId(player);
        AccountRegistry reg = PrimeBankState.get().accounts();

        // Lock for reading to ensure visibility
        ReentrantLock lock = AccountLockManager.getLock(id);
        lock.lock();
        try {
            if (!reg.exists(id)) {
                throw new PrimeBankException("Account not found for player: " + player);
            }
            Account acc = reg.get(id);
            return acc.getBalanceCents();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PrimeBankResult deposit(UUID player, long amount, String source, String description) {
        if (player == null)
            throw new IllegalArgumentException("UUID cannot be null");
        if (amount <= 0)
            return PrimeBankResult.INVALID_AMOUNT;
        checkServerSide();

        String id = PlayerAccounts.ensurePersonal(player); // ensurePersonal is safe? It calls accounts().create if
                                                           // needed.
        // ensurePersonal calls PrimeBankState.get().accounts().create inside.
        // We should lock around ensurePersonal or inside it, but ensurePersonal relies
        // on ConcurrentHashMap putIfAbsent.
        // However, to be perfectly safe and consistent with "getLock(id)" logic, let's
        // just get the ID and rely on registry thread-safety for existence, locking for
        // balance mutation.
        // PlayerAccounts.ensurePersonal checks existence and creates if missing. It is
        // thread-safe enough for creation.

        String sSource = sanitize(source);
        String sDesc = sanitize(description);

        ReentrantLock lock = AccountLockManager.getLock(id);
        lock.lock();
        try {
            Account acc = PrimeBankState.get().accounts().get(id);
            if (acc == null) {
                // Should potentially not happen given ensurePersonal, but safe check
                return PrimeBankResult.ACCOUNT_NOT_FOUND;
            }
            acc.deposit(amount);
            record(acc, "DEPOSIT_API", sSource, amount, sDesc);
            TransactionLogger
                    .log(String.format("API DEPOSIT: %s deposited %s to %s (Ref: %s)", sSource, amount, id, sDesc));

            // Note: We do NOT explicitly save here, relying on autosave as planned.
            return PrimeBankResult.SUCCESS;
        } catch (Exception e) {
            com.primebank.PrimeBankMod.LOGGER.error("[PrimeBank API] Error in deposit", e);
            return PrimeBankResult.INTERNAL_ERROR;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PrimeBankResult withdraw(UUID player, long amount, String source, String description) {
        if (player == null)
            throw new IllegalArgumentException("UUID cannot be null");
        if (amount <= 0)
            return PrimeBankResult.INVALID_AMOUNT;
        checkServerSide();

        String id = PlayerAccounts.personalAccountId(player);
        // Note: For withdraw, we generally don't "ensure" account if it doesn't exist,
        // as you can't withdraw from non-existent.
        // But PlayerAccounts.personalAccountId just generates the string ID "u:uuid".

        String sSource = sanitize(source);
        String sDesc = sanitize(description);

        ReentrantLock lock = AccountLockManager.getLock(id);
        lock.lock();
        try {
            Account acc = PrimeBankState.get().accounts().get(id);
            if (acc == null) {
                return PrimeBankResult.ACCOUNT_NOT_FOUND;
            }
            if (acc.getBalanceCents() < amount) {
                return PrimeBankResult.INSUFFICIENT_FUNDS;
            }
            acc.withdraw(amount);
            record(acc, "WITHDRAW_API", sSource, amount, sDesc);
            TransactionLogger
                    .log(String.format("API WITHDRAW: %s withdrew %s from %s (Ref: %s)", sSource, amount, id, sDesc));
            return PrimeBankResult.SUCCESS;
        } catch (Exception e) {
            com.primebank.PrimeBankMod.LOGGER.error("[PrimeBank API] Error in withdraw", e);
            return PrimeBankResult.INTERNAL_ERROR;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PrimeBankResult transfer(UUID from, UUID to, long amount, String source, String description) {
        if (from == null || to == null)
            throw new IllegalArgumentException("UUIDs cannot be null");
        if (amount <= 0)
            return PrimeBankResult.INVALID_AMOUNT;
        checkServerSide();

        if (from.equals(to)) {
            return PrimeBankResult.SUCCESS; // No-op
        }

        String fromId = PlayerAccounts.personalAccountId(from);
        String toId = PlayerAccounts.ensurePersonal(to); // Ensure recipient exists

        String sSource = sanitize(source);
        String sDesc = sanitize(description);

        List<String> keys = new ArrayList<>();
        keys.add(fromId);
        keys.add(toId);
        Collections.sort(keys); // Strict ordering to prevent deadlock

        ReentrantLock lock1 = AccountLockManager.getLock(keys.get(0));
        ReentrantLock lock2 = AccountLockManager.getLock(keys.get(1));

        lock1.lock();
        lock2.lock();
        try {
            Account accFrom = PrimeBankState.get().accounts().get(fromId);
            Account accTo = PrimeBankState.get().accounts().get(toId);

            if (accFrom == null) {
                return PrimeBankResult.ACCOUNT_NOT_FOUND; // Sender must exist
            }
            if (accTo == null) {
                return PrimeBankResult.ACCOUNT_NOT_FOUND; // Recipient must exist (guaranteed by ensurePersonal usually)
            }

            if (accFrom.getBalanceCents() < amount) {
                return PrimeBankResult.INSUFFICIENT_FUNDS;
            }

            accFrom.withdraw(amount);
            accTo.deposit(amount);

            record(accFrom, "TRANSFER_API_OUT", toId, amount, sDesc + " (to " + toId + ")");
            record(accTo, "TRANSFER_API_IN", fromId, amount, sDesc + " (from " + fromId + ")");

            TransactionLogger.log(String.format("API TRANSFER: %s transferred %s from %s to %s (Ref: %s)",
                    sSource, amount, fromId, toId, sDesc));

            return PrimeBankResult.SUCCESS;
        } catch (Exception e) {
            com.primebank.PrimeBankMod.LOGGER.error("[PrimeBank API] Error in transfer", e);
            return PrimeBankResult.INTERNAL_ERROR;
        } finally {
            lock2.unlock();
            lock1.unlock();
        }
    }
}
