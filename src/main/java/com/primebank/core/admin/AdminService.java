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

    private AdminService() {}

    public static void reload(File serverRoot) {
        ADMIN_UUIDS.clear();
        try {
            File cfg = new File(serverRoot, "serverconfig/primebank.toml");
            if (!cfg.exists()) return;
            try (BufferedReader br = new BufferedReader(new FileReader(cfg))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("admins")) {
                        int lb = line.indexOf('[');
                        int rb = line.indexOf(']');
                        if (lb >= 0 && rb > lb) {
                            String inner = line.substring(lb + 1, rb);
                            String[] parts = inner.split(",");
                            for (String raw : parts) {
                                String s = raw.trim();
                                if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
                                if (!s.isEmpty()) {
                                    try { ADMIN_UUIDS.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static boolean isAdmin(java.util.UUID uuid, net.minecraft.server.MinecraftServer server, net.minecraft.command.ICommandSender sender) {
        if (uuid == null) return false;
        try {
            // English: Admin commands require OP (permission level 2). If an allowlist is
            // configured, the sender must ALSO be in the allowlist.
            // Español: Los comandos de admin requieren OP (nivel de permiso 2). Si hay
            // una lista permitida (allowlist), el emisor TAMBIÉN debe estar en esa lista.
            boolean isOp = sender.canUseCommand(2, "gamemode");
            if (!isOp) return false;

            if (ADMIN_UUIDS.isEmpty()) return true;
            return ADMIN_UUIDS.contains(uuid);
        } catch (Throwable t) {
            return false;
        }
    }
}
