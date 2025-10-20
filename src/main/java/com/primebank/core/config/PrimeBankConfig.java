package com.primebank.core.config;

/*
 English: Default configuration constants for PrimeBank. These will later be backed by serverconfig/primebank.toml.
 Español: Constantes de configuración por defecto para PrimeBank. Más adelante se respaldarán en serverconfig/primebank.toml.
*/
public final class PrimeBankConfig {
    public static final int MARKET_BUYER_FEE_BPS = 250;
    public static final int MARKET_SELLER_FEE_BPS = 500;
    public static final int POS_BANK_FEE_BPS = 500;

    public static final double LOANS_DEFAULT_APR = 0.12;
    public static final int LOANS_DOWNPAYMENT_DEFAULT_BPS = 2000;
    public static final int LOANS_DOWNPAYMENT_MIN_BPS = 0;
    public static final int LOANS_DOWNPAYMENT_MAX_BPS = 8000;
    public static final int LOANS_INSTALLMENT_DAYS = 3;

    public static final boolean ALLOW_OFFLINE_MODE = true;

    private PrimeBankConfig() {}

    /*
     English: Reload defaults from disk (placeholder; uses constants for now).
     Español: Recargar valores por defecto desde disco (marcador de posición; usa constantes por ahora).
    */
    public static void reloadDefaults() {
    }
}
