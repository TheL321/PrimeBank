package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.content.blocks.TilePosPrimeBank;

/*
 English: C2S packet to set/link a POS to a specific company chosen by the owner.
 Español: Paquete C2S para establecer/enlazar un POS a una empresa específica elegida por el dueño.
*/
public class PacketPosSetCompany implements IMessage {
    private int x, y, z;
    private String companyId;

    public PacketPosSetCompany() {}
    public PacketPosSetCompany(BlockPos pos, String companyId) {
        this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ();
        this.companyId = companyId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt(); this.y = buf.readInt(); this.z = buf.readInt();
        this.companyId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.x); buf.writeInt(this.y); buf.writeInt(this.z);
        ByteBufUtils.writeUTF8String(buf, this.companyId == null ? "" : this.companyId);
    }

    public static class Handler implements IMessageHandler<PacketPosSetCompany, IMessage> {
        @Override
        public IMessage onMessage(PacketPosSetCompany message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                BlockPos pos = new BlockPos(message.x, message.y, message.z);
                net.minecraft.tileentity.TileEntity te = p.world.getTileEntity(pos);
                if (!(te instanceof TilePosPrimeBank)) return;
                TilePosPrimeBank t = (TilePosPrimeBank) te;
                // Validate company exists and is owned by the player
                com.primebank.core.company.Company company = com.primebank.core.state.PrimeBankState.get().companies().get(message.companyId);
                if (company == null) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.error.company_not_found"));
                    return;
                }
                if (company.ownerUuid == null || !company.ownerUuid.equals(p.getUniqueID())) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.price.not_owner"));
                    return;
                }
                t.companyId = message.companyId;
                t.markDirty();
                String label = com.primebank.core.state.PrimeBankState.get().getCompanyDisplay(message.companyId);
                p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.linked", label));
            });
            return null;
        }
    }
}
