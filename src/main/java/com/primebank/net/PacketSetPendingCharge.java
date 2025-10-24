package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.core.Money;
import com.primebank.core.state.PrimeBankState;

/*
 English: C2S packet to set merchant pending POS charge in cents for their default company.
 Español: Paquete C2S para establecer el cobro POS pendiente en centavos para la empresa por defecto del comerciante.
*/
public class PacketSetPendingCharge implements IMessage {
    public long cents;
    public String companyId;

    public PacketSetPendingCharge() {}
    public PacketSetPendingCharge(long cents, String companyId) {
        this.cents = cents;
        this.companyId = companyId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.cents = buf.readLong();
        this.companyId = net.minecraftforge.fml.common.network.ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.cents);
        net.minecraftforge.fml.common.network.ByteBufUtils.writeUTF8String(buf, this.companyId == null ? "" : this.companyId);
    }

    public static class Handler implements IMessageHandler<PacketSetPendingCharge, IMessage> {
        @Override
        public IMessage onMessage(PacketSetPendingCharge message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                String resolvedId = message.companyId;
                if (resolvedId == null || resolvedId.trim().isEmpty()) {
                    resolvedId = com.primebank.core.accounts.CompanyAccounts.ensureDefault(p.getUniqueID());
                }
                com.primebank.core.company.Company company = PrimeBankState.get().companies().get(resolvedId);
                if (company == null || company.ownerUuid == null || !company.ownerUuid.equals(p.getUniqueID())) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.price.not_owner"));
                    return;
                }
                if (message.cents <= 0) {
                    PrimeBankState.get().clearPendingCharge(resolvedId);
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.pending.cleared"));
                    // English: Persist snapshot to disk so pending map survives restarts.
                    // Español: Persistir snapshot en disco para que el mapa pendiente sobreviva reinicios.
                    com.primebank.persistence.BankPersistence.saveAllAsync();
                } else {
                    PrimeBankState.get().setPendingCharge(resolvedId, message.cents);
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.pending.set", Money.formatUsd(message.cents)));
                    // English: Persist snapshot to disk so pending map survives restarts.
                    // Español: Persistir snapshot en disco para que el mapa pendiente sobreviva reinicios.
                    com.primebank.persistence.BankPersistence.saveAllAsync();
                }
            });
            return null;
        }
    }
}
