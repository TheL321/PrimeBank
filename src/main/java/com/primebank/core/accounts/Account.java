package com.primebank.core.accounts;

import java.util.Objects;
import java.util.UUID;

/*
 English: Represents a bank account with a balance in cents (long).
 Español: Representa una cuenta bancaria con saldo en centavos (long).
*/
public class Account {
    private final String id;
    private final AccountType type;
    private final UUID ownerUuid;
    private long balanceCents;

    private final java.util.List<TransactionRecord> history = new java.util.ArrayList<>();

    public Account(String id, AccountType type, UUID ownerUuid, long initialBalanceCents) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.ownerUuid = ownerUuid;
        this.balanceCents = initialBalanceCents;
    }

    public String getId() {
        return id;
    }

    public AccountType getType() {
        return type;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public java.util.List<TransactionRecord> getHistory() {
        return java.util.Collections.unmodifiableList(history);
    }

    public void addTransaction(TransactionRecord record) {
        if (history.size() >= 20) {
            history.remove(0);
        }
        history.add(record);
    }

    // Used during loading
    public void setHistory(java.util.List<TransactionRecord> history) {
        this.history.clear();
        if (history != null) {
            this.history.addAll(history);
        }
    }

    /*
     * English: Increase balance by amount (>= 0).
     * Español: Incrementa el saldo por el monto (>= 0).
     */
    public void deposit(long amountCents) {
        if (amountCents < 0)
            throw new IllegalArgumentException("Invalid deposit amount / Monto de depósito inválido");
        balanceCents = Math.addExact(balanceCents, amountCents);
    }

    /*
     * English: Decrease balance by amount (>= 0) when sufficient funds.
     * Español: Disminuye el saldo por el monto (>= 0) cuando hay fondos
     * suficientes.
     */
    public void withdraw(long amountCents) {
        if (amountCents < 0)
            throw new IllegalArgumentException("Invalid withdraw amount / Monto de retiro inválido");
        if (balanceCents < amountCents)
            throw new IllegalStateException("Insufficient funds / Fondos insuficientes");
        balanceCents = Math.subtractExact(balanceCents, amountCents);
    }

    public static class TransactionRecord {
        public String timestamp;
        public String type; // DEPOSIT, WITHDRAW, TRANSFER, etc.
        public String otherParty; // Account ID or Name
        public long amount;
        public String description;

        public TransactionRecord() {
        }

        public TransactionRecord(String timestamp, String type, String otherParty, long amount, String description) {
            this.timestamp = timestamp;
            this.type = type;
            this.otherParty = otherParty;
            this.amount = amount;
            this.description = description;
        }
    }
}
