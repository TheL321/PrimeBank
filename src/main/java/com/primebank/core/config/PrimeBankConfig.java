package com.primebank.core.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/*
 English: Default configuration constants for PrimeBank. These will later be backed by serverconfig/primebank.toml.
 Español: Constantes de configuración por defecto para PrimeBank. Más adelante se respaldarán en serverconfig/primebank.toml.
*/
public final class PrimeBankConfig {
    public static final int MARKET_BUYER_FEE_BPS = 250;
    public static final int MARKET_SELLER_FEE_BPS = 500;
    public static final int POS_BANK_FEE_BPS = 500;
    /*
     * English: If true, clear the company's pending POS amount after each
     * successful sale.
     * Español: Si es verdadero, limpiar el monto POS pendiente de la empresa tras
     * cada venta exitosa.
     */
    public static final boolean POS_AUTOCLEAR_PENDING_AFTER_SALE = false;

    /*
     * English: Master switch for cashback feature; when false, cashback is globally
     * disabled regardless of BPS.
     * Español: Interruptor maestro para la función de cashback; cuando es falso, el
     * cashback queda deshabilitado sin importar los BPS.
     */
    public static boolean CASHBACK_ENABLED = true;

    public static final double LOANS_DEFAULT_APR = 0.12;
    public static final int LOANS_DOWNPAYMENT_DEFAULT_BPS = 2000;
    public static final int LOANS_DOWNPAYMENT_MIN_BPS = 0;
    public static final int LOANS_DOWNPAYMENT_MAX_BPS = 8000;
    public static final int LOANS_INSTALLMENT_DAYS = 3;

    public static final boolean ALLOW_OFFLINE_MODE = true;

    public static String DISCORD_WEBHOOK_URL = null;

    private PrimeBankConfig() {
    }

    /*
     * English: Reload defaults from disk (placeholder; uses constants for now).
     * Español: Recargar valores por defecto desde disco (marcador de posición; usa
     * constantes por ahora).
     */
    public static void reloadDefaults() {
    }

    public static void load(File serverRoot) {
        try {
            File cfg = new File(serverRoot, "serverconfig/primebank.toml");
            if (!cfg.exists())
                return;
            try (BufferedReader br = new BufferedReader(new FileReader(cfg))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("discord_webhook_url")) {
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String val = line.substring(eq + 1).trim();
                            if (val.startsWith("\"") && val.endsWith("\"")) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isEmpty()) {
                                DISCORD_WEBHOOK_URL = val;
                            }
                        }
                    } else if (line.startsWith("cashback_enabled")) {
                        // English: Parse cashback toggle from config (accepts true/false, 1/0).
                        // Español: Parsear el interruptor de cashback desde la config (acepta true/false, 1/0).
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String val = line.substring(eq + 1).trim();
                            if (val.startsWith("\"") && val.endsWith("\"")) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isEmpty()) {
                                if ("1".equals(val)) {
                                    CASHBACK_ENABLED = true;
                                } else if ("0".equals(val)) {
                                    CASHBACK_ENABLED = false;
                                } else {
                                    CASHBACK_ENABLED = Boolean.parseBoolean(val);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore config load errors
        }
    }
}
