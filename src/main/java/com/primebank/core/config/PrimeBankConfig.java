package com.primebank.core.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public static String DISCORD_WEBHOOK_URL = "";
    public static String DISCORD_VALUATION_WEBHOOK_URL = "";
    public static String DISCORD_MESSAGE_LANGUAGE = "en";

    private PrimeBankConfig() {
    }

    /*
     * English: Reload defaults from disk (placeholder; uses constants for now).
     * Español: Recargar valores por defecto desde disco (marcador de posición; usa
     * constantes por ahora).
     */
    public static void reloadDefaults() {
        // No-op for now, as defaults are inline
    }

    /*
     * English: Write the current configuration to the file.
     * Español: Escribir la configuración actual al archivo.
     */
    private static void save(File configFile) {
        try {
            List<String> lines = Arrays.asList(
                    "# PrimeBank configuration / Configuración de PrimeBank",
                    "",
                    "# Enable/disable cashback globally / Habilitar/deshabilitar cashback globalmente",
                    "cashback_enabled = " + CASHBACK_ENABLED,
                    "",
                    "# Optional: redirect central fee collections to a company account (null/empty = keep in central)",
                    "# Opcional: redirigir comisiones del banco central a una cuenta de empresa (null/vacío = mantener en central)",
                    "# Example: central_fee_redirect_company_id = \"c:<owner-uuid>\" or \"TICKER\"",
                    "# Ejemplo: central_fee_redirect_company_id = \"c:<owner-uuid>\" o \"TICKER\"",
                    "central_fee_redirect_company_id = \""
                            + (CENTRAL_FEE_REDIRECT_COMPANY_ID == null ? "" : CENTRAL_FEE_REDIRECT_COMPANY_ID) + "\"",
                    "",
                    "# Discord webhook for notifications (optional) / Webhook de Discord para notificaciones (opcional)",
                    "# Example / Ejemplo: https://discord.com/api/webhooks/...",
                    "discord_webhook_url = \"" + (DISCORD_WEBHOOK_URL == null ? "" : DISCORD_WEBHOOK_URL) + "\"",
                    "",
                    "# Discord webhook for valuation updates only (optional) / Webhook de Discord solo para valoraciones (opcional)",
                    "# Example / Ejemplo: https://discord.com/api/webhooks/...",
                    "discord_valuation_webhook_url = \""
                            + (DISCORD_VALUATION_WEBHOOK_URL == null ? "" : DISCORD_VALUATION_WEBHOOK_URL) + "\"",
                    "",
                    "# Language for Discord messages (en/es) / Idioma para mensajes de Discord (en/es)",
                    "discord_message_language = \""
                            + (DISCORD_MESSAGE_LANGUAGE == null ? "en" : DISCORD_MESSAGE_LANGUAGE)
                            + "\"",
                    "");
            Files.write(configFile.toPath(), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            com.primebank.PrimeBankMod.LOGGER.error("Failed to save PrimeBank configuration", e);
        }
    }

    /*
     * English: Company id to receive redirected central fee collections (null =
     * keep
     * in central).
     * Español: Id de empresa que recibe las comisiones del banco central
     * redireccionadas (null = mantener en central).
     */
    public static String CENTRAL_FEE_REDIRECT_COMPANY_ID = null;

    public static void load(File serverRoot) {
        // English: Reset values to defaults before reading to avoid stale data.
        // Español: Reiniciar valores a los predeterminados antes de leer para evitar
        // datos viejos.
        DISCORD_WEBHOOK_URL = "";
        DISCORD_VALUATION_WEBHOOK_URL = "";
        DISCORD_MESSAGE_LANGUAGE = "en";
        CENTRAL_FEE_REDIRECT_COMPANY_ID = null;
        CASHBACK_ENABLED = true;

        File cfg = new File(serverRoot, "serverconfig/primebank.toml");
        File serverConfigDir = cfg.getParentFile();
        if (serverConfigDir != null && !serverConfigDir.exists()) {
            serverConfigDir.mkdirs();
        }

        if (!cfg.exists()) {
            save(cfg);
            return;
        }

        Set<String> foundKeys = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(cfg))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                if (line.startsWith("discord_webhook_url")) {
                    foundKeys.add("discord_webhook_url");
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String val = line.substring(eq + 1).trim();
                        if (val.startsWith("\"") && val.endsWith("\"")) {
                            val = val.substring(1, val.length() - 1);
                        }
                        DISCORD_WEBHOOK_URL = val.isEmpty() ? "" : val;
                    }
                } else if (line.startsWith("discord_valuation_webhook_url")) {
                    foundKeys.add("discord_valuation_webhook_url");
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String val = line.substring(eq + 1).trim();
                        if (val.startsWith("\"") && val.endsWith("\"")) {
                            val = val.substring(1, val.length() - 1);
                        }
                        DISCORD_VALUATION_WEBHOOK_URL = val.isEmpty() ? "" : val;
                    }
                } else if (line.startsWith("discord_message_language")) {
                    foundKeys.add("discord_message_language");
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String val = line.substring(eq + 1).trim();
                        if (val.startsWith("\"") && val.endsWith("\"")) {
                            val = val.substring(1, val.length() - 1);
                        }
                        DISCORD_MESSAGE_LANGUAGE = val.isEmpty() ? "en" : val;
                    }
                } else if (line.startsWith("cashback_enabled")) {
                    foundKeys.add("cashback_enabled");
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
                    foundKeys.add("central_fee_redirect_company_id");
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
        } catch (Exception e) {
            com.primebank.PrimeBankMod.LOGGER.error("Failed to load PrimeBank configuration", e);
        }

        // Check if any key is missing, if so, rewrite file to update it
        if (!foundKeys.contains("discord_webhook_url") ||
                !foundKeys.contains("discord_valuation_webhook_url") ||
                !foundKeys.contains("discord_message_language") ||
                !foundKeys.contains("cashback_enabled") ||
                !foundKeys.contains("central_fee_redirect_company_id")) {

            com.primebank.PrimeBankMod.LOGGER.info("Updating PrimeBank configuration file with missing keys...");
            save(cfg);
        }
    }
}
