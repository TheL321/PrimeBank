package com.primebank.persistence;

import java.io.File;

/*
 English: Resolves and prepares filesystem paths under the world directory for PrimeBank persistence.
 Español: Resuelve y prepara rutas del sistema de archivos bajo el directorio del mundo para la persistencia de PrimeBank.
*/
public final class PersistencePaths {
    private static File worldDir;
    private static File baseDir;

    private PersistencePaths() {}

    public static void setWorldDir(File dir) {
        worldDir = dir;
        baseDir = new File(worldDir, "primebank");
        ensureDirs();
    }

    public static File base() {
        return baseDir;
    }

    public static File accountsFile() {
        return new File(baseDir, "accounts.json");
    }

    private static void ensureDirs() {
        if (baseDir != null && !baseDir.exists()) {
            baseDir.mkdirs();
        }
        File users = new File(baseDir, "users");
        if (!users.exists()) users.mkdirs();
        File companies = new File(baseDir, "companies");
        if (!companies.exists()) companies.mkdirs();
        File loans = new File(baseDir, "loans");
        if (!loans.exists()) loans.mkdirs();
        File cards = new File(baseDir, "cards");
        if (!cards.exists()) cards.mkdirs();
        File pos = new File(baseDir, "pos");
        if (!pos.exists()) pos.mkdirs();
        File logs = new File(baseDir, "logs");
        if (!logs.exists()) logs.mkdirs();
    }
}
