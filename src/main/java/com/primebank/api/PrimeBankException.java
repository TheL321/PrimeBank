package com.primebank.api;

/**
 * Base exception for PrimeBank API errors.
 * <p>
 * This is a RuntimeException to allow for fail-fast behavior without forcing
 * try-catch blocks for every call, especially for logic errors like
 * invalid arguments or missing accounts in getBalance.
 * </p>
 */
public class PrimeBankException extends RuntimeException {
    public PrimeBankException(String message) {
        super(message);
    }

    public PrimeBankException(String message, Throwable cause) {
        super(message, cause);
    }
}
