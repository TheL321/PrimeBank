package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import net.minecraft.client.Minecraft;

/*
 English: S2C packet to open a POS confirmation prompt with the given cents amount.
 Español: Paquete S2C para abrir un aviso de confirmación POS con el monto en centavos.
*/
public class PacketPosPrompt implements IMessage {
    public long cents;

    public PacketPosPrompt() {}
    public PacketPosPrompt(long cents) { this.cents = cents; }

    @Override
    public void fromBytes(ByteBuf buf) { this.cents = buf.readLong(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeLong(this.cents); }

    public static class Handler implements IMessageHandler<PacketPosPrompt, IMessage> {
        @Override
        public IMessage onMessage(PacketPosPrompt message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                // English: Open the GUI prompt on the client.
                // Español: Abrir la GUI de confirmación en el cliente.
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                mc.displayGuiScreen(new com.primebank.client.gui.GuiPosPrompt(message.cents));
            });
            return null;
        }
    }
}
