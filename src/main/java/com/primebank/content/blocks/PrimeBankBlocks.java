package com.primebank.content.blocks;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.primebank.PrimeBankMod;
import com.primebank.content.PrimeBankCreativeTab;

/*
 English: Static registry holder for PrimeBank blocks.
 Español: Contenedor estático de registro para bloques de PrimeBank.
*/
@Mod.EventBusSubscriber(modid = PrimeBankMod.MODID)
public final class PrimeBankBlocks {
    public static BlockTerminalPrimeBank TERMINAL;
    public static BlockPOSPrimeBank POS;

    private PrimeBankBlocks() {}

    /*
     English: Register blocks during the Forge registry event.
     Español: Registrar bloques durante el evento de registro de Forge.
    */
    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        TERMINAL = new BlockTerminalPrimeBank();
        TERMINAL.setCreativeTab(PrimeBankCreativeTab.TAB);
        POS = new BlockPOSPrimeBank();
        POS.setCreativeTab(PrimeBankCreativeTab.TAB);
        event.getRegistry().registerAll(TERMINAL, POS);
    }

    /*
     English: Register ItemBlocks for blocks so they can be held in inventory.
     Español: Registrar ItemBlocks para que los bloques puedan estar en inventario.
    */
    @SubscribeEvent
    public static void onRegisterItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(TERMINAL).setRegistryName(TERMINAL.getRegistryName()));
        event.getRegistry().register(new ItemBlock(POS).setRegistryName(POS.getRegistryName()));
    }
}
