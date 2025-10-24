package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.PrimeBankMod;

/*
 English: C2S request to open the terminal company selection GUI before setting POS prices.
 Español: Solicitud C2S para abrir la GUI de selección de empresa del terminal antes de fijar precios POS.
*/
public class PacketTerminalOpenChargeRequest implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
        // English: No payload to decode.
        // Español: Sin datos que decodificar.
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // English: No payload to encode.
        // Español: Sin datos que codificar.
    }

    public static class Handler implements IMessageHandler<PacketTerminalOpenChargeRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketTerminalOpenChargeRequest message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
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
                PrimeBankMod.NETWORK.sendTo(new PacketOpenTerminalSelectCompany(ids, labels), p);
            });
            return null;
        }
    }
}
