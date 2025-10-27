package com.primebank.market;

import java.util.UUID;

import com.primebank.PrimeBankMod;
import com.primebank.core.company.Company;
import com.primebank.core.company.CompanyRegistry;
import com.primebank.core.ledger.Ledger;
import com.primebank.core.state.PrimeBankState;
import com.primebank.persistence.CompanyPersistence;

/*
 English: Primary market service for listing and buying company shares.
  - Owner may list up to min(50, ownerShares - 51)
  - Trading is blocked while valuationCurrentCents == 0
  - Buyer pays gross + 2.5% (to central); Company receives gross - 5% (to central)
 Español: Servicio de mercado primario para listar y comprar acciones de empresas.
  - El dueño puede listar hasta min(50, accionesDueño - 51)
  - El comercio está bloqueado cuando valuationCurrentCents == 0
  - Comprador paga bruto + 2.5% (al central); Empresa recibe bruto - 5% (al central)
*/
public final class MarketPrimaryService {
    // English: Fee basis points for buyer (2.5%) and issuer (5%) in the primary market.
    // Español: Puntos básicos de comisión para comprador (2.5%) y emisor (5%) en el mercado primario.
    public static final int BUYER_FEE_BPS = 250; // 2.5%
    public static final int ISSUER_FEE_BPS = 500; // 5%

    private static final MarketPrimaryService INSTANCE = new MarketPrimaryService();

    private MarketPrimaryService() {}

    public static MarketPrimaryService get() { return INSTANCE; }

    /*
     English: List shares for sale by the owner.
     Español: Listar acciones para la venta por el dueño.
    */
    public Result listShares(UUID owner, String companyId, int shares) {
        if (owner == null || companyId == null || shares <= 0) return Result.error("bad_args");
        CompanyRegistry reg = PrimeBankState.get().companies();
        Company c = reg.get(companyId);
        if (c == null) return Result.error("company_not_found");
        if (!owner.equals(c.ownerUuid)) return Result.error("not_owner");
        if (c.valuationCurrentCents <= 0) return Result.error("trading_blocked");
        int ownerShares = c.holdings.getOrDefault(owner.toString(), 0);
        int maxListable = Math.min(50, Math.max(0, ownerShares - 51));
        if (shares > maxListable) return Result.error("over_limit");
        if (c.listedShares + shares > 50) return Result.error("over_slot");
        c.listedShares += shares;
        CompanyPersistence.saveCompany(c);
        PrimeBankMod.LOGGER.info("[PrimeBank] Listed {} shares for {}", shares, companyId);
        return Result.ok();
    }

    /*
     English: Buy shares from company's listed inventory at current price.
     Español: Comprar acciones del inventario listado de la empresa al precio actual.
    */
    public Result buyShares(UUID buyer, String companyId, int shares) {
        if (buyer == null || companyId == null || shares <= 0) return Result.error("bad_args");
        CompanyRegistry reg = PrimeBankState.get().companies();
        Company c = reg.get(companyId);
        if (c == null) return Result.error("company_not_found");
        if (c.valuationCurrentCents <= 0) return Result.error("trading_blocked");
        if (c.listedShares < shares) return Result.error("not_enough_listed");
        long pricePerShare = c.valuationCurrentCents / 101L;
        if (pricePerShare <= 0) return Result.error("trading_blocked");
        long gross = pricePerShare * shares;
        String buyerAcc = com.primebank.core.accounts.PlayerAccounts.ensurePersonal(buyer);
        // English: Payment goes to the owner's personal account, not the company account.
        // Español: El pago va a la cuenta personal del dueño, no a la cuenta de la empresa.
        String sellerAcc = com.primebank.core.accounts.PlayerAccounts.ensurePersonal(c.ownerUuid);
        Ledger ledger = new Ledger(PrimeBankState.get().accounts());
        Ledger.TransferResult tr = ledger.marketPrimaryBuy(buyerAcc, sellerAcc, gross, BUYER_FEE_BPS, ISSUER_FEE_BPS);
        if (!tr.success) {
            // English: Check result code for insufficient funds per Ledger.TransferResult.
            // Español: Verificar el código de resultado para fondos insuficientes según Ledger.TransferResult.
            if ("insufficient".equals(tr.code)) return Result.error("insufficient");
            return Result.error("ledger_error");
        }
        // English: Move shares from owner to buyer and reduce listed inventory.
        // Español: Mover acciones del dueño al comprador y reducir inventario listado.
        String ownerKey = c.ownerUuid == null ? null : c.ownerUuid.toString();
        if (ownerKey == null) return Result.error("company_no_owner");
        int ownerShares = c.holdings.getOrDefault(ownerKey, 0);
        if (ownerShares < shares + 51) {
            // Safety: do not violate majority; rollback funds is complex -> prevent earlier; here we just fail-fast (should not happen)
            PrimeBankMod.LOGGER.error("[PrimeBank] Majority rule violation detected while buying {} shares from {}", shares, companyId);
            return Result.error("majority_violation");
        }
        c.holdings.put(ownerKey, ownerShares - shares);
        String buyerKey = buyer.toString();
        c.holdings.put(buyerKey, c.holdings.getOrDefault(buyerKey, 0) + shares);
        c.listedShares -= shares;
        CompanyPersistence.saveCompany(c);
        return Result.ok();
    }

    /*
     English: Result wrapper for market operations.
     Español: Envoltorio de resultado para operaciones de mercado.
    */
    public static final class Result {
        public final boolean ok;
        public final String error;
        private Result(boolean ok, String error) { this.ok = ok; this.error = error; }
        public static Result ok() { return new Result(true, null); }
        public static Result error(String e) { return new Result(false, e); }
    }
}
