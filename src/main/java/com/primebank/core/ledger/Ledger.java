package com.primebank.core.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.primebank.core.Money;
import com.primebank.core.accounts.Account;
import com.primebank.core.accounts.AccountRegistry;
import com.primebank.core.state.PrimeBankState;
import com.primebank.core.locks.AccountLockManager;

/*
 English: Ledger operations with atomic transfers and fee logic.
 Español: Operaciones del libro mayor con transferencias atómicas y lógica de comisiones.
*/
public final class Ledger {
    private final AccountRegistry accounts;

    public Ledger(AccountRegistry accounts) {
        this.accounts = accounts;
    }

    /*
     English: POS charge: withdraw full amount from buyer, deposit 95% to company and 5% to central atomically.
     Español: Cobro POS: retirar el monto completo del comprador, depositar 95% a la empresa y 5% al banco central atómicamente.
    */
    public TransferResult posCharge(String buyerId, String companyId, long amountCents) {
        if (amountCents <= 0) {
            return new TransferResult(false, "amount_le_zero", "Amount must be > 0", false, 0);
        }
        if (buyerId == null || companyId == null || buyerId.equals(companyId)) {
            return new TransferResult(false, "invalid_accounts", "Invalid accounts", false, 0);
        }
        Account buyer = accounts.get(buyerId);
        Account company = accounts.get(companyId);
        if (buyer == null || company == null) {
            return new TransferResult(false, "account_not_found", "Account not found", false, 0);
        }
        String centralId = PrimeBankState.CENTRAL_ACCOUNT_ID;
        Account central = accounts.get(centralId);
        if (central == null) central = PrimeBankState.get().ensureCentralAccount();

        List<String> keys = new ArrayList<>();
        keys.add(buyerId); keys.add(companyId); keys.add(centralId);
        Collections.sort(keys);
        List<ReentrantLock> locks = new ArrayList<>(keys.size());
        for (String k : keys) { ReentrantLock l = AccountLockManager.getLock(k); l.lock(); locks.add(l); }
        try {
            long bal = buyer.getBalanceCents();
            if (bal < amountCents) {
                return new TransferResult(false, "insufficient", "Insufficient funds", false, 0);
            }
            long toCompany = Money.multiplyBps(amountCents, 9500);
            long toCentral = Money.add(amountCents, -toCompany);
            buyer.withdraw(amountCents);
            company.deposit(toCompany);
            if (toCentral > 0) central.deposit(toCentral);
            return new TransferResult(true, "ok", "Transfer completed", false, 0);
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) locks.get(i).unlock();
        }
    }

    /*
     English: Result of a deposit or withdraw operation.
     Español: Resultado de una operación de depósito o retiro.
    */
    public static final class OpResult {
        public final boolean success;
        public final String code; // en: result code for i18n mapping; es: código de resultado para i18n
        public final String message;

        public OpResult(boolean success, String code, String message) {
            this.success = success;
            this.code = code;
            this.message = message;
        }
    }

    /*
     English: Result of a transfer operation.
     Español: Resultado de una operación de transferencia.
    */
    public static final class TransferResult {
        public final boolean success;
        public final String code; // en: result code for i18n; es: código de resultado para i18n
        public final String message;
        public final boolean feeApplied;
        public final long feeCents;

        public TransferResult(boolean success, String code, String message, boolean feeApplied, long feeCents) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.feeApplied = feeApplied;
            this.feeCents = feeCents;
        }
    }

    /*
     English: Deposit amount into an account.
     Español: Depositar monto en una cuenta.
    */
    public OpResult deposit(String accountId, long amountCents) {
        if (amountCents <= 0) return new OpResult(false, "amount_le_zero", "Amount must be > 0");
        Account acc = accounts.get(accountId);
        if (acc == null) return new OpResult(false, "account_not_found", "Account not found");
        ReentrantLock lock = AccountLockManager.getLock(accountId);
        lock.lock();
        try {
            acc.deposit(amountCents);
            return new OpResult(true, "ok", "Deposit completed");
        } finally {
            lock.unlock();
        }
    }

    /*
     English: Withdraw amount from an account.
     Español: Retirar monto de una cuenta.
    */
    public OpResult withdraw(String accountId, long amountCents) {
        if (amountCents <= 0) return new OpResult(false, "amount_le_zero", "Amount must be > 0");
        Account acc = accounts.get(accountId);
        if (acc == null) return new OpResult(false, "account_not_found", "Account not found");
        ReentrantLock lock = AccountLockManager.getLock(accountId);
        lock.lock();
        try {
            if (acc.getBalanceCents() < amountCents) {
                return new OpResult(false, "insufficient", "Insufficient funds");
            }
            acc.withdraw(amountCents);
            return new OpResult(true, "ok", "Withdraw completed");
        } finally {
            lock.unlock();
        }
    }

    /*
     English: Transfer amount from one account to another. If amount > 50% of sender's starting balance, apply 2% fee to sender and route it to the central bank.
     Español: Transferir monto de una cuenta a otra. Si el monto > 50% del saldo inicial del remitente, aplicar 2% de comisión al remitente y enviarla al banco central.
    */
    public TransferResult transfer(String fromId, String toId, long amountCents) {
        if (amountCents <= 0) {
            return new TransferResult(false, "amount_le_zero", "Amount must be > 0", false, 0);
        }
        if (fromId == null || toId == null || fromId.equals(toId)) {
            return new TransferResult(false, "invalid_accounts", "Invalid accounts", false, 0);
        }
        Account from = accounts.get(fromId);
        Account to = accounts.get(toId);
        if (from == null || to == null) {
            return new TransferResult(false, "account_not_found", "Account not found", false, 0);
        }
        String centralId = PrimeBankState.CENTRAL_ACCOUNT_ID;
        Account central = accounts.get(centralId);
        if (central == null) {
            central = PrimeBankState.get().ensureCentralAccount();
        }

        Set<String> keys = new HashSet<>();
        keys.add(fromId);
        keys.add(toId);
        keys.add(centralId);
        List<String> ordered = new ArrayList<>(keys);
        Collections.sort(ordered);

        List<ReentrantLock> locks = new ArrayList<>(ordered.size());
        for (String k : ordered) {
            ReentrantLock l = AccountLockManager.getLock(k);
            l.lock();
            locks.add(l);
        }
        try {
            long startingBalance = from.getBalanceCents();
            boolean feeApplied = amountCents > (startingBalance / 2);
            long fee = feeApplied ? Money.multiplyBps(amountCents, 200) : 0L;
            long totalDebit = Money.add(amountCents, fee);
            if (startingBalance < totalDebit) {
                return new TransferResult(false, "insufficient", "Insufficient funds", feeApplied, fee);
            }
            from.withdraw(totalDebit);
            to.deposit(amountCents);
            if (fee > 0) {
                central.deposit(fee);
            }
            return new TransferResult(true, "ok", "Transfer completed", feeApplied, fee);
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                locks.get(i).unlock();
            }
        }
    }
}
