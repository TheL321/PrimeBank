package com.primebank.persistence;

import java.io.File;
import java.util.Locale;

import com.primebank.PrimeBankMod;
import com.primebank.core.company.Company;
import com.primebank.core.state.PrimeBankState;

/*
 English: Persistence for Company objects under world/primebank/companies/.
 Español: Persistencia para objetos Company bajo world/primebank/companies/.
*/
public final class CompanyPersistence {
    private CompanyPersistence() {}

    private static File dir() { return new File(PersistencePaths.base(), "companies"); }

    public static void loadAll() {
        try {
            File d = dir();
            if (!d.exists()) return;
            File[] files = d.listFiles((f, n) -> n.endsWith(".json"));
            if (files == null) return;
            int n = 0;
            for (File f : files) {
                Company c = JsonUtil.read(f, Company.class);
                if (c != null && c.id != null) {
                    if (c.valuationHistoryCents == null) {
                        c.valuationHistoryCents = new java.util.ArrayList<>();
                    } else if (c.valuationHistoryCents.size() > 26) {
                        /*
                         English: Trim valuation history to last 26 entries to honor UI graph cap.
                         Español: Recortar el historial de valoraciones a las últimas 26 entradas para respetar el límite de la UI.
                        */
                        int excess = c.valuationHistoryCents.size() - 26;
                        for (int i = 0; i < excess; i++) {
                            c.valuationHistoryCents.remove(0);
                        }
                    }
                    if (c.shortName != null) {
                        c.shortName = c.shortName.replaceAll("[^A-Za-z0-9]", "");
                        c.shortName = c.shortName.trim().toUpperCase(Locale.ROOT);
                        if (c.shortName.isEmpty()) {
                            c.shortName = null;
                        }
                    }
                    // English: Normalize possible old/edited JSON to avoid wiping valuations.
                    // Español: Normalizar posibles JSON antiguos/editados para evitar borrar valoraciones.
                    for (int i = 0; i < c.valuationHistoryCents.size(); i++) {
                        Long v = c.valuationHistoryCents.get(i);
                        if (v == null || v.longValue() < 0L) c.valuationHistoryCents.set(i, 0L);
                    }
                    if (!c.valuationHistoryCents.isEmpty()) {
                        long lastVal = c.valuationHistoryCents.get(c.valuationHistoryCents.size() - 1);
                        if (c.valuationCurrentCents <= 0L && lastVal > 0L) {
                            // English: Restore current valuation from last history point if missing.
                            // Español: Restaurar valoración actual desde el último punto del historial si falta.
                            c.valuationCurrentCents = lastVal;
                        }
                        if (c.lastValuationAt <= 0L && c.approvedAt > 0L) {
                            // English: Approximate last valuation timestamp from approvedAt and history length.
                            // Español: Aproximar la marca de tiempo de la última valoración desde approvedAt y longitud del historial.
                            long DAY_MS = 24L * 60L * 60L * 1000L;
                            int histCount = c.valuationHistoryCents.size();
                            c.lastValuationAt = c.approvedAt + 8L * DAY_MS + (long) Math.max(0, histCount - 1) * 7L * DAY_MS;
                        }
                    }
                    PrimeBankState.get().companies().put(c);
                    // English: If there's a stored company name and no display mapping yet, set it so UIs (POS) show it.
                    // Español: Si hay nombre almacenado y no hay mapeo visible aún, establecerlo para que las UIs (POS) lo muestren.
                    if (c.name != null && !c.name.trim().isEmpty()) {
                        String existing = PrimeBankState.get().getCompanyName(c.id);
                        if (existing == null || existing.trim().isEmpty()) {
                            PrimeBankState.get().setCompanyName(c.id, c.name.trim());
                        }
                    }
                    if (c.shortName != null && !c.shortName.isEmpty()) {
                        PrimeBankState.get().setCompanyShortName(c.id, c.shortName);
                    }
                    n++;
                }
            }
            PrimeBankMod.LOGGER.info("[PrimeBank] Loaded companies: {}", n);
        } catch (Exception ex) {
            PrimeBankMod.LOGGER.error("[PrimeBank] Failed to load companies", ex);
        }
    }

    public static void saveCompany(Company c) {
        try {
            File f = new File(dir(), sanitize(c.id) + ".json");
            JsonUtil.write(f, c);
        } catch (Exception ex) {
            PrimeBankMod.LOGGER.error("[PrimeBank] Failed to save company {}", c.id, ex);
        }
    }

    public static void saveAll() {
        for (Company c : PrimeBankState.get().companies().all()) {
            saveCompany(c);
        }
    }

    private static String sanitize(String id) {
        return id.replace(':', '_');
    }
}
