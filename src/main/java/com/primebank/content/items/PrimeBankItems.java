package com.primebank.content.items;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.primebank.PrimeBankMod;
import com.primebank.content.PrimeBankCreativeTab;

/*
 English: Static registry holder for PrimeBank items (currency denominations).
 Español: Contenedor estático de registro para ítems de PrimeBank (denominaciones de moneda).
*/
@Mod.EventBusSubscriber(modid = PrimeBankMod.MODID)
public final class PrimeBankItems {
    public static ItemCurrency CURRENCY_1C;
    public static ItemCurrency CURRENCY_5C;
    public static ItemCurrency CURRENCY_10C;
    public static ItemCurrency CURRENCY_25C;
    public static ItemCurrency CURRENCY_50C;
    public static ItemCurrency CURRENCY_1D;
    public static ItemCurrency CURRENCY_5D;
    public static ItemCurrency CURRENCY_10D;
    public static ItemCurrency CURRENCY_20D;
    public static ItemCurrency CURRENCY_50D;
    public static ItemCurrency CURRENCY_100D;
    public static ItemCard CARD;

    private PrimeBankItems() {}

    /*
     English: Register items during the Forge registry event.
     Español: Registrar ítems durante el evento de registro de Forge.
    */
    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        CURRENCY_1C = makeCurrency("currency_1c", "primebank.currency_1c", 1);
        CURRENCY_5C = makeCurrency("currency_5c", "primebank.currency_5c", 5);
        CURRENCY_10C = makeCurrency("currency_10c", "primebank.currency_10c", 10);
        CURRENCY_25C = makeCurrency("currency_25c", "primebank.currency_25c", 25);
        CURRENCY_50C = makeCurrency("currency_50c", "primebank.currency_50c", 50);
        CURRENCY_1D = makeCurrency("currency_1d", "primebank.currency_1d", 100);
        CURRENCY_5D = makeCurrency("currency_5d", "primebank.currency_5d", 500);
        CURRENCY_10D = makeCurrency("currency_10d", "primebank.currency_10d", 1000);
        CURRENCY_20D = makeCurrency("currency_20d", "primebank.currency_20d", 2000);
        CURRENCY_50D = makeCurrency("currency_50d", "primebank.currency_50d", 5000);
        CURRENCY_100D = makeCurrency("currency_100d", "primebank.currency_100d", 10000);
        CARD = new ItemCard("card", "primebank.card");

        event.getRegistry().registerAll(
            CURRENCY_1C.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_5C.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_10C.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_25C.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_50C.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_1D.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_5D.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_10D.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_20D.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_50D.setCreativeTab(PrimeBankCreativeTab.TAB),
            CURRENCY_100D.setCreativeTab(PrimeBankCreativeTab.TAB),
            CARD.setCreativeTab(PrimeBankCreativeTab.TAB)
        );
    }

    private static ItemCurrency makeCurrency(String reg, String key, long cents) {
        return new ItemCurrency(reg, key, cents);
    }
}
