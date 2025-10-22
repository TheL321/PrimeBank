package com.primebank.persistence;

import java.io.File;

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
                    PrimeBankState.get().companies().put(c);
                    // English: If there's a stored company name and no display mapping yet, set it so UIs (POS) show it.
                    // Español: Si hay nombre almacenado y no hay mapeo visible aún, establecerlo para que las UIs (POS) lo muestren.
                    if (c.name != null && !c.name.trim().isEmpty()) {
                        String existing = PrimeBankState.get().getCompanyName(c.id);
                        if (existing == null || existing.trim().isEmpty()) {
                            PrimeBankState.get().setCompanyName(c.id, c.name.trim());
                        }
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
