package com.primebank.core.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
 English: Loads admin UUIDs from serverconfig/primebank.toml (key: admins=["uuid1","uuid2"]). Fallback to empty set.
 Español: Carga UUIDs de administradores desde serverconfig/primebank.toml (clave: admins=["uuid1","uuid2"]). Repliegue a conjunto vacío.
*/
public final class AdminService {
    private static final Set<UUID> ADMIN_UUIDS = new HashSet<>();
    private static boolean configExists = false; // Track if config file was found.
    private static boolean adminsKeyPresent = false; // Track if an explicit admins allowlist is set.

    private AdminService() {
    }

    public static void reload(File serverRoot) {
        Set<UUID> tempInfo = new HashSet<>();
        boolean found = false;
        boolean adminsKeyFound = false;
        try {
            File cfg = new File(serverRoot, "serverconfig/primebank.toml");
            if (!cfg.exists()) {
                configExists = false;
                adminsKeyPresent = false;
                ADMIN_UUIDS.clear(); // If config deleted, fall back to "allow all" (standard mod behavior) or "allow
                                     // none"?
                // Standard behavior: if no config, OP is enough.
                return;
            }
            found = true;
            try (BufferedReader br = new BufferedReader(new FileReader(cfg))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("admins")) {
                        adminsKeyFound = true;
                        int lb = line.indexOf('[');
                        int rb = line.indexOf(']');
                        if (lb >= 0 && rb > lb) {
                            String inner = line.substring(lb + 1, rb);
                            String[] parts = inner.split(",");
                            for (String raw : parts) {
                                String s = raw.trim();
                                if (s.startsWith("\"") && s.endsWith("\""))
                                    s = s.substring(1, s.length() - 1);
                                if (!s.isEmpty()) {
                                    try {
                                        tempInfo.add(UUID.fromString(s));
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Success: update live list
            ADMIN_UUIDS.clear();
            ADMIN_UUIDS.addAll(tempInfo);
            configExists = true;
            adminsKeyPresent = adminsKeyFound;
            com.primebank.PrimeBankMod.LOGGER.info("[PrimeBank] Loaded {} admins.", ADMIN_UUIDS.size());
        } catch (Exception ex) {
            com.primebank.PrimeBankMod.LOGGER
                    .error("[PrimeBank] Failed to load admin config! Keeping previous config if any.", ex);
            // Do NOT clear ADMIN_UUIDS on error, protecting against allow-all fallbacks due
            // to malformed files.
            // If it's the first load and it fails, admin list remains empty AND
            // configExists remains false (default) or check logic below.
            // If failed to read but file existed... we should probably treat it as "File
            // Exists but Failed".
            if (found)
                configExists = true;
        }
    }

    public static boolean isAdmin(java.util.UUID uuid, net.minecraft.server.MinecraftServer server,
            net.minecraft.command.ICommandSender sender) {
        if (uuid == null)
            return false;
        try {
            // English: Admin commands require OP (permission level 2). If an allowlist is
            // configured, the sender must ALSO be in the allowlist.
            // Español: Los comandos de admin requieren OP (nivel de permiso 2). Si hay
            // una lista permitida (allowlist), el emisor TAMBIÉN debe estar en esa lista.
            boolean isOp = sender.canUseCommand(2, "gamemode");
            if (!isOp)
                return false;

            // English: If the config file does not exist OR it does not define an explicit
            // admins allowlist, fall back to "OPs are admins".
            // Español: Si el archivo de config no existe O no define una allowlist explícita
            // de admins, usar el repliegue "los OPs son admins".
            if (!configExists || !adminsKeyPresent)
                return true;

            // English: If an explicit allowlist is configured, enforce it in addition to OP.
            // Español: Si hay un allowlist explícito configurado, aplicarlo además de OP.
            return ADMIN_UUIDS.contains(uuid);
        } catch (Throwable t) {
            return false;
        }
    }
}
