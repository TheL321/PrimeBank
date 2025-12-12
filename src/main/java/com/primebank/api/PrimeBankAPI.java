package com.primebank.api;

import java.util.UUID;

/**
 * Public API for PrimeBank.
 * <p>
 * This API allows server-side mods to interact with player bank accounts
 * securely.
 * Accessing this API from the client side will throw a
 * {@link PrimeBankSecurityException}.
 * </p>
 */
public interface PrimeBankAPI {

    /**
     * Gets the singleton instance of the PrimeBank API.
     *
     * @return The API instance.
     */
    static PrimeBankAPI getInstance() {
        return com.primebank.core.api.PrimeBankAPIImpl.instance();
    }

    /**
     * Gets the balance of a player's personal account.
     *
     * @param player The UUID of the player. cannot be null.
     * @return The balance in cents.
     * @throws IllegalArgumentException   If valid is null.
     * @throws PrimeBankSecurityException If called from the client side.
     * @throws PrimeBankException         If the account does not exist or other
     *                                    error.
     */
    long getBalance(UUID player);

    /**
     * Deposits money into a player's personal account.
     *
     * @param player      The UUID of the player. Cannot be null.
     * @param amount      The amount to deposit in cents. Must be positive.
     * @param source      The source of the deposit (e.g., "VendingMachine").
     *                    Defaults to "Unknown" if null/empty.
     * @param description A defined description for the transaction history.
     *                    Defaults to "Unknown" if null/empty.
     * @return A {@link PrimeBankResult} indicating the outcome.
     * @throws IllegalArgumentException   If player is null.
     * @throws PrimeBankSecurityException If called from the client side.
     */
    PrimeBankResult deposit(UUID player, long amount, String source, String description);

    /**
     * Withdraws money from a player's personal account.
     *
     * @param player      The UUID of the player. Cannot be null.
     * @param amount      The amount to withdraw in cents. Must be positive.
     * @param source      The source of the withdrawal (e.g., "VendingMachine").
     *                    Defaults to "Unknown" if null/empty.
     * @param description A defined description for the transaction history.
     *                    Defaults to "Unknown" if null/empty.
     * @return A {@link PrimeBankResult} indicating the outcome (e.g.,
     *         INSUFFICIENT_FUNDS).
     * @throws IllegalArgumentException   If player is null.
     * @throws PrimeBankSecurityException If called from the client side.
     */
    PrimeBankResult withdraw(UUID player, long amount, String source, String description);

    /**
     * Transfers money from one player to another.
     *
     * @param from        The UUID of the sender. Cannot be null.
     * @param to          The UUID of the recipient. Cannot be null.
     * @param amount      The amount to transfer in cents. Must be positive.
     * @param source      The source/context of the transfer. Defaults to "Unknown"
     *                    if null/empty.
     * @param description A defined description for the transaction history.
     *                    Defaults to "Unknown" if null/empty.
     * @return A {@link PrimeBankResult} indicating the outcome.
     * @throws IllegalArgumentException   If from or to is null.
     * @throws PrimeBankSecurityException If called from the client side.
     */
    PrimeBankResult transfer(UUID from, UUID to, long amount, String source, String description);
}
