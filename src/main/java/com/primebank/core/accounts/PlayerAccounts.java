package com.primebank.core.accounts;

import java.util.UUID;

import com.primebank.core.state.PrimeBankState;

/*
 English: Helpers to manage player personal accounts.
 Español: Ayudantes para gestionar cuentas personales de jugadores.
*/
public final class PlayerAccounts {
    private PlayerAccounts() {}

    public static String personalAccountId(UUID uuid) {
        return "u:" + uuid.toString();
    }

    /*
     English: Ensure a personal account for the given UUID exists and return its id.
     Español: Asegura que exista una cuenta personal para el UUID dado y devuelve su id.
    */
    public static String ensurePersonal(UUID uuid) {
        String id = personalAccountId(uuid);
        if (!PrimeBankState.get().accounts().exists(id)) {
            PrimeBankState.get().accounts().create(id, AccountType.PERSONAL, uuid, 0L);
        }
        return id;
    }
}
