package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.util.IThreadListener;

import java.util.ArrayList;
import java.util.List;

/*
 English: S2C response with market home company listings.
 Español: Respuesta S2C con listados de empresas del mercado principal.
*/
public class PacketMarketHomeList implements IMessage {
    public List<CompanyListing> listings;

    public PacketMarketHomeList() {}
    
    public PacketMarketHomeList(List<PacketMarketHomeRequest.CompanyListing> listings) {
        this.listings = new ArrayList<>();
        for (PacketMarketHomeRequest.CompanyListing listing : listings) {
            this.listings.add(new CompanyListing(
                listing.id,
                listing.displayName,
                listing.shortName,
                listing.valuationCents,
                listing.listedShares
            ));
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        this.listings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = ByteBufUtils.readUTF8String(buf);
            String displayName = ByteBufUtils.readUTF8String(buf);
            String shortName = ByteBufUtils.readUTF8String(buf);
            long valuationCents = buf.readLong();
            int listedShares = buf.readInt();
            this.listings.add(new CompanyListing(id, displayName, shortName, valuationCents, listedShares));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        List<CompanyListing> list = this.listings == null ? new ArrayList<>() : this.listings;
        buf.writeInt(list.size());
        for (CompanyListing listing : list) {
            ByteBufUtils.writeUTF8String(buf, listing.id == null ? "" : listing.id);
            ByteBufUtils.writeUTF8String(buf, listing.displayName == null ? "" : listing.displayName);
            ByteBufUtils.writeUTF8String(buf, listing.shortName == null ? "" : listing.shortName);
            buf.writeLong(listing.valuationCents);
            buf.writeInt(listing.listedShares);
        }
    }

    public static class Handler implements IMessageHandler<PacketMarketHomeList, IMessage> {
        @Override
        public IMessage onMessage(PacketMarketHomeList message, MessageContext ctx) {
            IThreadListener thread = net.minecraft.client.Minecraft.getMinecraft();
            thread.addScheduledTask(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                if (mc.currentScreen instanceof com.primebank.client.gui.GuiMarketHome) {
                    com.primebank.client.gui.GuiMarketHome gui = (com.primebank.client.gui.GuiMarketHome) mc.currentScreen;
                    // English: Update the GUI with the listings from server.
                    // Español: Actualizar la GUI con los listados del servidor.
                    gui.onListingsReceived(message.listings);
                }
            });
            return null;
        }
    }
    
    /*
     English: Company listing data for market home.
     Español: Datos de listado de empresa para mercado principal.
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
