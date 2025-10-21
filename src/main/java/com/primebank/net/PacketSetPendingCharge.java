package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.core.Money;
import com.primebank.core.accounts.CompanyAccounts;
import com.primebank.core.state.PrimeBankState;

/*
 English: C2S packet to set merchant pending POS charge in cents for their default company.
 Español: Paquete C2S para establecer el cobro POS pendiente en centavos para la empresa por defecto del comerciante.
*/
public class PacketSetPendingCharge implements IMessage {
    public long cents;

    public PacketSetPendingCharge() {}
    public PacketSetPendingCharge(long cents) { this.cents = cents; }

    @Override
    public void fromBytes(ByteBuf buf) { this.cents = buf.readLong(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeLong(this.cents); }

    public static class Handler implements IMessageHandler<PacketSetPendingCharge, IMessage> {
        @Override
        public IMessage onMessage(PacketSetPendingCharge message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                String companyId = CompanyAccounts.ensureDefault(p.getUniqueID());
                if (message.cents <= 0) {
                    PrimeBankState.get().clearPendingCharge(companyId);
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.pending.cleared"));
                } else {
                    PrimeBankState.get().setPendingCharge(companyId, message.cents);
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.pending.set", Money.formatUsd(message.cents)));
                }
            });
            return null;
        }
    }
}
