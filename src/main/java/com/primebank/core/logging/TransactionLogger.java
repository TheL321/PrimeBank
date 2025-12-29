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
                com.primebank.PrimeBankMod.LOGGER.error("[PrimeBank] Failed to log transaction / Error al registrar transacción", e);
            }

            // English: Optionally forward to Discord when configured.
            // Español: Opcionalmente reenviar a Discord cuando esté configurado.
            sendToWebhook(PrimeBankConfig.DISCORD_WEBHOOK_URL, logEntry);
        });
    }

    /*
     * English: Log valuation events to an optional secondary webhook. Still writes to
     * the local audit log for consistency.
     * Español: Registrar eventos de valoración a un webhook secundario opcional.
     * También escribe en el log local para consistencia.
     */
    public static void logValuation(String message) {
        EXECUTOR.submit(() -> {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String logEntry = String.format("[%s] %s", timestamp, message);

            // English: Persist locally as part of the audit trail.
            // Español: Persistir localmente como parte de la trazabilidad.
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                writer.write(logEntry);
                writer.newLine();
            } catch (IOException e) {
                com.primebank.PrimeBankMod.LOGGER
                        .error("[PrimeBank] Failed to log valuation / Error al registrar valoración", e);
            }

            // English: Only send to the valuation webhook; avoid sending regular transactions here.
            // Español: Solo enviar al webhook de valoraciones; evitar enviar transacciones regulares aquí.
            sendToWebhook(PrimeBankConfig.DISCORD_VALUATION_WEBHOOK_URL, logEntry);
        });
    }

    private static void sendToWebhook(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;
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
                        .warn("[PrimeBank] Discord webhook responded with {} / Webhook de Discord respondió con {}", code, code);
            }
            conn.disconnect();
        } catch (Exception e) {
            // English: Swallow errors to avoid breaking gameplay, but log once for admins.
            // Español: Tragar errores para no romper el juego, pero registrar una vez para admins.
            com.primebank.PrimeBankMod.LOGGER
                    .warn("[PrimeBank] Discord webhook delivery failed / Error enviando al webhook de Discord", e);
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
