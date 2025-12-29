package com.primebank.core.ledger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.primebank.core.Money;
import com.primebank.core.accounts.Account;
import com.primebank.core.accounts.AccountRegistry;
import com.primebank.core.config.PrimeBankConfig;
import com.primebank.core.state.PrimeBankState;
import com.primebank.core.locks.AccountLockManager;

/*
 English: Ledger operations with atomic transfers and fee logic.
 Español: Operaciones del libro mayor con transferencias atómicas y lógica de comisiones.
*/
public final class Ledger {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AccountRegistry accounts;

    public Ledger(AccountRegistry accounts) {
        this.accounts = accounts;
    }

    /*
     * English: Resolve where central fee income should be deposited (config redirect or
     * central fallback).
     * Español: Resolver dónde debe depositarse el ingreso de comisiones del banco
     * central (redirección por config o central por defecto).
     */
    private String getCentralFeeSinkId() {
        String cfg = PrimeBankConfig.CENTRAL_FEE_REDIRECT_COMPANY_ID;
        if (cfg != null) {
            String trimmed = cfg.trim();
            if (!trimmed.isEmpty()) {
                // English: Allow redirect to either a direct account id (e.g. "c:<uuid>")
                // or a company identifier (ticker/id).
                // Español: Permitir redirección a un id de cuenta directo (ej. "c:<uuid>")
                // o a un identificador de empresa (ticker/id).
                if (accounts.get(trimmed) != null) {
                    return trimmed;
                }

                String resolvedCompanyId = PrimeBankState.get().resolveCompanyIdentifier(trimmed);
                if (resolvedCompanyId != null) {
                    // English: Ensure the company account exists so fee deposits don't silently
                    // fall back to central.
                    // Español: Asegurar que la cuenta de la empresa exista para que los depósitos
                    // de comisiones no vuelvan silenciosamente al central.
                    if (accounts.get(resolvedCompanyId) == null) {
                        com.primebank.core.company.Company c = PrimeBankState.get().companies().get(resolvedCompanyId);
                        if (c != null) {
                            accounts.create(resolvedCompanyId, com.primebank.core.accounts.AccountType.COMPANY,
                                    c.ownerUuid, 0L);
                        }
                    }
                    if (accounts.get(resolvedCompanyId) != null) {
                        return resolvedCompanyId;
                    }
                }

                com.primebank.PrimeBankMod.LOGGER.warn(
                        "[PrimeBank] Central fee redirect target '{}' not found; keeping fees in central / destino no encontrado; se mantienen las comisiones en central",
                        trimmed);
            }
        }
        return PrimeBankState.CENTRAL_ACCOUNT_ID;
    }

    /*
     * English: Deposit fee into central or redirected sink, recording the collection.
     * Español: Depositar comisión en el central o destino redirigido, registrando la
     * cobranza.
     */
    private void depositCentralFee(Account central, String sinkId, long amountCents, String sourceLabel) {
        Account sink = sinkId.equals(central.getId()) ? central : accounts.get(sinkId);
        if (sink == null) {
            sink = central;
            sinkId = central.getId();
            com.primebank.PrimeBankMod.LOGGER.warn(
                    "[PrimeBank] Fee sink missing; depositing to central instead / Destino de comisión faltante; se deposita en central");
        }
        sink.deposit(amountCents);
        record(sink, "FEE_COLLECT", sourceLabel, amountCents, "Fee collected (" + sourceLabel + ")");
        if (!sinkId.equals(central.getId())) {
            com.primebank.PrimeBankMod.LOGGER.info(
                    "[PrimeBank] Central fee redirect: {} cents from {} routed to {} / Redirección de comisión central: {} centavos desde {} hacia {}",
                    amountCents, sourceLabel, sinkId, amountCents, sourceLabel, sinkId);
        }
    }

    private void record(Account acc, String type, String other, long amount, String desc) {
        String ts = LocalDateTime.now().format(DATE_FORMAT);
        acc.addTransaction(new Account.TransactionRecord(ts, type, other, amount, desc));
    }

    /*
     * English: Apply cashback to buyer funded by the central bank, atomically (min
     * with central balance).
     * Español: Aplicar cashback al comprador financiado por el banco central,
     * atómicamente (mínimo con saldo del central).
     */
    public OpResult applyCashbackToBuyer(String buyerId, long cashbackCents) {
        if (cashbackCents <= 0)
            return new OpResult(true, "ok", "No cashback");
        String centralId = PrimeBankState.CENTRAL_ACCOUNT_ID;
        Account buyer = accounts.get(buyerId);
        Account central = accounts.get(centralId);
        if (buyer == null || central == null)
            central = PrimeBankState.get().ensureCentralAccount();
        List<String> keys = new ArrayList<>();
        keys.add(buyerId);
        keys.add(centralId);
        Collections.sort(keys);
        List<ReentrantLock> locks = new ArrayList<>(keys.size());
        for (String k : keys) {
            ReentrantLock l = com.primebank.core.locks.AccountLockManager.getLock(k);
            l.lock();
            locks.add(l);
        }
        try {
            long centralBal = central.getBalanceCents();
            long amt = Math.min(cashbackCents, Math.max(0, centralBal));

            // English: SECURITY FIX: Warn admins if central bank cannot fulfill cashback
            // request.
            // Español: CORRECCIÓN DE SEGURIDAD: Advertir a admins si banco central no puede
            // cumplir solicitud de cashback.
            if (amt < cashbackCents) {
                com.primebank.PrimeBankMod.LOGGER.warn(
                        "[PrimeBank] SECURITY WARNING: Central bank balance ({} cents) is insufficient for cashback request ({} cents). Only {} cents will be credited.",
                        centralBal, cashbackCents, amt);
            }

            if (amt <= 0)
                return new OpResult(false, "central_insufficient", "Central has no funds");
            central.withdraw(amt);
            buyer.deposit(amt);

            record(central, "CASHBACK_OUT", buyerId, amt, "Cashback to buyer");
            record(buyer, "CASHBACK_IN", centralId, amt, "Cashback from central");

            com.primebank.core.logging.TransactionLogger
                    .log(String.format("CASHBACK: Buyer %s received %s cents from Central", buyerId, amt));
            return new OpResult(true, "ok", "Cashback applied");
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--)
                locks.get(i).unlock();
        }
    }

    /*
     * English: Primary market buy: buyer pays gross + buyerFee; company receives
     * gross - issuerFee; fees to central.
     * Español: Compra en mercado primario: comprador paga bruto + comisión
     * comprador; empresa recibe bruto - comisión emisor; comisiones al central.
     */
    public TransferResult marketPrimaryBuy(String buyerId, String companyId, long grossCents, int buyerFeeBps,
            int issuerFeeBps) {
        if (grossCents <= 0)
            return new TransferResult(false, "amount_le_zero", "Amount must be > 0", false, 0);
        if (buyerId == null || companyId == null || buyerId.equals(companyId)) {
            return new TransferResult(false, "invalid_accounts", "Invalid accounts", false, 0);
        }
        Account buyer = accounts.get(buyerId);
        Account company = accounts.get(companyId);
        if (buyer == null || company == null)
            return new TransferResult(false, "account_not_found", "Account not found", false, 0);
        String centralId = PrimeBankState.CENTRAL_ACCOUNT_ID;
        Account central = accounts.get(centralId);
        if (central == null)
            central = PrimeBankState.get().ensureCentralAccount();
        String sinkId = getCentralFeeSinkId();

        Set<String> keys = new HashSet<>();
        keys.add(buyerId);
        keys.add(companyId);
        keys.add(centralId);
        keys.add(sinkId);
        List<String> ordered = new ArrayList<>(keys);
        Collections.sort(ordered);
        List<ReentrantLock> locks = new ArrayList<>(ordered.size());
        for (String k : ordered) {
            ReentrantLock l = com.primebank.core.locks.AccountLockManager.getLock(k);
            l.lock();
            locks.add(l);
        }
        try {
            long buyerFee = Money.multiplyBps(grossCents, buyerFeeBps);
            long issuerFee = Money.multiplyBps(grossCents, issuerFeeBps);
            long totalDebit = Money.add(grossCents, buyerFee);
            if (buyer.getBalanceCents() < totalDebit) {
                return new TransferResult(false, "insufficient", "Insufficient funds", true, buyerFee);
            }
            buyer.withdraw(totalDebit);
            long netToCompany = Money.add(grossCents, -issuerFee);
            company.deposit(netToCompany);
            long toCentral = Money.add(buyerFee, issuerFee);
            if (toCentral > 0)
                depositCentralFee(central, sinkId, toCentral, "MARKET");

            record(buyer, "MARKET_BUY", companyId, totalDebit, "Shares buy (incl fees)");
            record(company, "MARKET_SELL", buyerId, netToCompany, "Shares sell (net)");

            com.primebank.core.logging.TransactionLogger.log(
                    String.format("MARKET BUY: Buyer %s bought from Company %s. Gross: %s, BuyerFee: %s, IssuerFee: %s",
                            buyerId, companyId, grossCents, buyerFee, issuerFee));
            return new TransferResult(true, "ok", "Market primary completed", true, buyerFee);
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--)
                locks.get(i).unlock();
        }
    }

    /*
     * English: POS charge: withdraw full amount from buyer, deposit 95% to company
     * and 5% to central atomically.
     * Español: Cobro POS: retirar el monto completo del comprador, depositar 95% a
     * la empresa y 5% al banco central atómicamente.
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
        if (central == null)
            central = PrimeBankState.get().ensureCentralAccount();
        String sinkId = getCentralFeeSinkId();

        List<String> keys = new ArrayList<>();
        keys.add(buyerId);
        keys.add(companyId);
        keys.add(centralId);
        keys.add(sinkId);
        Collections.sort(keys);
        List<ReentrantLock> locks = new ArrayList<>(keys.size());
        for (String k : keys) {
            ReentrantLock l = AccountLockManager.getLock(k);
            l.lock();
            locks.add(l);
        }
        try {
            long bal = buyer.getBalanceCents();
            if (bal < amountCents) {
                return new TransferResult(false, "insufficient", "Insufficient funds", false, 0);
            }
            long toCompany = Money.multiplyBps(amountCents, 9500);
            long toCentral = Money.add(amountCents, -toCompany);
            buyer.withdraw(amountCents);
            company.deposit(toCompany);
            if (toCentral > 0)
                depositCentralFee(central, sinkId, toCentral, "POS");

            record(buyer, "POS_PAY", companyId, amountCents, "POS Payment");
            record(company, "POS_RECEIVE", buyerId, toCompany, "POS Revenue (95%)");

            com.primebank.core.logging.TransactionLogger
                    .log(String.format("POS CHARGE: Buyer %s paid Company %s. Amount: %s, ToCompany: %s, ToCentral: %s",
                            buyerId, companyId, amountCents, toCompany, toCentral));
            return new TransferResult(true, "ok", "Transfer completed", false, 0);
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--)
                locks.get(i).unlock();
        }
    }

    /*
     * English: Result of a deposit or withdraw operation.
     * Español: Resultado de una operación de depósito o retiro.
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
     * English: Result of a transfer operation.
     * Español: Resultado de una operación de transferencia.
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
     * English: Deposit amount into an account.
     * Español: Depositar monto en una cuenta.
     */
    public OpResult deposit(String accountId, long amountCents) {
        if (amountCents <= 0)
            return new OpResult(false, "amount_le_zero", "Amount must be > 0");
        Account acc = accounts.get(accountId);
        if (acc == null)
            return new OpResult(false, "account_not_found", "Account not found");
        ReentrantLock lock = AccountLockManager.getLock(accountId);
        lock.lock();
        try {
            acc.deposit(amountCents);
            record(acc, "DEPOSIT", "SYSTEM", amountCents, "Manual Deposit");
            com.primebank.core.logging.TransactionLogger
                    .log(String.format("DEPOSIT: Account %s deposited %s cents", accountId, amountCents));
            return new OpResult(true, "ok", "Deposit completed");
        } finally {
            lock.unlock();
        }
    }

    /*
     * English: Withdraw amount from an account.
     * Español: Retirar monto de una cuenta.
     */
    public OpResult withdraw(String accountId, long amountCents) {
        if (amountCents <= 0)
            return new OpResult(false, "amount_le_zero", "Amount must be > 0");
        Account acc = accounts.get(accountId);
        if (acc == null)
            return new OpResult(false, "account_not_found", "Account not found");
        ReentrantLock lock = AccountLockManager.getLock(accountId);
        lock.lock();
        try {
            if (acc.getBalanceCents() < amountCents) {
                return new OpResult(false, "insufficient", "Insufficient funds");
            }
            acc.withdraw(amountCents);
            record(acc, "WITHDRAW", "SYSTEM", amountCents, "Manual Withdraw");
            com.primebank.core.logging.TransactionLogger
                    .log(String.format("WITHDRAW: Account %s withdrew %s cents", accountId, amountCents));
            return new OpResult(true, "ok", "Withdraw completed");
        } finally {
            lock.unlock();
        }
    }

    /*
     * English: Transfer amount from one account to another. If amount > 50% of
     * sender's starting balance, apply 2% fee to sender and route it to the central
     * bank.
     * Español: Transferir monto de una cuenta a otra. Si el monto > 50% del saldo
     * inicial del remitente, aplicar 2% de comisión al remitente y enviarla al
     * banco central.
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
        String sinkId = getCentralFeeSinkId();

        Set<String> keys = new HashSet<>();
        keys.add(fromId);
        keys.add(toId);
        keys.add(centralId);
        keys.add(sinkId);
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
                depositCentralFee(central, sinkId, fee, "TRANSFER");
            }

            record(from, "TRANSFER_OUT", toId, totalDebit, "Transfer to " + toId);
            record(to, "TRANSFER_IN", fromId, amountCents, "Transfer from " + fromId);

            com.primebank.core.logging.TransactionLogger
                    .log(String.format("TRANSFER: From %s to %s. Amount: %s, Fee: %s", fromId, toId, amountCents, fee));
            return new TransferResult(true, "ok", "Transfer completed", feeApplied, fee);
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                locks.get(i).unlock();
            }
        }
    }

    /*
     * English: Admin withdraw from central bank.
     * Español: Retiro de administrador del banco central.
     */
    public OpResult centralWithdraw(String adminName, long amountCents) {
        if (amountCents <= 0)
            return new OpResult(false, "amount_le_zero", "Amount must be > 0");
        String centralId = PrimeBankState.CENTRAL_ACCOUNT_ID;
        Account central = accounts.get(centralId);
        if (central == null)
            central = PrimeBankState.get().ensureCentralAccount();

        ReentrantLock lock = AccountLockManager.getLock(centralId);
        lock.lock();
        try {
            if (central.getBalanceCents() < amountCents) {
                return new OpResult(false, "insufficient", "Insufficient funds");
            }
            central.withdraw(amountCents);
            record(central, "ADMIN_WITHDRAW", adminName, amountCents, "Admin withdraw");
            com.primebank.core.logging.TransactionLogger
                    .log(String.format("CENTRAL WITHDRAW: Admin %s withdrew %s cents", adminName, amountCents));
            return new OpResult(true, "ok", "Withdraw completed");
        } finally {
            lock.unlock();
        }
    }

}
