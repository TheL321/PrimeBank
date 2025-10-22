package com.primebank.content.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.util.List;

import java.util.UUID;

/*
 English: Placeholder for a PrimeBank Card item (Phase 2). Owner-only and cashback logic will be added later.
 Additionally, provides NBT helpers to store owner UUID and cardId.
 Español: Provisional para un ítem Tarjeta de PrimeBank (Fase 2). La lógica de propietario y cashback se agregará más adelante.
 Además, provee ayudas NBT para guardar el UUID del dueño y el cardId.
*/
public class ItemCard extends Item {
    public ItemCard(String reg, String key) {
        setRegistryName("primebank", reg);
        setUnlocalizedName(key);
        setMaxStackSize(1);
    }

    /*
     English: Ensure the stack has a tag compound and return it.
     Español: Asegura que la pila tenga un compuesto NBT y lo devuelve.
    */
    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        return tag;
    }

    /*
     English: Get/set the owner UUID on the card.
     Español: Obtener/establecer el UUID del dueño en la tarjeta.
    */
    public static UUID getOwnerUUID(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasUniqueId("owner")) return tag.getUniqueId("owner");
        return null;
    }
    public static void setOwnerUUID(ItemStack stack, UUID owner) {
        if (owner == null) return;
        getOrCreateTag(stack).setUniqueId("owner", owner);
    }

    /*
     English: Get/set the cardId (string) on the card.
     Español: Obtener/establecer el cardId (cadena) en la tarjeta.
    */
    public static String getCardId(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return (tag != null && tag.hasKey("cardId", 8)) ? tag.getString("cardId") : null; // 8 = NBT string
    }
    public static void setCardId(ItemStack stack, String id) {
        if (id == null || id.isEmpty()) return;
        getOrCreateTag(stack).setString("cardId", id);
    }

    /*
     English: Ensure the card has an ID (short UUID fragment) to help identify it.
     Español: Asegura que la tarjeta tenga un ID (fragmento corto de UUID) para ayudar a identificarla.
    */
    private static void ensureCardId(ItemStack stack) {
        String id = getCardId(stack);
        if (id == null || id.isEmpty()) {
            String shortId = java.util.UUID.randomUUID().toString().substring(0, 8);
            setCardId(stack, shortId);
        }
    }

    /*
     English: When the item is created, generate a cardId if missing.
     Español: Cuando se crea el ítem, generar un cardId si falta.
    */
    @Override
    public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
        ensureCardId(stack);
    }

    /*
     English: Client tooltip showing owner (if linked) and card ID. Also ensures an ID exists.
     Español: Tooltip del cliente que muestra el dueño (si está vinculado) y el ID de tarjeta. También asegura que exista un ID.
    */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        ensureCardId(stack);
        UUID owner = getOwnerUUID(stack);
        if (owner == null) {
            tooltip.add(I18n.format("primebank.card.tooltip.unlinked"));
        } else {
            tooltip.add(I18n.format("primebank.card.tooltip.owner", owner.toString()));
        }
        String id = getCardId(stack);
        if (id != null && !id.isEmpty()) {
            tooltip.add(I18n.format("primebank.card.tooltip.id", id));
        }
    }
}
