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

import com.primebank.core.config.PrimeBankConfig;

/*
 English: Logs transactions to a local file and optionally to a Discord webhook.
 Español: Registra transacciones en un archivo local y opcionalmente en un webhook de Discord.
*/
public class TransactionLogger {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final File LOG_FILE = new File("primebank_transactions.log");

    public static void log(String message) {
        EXECUTOR.submit(() -> {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String logEntry = String.format("[%s] %s", timestamp, message);

            // Log to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                writer.write(logEntry);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Log to Discord
            if (PrimeBankConfig.DISCORD_WEBHOOK_URL != null && !PrimeBankConfig.DISCORD_WEBHOOK_URL.isEmpty()) {
                sendToDiscord(logEntry);
            }
        });
    }

    private static void sendToDiscord(String message) {
        try {
            URL url = new URL(PrimeBankConfig.DISCORD_WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonPayload = String.format("{\"content\": \"%s\"}", escapeJson(message));

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            conn.getResponseCode(); // Trigger request
            conn.disconnect();
        } catch (Exception e) {
            // Fail silently for Discord errors to not spam server logs
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
