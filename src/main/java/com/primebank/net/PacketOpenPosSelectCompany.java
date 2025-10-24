package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 English: S2C packet to open a GUI for selecting which company to assign to a POS.
 Español: Paquete S2C para abrir una GUI de selección de empresa para asignar al POS.
*/
public class PacketOpenPosSelectCompany implements IMessage {
    private int x, y, z;
    private java.util.List<String> ids;
    private java.util.List<String> labels;

    public PacketOpenPosSelectCompany() {}
    public PacketOpenPosSelectCompany(net.minecraft.util.math.BlockPos pos, java.util.List<String> ids, java.util.List<String> labels) {
        this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ();
        this.ids = ids; this.labels = labels;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt(); this.y = buf.readInt(); this.z = buf.readInt();
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
        buf.writeInt(this.x); buf.writeInt(this.y); buf.writeInt(this.z);
        int n = (ids == null) ? 0 : ids.size();
        buf.writeInt(n);
        for (int i = 0; i < n; i++) {
            ByteBufUtils.writeUTF8String(buf, ids.get(i));
            ByteBufUtils.writeUTF8String(buf, labels.get(i));
        }
    }

    public static class Handler implements IMessageHandler<PacketOpenPosSelectCompany, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenPosSelectCompany message, MessageContext ctx) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(message.x, message.y, message.z);
                mc.displayGuiScreen(new com.primebank.client.gui.GuiPosSelectCompany(pos, message.ids, message.labels));
            });
            return null;
        }
    }
}
