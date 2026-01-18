package com.primebank.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/*
 English: C2S request for market company details.
 Español: Solicitud C2S de detalles de empresa del mercado.
*/
public class PacketMarketDetailsRequest implements IMessage {
    public String companyId;

    public PacketMarketDetailsRequest() {
    }

    public PacketMarketDetailsRequest(String companyId) {
        this.companyId = companyId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.companyId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.companyId == null ? "" : this.companyId);
    }

    public static class Handler implements IMessageHandler<PacketMarketDetailsRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketMarketDetailsRequest message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                com.primebank.core.state.PrimeBankState state = com.primebank.core.state.PrimeBankState.get();
                // English: Allow clients to send ticker or id; resolve to the canonical company
                // id.
                // Español: Permitir que el cliente envíe ticker o id; resolver al id canónico
                // de la empresa.
                String cidIn = message.companyId;
                String cid = state.resolveCompanyIdentifier(cidIn);
                if (cid == null)
                    cid = cidIn;
                com.primebank.core.company.Company c = state.companies().get(cid);
                String displayName = state.getCompanyName(cid);
                String shortName = state.getCompanyShortName(cid);
                if (displayName == null || displayName.isEmpty())
                    displayName = cid;
                long valuationCurrent = 0L;
                long[] valuationHistory = new long[0];
                long pps = 0L;
                int listed = 0;
                int holdings = 0;
                boolean blocked = true;
                boolean owner = false;
                if (c != null) {
                    valuationCurrent = c.valuationCurrentCents;
                    List<Long> historyList = c.valuationHistoryCents;
                    if (historyList != null && !historyList.isEmpty()) {
                        int max = Math.min(26, historyList.size());
                        valuationHistory = new long[max];
                        int start = historyList.size() - max;
                        for (int i = 0; i < max; i++) {
                            valuationHistory[i] = historyList.get(start + i);
                        }
                    }
                    pps = valuationCurrent <= 0 ? 0L : (valuationCurrent / 101L);
                    listed = c.listedShares;
                    if (c.sellerListings != null) {
                        for (int qty : c.sellerListings.values()) {
                            listed += qty;
                        }
                    }
                    String key = p.getUniqueID().toString();
                    holdings = c.holdings == null ? 0 : c.holdings.getOrDefault(key, 0);
                    blocked = valuationCurrent <= 0;
                    owner = c.ownerUuid != null && c.ownerUuid.equals(p.getUniqueID());
                }
                PacketMarketDetails resp = new PacketMarketDetails(cid, displayName, shortName, valuationCurrent,
                        valuationHistory, pps, listed, holdings, blocked, owner);
                com.primebank.PrimeBankMod.NETWORK.sendTo(resp, p);
            });
            return null;
        }
    }
}
