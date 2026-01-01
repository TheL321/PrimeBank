package com.primebank.market;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.primebank.PrimeBankMod;
import com.primebank.core.company.Company;
import com.primebank.core.company.CompanyRegistry;
import com.primebank.core.state.PrimeBankState;
import com.primebank.persistence.CompanyPersistence;

/*
 English: Daily valuation engine using real-time windows.
  - Day 0 = company.approvedAt
  - First valuation at end of real day 1 (24 hours)
  - Thereafter every 1 real day (24 hours)
  - Formula: V1 = 6 * sales7Days; Vn = (6 * sales7Days + 2 * V(n-1)) / 3
  - sales7Days = sum of the last 7 daily sales totals (rolling window)
  - Price = floor(V / 101); trading disabled while V == 0
 Español: Motor de valoración diaria usando ventanas de tiempo real.
  - Día 0 = company.approvedAt
  - Primera valoración al final del día 1 (24 horas)
  - Después, cada 1 día (24 horas)
  - Fórmula: V1 = 6 * ventas7Días; Vn = (6 * ventas7Días + 2 * V(n-1)) / 3
  - ventas7Días = suma de los últimos 7 totales de ventas diarias (ventana rodante)
  - Precio = floor(V / 101); trading deshabilitado mientras V == 0
*/
public final class ValuationService {
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private static final ValuationService INSTANCE = new ValuationService();
    private ScheduledExecutorService scheduler;

    private ValuationService() {
    }

    public static ValuationService get() {
        return INSTANCE;
    }

    /*
     * English: Start periodic valuation checks. Runs every 5 minutes.
     * Español: Iniciar verificaciones periódicas de valoración. Corre cada 5
     * minutos.
     */
    public synchronized void start() {
        if (scheduler != null)
            return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PrimeBank-Valuation");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::runOnceSafe, 1, 5, TimeUnit.MINUTES);
        PrimeBankMod.LOGGER.info("[PrimeBank] ValuationService started");
    }

    /*
     * English: Stop scheduler gracefully.
     * Español: Detener el scheduler de forma limpia.
     */
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            PrimeBankMod.LOGGER.info("[PrimeBank] ValuationService stopped");
        }
    }

    private void runOnceSafe() {
        try {
            runOnce();
        } catch (Exception t) {
            PrimeBankMod.LOGGER.error("[PrimeBank] Valuation run failed", t);
        }
    }

    /*
     * English: Iterate companies and compute valuations if due.
     * Español: Iterar empresas y calcular valoraciones si corresponde.
     */
    private void runOnce() {
        CompanyRegistry reg = PrimeBankState.get().companies();
        List<Company> updated = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Company c : reg.all()) {
            if (c == null || !c.approved)
                continue;
            if (c.approvedAt <= 0)
                continue;
            if (c.salesLast7DaysCents == null) {
                // English: Initialize rolling window to avoid null checks later. Español:
                // Inicializar ventana rodante para evitar nulls.
                c.salesLast7DaysCents = new java.util.ArrayList<>();
            }
            long lastValuation = c.lastValuationAt;
            long dueAt = (lastValuation > 0) ? lastValuation + DAY_MS : c.approvedAt + DAY_MS;
            if (now < dueAt)
                continue;

            long previousValuation = c.valuationCurrentCents;
            long sales = c.salesWeekCents;
            boolean changed = false;

            // English: Safeguard to prevent runaway catch-up loops (max 365 days = 1 year).
            // Español: Protección para prevenir bucles catch-up descontrolados (máx 365
            // días = 1 año).
            int maxCatchupDays = 365;
            int catchupCount = 0;

            while (now >= dueAt && catchupCount < maxCatchupDays) {
                catchupCount++;
                long dailySales = Math.max(0L, sales);
                c.salesLast7DaysCents.add(dailySales);
                if (c.salesLast7DaysCents.size() > 7) {
                    int excessDays = c.salesLast7DaysCents.size() - 7;
                    for (int i = 0; i < excessDays; i++)
                        c.salesLast7DaysCents.remove(0);
                }
                long windowSales = 0L;
                for (Long daySales : c.salesLast7DaysCents) {
                    if (daySales != null && daySales.longValue() > 0L) {
                        windowSales = Math.addExact(windowSales, Math.max(0L, daySales.longValue()));
                    }
                }
                long valuation;
                if (lastValuation <= 0) {
                    valuation = Math.max(0L, Math.multiplyExact(windowSales, 6L));
                } else {
                    long term = Math.addExact(Math.multiplyExact(windowSales, 6L),
                            Math.multiplyExact(previousValuation, 2L));
                    long computed = term / 3L;
                    valuation = Math.max(0L, computed);
                }

                if (c.valuationHistoryCents == null)
                    c.valuationHistoryCents = new java.util.ArrayList<>();
                c.valuationHistoryCents.add(valuation);
                if (c.valuationHistoryCents.size() > 26) {
                    int excess = c.valuationHistoryCents.size() - 26;
                    for (int i = 0; i < excess; i++)
                        c.valuationHistoryCents.remove(0);
                }

                previousValuation = valuation;
                lastValuation = dueAt;
                dueAt = lastValuation + DAY_MS;
                sales = 0L; // English: After first catch-up, remaining days assume no recorded sales.
                            // Español: Tras el primer catch-up, se asume cero ventas en días restantes.
                changed = true;
            }

            if (changed) {
                c.valuationCurrentCents = previousValuation;
                c.lastValuationAt = lastValuation;
                c.salesWeekCents = sales;
                updated.add(c);
                // English: Emit valuation change to Discord valuation webhook (and local audit
                // log).
                // Español: Emitir cambio de valoración al webhook de valoraciones (y log local
                // de auditoría).
                long pricePerShare = previousValuation / 101L;
                // English: Get previous valuation from history (second-to-last value, or 0 if
                // first valuation)
                // Español: Obtener valoración anterior del historial (penúltimo valor, o 0 si
                // es la primera valoración)
                long prevValuation = 0L;
                if (c.valuationHistoryCents != null && c.valuationHistoryCents.size() >= 2) {
                    prevValuation = c.valuationHistoryCents.get(c.valuationHistoryCents.size() - 2);
                }
                long prevPrice = prevValuation / 101L;

                com.primebank.core.logging.TransactionLogger.logValuation(
                        c.shortName != null ? c.shortName : c.id,
                        previousValuation,
                        pricePerShare,
                        prevValuation,
                        prevPrice,
                        c.listedShares,
                        lastValuation);
            }
        }
        for (Company c : updated) {
            CompanyPersistence.saveCompany(c);
        }
        if (!updated.isEmpty()) {
            PrimeBankMod.LOGGER.info("[PrimeBank] Valuations updated for {} companies", updated.size());
        }
    }
}
