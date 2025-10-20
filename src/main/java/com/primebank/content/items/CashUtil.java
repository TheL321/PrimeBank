package com.primebank.content.items;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;

/*
 English: Utility to collect all ItemCurrency stacks from a player's inventory and return total cents.
 Español: Utilidad para recolectar todas las pilas de ItemCurrency del inventario del jugador y devolver el total en centavos.
*/
public final class CashUtil {
    private CashUtil() {}

    public static long collectAllCurrency(EntityPlayerMP player) {
        long total = 0L;
        NonNullList<ItemStack> main = player.inventory.mainInventory;
        for (int i = 0; i < main.size(); i++) {
            ItemStack st = main.get(i);
            if (!st.isEmpty()) {
                Item it = st.getItem();
                if (it instanceof ItemCurrency) {
                    ItemCurrency cur = (ItemCurrency) it;
                    total += cur.getValueCents() * st.getCount();
                    main.set(i, ItemStack.EMPTY);
                }
            }
        }
        NonNullList<ItemStack> off = player.inventory.offHandInventory;
        for (int i = 0; i < off.size(); i++) {
            ItemStack st = off.get(i);
            if (!st.isEmpty()) {
                Item it = st.getItem();
                if (it instanceof ItemCurrency) {
                    ItemCurrency cur = (ItemCurrency) it;
                    total += cur.getValueCents() * st.getCount();
                    off.set(i, ItemStack.EMPTY);
                }
            }
        }
        player.inventory.markDirty();
        return total;
    }

    /*
     English: Try to spend exactly 'amountCents' from player's currency stacks. Returns true if removed, false otherwise.
     Español: Intenta gastar exactamente 'amountCents' de las pilas de moneda del jugador. Devuelve true si se removió, false en caso contrario.
    */
    public static boolean spendCurrency(EntityPlayerMP player, long amountCents) {
        if (amountCents <= 0) return false;
        // Snapshot of currency slots: arrays for performance and simplicity
        final int MAIN = 0, OFF = 1;
        int capacity = player.inventory.mainInventory.size() + player.inventory.offHandInventory.size();
        int[] invType = new int[capacity];
        int[] index = new int[capacity];
        long[] value = new long[capacity];
        int[] count = new int[capacity];
        int n = 0;
        NonNullList<ItemStack> main = player.inventory.mainInventory;
        for (int i = 0; i < main.size(); i++) {
            ItemStack st = main.get(i);
            if (!st.isEmpty() && st.getItem() instanceof ItemCurrency) {
                invType[n] = MAIN; index[n] = i; value[n] = ((ItemCurrency) st.getItem()).getValueCents(); count[n] = st.getCount(); n++;
            }
        }
        NonNullList<ItemStack> off = player.inventory.offHandInventory;
        for (int i = 0; i < off.size(); i++) {
            ItemStack st = off.get(i);
            if (!st.isEmpty() && st.getItem() instanceof ItemCurrency) {
                invType[n] = OFF; index[n] = i; value[n] = ((ItemCurrency) st.getItem()).getValueCents(); count[n] = st.getCount(); n++;
            }
        }
        if (n == 0) return false;
        // Sort slots by value descending (simple selection sort for small n)
        for (int i = 0; i < n; i++) {
            int max = i;
            for (int j = i + 1; j < n; j++) if (value[j] > value[max]) max = j;
            if (max != i) {
                int ti = invType[i]; invType[i] = invType[max]; invType[max] = ti;
                int ii = index[i]; index[i] = index[max]; index[max] = ii;
                long vi = value[i]; value[i] = value[max]; value[max] = vi;
                int ci = count[i]; count[i] = count[max]; count[max] = ci;
            }
        }
        long remaining = amountCents;
        int[] use = new int[n]; // units to consume per slot
        for (int i = 0; i < n && remaining > 0; i++) {
            long v = value[i];
            if (v <= 0) continue;
            long maxUnits = Math.min(count[i], (int) Math.min(Integer.MAX_VALUE, remaining / v));
            if (maxUnits > 0) {
                use[i] = (int) maxUnits;
                remaining -= v * maxUnits;
            }
        }
        if (remaining != 0) return false; // Cannot compose exact amount
        // Apply decrements to actual inventory
        for (int i = 0; i < n; i++) {
            if (use[i] <= 0) continue;
            NonNullList<ItemStack> list = (invType[i] == MAIN) ? player.inventory.mainInventory : player.inventory.offHandInventory;
            ItemStack st = list.get(index[i]);
            if (!st.isEmpty()) {
                st.shrink(use[i]);
                if (st.getCount() <= 0) list.set(index[i], ItemStack.EMPTY);
            }
        }
        player.inventory.markDirty();
        return true;
    }

    /*
     English: Give currency items to the player for the specified cents, using highest denominations first.
     Splits into stacks of up to 64 and adds to inventory; if inventory is full, drops on the ground.
     Español: Entrega ítems de moneda al jugador por la cantidad en centavos, usando primero las denominaciones más altas.
     Divide en pilas de hasta 64 y las añade al inventario; si el inventario está lleno, las suelta al suelo.
    */
    public static void giveCurrency(EntityPlayerMP player, long amountCents) {
        if (amountCents <= 0) return;
        // Denominations in descending order
        ItemCurrency[] denoms = new ItemCurrency[] {
            PrimeBankItems.CURRENCY_100D,
            PrimeBankItems.CURRENCY_50D,
            PrimeBankItems.CURRENCY_20D,
            PrimeBankItems.CURRENCY_10D,
            PrimeBankItems.CURRENCY_5D,
            PrimeBankItems.CURRENCY_1D,
            PrimeBankItems.CURRENCY_50C,
            PrimeBankItems.CURRENCY_25C,
            PrimeBankItems.CURRENCY_10C,
            PrimeBankItems.CURRENCY_5C,
            PrimeBankItems.CURRENCY_1C
        };
        long remaining = amountCents;
        boolean gaveAny = false;
        for (ItemCurrency cur : denoms) {
            if (cur == null) continue;
            long v = cur.getValueCents();
            if (v <= 0) continue;
            long units = remaining / v;
            while (units > 0) {
                int stackCount = (int) Math.min(64, units);
                ItemStack stack = new ItemStack(cur, stackCount);
                boolean added = player.inventory.addItemStackToInventory(stack);
                if (!added) {
                    // English: Drop near player if inventory cannot accept more.
                    // Español: Soltar cerca del jugador si el inventario no acepta más.
                    player.dropItem(stack, false);
                }
                gaveAny = true;
                units -= stackCount;
            }
            remaining = remaining % v;
        }
        player.inventory.markDirty();
        if (gaveAny) {
            // English: Play item pickup sound at player's position to provide feedback.
            // Español: Reproducir sonido de recoger ítem en la posición del jugador para dar retroalimentación.
            float pitch = 0.9F + player.world.rand.nextFloat() * 0.2F;
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.25F, pitch);
        }
    }
}
