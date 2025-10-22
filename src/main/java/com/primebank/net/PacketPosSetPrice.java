package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.content.blocks.TilePosPrimeBank;

/*
 English: C2S packet to set the price of a specific POS block. Server enforces that only the company owner can set it.
 Español: Paquete C2S para establecer el precio de un bloque POS específico. El servidor exige que solo el dueño de la empresa pueda hacerlo.
*/
public class PacketPosSetPrice implements IMessage {
    private int x, y, z;
    private long cents;

    public PacketPosSetPrice() {}
    public PacketPosSetPrice(BlockPos pos, long cents) { this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ(); this.cents = cents; }

    @Override
    public void fromBytes(ByteBuf buf) { this.x = buf.readInt(); this.y = buf.readInt(); this.z = buf.readInt(); this.cents = buf.readLong(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeInt(this.x); buf.writeInt(this.y); buf.writeInt(this.z); buf.writeLong(this.cents); }

    public static class Handler implements IMessageHandler<PacketPosSetPrice, IMessage> {
        @Override
        public IMessage onMessage(PacketPosSetPrice message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                BlockPos pos = new BlockPos(message.x, message.y, message.z);
                net.minecraft.tileentity.TileEntity te = p.world.getTileEntity(pos);
                if (!(te instanceof TilePosPrimeBank)) return;
                TilePosPrimeBank t = (TilePosPrimeBank) te;
                if (t.companyId == null) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.error.not_linked"));
                    return;
                }
                // English: Check that player is the owner of the company linked to this POS.
                // Español: Verificar que el jugador sea el dueño de la empresa vinculada a este POS.
                boolean isOwner = false;
                if (t.companyId.startsWith("c:")) {
                    String expected = "c:" + p.getUniqueID().toString();
                    isOwner = expected.equals(t.companyId);
                }
                if (!isOwner) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.price.not_owner"));
                    return;
                }
                if (message.cents <= 0) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.price.bad_amount"));
                    return;
                }
                t.pendingCents = message.cents;
                t.markDirty();
                p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.price.set", com.primebank.core.Money.formatUsd(message.cents)));
            });
            return null;
        }
    }
}
