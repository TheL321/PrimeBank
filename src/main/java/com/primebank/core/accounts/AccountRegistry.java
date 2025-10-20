package com.primebank.core.accounts;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
 English: Registry of accounts stored in memory.
 Español: Registro de cuentas almacenadas en memoria.
*/
public class AccountRegistry {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public Account create(String id, AccountType type, UUID ownerUuid, long initialBalanceCents) {
        Account acc = new Account(id, type, ownerUuid, initialBalanceCents);
        Account prev = accounts.putIfAbsent(id, acc);
        if (prev != null) return prev;
        return acc;
    }

    public Account get(String id) {
        return accounts.get(id);
    }

    public boolean exists(String id) {
        return accounts.containsKey(id);
    }

    public Collection<Account> all() {
        return Collections.unmodifiableCollection(accounts.values());
    }
}
