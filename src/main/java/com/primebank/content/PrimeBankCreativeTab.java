package com.primebank.content;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;

/*
 English: Creative tab for PrimeBank items.
 Español: Pestaña creativa para ítems de PrimeBank.
*/
public final class PrimeBankCreativeTab {
    public static final CreativeTabs TAB = new CreativeTabs("primebank") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(Items.EMERALD);
        }
    };
    private PrimeBankCreativeTab() {}
}
