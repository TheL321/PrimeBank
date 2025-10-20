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

    public Account(String id, AccountType type, UUID ownerUuid, long initialBalanceCents) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.ownerUuid = ownerUuid;
        this.balanceCents = initialBalanceCents;
    }

    public String getId() { return id; }

    public AccountType getType() { return type; }

    public UUID getOwnerUuid() { return ownerUuid; }

    public long getBalanceCents() { return balanceCents; }

    /*
     English: Increase balance by amount (>= 0).
     Español: Incrementa el saldo por el monto (>= 0).
    */
    public void deposit(long amountCents) {
        if (amountCents < 0) throw new IllegalArgumentException("Invalid deposit amount / Monto de depósito inválido");
        balanceCents = Math.addExact(balanceCents, amountCents);
    }

    /*
     English: Decrease balance by amount (>= 0) when sufficient funds.
     Español: Disminuye el saldo por el monto (>= 0) cuando hay fondos suficientes.
    */
    public void withdraw(long amountCents) {
        if (amountCents < 0) throw new IllegalArgumentException("Invalid withdraw amount / Monto de retiro inválido");
        if (balanceCents < amountCents) throw new IllegalStateException("Insufficient funds / Fondos insuficientes");
        balanceCents = Math.subtractExact(balanceCents, amountCents);
    }
}
