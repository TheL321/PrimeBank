package com.primebank.core.state;

import java.util.UUID;

import com.primebank.PrimeBankMod;
import com.primebank.core.accounts.Account;
import com.primebank.core.accounts.AccountRegistry;
import com.primebank.core.accounts.AccountType;

/*
 English: Global in-memory state. Holds registries and ensures special accounts exist.
 Español: Estado global en memoria. Mantiene registros y asegura que existan cuentas especiales.
*/
public final class PrimeBankState {
    private static final PrimeBankState INSTANCE = new PrimeBankState();

    public static final String CENTRAL_ACCOUNT_ID = "central";

    private final AccountRegistry accounts = new AccountRegistry();

    private PrimeBankState() {}

    public static PrimeBankState get() { return INSTANCE; }

    /*
     English: Initialize global state and ensure the central bank account exists.
     Español: Inicializa el estado global y asegura la existencia de la cuenta del banco central.
    */
    public void init() {
        ensureCentralAccount();
    }

    public AccountRegistry accounts() { return accounts; }

    public Account ensureCentralAccount() {
        if (!accounts.exists(CENTRAL_ACCOUNT_ID)) {
            PrimeBankMod.LOGGER.info("[PrimeBank] Creating central bank account / Creando cuenta del banco central");
            accounts.create(CENTRAL_ACCOUNT_ID, AccountType.CENTRAL, (UUID) null, 0L);
        }
        return accounts.get(CENTRAL_ACCOUNT_ID);
    }
}
