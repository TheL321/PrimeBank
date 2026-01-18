package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;

/*
 English: C2S request for market home company list.
 Español: Solicitud C2S para la lista de empresas del mercado principal.
*/
public class PacketMarketHomeRequest implements IMessage {
    public PacketMarketHomeRequest() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<PacketMarketHomeRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketMarketHomeRequest message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                com.primebank.core.state.PrimeBankState state = com.primebank.core.state.PrimeBankState.get();

                // English: Collect all approved companies with their market data.
                // Español: Recopilar todas las empresas aprobadas con sus datos de mercado.
                java.util.List<CompanyListing> listings = new java.util.ArrayList<>();
                for (com.primebank.core.company.Company company : state.companies().all()) {
                    if (company == null || !company.approved)
                        continue;

                    String displayName = state.getCompanyName(company.id);
                    String shortName = state.getCompanyShortName(company.id);
                    if (displayName == null || displayName.isEmpty())
                        displayName = company.id;

                    int totalListed = company.listedShares;
                    if (company.sellerListings != null) {
                        for (int qty : company.sellerListings.values()) {
                            totalListed += qty;
                        }
                    }

                    listings.add(new CompanyListing(
                            company.id,
                            displayName,
                            shortName == null ? "" : shortName,
                            company.valuationCurrentCents,
                            totalListed));
                }

                PacketMarketHomeList response = new PacketMarketHomeList(listings);
                com.primebank.PrimeBankMod.NETWORK.sendTo(response, p);
            });
            return null;
        }
    }

    /*
     * English: Company listing data for market home.
     * Español: Datos de listado de empresa para mercado principal.
     */
    public static class CompanyListing {
        public final String id;
        public final String displayName;
        public final String shortName;
        public final long valuationCents;
        public final int listedShares;

        public CompanyListing(String id, String displayName, String shortName, long valuationCents, int listedShares) {
            this.id = id;
            this.displayName = displayName;
            this.shortName = shortName;
            this.valuationCents = valuationCents;
            this.listedShares = listedShares;
        }
    }
}
