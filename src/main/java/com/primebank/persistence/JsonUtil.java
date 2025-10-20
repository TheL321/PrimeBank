package com.primebank.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/*
 English: Minimal JSON helpers for reading/writing UTF-8 files.
 Español: Utilidades mínimas para leer/escribir archivos JSON en UTF-8.
*/
public final class JsonUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonUtil() {}

    public static <T> T read(File file, Class<T> type) {
        try {
            if (file == null || !file.exists()) return null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                return GSON.fromJson(br, type);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON / Falló la lectura de JSON: " + file, e);
        }
    }

    public static void write(File file, Object obj) {
        try {
            file.getParentFile().mkdirs();
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                GSON.toJson(obj, bw);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write JSON / Falló la escritura de JSON: " + file, e);
        }
    }
}
