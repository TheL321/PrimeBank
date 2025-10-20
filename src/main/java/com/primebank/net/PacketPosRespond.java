package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 English: C2S packet with the client's POS decision (accepted/cancelled) and amount in cents.
 Español: Paquete C2S con la decisión del cliente en el POS (aceptado/cancelado) y el monto en centavos.
*/
public class PacketPosRespond implements IMessage {
    public boolean accepted;
    public long cents;

    public PacketPosRespond() {}
    public PacketPosRespond(boolean accepted, long cents) {
        this.accepted = accepted; this.cents = cents;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.accepted = buf.readBoolean();
        this.cents = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.accepted);
        buf.writeLong(this.cents);
    }

    public static class Handler implements IMessageHandler<PacketPosRespond, IMessage> {
        @Override
        public IMessage onMessage(PacketPosRespond message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                if (message.accepted) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                        "primebank.pos.result.accepted"));
                } else {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                        "primebank.pos.result.cancelled"));
                }
            });
            return null;
        }
    }
}
