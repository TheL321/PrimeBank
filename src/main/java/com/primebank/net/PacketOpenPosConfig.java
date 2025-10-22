package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 English: S2C packet to open the POS configuration GUI with the current price.
 Español: Paquete S2C para abrir la GUI de configuración del POS con el precio actual.
*/
public class PacketOpenPosConfig implements IMessage {
    private int x, y, z;
    private long cents;

    public PacketOpenPosConfig() {}
    public PacketOpenPosConfig(net.minecraft.util.math.BlockPos pos, long cents) {
        this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ(); this.cents = cents;
    }

    @Override
    public void fromBytes(ByteBuf buf) { this.x = buf.readInt(); this.y = buf.readInt(); this.z = buf.readInt(); this.cents = buf.readLong(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeInt(this.x); buf.writeInt(this.y); buf.writeInt(this.z); buf.writeLong(this.cents); }

    public static class Handler implements IMessageHandler<PacketOpenPosConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenPosConfig message, MessageContext ctx) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(message.x, message.y, message.z);
                mc.displayGuiScreen(new com.primebank.client.gui.GuiPosConfig(pos, message.cents));
            });
            return null;
        }
    }
}
