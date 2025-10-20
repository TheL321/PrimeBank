package com.primebank.content.items;

import net.minecraft.item.Item;
import com.primebank.PrimeBankMod;

/*
 English: Currency item that carries a fixed value in cents.
 Español: Ítem de moneda que contiene un valor fijo en centavos.
*/
public class ItemCurrency extends Item {
    private final long valueCents;

    public ItemCurrency(String registryName, String translationKey, long valueCents) {
        this.valueCents = valueCents;
        // English: Ensure the registry name uses our mod domain to link models/textures correctly.
        // Español: Asegurar que el nombre de registro use nuestro dominio del mod para enlazar modelos/texturas correctamente.
        setRegistryName(PrimeBankMod.MODID, registryName);
        setUnlocalizedName(translationKey);
        setMaxStackSize(64);
    }

    public long getValueCents() {
        return valueCents;
    }
}
