package com.primebank.persistence;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
 Now uses atomic file moves for crash safety and read/write locks for consistency.
 Español: Servicio de persistencia para PrimeBank. Carga/guarda instantáneas JSON de forma asíncrona.
 Ahora usa movimientos de archivo atómicos para seguridad ante fallos y bloqueos de lectura/escritura para consistencia.
*/
public class BankPersistence {
    private static final ExecutorService EXEC = Executors
            .newSingleThreadExecutor(r -> new Thread(r, "PrimeBank-Persistence"));

    // English: Global lock to ensure we don't save while the state is being
    // modified, if needed.
    // For now, we rely on the fact that we copy data quickly. But to be safe, we
    // could lock.
    // However, PrimeBankState doesn't expose a global lock. We will synchronize the
    // snapshot creation.
    // Español: Bloqueo global para asegurar que no guardamos mientras se modifica
    // el estado, si es necesario.

    private BankPersistence() {
    }

    /*
     * English: Load accounts from accounts.json if present.
     * Español: Cargar cuentas desde accounts.json si existe.
     */
    public static void loadAll() {
        File file = PersistencePaths.accountsFile();
        if (!file.exists())
            return;

        AccountsSnapshot snap = null;
        try {
            snap = JsonUtil.read(file, AccountsSnapshot.class);
        } catch (Exception e) {
             PrimeBankMod.LOGGER.error("[PrimeBank] CRITICAL: Failed to load accounts.json. File may be corrupted. / CRÍTICO: Falló la carga de accounts.json. El archivo puede estar corrupto.", e);
             // English: Backup corrupted file to prevent data loss on overwrite.
             // Español: Respaldar archivo corrupto para prevenir pérdida de datos al sobrescribir.
             File backup = new File(file.getParentFile(), file.getName() + ".corrupted." + System.currentTimeMillis());
             try {
                 Files.copy(file.toPath(), backup.toPath());
                 PrimeBankMod.LOGGER.warn("[PrimeBank] Corrupted file backed up to: " + backup.getName());
             } catch (Exception ex) {
                 PrimeBankMod.LOGGER.error("[PrimeBank] Failed to backup corrupted file!", ex);
             }
             return;
        }

        if (snap == null || snap.accounts == null)
            return;

        AccountRegistry reg = PrimeBankState.get().accounts();
        for (AccountRecord r : snap.accounts) {
            AccountType type = AccountType.valueOf(r.type);
            Account acc = reg.create(r.id, type, r.ownerUuid == null ? null : java.util.UUID.fromString(r.ownerUuid),
                    r.balanceCents);
            acc.setHistory(r.history);
        }
        PrimeBankMod.LOGGER.info("[PrimeBank] Loaded accounts: {}", snap.accounts.size());

        if (snap.posPending != null) {
            PrimeBankState.get().loadPendingCharges(snap.posPending);
        }
        if (snap.companyNames != null) {
            PrimeBankState.get().loadCompanyNames(snap.companyNames);
        }
        if (snap.companyShortNames != null) {
            PrimeBankState.get().loadCompanyShortNames(snap.companyShortNames);
        }
        if (snap.globalCashbackBps != null) {
            PrimeBankState.get().setGlobalCashbackBps(snap.globalCashbackBps.intValue());
        }
    }

    /*
     * English: Enqueue an asynchronous snapshot save
     * Español: Encolar un guardado asíncrono de instantáneas
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
     * English: Save snapshot synchronously (blocking) using atomic write.
     * Español: Guardar snapshot de forma síncrona (bloqueante) usando escritura
     * atómica.
     */
    public static synchronized void saveAllBlocking() {
        File file = PersistencePaths.accountsFile();
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");

        AccountsSnapshot snap = new AccountsSnapshot();
        snap.accounts = new ArrayList<>();

        // English: Capture state. Ideally we should lock accounts, but for a snapshot,
        // iterating is usually "good enough"
        // for a game mod unless we want strict consistency. Given the requirements,
        // we'll just iterate.
        // Español: Capturar estado. Idealmente deberíamos bloquear cuentas, pero para
        // un snapshot, iterar es usualmente "suficiente"
        // para un mod de juego a menos que queramos consistencia estricta. Dados los
        // requisitos, solo iteraremos.
        for (Account a : PrimeBankState.get().accounts().all()) {
            AccountRecord r = new AccountRecord();
            // English: Acquire lock to prevent torn reads (inconsistent state).
            // Español: Adquirir bloqueo para evitar lecturas inconsistentes.
            java.util.concurrent.locks.ReentrantLock lock = com.primebank.core.locks.AccountLockManager
                    .getLock(a.getId());
            lock.lock();
            try {
                r.id = a.getId();
                r.type = a.getType().name();
                r.ownerUuid = a.getOwnerUuid() == null ? null : a.getOwnerUuid().toString();
                r.balanceCents = a.getBalanceCents();
                r.history = new ArrayList<>(a.getHistory());
            } finally {
                lock.unlock();
            }
            snap.accounts.add(r);
        }

        snap.posPending = new java.util.HashMap<>(PrimeBankState.get().getAllPendingCharges());
        snap.companyNames = new java.util.HashMap<>(PrimeBankState.get().getAllCompanyNames());
        snap.companyShortNames = new java.util.HashMap<>(PrimeBankState.get().getAllCompanyShortNames());
        snap.globalCashbackBps = PrimeBankState.get().getGlobalCashbackBps();

        // English: Write atomically using helper.
        // Español: Escribir atómicamente usando ayudante.
        try {
            JsonUtil.writeAtomic(file, snap);
            PrimeBankMod.LOGGER.info("[PrimeBank] Snapshot saved atomically: {} accounts", snap.accounts.size());
        } catch (Exception e) {
            PrimeBankMod.LOGGER.error(
                    "[PrimeBank] Failed to save snapshot atomically / Fallo al guardar snapshot atómicamente", e);
        }
    }

    public static class AccountsSnapshot {
        @SerializedName("accounts")
        public List<AccountRecord> accounts;
        @SerializedName("posPending")
        public java.util.Map<String, Long> posPending;
        @SerializedName("companyNames")
        public java.util.Map<String, String> companyNames;
        @SerializedName("companyShortNames")
        public java.util.Map<String, String> companyShortNames;
        @SerializedName("globalCashbackBps")
        public Integer globalCashbackBps;
    }

    public static class AccountRecord {
        @SerializedName("id")
        public String id;
        @SerializedName("type")
        public String type;
        @SerializedName("ownerUuid")
        public String ownerUuid;
        @SerializedName("balanceCents")
        public long balanceCents;
        @SerializedName("history")
        public List<Account.TransactionRecord> history;
    }
}
