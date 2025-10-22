package com.primebank.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.primebank.PrimeBankMod;
import com.primebank.core.accounts.Account;
import com.primebank.core.accounts.AccountRegistry;
import com.primebank.core.accounts.AccountType;
import com.primebank.core.state.PrimeBankState;
import com.google.gson.annotations.SerializedName;

/*
 English: Persistence service for PrimeBank. Loads/saves JSON snapshots asynchronously.
 Español: Servicio de persistencia para PrimeBank. Carga/guarda instantáneas JSON de forma asíncrona.
*/
public final class BankPersistence {
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> new Thread(r, "PrimeBank-Persist"));

    private BankPersistence() {}

    /*
     English: Load accounts from accounts.json if present.
     Español: Cargar cuentas desde accounts.json si existe.
    */
    public static void loadAll() {
        File file = PersistencePaths.accountsFile();
        AccountsSnapshot snap = JsonUtil.read(file, AccountsSnapshot.class);
        if (snap == null || snap.accounts == null) return;
        AccountRegistry reg = PrimeBankState.get().accounts();
        for (AccountRecord r : snap.accounts) {
            AccountType type = AccountType.valueOf(r.type);
            reg.create(r.id, type, r.ownerUuid == null ? null : java.util.UUID.fromString(r.ownerUuid), r.balanceCents);
        }
        PrimeBankMod.LOGGER.info("[PrimeBank] Loaded accounts: {}", snap.accounts.size());
        // English: Load pending POS charges map if present.
        // Español: Cargar el mapa de cargos POS pendientes si existe.
        if (snap.posPending != null) {
            PrimeBankState.get().loadPendingCharges(snap.posPending);
        }
        // English: Load company display names if present.
        // Español: Cargar nombres visibles de empresas si existen.
        if (snap.companyNames != null) {
            PrimeBankState.get().loadCompanyNames(snap.companyNames);
        }
        // English: Load global cashback bps if present.
        // Español: Cargar cashback global en bps si está presente.
        if (snap.globalCashbackBps != null) {
            PrimeBankState.get().setGlobalCashbackBps(snap.globalCashbackBps.intValue());
        }
    }

    /*
     English: Enqueue an asynchronous snapshot save of accounts.
     Español: Encolar un guardado asíncrono de instantáneas de cuentas.
    */
    public static void saveAllAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                saveAllBlocking();
            } catch (Exception ex) {
                PrimeBankMod.LOGGER.error("[PrimeBank] Failed to save snapshot / Error al guardar snapshot", ex);
            }
        }, EXEC);
    }

    /*
     English: Save snapshot synchronously (blocking).
     Español: Guardar snapshot de forma síncrona (bloqueante).
    */
    public static void saveAllBlocking() {
        File file = PersistencePaths.accountsFile();
        AccountsSnapshot snap = new AccountsSnapshot();
        snap.accounts = new ArrayList<>();
        for (Account a : PrimeBankState.get().accounts().all()) {
            AccountRecord r = new AccountRecord();
            r.id = a.getId();
            r.type = a.getType().name();
            r.ownerUuid = a.getOwnerUuid() == null ? null : a.getOwnerUuid().toString();
            r.balanceCents = a.getBalanceCents();
            snap.accounts.add(r);
        }
        // English: Include a copy of pending POS charges to persist across restarts.
        // Español: Incluir una copia de los cargos POS pendientes para persistir entre reinicios.
        snap.posPending = new java.util.HashMap<>(PrimeBankState.get().getAllPendingCharges());
        // English: Include company display names.
        // Español: Incluir nombres visibles de empresas.
        snap.companyNames = new java.util.HashMap<>(PrimeBankState.get().getAllCompanyNames());
        // English: Persist global cashback bps.
        // Español: Persistir cashback global en bps.
        snap.globalCashbackBps = PrimeBankState.get().getGlobalCashbackBps();
        JsonUtil.write(file, snap);
        PrimeBankMod.LOGGER.info("[PrimeBank] Snapshot saved: {} accounts / cuentas", snap.accounts.size());
    }

    /*
     English: Snapshot models used for JSON persistence.
     Español: Modelos de snapshot usados para persistencia JSON.
    */
    public static class AccountsSnapshot {
        @SerializedName("accounts")
        public List<AccountRecord> accounts;
        @SerializedName("posPending")
        public java.util.Map<String, Long> posPending;
        @SerializedName("companyNames")
        public java.util.Map<String, String> companyNames;
        @SerializedName("globalCashbackBps")
        public Integer globalCashbackBps;
    }

    public static class AccountRecord {
        @SerializedName("id") public String id;
        @SerializedName("type") public String type;
        @SerializedName("ownerUuid") public String ownerUuid;
        @SerializedName("balanceCents") public long balanceCents;
    }
}
