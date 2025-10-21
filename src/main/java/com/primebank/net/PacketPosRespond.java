package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.core.accounts.PlayerAccounts;
import com.primebank.core.ledger.Ledger;
import com.primebank.core.state.PrimeBankState;

/*
 English: C2S packet with the client's POS decision (accepted/cancelled), amount in cents, and companyId.
 Español: Paquete C2S con la decisión del cliente en el POS (aceptado/cancelado), monto en centavos y companyId.
*/
public class PacketPosRespond implements IMessage {
    public boolean accepted;
    public long cents;
    public String companyId;

    public PacketPosRespond() {}
    public PacketPosRespond(boolean accepted, long cents, String companyId) {
        this.accepted = accepted; this.cents = cents; this.companyId = companyId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.accepted = buf.readBoolean();
        this.cents = buf.readLong();
        this.companyId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.accepted);
        buf.writeLong(this.cents);
        ByteBufUtils.writeUTF8String(buf, this.companyId == null ? "" : this.companyId);
    }

    public static class Handler implements IMessageHandler<PacketPosRespond, IMessage> {
        @Override
        public IMessage onMessage(PacketPosRespond message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                if (!message.accepted) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                        "primebank.pos.result.cancelled"));
                    return;
                }
                // English: Ensure buyer personal account exists and perform POS charge split 95/5.
                // Español: Asegurar cuenta personal del comprador y ejecutar el cobro POS 95/5.
                String buyerId = PlayerAccounts.ensurePersonal(p.getUniqueID());
                Ledger ledger = new Ledger(PrimeBankState.get().accounts());
                Ledger.TransferResult res = ledger.posCharge(buyerId, message.companyId, message.cents);
                if (res.success) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                        "primebank.pos.result.accepted"));
                } else {
                    String key = "primebank.transfer.error." + res.code;
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(key));
                }
            });
            return null;
        }
    }
}
