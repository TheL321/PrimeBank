package com.primebank.client;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.primebank.PrimeBankMod;
import com.primebank.content.blocks.PrimeBankBlocks;
import com.primebank.content.items.PrimeBankItems;

/*
 English: Registers item and block models on the client so textures appear instead of the missing texture.
 Español: Registra modelos de ítems y bloques en el cliente para que aparezcan texturas en lugar de la textura faltante.
*/
@Mod.EventBusSubscriber(modid = PrimeBankMod.MODID, value = Side.CLIENT)
public final class ClientModels {
    private ClientModels() {}

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent e) {
        register(PrimeBankItems.CURRENCY_1C);
        register(PrimeBankItems.CURRENCY_5C);
        register(PrimeBankItems.CURRENCY_10C);
        register(PrimeBankItems.CURRENCY_25C);
        register(PrimeBankItems.CURRENCY_50C);
        register(PrimeBankItems.CURRENCY_1D);
        register(PrimeBankItems.CURRENCY_5D);
        register(PrimeBankItems.CURRENCY_10D);
        register(PrimeBankItems.CURRENCY_20D);
        register(PrimeBankItems.CURRENCY_50D);
        register(PrimeBankItems.CURRENCY_100D);
        register(Item.getItemFromBlock(PrimeBankBlocks.TERMINAL));
        // English: Register Card item and POS block so they render.
        // Español: Registrar el ítem Tarjeta y el bloque POS para que se rendericen.
        register(PrimeBankItems.CARD);
        register(Item.getItemFromBlock(PrimeBankBlocks.POS));
    }

    /*
     English: Helper to link an item with its inventory model.
     Español: Ayudante para enlazar un ítem con su modelo de inventario.
    */
    private static void register(Item item) {
        if (item == null) return;
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
