package com.primebank.core.company;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
 English: In-memory registry of companies; loaded/saved via CompanyPersistence.
 Español: Registro en memoria de empresas; cargado/guardado mediante CompanyPersistence.
*/
public final class CompanyRegistry {
    private final Map<String, Company> companies = new ConcurrentHashMap<>();

    public Company get(String id) { return companies.get(id); }

    public Collection<Company> all() { return Collections.unmodifiableCollection(companies.values()); }

    public Company ensureDefault(UUID owner) {
        String id = "c:" + owner.toString();
        return companies.computeIfAbsent(id, k -> {
            Company c = new Company();
            c.id = id;
            c.ownerUuid = owner;
            c.name = null;
            c.description = null;
            c.approved = false;
            c.appliedAt = System.currentTimeMillis();
            return c;
        });
    }

    public void put(Company c) {
        companies.put(c.id, c);
    }

    public boolean isApproved(String companyId) {
        Company c = companies.get(companyId);
        return c != null && c.approved;
    }

    public void addSales(String companyId, long cents) {
        Company c = companies.get(companyId);
        if (c != null && cents > 0) {
            c.salesWeekCents = Math.addExact(c.salesWeekCents, cents);
        }
    }
}
