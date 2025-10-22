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
    private final java.util.concurrent.ConcurrentHashMap<String, Long> posPending = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, String> companyNames = new java.util.concurrent.ConcurrentHashMap<>();

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

    /*
     English: Set/get/clear pending POS charges by company id (cents).
     Español: Establecer/obtener/limpiar cargos POS pendientes por id de empresa (centavos).
    */
    public void setPendingCharge(String companyId, long cents) {
        if (companyId == null) return;
        if (cents <= 0) { posPending.remove(companyId); } else { posPending.put(companyId, cents); }
    }
    public long getPendingCharge(String companyId) {
        Long v = posPending.get(companyId);
        return v == null ? 0L : v.longValue();
    }
    public void clearPendingCharge(String companyId) { if (companyId != null) posPending.remove(companyId); }

    /*
     English: Return a snapshot view of all pending charges.
     Español: Devolver una vista instantánea de todos los cargos pendientes.
    */
    public java.util.Map<String, Long> getAllPendingCharges() {
        return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(posPending));
    }

    /*
     English: Replace the pending charges map from persistence on server load.
     Español: Reemplazar el mapa de cargos pendientes desde la persistencia al cargar el servidor.
    */
    public void loadPendingCharges(java.util.Map<String, Long> data) {
        posPending.clear();
        if (data != null) posPending.putAll(data);
    }

    /*
     English: Set/get company display names, and snapshot/loader for persistence.
     Español: Establecer/obtener nombres visibles de empresas, y snapshot/cargador para persistencia.
    */
    public void setCompanyName(String companyId, String name) {
        if (companyId == null) return;
        if (name == null || name.trim().isEmpty()) companyNames.remove(companyId);
        else companyNames.put(companyId, name.trim());
    }
    public String getCompanyName(String companyId) {
        return companyId == null ? null : companyNames.get(companyId);
    }
    public java.util.Map<String, String> getAllCompanyNames() {
        return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(companyNames));
    }
    public void loadCompanyNames(java.util.Map<String, String> names) {
        companyNames.clear();
        if (names != null) companyNames.putAll(names);
    }

    public Account ensureCentralAccount() {
        if (!accounts.exists(CENTRAL_ACCOUNT_ID)) {
            PrimeBankMod.LOGGER.info("[PrimeBank] Creating central bank account / Creando cuenta del banco central");
            accounts.create(CENTRAL_ACCOUNT_ID, AccountType.CENTRAL, (UUID) null, 0L);
        }
        return accounts.get(CENTRAL_ACCOUNT_ID);
    }
}
