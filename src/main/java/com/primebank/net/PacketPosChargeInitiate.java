package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.core.Money;
import com.primebank.content.blocks.TilePosPrimeBank;

/*
 English: C2S packet to initiate a POS charge; carries the POS position.
 Español: Paquete C2S para iniciar un cobro POS; lleva la posición del POS.
*/
public class PacketPosChargeInitiate implements IMessage {
    private int x, y, z;

    public PacketPosChargeInitiate() {}
    public PacketPosChargeInitiate(BlockPos pos) { this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ(); }

    @Override
    public void fromBytes(ByteBuf buf) { this.x = buf.readInt(); this.y = buf.readInt(); this.z = buf.readInt(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeInt(this.x); buf.writeInt(this.y); buf.writeInt(this.z); }

    public static class Handler implements IMessageHandler<PacketPosChargeInitiate, IMessage> {
        @Override
        public IMessage onMessage(PacketPosChargeInitiate message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                BlockPos pos = new BlockPos(message.x, message.y, message.z);
                net.minecraft.tileentity.TileEntity te = p.world.getTileEntity(pos);
                if (!(te instanceof TilePosPrimeBank)) return;
                TilePosPrimeBank t = (TilePosPrimeBank) te;
                String companyId = t.companyId;
                if (companyId == null) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.error.not_linked"));
                    return;
                }
                long cents = com.primebank.core.state.PrimeBankState.get().getPendingCharge(companyId);
                if (cents <= 0) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.error.no_pending"));
                    return;
                }
                // English: Send S2C prompt with amount and company id.
                // Español: Enviar aviso S2C con monto e id de empresa.
                com.primebank.PrimeBankMod.NETWORK.sendTo(new PacketPosPrompt(cents, companyId), p);
            });
            return null;
        }
    }
}
