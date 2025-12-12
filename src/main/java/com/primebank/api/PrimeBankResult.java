package com.primebank.api;

/**
 * Result codes for PrimeBank API mutation operations.
 */
public enum PrimeBankResult {
    /**
     * Operation completed successfully.
     */
    SUCCESS,

    /**
     * The specified account (player or company) was not found.
     */
    ACCOUNT_NOT_FOUND,

    /**
     * The source account has insufficient funds to complete the transaction.
     */
    INSUFFICIENT_FUNDS,

    /**
     * The amount specified is invalid (e.g., negative or zero).
     */
    INVALID_AMOUNT,

    /**
     * An internal error occurred during the operation.
     */
    INTERNAL_ERROR
}
