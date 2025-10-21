package com.primebank.core.accounts;

import java.util.UUID;

import com.primebank.core.state.PrimeBankState;

/*
 English: Helpers to manage company accounts (one default company per owner for now).
 Español: Ayudantes para gestionar cuentas de empresa (por ahora una empresa por dueño).
*/
public final class CompanyAccounts {
    private CompanyAccounts() {}

    /*
     English: Compute a default company id for an owner UUID (format: c:<ownerUuid>).
     Español: Calcular un id de empresa por defecto para un dueño (formato: c:<ownerUuid>).
    */
    public static String defaultCompanyId(UUID owner) {
        return "c:" + owner.toString();
    }

    /*
     English: Ensure the default company account exists and return its id.
     Español: Asegurar que exista la cuenta de empresa por defecto y devolver su id.
    */
    public static String ensureDefault(UUID owner) {
        String id = defaultCompanyId(owner);
        if (!PrimeBankState.get().accounts().exists(id)) {
            PrimeBankState.get().accounts().create(id, AccountType.COMPANY, owner, 0L);
        }
        return id;
    }
}
