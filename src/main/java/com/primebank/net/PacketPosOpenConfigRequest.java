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
 English: C2S request to open the POS config GUI for a specific POS block.
 Español: Solicitud C2S para abrir la GUI de configuración del POS para un bloque POS específico.
*/
public class PacketPosOpenConfigRequest implements IMessage {
    private int x, y, z;

    public PacketPosOpenConfigRequest() {}
    public PacketPosOpenConfigRequest(BlockPos pos) { this.x = pos.getX(); this.y = pos.getY(); this.z = pos.getZ(); }

    @Override
    public void fromBytes(ByteBuf buf) { this.x = buf.readInt(); this.y = buf.readInt(); this.z = buf.readInt(); }

    @Override
    public void toBytes(ByteBuf buf) { buf.writeInt(this.x); buf.writeInt(this.y); buf.writeInt(this.z); }

    public static class Handler implements IMessageHandler<PacketPosOpenConfigRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketPosOpenConfigRequest message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                BlockPos pos = new BlockPos(message.x, message.y, message.z);
                net.minecraft.tileentity.TileEntity te = p.world.getTileEntity(pos);
                if (!(te instanceof TilePosPrimeBank)) return;
                TilePosPrimeBank t = (TilePosPrimeBank) te;
                if (t.companyId == null) {
                    // English: Build the player's company list (id + display label) to open selection GUI.
                    // Español: Construir la lista de empresas del jugador (id + etiqueta) para abrir GUI de selección.
                    java.util.List<String> ids = new java.util.ArrayList<>();
                    java.util.List<String> labels = new java.util.ArrayList<>();
                    java.util.UUID me = p.getUniqueID();
                    for (com.primebank.core.company.Company c : com.primebank.core.state.PrimeBankState.get().companies().all()) {
                        if (c.ownerUuid != null && c.ownerUuid.equals(me)) {
                            ids.add(c.id);
                            labels.add(com.primebank.core.state.PrimeBankState.get().getCompanyDisplay(c.id));
                        }
                    }
                    if (ids.isEmpty()) {
                        p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.error.no_companies"));
                        return;
                    }
                    com.primebank.PrimeBankMod.NETWORK.sendTo(new PacketOpenPosSelectCompany(pos, ids, labels), p);
                    return;
                }
                // English: Validate ownership via company registry instead of relying on id prefix.
                // Español: Validar propiedad a través del registro de empresas en lugar de depender del prefijo del id.
                com.primebank.core.company.Company company = com.primebank.core.state.PrimeBankState.get().companies().get(t.companyId);
                boolean isOwner = company != null && company.ownerUuid != null && company.ownerUuid.equals(p.getUniqueID());
                if (!isOwner) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.pos.price.not_owner"));
                    return;
                }
                com.primebank.PrimeBankMod.NETWORK.sendTo(new PacketOpenPosConfig(pos, t.pendingCents), p);
            });
            return null;
        }
    }
}
