package com.primebank.api;

/**
 * Thrown when a PrimeBank API method is called from a context that is not
 * allowed,
 * such as the client side.
 */
public class PrimeBankSecurityException extends PrimeBankException {
    public PrimeBankSecurityException(String message) {
        super(message);
    }
}
