package com.primebank.core.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /*
     * English: Write a default config file with all supported keys and example values.
     * Español: Escribir un archivo de configuración por defecto con todas las claves soportadas y valores de ejemplo.
     */
    private static void writeDefaultConfig(File configFile) throws IOException {
        List<String> lines = Arrays.asList(
                "# PrimeBank configuration / Configuración de PrimeBank",
                "",
                "# Enable/disable cashback globally / Habilitar/deshabilitar cashback globalmente",
                "cashback_enabled = true",
                "",
                "# Optional: redirect central fee collections to a company account (null/empty = keep in central)",
                "# Opcional: redirigir comisiones del banco central a una cuenta de empresa (null/vacío = mantener en central)",
                "# Example: central_fee_redirect_company_id = \"c:<owner-uuid>\" or \"TICKER\"",
                "# Ejemplo: central_fee_redirect_company_id = \"c:<owner-uuid>\" o \"TICKER\"",
                "central_fee_redirect_company_id = \"\"",
                "",
                "# Discord webhook for notifications (optional) / Webhook de Discord para notificaciones (opcional)",
                "# Example / Ejemplo: https://discord.com/api/webhooks/...",
                "discord_webhook_url = \"\"",
                ""
        );
        Files.write(configFile.toPath(), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /*
     * English: Company id to receive redirected central fee collections (null = keep
     * in central).
     * Español: Id de empresa que recibe las comisiones del banco central
     * redireccionadas (null = mantener en central).
     */
    public static String CENTRAL_FEE_REDIRECT_COMPANY_ID = null;

    public static void load(File serverRoot) {
        // English: Reset values to defaults before reading to avoid stale data.
        // Español: Reiniciar valores a los predeterminados antes de leer para evitar datos viejos.
        DISCORD_WEBHOOK_URL = null;
        CENTRAL_FEE_REDIRECT_COMPANY_ID = null;
        CASHBACK_ENABLED = true;
        try {
            File cfg = new File(serverRoot, "serverconfig/primebank.toml");
            if (!cfg.exists()) {
                // English: Ensure serverconfig directory exists, then write default config.
                // Español: Asegurar que el directorio serverconfig exista, luego escribir config por defecto.
                File serverConfigDir = cfg.getParentFile();
                if (serverConfigDir != null && !serverConfigDir.exists()) {
                    serverConfigDir.mkdirs();
                }
                writeDefaultConfig(cfg);
                return;
            }
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
                            // English: Allow clearing by setting empty string; ignore malformed/blank lines.
                            // Español: Permitir limpiar usando cadena vacía; ignorar líneas mal formadas o vacías.
                            DISCORD_WEBHOOK_URL = val.isEmpty() ? null : val;
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
                    } else if (line.startsWith("central_fee_redirect_company_id")) {
                        // English: Parse optional company id for redirecting central collections.
                        // Español: Parsear id de empresa opcional para redirigir cobros del central.
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String val = line.substring(eq + 1).trim();
                            if (val.startsWith("\"") && val.endsWith("\"")) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (val != null && !val.trim().isEmpty()) {
                                CENTRAL_FEE_REDIRECT_COMPANY_ID = val.trim();
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
