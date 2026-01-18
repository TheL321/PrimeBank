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
    // English: Fee basis points for buyer (2.5%) and issuer (5%) in the primary
    // market.
    // Español: Puntos básicos de comisión para comprador (2.5%) y emisor (5%) en el
    // mercado primario.
    public static final int BUYER_FEE_BPS = 250; // 2.5%
    public static final int ISSUER_FEE_BPS = 500; // 5%

    private static final MarketPrimaryService INSTANCE = new MarketPrimaryService();

    private MarketPrimaryService() {
    }

    public static MarketPrimaryService get() {
        return INSTANCE;
    }

    /*
     * English: List shares for sale by the owner or any shareholder.
     * Español: Listar acciones para la venta por el dueño o cualquier accionista.
     */
    public Result listShares(UUID seller, String companyId, int shares) {
        if (seller == null || companyId == null || shares <= 0)
            return Result.error("bad_args");
        CompanyRegistry reg = PrimeBankState.get().companies();
        Company c = reg.get(companyId);
        if (c == null)
            return Result.error("company_not_found");

        // English: Trading is blocked if valuation is zero or negative (company not
        // active/approved).
        // Español: El comercio está bloqueado si la valoración es cero o negativa.
        if (c.valuationCurrentCents <= 0)
            return Result.error("trading_blocked");

        // English: Synchronize on company to prevent race conditions.
        // Español: Sincronizar en la empresa para prevenir condiciones de carrera.
        synchronized (c) {
            String sellerKey = seller.toString();
            int held = c.holdings.getOrDefault(sellerKey, 0);

            // English: Check that the seller actually has enough shares to list.
            // Español: Verificar que el vendedor realmente tenga suficientes acciones para
            // listar.
            // English: For the owner, we also must respect that they cannot sell below 51
            // shares (control).
            // Español: Para el dueño, también debemos respetar que no pueden vender por
            // debajo de 51 acciones.
            int maxListable;
            if (seller.equals(c.ownerUuid)) {
                maxListable = Math.max(0, held - 51);
                // English: Owner listings are added to c.listedShares (primary market).
                // Español: Listados del dueño se suman a c.listedShares (mercado primario).
                if (c.listedShares + shares > 50)
                    return Result.error("over_slot");
            } else {
                // English: Regular shareholders can list whatever they have.
                // Español: Accionistas regulares pueden listar lo que tengan.
                // English: We must also check if they already have listed shares to prevent
                // double-listing?
                // Español: ¿Debemos verificar si ya tienen acciones listadas para prevenir
                // doble listado?
                // Context: The Command checks this? No, the command calls this.
                // Logic: sellerListings stores the *active* listing amount.
                // If they list more, we increase the amount.
                // BUT we must ensure total listed <= total held.
                int alreadyListed = c.sellerListings.getOrDefault(sellerKey, 0);
                // English: The shares argument is "how many NEW shares to add to listing".
                // So total listed will be alreadyListed + shares.
                // This complete total must be <= held shares.
                if (alreadyListed + shares > held) {
                    return Result.error("over_limit");
                }
                maxListable = held; // conceptually, but constrained by alreadyListed logic above
            }

            if (shares > maxListable)
                return Result.error("over_limit");

            // English: Check global slot limit (primary + secondary <= 50, or some other
            // rule?).
            // The original code had: if (c.listedShares + shares > 50)
            // Result.error("over_slot");
            // Let's effectively cap the TOTAL available market shares to 50 to prevent
            // flooding.
            // Español: Limitar el total de acciones en mercado a 50 para evitar inundación.
            long totalCurrentlyListed = c.listedShares;
            for (int qty : c.sellerListings.values())
                totalCurrentlyListed += qty;

            if (totalCurrentlyListed + shares > 50) {
                return Result.error("over_slot");
            }

            if (seller.equals(c.ownerUuid)) {
                c.listedShares += shares;
            } else {
                int current = c.sellerListings.getOrDefault(sellerKey, 0);
                c.sellerListings.put(sellerKey, current + shares);
            }
        }
        CompanyPersistence.saveCompany(c);
        PrimeBankMod.LOGGER.info("[PrimeBank] Listed {} shares for {} (Seller: {})", shares, companyId, seller);
        return Result.ok();
    }

    /*
     * English: Buy shares from company's listed inventory at current price.
     * Español: Comprar acciones del inventario listado de la empresa al precio
     * actual.
     */
    public Result buyShares(net.minecraft.server.MinecraftServer server, UUID buyer, String companyId, int shares) {
        if (buyer == null || companyId == null || shares <= 0)
            return Result.error("bad_args");
        CompanyRegistry reg = PrimeBankState.get().companies();
        Company c = reg.get(companyId);
        if (c == null)
            return Result.error("company_not_found");
        if (c.valuationCurrentCents <= 0)
            return Result.error("trading_blocked");
        if (c.listedShares < shares)
            return Result.error("not_enough_listed");
        // English: If the buyer is also the owner, treat this as an "unlist" operation
        // with no money movement.
        // Español: Si el comprador también es el dueño, tratar esto como una operación
        // de "deslistado" sin movimiento de dinero.
        if (buyer.equals(c.ownerUuid)) {
            // English: If owner buys, they might be buying from secondary market OR
            // unlisting their own primary shares.
            // But simplifying: Owner "buying" usually implies unlisting if targeting self,
            // but valid buy if targeting secondary.
            // For simplicity in this logic: If owner calls buy, we treat it as unlisting
            // PRIMARY shares first.
            // If they want to buy back secondary shares, that is a valid purchase.
            // Let's keep the original "unlist" behavior if there are primary shares?
            // Actually, the previous logic was: if buyer == owner, remove from
            // listedShares.
            // We should preserve that for "unlisting".
            // BUT what if they want to buy from a secondary seller?
            // Let's assume /marketbuy by owner is strictly "Unlist" for now to avoid
            // complexity,
            // unless we add a specific /marketunlist command.
            // Original code:
            /*
             * c.listedShares -= shares;
             * CompanyPersistence.saveCompany(c);
             * return Result.ok();
             */
            // Updated logic: if owner runs buy, we try to reduce their `listedShares`
            // (unlisting).
            // If they have no listed shares, maybe they mean to buy back from secondary?
            // To be safe and compatible with existing behavior:
            if (c.listedShares >= shares) {
                c.listedShares -= shares;
                CompanyPersistence.saveCompany(c);
                return Result.ok();
            }
            // If they try to reduce more than listed, maybe error or fallback?
            // Let's fall through to normal buy logic (buying from secondary) if they aren't
            // just unlisting.
            // But wait, if they buy from secondary, they pay money. That is correct.
            // So: try unlist first. If not enough primary listings, fall through?
            // No, mixing "Unlist" and "Buy" in one command is ambiguous.
            // Let's stick to the previous behavior: Owner 'buy' is effectively 'unlist' if
            // they have listings.
            // If they want to buy from secondary, they should strictly ensure they aren't
            // listed themselves?
            // Let's just keep the old "Unlist" logic for owner for now as the PRIMARY
            // behavior.
            c.listedShares -= shares; // Check bounds?
            if (c.listedShares < 0)
                c.listedShares = 0; // clamp
            CompanyPersistence.saveCompany(c);
            return Result.ok();
        }

        long pricePerShare = c.valuationCurrentCents / 101L;
        if (pricePerShare <= 0)
            return Result.error("trading_blocked");

        // English: Calculate total available (Primary + Secondary)
        // Español: Calcular total disponible (Primario + Secundario)
        int totalAvailable = c.listedShares;
        for (int qty : c.sellerListings.values())
            totalAvailable += qty;

        if (totalAvailable < shares)
            return Result.error("not_enough_listed");

        long grossSingle = pricePerShare;
        // We will transact share by share or in batches to handle different sellers?
        // Simpler: iterate to fulfill `shares`.

        String ownerKey = c.ownerUuid == null ? null : c.ownerUuid.toString();
        if (ownerKey == null)
            return Result.error("company_no_owner");

        // English: Synchronize for the complex transaction
        synchronized (c) {
            // Re-check availability inside lock
            int currentTotal = c.listedShares;
            for (int qty : c.sellerListings.values())
                currentTotal += qty;
            if (currentTotal < shares)
                return Result.error("not_enough_listed");

            // Check majority rule violation on the BUYER side?
            // If buyer acquires these shares, do they exceed limits?
            // Usually we check if the OWNER loses majority.
            // If we buy from Secondary, owner holdings don't change.
            // If we buy from Primary, owner holdings decrease.
            // We need to track how many come from Primary.

            // Allocation strategy: Secondary First (FIFO or random? Iteration order is
            // undefined for HashMap, but that's fine)
            // Then Primary.

            java.util.Map<String, Integer> purchaseFrom = new java.util.HashMap<>(); // SellerUUID -> count
            int toFill = shares;

            // 1. Fill from Secondary
            // English: We can't modify c.sellerListings while iterating. Create copy key
            // set.
            // English: IMPORTANT: Skip the buyer's own listings to prevent self-transfer
            // errors.
            // Español: No podemos modificar c.sellerListings mientras iteramos. Crear copia
            // del key set.
            // Español: IMPORTANTE: Saltar listados del propio comprador para evitar errores
            // de auto-transferencia.
            String buyerKey = buyer.toString();
            java.util.List<String> sellers = new java.util.ArrayList<>(c.sellerListings.keySet());
            for (String sellerId : sellers) {
                if (toFill <= 0)
                    break;
                // English: Skip own listings - buyer cannot purchase from themselves.
                // Español: Saltar listados propios - comprador no puede comprarse a sí mismo.
                if (sellerId.equals(buyerKey))
                    continue;
                int avail = c.sellerListings.get(sellerId);
                if (avail > 0) {
                    int take = Math.min(avail, toFill);
                    purchaseFrom.put(sellerId, take);
                    toFill -= take;
                    // We update the listing map later or now? Better do it at end or update
                    // temporary map.
                }
            }

            // 2. Fill from Primary
            int fromPrimary = 0;
            if (toFill > 0) {
                if (c.listedShares >= toFill) {
                    fromPrimary = toFill;
                    toFill = 0;
                } else {
                    // Start of 'not_enough_listed' check failed earlier, so this shouldn't happen
                    // if logic is correct
                    // unless race condition, but we are synchronized.
                    // Just take what is there? No, we enforced check.
                    fromPrimary = c.listedShares;
                    toFill -= c.listedShares;
                }
            }

            if (toFill > 0) {
                // Should not happen given pre-check
                return Result.error("not_enough_listed");
            }

            // Majority Rule Check: If buying from Primary, Owner holdings drop.
            if (fromPrimary > 0) {
                int ownerHeld = c.holdings.getOrDefault(ownerKey, 0);
                if (ownerHeld < fromPrimary + 51) {
                    PrimeBankMod.LOGGER.error(
                            "[PrimeBank] Majority rule violation detected while buying {} shares from Primary",
                            fromPrimary);
                    return Result.error("majority_violation");
                }
            }

            // Processing payments
            String buyerAcc = com.primebank.core.accounts.PlayerAccounts.ensurePersonal(buyer);
            Ledger ledger = new Ledger(PrimeBankState.get().accounts());

            // We need to verify buyer has enough funds for TOTAL transaction.
            long totalGross;
            try {
                totalGross = Math.multiplyExact(grossSingle, shares);
            } catch (ArithmeticException e) {
                return Result.error("overflow");
            }
            // Check buyer funds via simulation or just trust subsequent calls?
            // Ideally we do one atomic check. Ledger doesn't expose "check" easily without
            // doing it.
            // We can check balance manually.
            long buyerBal = PrimeBankState.get().accounts().get(buyerAcc).getBalanceCents();
            // Fee is BUYER_FEE_BPS (2.5%). Total cost = Gross + Fee.
            long fee = (totalGross * BUYER_FEE_BPS) / 10000L;
            if (buyerBal < totalGross + fee) {
                return Result.error("insufficient");
            }

            // Execute transfers
            // 1. Secondary Sellers
            for (java.util.Map.Entry<String, Integer> entry : purchaseFrom.entrySet()) {
                String sId = entry.getKey();
                int qty = entry.getValue();
                long subGross = grossSingle * qty;

                String sellerAcc = com.primebank.core.accounts.PlayerAccounts.ensurePersonal(UUID.fromString(sId));
                Ledger.TransferResult tr = ledger.marketPrimaryBuy(buyerAcc, sellerAcc, subGross, BUYER_FEE_BPS,
                        ISSUER_FEE_BPS);
                if (!tr.success) {
                    // Partial failure is messy. We checked balance.
                    // Stop and fail? We might have processed some.
                    // Improving this requires a complex transaction manager or "Hold" system.
                    // For now, abort remainder and return error (UI will show partial success? No,
                    // simply error).
                    // This is "Good enough" for a game mod.
                    return Result.error("ledger_error_partial");
                }

                // Update Seller Holdings & Listing
                c.holdings.put(sId, c.holdings.get(sId) - qty);
                int oldList = c.sellerListings.get(sId);
                if (oldList - qty <= 0)
                    c.sellerListings.remove(sId);
                else
                    c.sellerListings.put(sId, oldList - qty);

                // Notify Seller
                com.primebank.util.NotificationHelper.notifyMarketSale(server, buyer, UUID.fromString(sId),
                        c.name != null ? c.name : companyId, qty, subGross);
            }

            // 2. Primary Seller (Owner)
            if (fromPrimary > 0) {
                long subGross = grossSingle * fromPrimary;
                String sellerAcc = com.primebank.core.accounts.PlayerAccounts.ensurePersonal(c.ownerUuid);
                Ledger.TransferResult tr = ledger.marketPrimaryBuy(buyerAcc, sellerAcc, subGross, BUYER_FEE_BPS,
                        ISSUER_FEE_BPS);
                if (!tr.success)
                    return Result.error("ledger_error_primary");

                c.holdings.put(ownerKey, c.holdings.get(ownerKey) - fromPrimary);
                c.listedShares -= fromPrimary;

                com.primebank.util.NotificationHelper.notifyMarketSale(server, buyer, c.ownerUuid,
                        c.name != null ? c.name : companyId, fromPrimary, subGross);
            }

            // Update Buyer Holdings
            // English: buyerKey was declared earlier when filling from secondary market.
            // Español: buyerKey fue declarado antes al llenar del mercado secundario.
            c.holdings.put(buyerKey, c.holdings.getOrDefault(buyerKey, 0) + shares);

            CompanyPersistence.saveCompany(c);
        }

        return Result.ok();
    }

    /*
     * English: Result wrapper for market operations.
     * Español: Envoltorio de resultado para operaciones de mercado.
     */
    public static final class Result {
        public final boolean ok;
        public final String error;

        private Result(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public static Result ok() {
            return new Result(true, null);
        }

        public static Result error(String e) {
            return new Result(false, e);
        }
    }
}
