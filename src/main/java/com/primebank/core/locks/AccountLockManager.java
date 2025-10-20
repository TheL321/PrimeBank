package com.primebank.core.locks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/*
 English: Provides per-key reentrant locks to serialize operations on accounts/companies.
 Español: Proporciona locks reentrantes por clave para serializar operaciones en cuentas/compañías.
*/
public final class AccountLockManager {
    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private AccountLockManager() {}

    public static ReentrantLock getLock(String key) {
        return LOCKS.computeIfAbsent(key, k -> new ReentrantLock(true));
    }
}
