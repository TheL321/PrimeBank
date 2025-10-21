package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import net.minecraft.client.Minecraft;

/*
 English: S2C packet to open a POS confirmation prompt with the given cents amount and company id.
 Español: Paquete S2C para abrir un aviso de confirmación POS con el monto en centavos y el id de empresa.
*/
public class PacketPosPrompt implements IMessage {
    public long cents;
    public String companyId;

    public PacketPosPrompt() {}
    public PacketPosPrompt(long cents, String companyId) { this.cents = cents; this.companyId = companyId; }

    @Override
    public void fromBytes(ByteBuf buf) { this.cents = buf.readLong(); this.companyId = ByteBufUtils.readUTF8String(buf); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeLong(this.cents); ByteBufUtils.writeUTF8String(buf, this.companyId == null ? "" : this.companyId); }

    public static class Handler implements IMessageHandler<PacketPosPrompt, IMessage> {
        @Override
        public IMessage onMessage(PacketPosPrompt message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                // English: Open the GUI prompt on the client.
                // Español: Abrir la GUI de confirmación en el cliente.
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                mc.displayGuiScreen(new com.primebank.client.gui.GuiPosPrompt(message.cents, message.companyId));
            });
            return null;
        }
    }
}
