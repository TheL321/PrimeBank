package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 English: S2C packet to open the terminal company selection GUI before setting POS prices.
 Español: Paquete S2C para abrir la GUI de selección de empresa del terminal antes de fijar precios POS.
*/
public class PacketOpenTerminalSelectCompany implements IMessage {
    private java.util.List<String> ids;
    private java.util.List<String> labels;

    public PacketOpenTerminalSelectCompany() {}
    public PacketOpenTerminalSelectCompany(java.util.List<String> ids, java.util.List<String> labels) {
        this.ids = ids;
        this.labels = labels;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int n = buf.readInt();
        this.ids = new java.util.ArrayList<>(n);
        this.labels = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            this.ids.add(ByteBufUtils.readUTF8String(buf));
            this.labels.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int n = (ids == null) ? 0 : ids.size();
        buf.writeInt(n);
        for (int i = 0; i < n; i++) {
            ByteBufUtils.writeUTF8String(buf, ids.get(i));
            ByteBufUtils.writeUTF8String(buf, labels.get(i));
        }
    }

    public static class Handler implements IMessageHandler<PacketOpenTerminalSelectCompany, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenTerminalSelectCompany message, MessageContext ctx) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            mc.addScheduledTask(() -> mc.displayGuiScreen(new com.primebank.client.gui.GuiTerminalSelectCompany(message.ids, message.labels)));
            return null;
        }
    }
}
