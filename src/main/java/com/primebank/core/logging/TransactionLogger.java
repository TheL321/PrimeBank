package com.primebank.core.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.primebank.core.config.PrimeBankConfig;

/*
 English: Logs transactions to a local file and optionally to a Discord webhook.
 Español: Registra transacciones en un archivo local y opcionalmente en un webhook de Discord.
*/
public class TransactionLogger {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final File LOG_FILE = new File("primebank_transactions.log");
    private static final int DISCORD_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(5);

    public static void log(String message) {
        EXECUTOR.submit(() -> {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String logEntry = String.format("[%s] %s", timestamp, message);

            // English: Always persist locally for auditability.
            // Español: Siempre persistir localmente para auditoría.
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                writer.write(logEntry);
                writer.newLine();
            } catch (IOException e) {
                com.primebank.PrimeBankMod.LOGGER
                        .error("[PrimeBank] Failed to log transaction / Error al registrar transacción", e);
            }

            // English: Optionally forward to Discord when configured.
            // Español: Opcionalmente reenviar a Discord cuando esté configurado.
            sendToWebhook(PrimeBankConfig.DISCORD_WEBHOOK_URL, logEntry);
        });
    }

    /*
     * English: Log valuation events to an optional secondary webhook. Still writes
     * to
     * the local audit log for consistency.
     * Español: Registrar eventos de valoración a un webhook secundario opcional.
     * También escribe en el log local para consistencia.
     */
    public static void logValuation(String companyName, long valuationCents, long pricePerShareCents,
            long previousValuationCents, long previousPriceCents, int listedShares, long timestampArg) {
        EXECUTOR.submit(() -> {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            // English: Plain text for local log
            // Español: Texto plano para log local
            String logEntry = String.format(
                    "[%s] VALUATION: %s -> valuation=%d cents (prev=%d), price=%d cents/share (prev=%d), listed=%d shares (run at %s)",
                    timestamp, companyName, valuationCents, previousValuationCents, pricePerShareCents,
                    previousPriceCents, listedShares, new java.util.Date(timestampArg));

            // English: Persist locally as part of the audit trail.
            // Español: Persistir localmente como parte de la trazabilidad.
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                writer.write(logEntry);
                writer.newLine();
            } catch (IOException e) {
                com.primebank.PrimeBankMod.LOGGER
                        .error("[PrimeBank] Failed to log valuation / Error al registrar valoración", e);
            }

            // English: Build JSON Embed for Discord
            // Español: Construir Embed JSON para Discord
            String lang = PrimeBankConfig.DISCORD_MESSAGE_LANGUAGE;
            boolean isEs = "es".equalsIgnoreCase(lang);

            String title = isEs ? "Nueva Valoración" : "New Valuation";
            String fieldCompany = isEs ? "Empresa" : "Company";
            String fieldValuation = isEs ? "Valoración" : "Valuation";
            String fieldPrevValuation = isEs ? "Valoración Anterior" : "Previous Valuation";
            String fieldPrice = isEs ? "Precio por Acción" : "Price per Share";
            String fieldPrevPrice = isEs ? "Precio Anterior" : "Previous Price";
            String fieldDifference = isEs ? "Diferencia" : "Difference";
            String fieldListedShares = isEs ? "Acciones Listadas" : "Listed Shares";
            String footerText = isEs ? "Actualizado" : "Updated";

            // Format numbers nicely
            String valFormatted = String.format("$%.2f", valuationCents / 100.0);
            String priceFormatted = String.format("$%.2f", pricePerShareCents / 100.0);
            String prevValFormatted = String.format("$%.2f", previousValuationCents / 100.0);
            String prevPriceFormatted = String.format("$%.2f", previousPriceCents / 100.0);

            // Calculate differences
            long valuationDiff = valuationCents - previousValuationCents;
            long priceDiff = pricePerShareCents - previousPriceCents;
            String valuationDiffFormatted = String.format("%s$%.2f", valuationDiff >= 0 ? "+" : "",
                    valuationDiff / 100.0);
            String priceDiffFormatted = String.format("%s$%.2f", priceDiff >= 0 ? "+" : "", priceDiff / 100.0);

            // Discord Embed JSON structure
            // Color: 0x00FF00 (Green) for positive, 0xFF0000 (Red) for negative
            int embedColor = valuationDiff >= 0 ? 65280 : 16711680;

            String jsonPayload = String.format(
                    "{" +
                            "\"embeds\": [{" +
                            "\"title\": \"%s\"," +
                            "\"color\": %d," +
                            "\"fields\": [" +
                            "{\"name\": \"%s\", \"value\": \"%s\", \"inline\": true}," +
                            "{\"name\": \"%s\", \"value\": \"%s (%s)\", \"inline\": true}," +
                            "{\"name\": \"%s\", \"value\": \"%s\", \"inline\": true}," +
                            "{\"name\": \"%s\", \"value\": \"%s (%s)\", \"inline\": true}," +
                            "{\"name\": \"%s\", \"value\": \"%s\", \"inline\": true}," +
                            "{\"name\": \"%s\", \"value\": \"%d\", \"inline\": true}" +
                            "]," +
                            "\"footer\": {\"text\": \"%s: %s\"}" +
                            "}]" +
                            "}",
                    title,
                    embedColor,
                    fieldCompany, escapeJson(companyName),
                    fieldValuation, valFormatted, valuationDiffFormatted,
                    fieldPrevValuation, prevValFormatted,
                    fieldPrice, priceFormatted, priceDiffFormatted,
                    fieldPrevPrice, prevPriceFormatted,
                    fieldListedShares, listedShares,
                    footerText, timestamp);

            sendToWebhookRaw(PrimeBankConfig.DISCORD_VALUATION_WEBHOOK_URL, jsonPayload);
        });
    }

    // New helper to send raw JSON payload
    private static void sendToWebhookRaw(String webhookUrl, String jsonPayload) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }
        if (!webhookUrl.startsWith("http")) {
            com.primebank.PrimeBankMod.LOGGER.warn("[PrimeBank] Invalid Discord webhook URL: {}", webhookUrl);
            return;
        }

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(DISCORD_TIMEOUT_MS);
            conn.setReadTimeout(DISCORD_TIMEOUT_MS);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "PrimeBank/DiscordLogger");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                com.primebank.PrimeBankMod.LOGGER
                        .warn("[PrimeBank] Discord webhook responded with {} / Webhook de Discord respondió con {}",
                                code, code);
            }
            conn.disconnect();
        } catch (Exception e) {
            com.primebank.PrimeBankMod.LOGGER
                    .warn("[PrimeBank] Discord webhook delivery failed / Error enviando al webhook de Discord: {}",
                            e.getMessage());
        }
    }

    private static void sendToWebhook(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return;
        }

        // Basic validation: must be a valid http/https URL
        if (!webhookUrl.startsWith("http")) {
            com.primebank.PrimeBankMod.LOGGER.warn("[PrimeBank] Invalid Discord webhook URL: {}", webhookUrl);
            return;
        }

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(DISCORD_TIMEOUT_MS);
            conn.setReadTimeout(DISCORD_TIMEOUT_MS);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "PrimeBank/DiscordLogger");
            conn.setDoOutput(true);

            String jsonPayload = String.format("{\"content\": \"%s\"}", escapeJson(message));

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode(); // Trigger request
            if (code < 200 || code >= 300) {
                com.primebank.PrimeBankMod.LOGGER
                        .warn("[PrimeBank] Discord webhook responded with {} / Webhook de Discord respondió con {}",
                                code, code);
            }
            conn.disconnect();
        } catch (Exception e) {
            // English: Swallow errors to avoid breaking gameplay, but log once for admins.
            // Español: Tragar errores para no romper el juego, pero registrar una vez para
            // admins.
            com.primebank.PrimeBankMod.LOGGER
                    .warn("[PrimeBank] Discord webhook delivery failed / Error enviando al webhook de Discord: {}",
                            e.getMessage());
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
