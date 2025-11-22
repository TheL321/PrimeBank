package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.text.TextComponentTranslation;

/*
 English: C2S packet to buy shares from a company's listed inventory.
 Español: Paquete C2S para comprar acciones del inventario listado de una empresa.
*/
public class PacketMarketBuy implements IMessage {
    public String companyId;
    public int shares;

    public PacketMarketBuy() {
    }

    public PacketMarketBuy(String companyId, int shares) {
        this.companyId = companyId;
        this.shares = shares;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.companyId = ByteBufUtils.readUTF8String(buf);
        this.shares = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.companyId == null ? "" : this.companyId);
        buf.writeInt(this.shares);
    }

    public static class Handler implements IMessageHandler<PacketMarketBuy, IMessage> {
        @Override
        public IMessage onMessage(PacketMarketBuy message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                java.util.UUID buyer = p.getUniqueID();
                // English: Accept ticker or canonical id from client; resolve to id.
                // Español: Aceptar ticker o id canónico del cliente; resolver a id.
                com.primebank.core.state.PrimeBankState state = com.primebank.core.state.PrimeBankState.get();
                String cidIn = message.companyId;
                String cid = state.resolveCompanyIdentifier(cidIn);
                if (cid == null)
                    cid = cidIn;
                int shares = message.shares;
                if (shares <= 0) {
                    p.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    return;
                }
                // English: Precompute price for feedback.
                // Español: Precalcular precio para retroalimentación.
                com.primebank.core.company.Company c = state.companies().get(cid);
                long pps = (c == null) ? 0L : (c.valuationCurrentCents / 101L);
                long gross = pps * shares;
                com.primebank.market.MarketPrimaryService.Result r = com.primebank.market.MarketPrimaryService.get()
                        .buyShares(p.getServerWorld().getMinecraftServer(), buyer, cid, shares);
                if (r.ok) {
                    String label = state.getCompanyDisplay(cid);
                    p.sendMessage(new TextComponentTranslation("primebank.market.buy.ok", shares, label,
                            com.primebank.core.Money.formatUsd(pps), com.primebank.core.Money.formatUsd(gross)));
                    // English: Send updated details back to refresh the GUI.
                    // Español: Enviar detalles actualizados para refrescar la GUI.
                    String displayName = state.getCompanyName(cid);
                    String shortName = state.getCompanyShortName(cid);
                    if (displayName == null || displayName.isEmpty())
                        displayName = cid;
                    c = state.companies().get(cid);
                    long valuationCurrent = c == null ? 0L : c.valuationCurrentCents;
                    long[] valuationHistory = new long[0];
                    if (c != null && c.valuationHistoryCents != null && !c.valuationHistoryCents.isEmpty()) {
                        int max = Math.min(26, c.valuationHistoryCents.size());
                        valuationHistory = new long[max];
                        int start = c.valuationHistoryCents.size() - max;
                        for (int i = 0; i < max; i++) {
                            valuationHistory[i] = c.valuationHistoryCents.get(start + i);
                        }
                    }
                    long refreshedPps = valuationCurrent <= 0 ? 0L : (valuationCurrent / 101L);
                    int listed = c == null ? 0 : c.listedShares;
                    int yourHoldings = (c == null || c.holdings == null) ? 0
                            : c.holdings.getOrDefault(buyer.toString(), 0);
                    boolean blocked = valuationCurrent <= 0;
                    boolean owner = c != null && c.ownerUuid != null && c.ownerUuid.equals(buyer);
                    com.primebank.PrimeBankMod.NETWORK.sendTo(new PacketMarketDetails(cid, displayName, shortName,
                            valuationCurrent, valuationHistory, refreshedPps, listed, yourHoldings, blocked, owner), p);
                } else {
                    p.sendMessage(new TextComponentTranslation("primebank.market.buy.error." + r.error));
                }
            });
            return null;
        }
    }
}
