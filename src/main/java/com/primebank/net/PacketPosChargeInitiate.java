package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.core.Money;

/*
 English: C2S packet to initiate a POS charge (skeleton). Carries the amount in cents.
 Español: Paquete C2S para iniciar un cobro POS (esqueleto). Lleva el monto en centavos.
*/
public class PacketPosChargeInitiate implements IMessage {
    private long cents;

    public PacketPosChargeInitiate() {}
    public PacketPosChargeInitiate(long cents) { this.cents = cents; }

    @Override
    public void fromBytes(ByteBuf buf) { this.cents = buf.readLong(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeLong(this.cents); }

    public static class Handler implements IMessageHandler<PacketPosChargeInitiate, IMessage> {
        @Override
        public IMessage onMessage(PacketPosChargeInitiate message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                // English: For now just acknowledge on server with the amount.
                // Español: Por ahora solo reconocer en el servidor con el monto.
                EntityPlayerMP p = ctx.getServerHandler().player;
                p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                    "primebank.pos.init.recv", Money.formatUsd(message.cents)));
                // English: Send S2C prompt to client to confirm/cancel this charge.
                // Español: Enviar aviso S2C al cliente para confirmar/cancelar este cobro.
                com.primebank.PrimeBankMod.NETWORK.sendTo(new PacketPosPrompt(message.cents), p);
            });
            return null;
        }
    }
}
