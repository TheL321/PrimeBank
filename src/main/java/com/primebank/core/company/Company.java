package com.primebank.core.company;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.annotations.SerializedName;

/*
 English: Company model persisted per file in world/primebank/companies/.
 Español: Modelo de empresa persistido por archivo en world/primebank/companies/.
*/
public class Company {
    public String id; // e.g., "c:<ownerUuid>"
    public UUID ownerUuid;
    public String name;
    // English: Stock-style short name (ticker) displayed in UIs.
    // Español: Nombre corto tipo ticker mostrado en las UIs.
    public String shortName;
    public String description;
    public boolean approved;
    public long appliedAt;    // ms epoch when applied
    public long approvedAt;   // ms epoch when approved (Day 0)

    // Phase 3 fields
    public long salesWeekCents;        // accumulator for current period sales
    public long valuationCurrentCents; // V
    public long lastValuationAt;       // ms epoch for last valuation
    @SerializedName(value = "valuationHistoryCents", alternate = {"valuationHistory"})
    public List<Long> valuationHistoryCents = new ArrayList<>(); // last 26 values

    // English: Basic share model — track per-holder shares. Owner gets 101 on approval.
    // Español: Modelo básico de acciones — rastrear acciones por tenedor. El dueño recibe 101 al aprobar.
    public java.util.Map<String, Integer> holdings = new java.util.HashMap<>(); // key: holder UUID string
    // English: Primary market listed shares pending sale (at current price).
    // Español: Acciones listadas en mercado primario pendientes de venta (al precio actual).
    public int listedShares = 0;

    public Company() {}
}
