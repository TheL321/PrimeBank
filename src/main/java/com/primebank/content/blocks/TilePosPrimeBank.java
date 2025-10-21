package com.primebank.content.blocks;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/*
 English: Tile entity for POS: stores linked companyId and pending charge amount (in cents).
 Español: Entidad de bloque para POS: guarda companyId enlazado y el monto pendiente (en centavos).
*/
public class TilePosPrimeBank extends TileEntity {
    public String companyId = null;
    public long pendingCents = 0L;

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (companyId != null) compound.setString("companyId", companyId);
        compound.setLong("pendingCents", pendingCents);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.companyId = compound.hasKey("companyId") ? compound.getString("companyId") : null;
        this.pendingCents = compound.getLong("pendingCents");
    }
}
