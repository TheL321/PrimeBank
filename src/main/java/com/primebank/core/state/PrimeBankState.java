package com.primebank.core.state;

import java.util.Locale;
import java.util.UUID;

import com.primebank.PrimeBankMod;
import com.primebank.core.accounts.Account;
import com.primebank.core.accounts.AccountRegistry;
import com.primebank.core.accounts.AccountType;
import com.primebank.core.company.Company;

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
    private final java.util.concurrent.ConcurrentHashMap<String, String> companyShortNames = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, String> companyShortToId = new java.util.concurrent.ConcurrentHashMap<>();
    private final com.primebank.core.company.CompanyRegistry companies = new com.primebank.core.company.CompanyRegistry();
    private volatile int globalCashbackBps = 0;

    /*
     * English: Server-side tracking of pending POS charges per player to prevent
     * client manipulation.
     * Español: Seguimiento del lado del servidor de cargos POS pendientes por
     * jugador para prevenir manipulación del cliente.
     */
    private final java.util.concurrent.ConcurrentHashMap<UUID, PendingPosCharge> playerPosPending = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long POS_CHARGE_TIMEOUT_MS = 5L * 60 * 1000; // 5 minutes

    private PrimeBankState() {
    }

    public static PrimeBankState get() {
        return INSTANCE;
    }

    /*
     * English: Initialize global state and ensure the central bank account exists.
     * Español: Inicializa el estado global y asegura la existencia de la cuenta del
     * banco central.
     */
    public void init() {
        ensureCentralAccount();
    }

    public AccountRegistry accounts() {
        return accounts;
    }

    /*
     * English: Access the in-memory registry of companies.
     * Español: Acceder al registro en memoria de empresas.
     */
    public com.primebank.core.company.CompanyRegistry companies() {
        return companies;
    }

    /*
     * English: Global cashback (bps) applied to POS purchases and credited to buyer
     * from central.
     * Español: Cashback global (bps) aplicado a compras POS y acreditado al
     * comprador desde el central.
     */
    public int getGlobalCashbackBps() {
        return globalCashbackBps;
    }

    public void setGlobalCashbackBps(int bps) {
        this.globalCashbackBps = Math.max(0, bps);
    }

    /*
     * English: Set/get/clear pending POS charges by company id (cents).
     * Español: Establecer/obtener/limpiar cargos POS pendientes por id de empresa
     * (centavos).
     */
    public void setPendingCharge(String companyId, long cents) {
        if (companyId == null)
            return;
        if (cents <= 0) {
            posPending.remove(companyId);
        } else {
            posPending.put(companyId, cents);
        }
    }

    public long getPendingCharge(String companyId) {
        Long v = posPending.get(companyId);
        return v == null ? 0L : v.longValue();
    }

    public void clearPendingCharge(String companyId) {
        if (companyId != null)
            posPending.remove(companyId);
    }

    /*
     * English: Return a snapshot view of all pending charges.
     * Español: Devolver una vista instantánea de todos los cargos pendientes.
     */
    public java.util.Map<String, Long> getAllPendingCharges() {
        return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(posPending));
    }

    /*
     * English: Replace the pending charges map from persistence on server load.
     * Español: Reemplazar el mapa de cargos pendientes desde la persistencia al
     * cargar el servidor.
     */
    public void loadPendingCharges(java.util.Map<String, Long> data) {
        posPending.clear();
        if (data != null)
            posPending.putAll(data);
    }

    /*
     * English: Set/get company display names, and snapshot/loader for persistence.
     * Español: Establecer/obtener nombres visibles de empresas, y snapshot/cargador
     * para persistencia.
     */
    public boolean setCompanyName(String companyId, String name) {
        if (companyId == null)
            return false;
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty())
            return false; // English: Do not allow clearing names; keep existing. / Español: No permitir limpiar nombres; mantener el existente.
        // English: Reject duplicate display names across companies.
        // Español: Rechazar nombres visibles duplicados entre empresas.
        for (java.util.Map.Entry<String, String> entry : companyNames.entrySet()) {
            if (!entry.getKey().equals(companyId) && trimmed.equalsIgnoreCase(entry.getValue())) {
                return false;
            }
        }
        companyNames.put(companyId, trimmed);
        return true;
    }

    public String getCompanyName(String companyId) {
        return companyId == null ? null : companyNames.get(companyId);
    }

    public java.util.Map<String, String> getAllCompanyNames() {
        return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(companyNames));
    }

    public void loadCompanyNames(java.util.Map<String, String> names) {
        companyNames.clear();
        if (names != null)
            companyNames.putAll(names);
    }

    /*
     * English: Set/get company short tickers, and snapshot/loader counterparts.
     * Español: Establecer/obtener tickers cortos de empresa y sus contrapartes para
     * snapshots.
     */
    public synchronized boolean setCompanyShortName(String companyId, String shortName) {
        if (companyId == null)
            return false;
        String sanitized = shortName == null ? "" : shortName.trim();
        // English: Keep ticker to a single alphanumeric word and uppercase it.
        // Español: Mantener el ticker como una sola palabra alfanumérica y en mayúsculas.
        sanitized = sanitized.replaceAll("[^A-Za-z0-9]", "");
        if (sanitized.isEmpty()) {
            return false; // English: Do not allow clearing tickers; ignore empty values. / Español: No permitir limpiar tickers; ignorar valores vacíos.
        }
        sanitized = sanitized.toUpperCase(Locale.ROOT);
        // English: Reject duplicate tickers across companies.
        // Español: Rechazar tickers duplicados entre empresas.
        String existingOwner = companyShortToId.get(sanitized);
        if (existingOwner != null && !existingOwner.equals(companyId)) {
            return false;
        }

        String previous = companyShortNames.get(companyId);
        if (previous != null) {
            String owner = companyShortToId.get(previous);
            if (owner != null && owner.equals(companyId)) {
                companyShortToId.remove(previous);
            }
        }

        String other = companyShortToId.put(sanitized, companyId);
        if (other != null && !other.equals(companyId)) {
            companyShortNames.remove(other);
            Company conflict = companies().get(other);
            if (conflict != null) {
                conflict.shortName = null;
            }
        }
        companyShortNames.put(companyId, sanitized);
        Company self = companies().get(companyId);
        if (self != null) {
            self.shortName = sanitized;
        }
        return true;
    }

    public String getCompanyShortName(String companyId) {
        return companyId == null ? null : companyShortNames.get(companyId);
    }

    public java.util.Map<String, String> getAllCompanyShortNames() {
        return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(companyShortNames));
    }

    public void loadCompanyShortNames(java.util.Map<String, String> names) {
        companyShortNames.clear();
        companyShortToId.clear();
        if (names != null) {
            for (java.util.Map.Entry<String, String> entry : names.entrySet()) {
                setCompanyShortName(entry.getKey(), entry.getValue());
            }
        }
    }

    /*
     * English: Reset all in-memory state when switching worlds to avoid cross-world
     * leakage.
     * Español: Reiniciar todo el estado en memoria al cambiar de mundo para evitar
     * fugas entre mundos.
     */
    public void resetForNewWorld() {
        // Clear registries/maps
        accounts.clear();
        posPending.clear();
        companyNames.clear();
        companyShortNames.clear();
        companyShortToId.clear();
        companies.clear();
        globalCashbackBps = 0;
        playerPosPending.clear();
    }

    public Account ensureCentralAccount() {
        if (!accounts.exists(CENTRAL_ACCOUNT_ID)) {
            PrimeBankMod.LOGGER.info("[PrimeBank] Creating central bank account / Creando cuenta del banco central");
            accounts.create(CENTRAL_ACCOUNT_ID, AccountType.CENTRAL, (UUID) null, 0L);
        }
        return accounts.get(CENTRAL_ACCOUNT_ID);
    }

    public String findCompanyIdByTicker(String shortName) {
        if (shortName == null)
            return null;
        String key = shortName.trim().toUpperCase(Locale.ROOT);
        if (key.isEmpty())
            return null;
        return companyShortToId.get(key);
    }

    public String resolveCompanyIdentifier(String identifier) {
        if (identifier == null)
            return null;
        String trimmed = identifier.trim();
        if (trimmed.isEmpty())
            return null;
        if (companies.get(trimmed) != null)
            return trimmed;
        String resolved = findCompanyIdByTicker(trimmed);
        if (resolved != null)
            return resolved;
        return null;
    }

    public String getCompanyDisplay(String companyId) {
        // English: Prefer display name (optionally annotated with ticker), else ticker,
        // else raw id.
        // Español: Preferir nombre visible (opcionalmente anotado con ticker), sino
        // ticker, sino id crudo.
        if (companyId == null || companyId.isEmpty())
            return companyId;
        String name = getCompanyName(companyId);
        String ticker = getCompanyShortName(companyId);
        if (name != null && !name.trim().isEmpty()) {
            if (ticker != null && !ticker.trim().isEmpty()) {
                return String.format("%s (%s)", name.trim(), ticker.trim());
            }
            return name.trim();
        }
        if (ticker != null && !ticker.trim().isEmpty())
            return ticker.trim();
        return companyId;
    }

    /*
     * English: Set a pending POS charge for a specific player. This is called when
     * the server sends a POS prompt.
     * Español: Establecer un cargo POS pendiente para un jugador específico. Se
     * llama cuando el servidor envía un aviso POS.
     */
    public void setPendingPosCharge(UUID playerUuid, String companyId, long cents) {
        if (playerUuid == null || companyId == null || cents <= 0)
            return;
        playerPosPending.put(playerUuid, new PendingPosCharge(companyId, cents, System.currentTimeMillis()));
    }

    /*
     * English: Get the pending POS charge for a player, or null if none exists or
     * it has expired.
     * Español: Obtener el cargo POS pendiente para un jugador, o null si no existe
     * o ha expirado.
     */
    public PendingPosCharge getPendingPosCharge(UUID playerUuid) {
        if (playerUuid == null)
            return null;
        PendingPosCharge charge = playerPosPending.get(playerUuid);
        if (charge == null)
            return null;
        // English: Check if expired (5 minute timeout)
        // Español: Verificar si expiró (timeout de 5 minutos)
        if (System.currentTimeMillis() - charge.timestamp > POS_CHARGE_TIMEOUT_MS) {
            playerPosPending.remove(playerUuid);
            return null;
        }
        return charge;
    }

    /*
     * English: Clear the pending POS charge for a player.
     * Español: Limpiar el cargo POS pendiente para un jugador.
     */
    public void clearPendingPosCharge(UUID playerUuid) {
        if (playerUuid != null)
            playerPosPending.remove(playerUuid);
    }

    /*
     * English: Inner class to hold pending POS charge data.
     * Español: Clase interna para mantener datos de cargo POS pendiente.
     */
    public static final class PendingPosCharge {
        public final String companyId;
        public final long cents;
        public final long timestamp;

        public PendingPosCharge(String companyId, long cents, long timestamp) {
            this.companyId = companyId;
            this.cents = cents;
            this.timestamp = timestamp;
        }
    }
}
