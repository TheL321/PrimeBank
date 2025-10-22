package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.util.IThreadListener;

/*
 English: S2C response with market company details.
 Español: Respuesta S2C con detalles de la empresa del mercado.
*/
public class PacketMarketDetails implements IMessage {
    public String companyId;
    public String displayName;
    public long valuationCurrentCents;
    public long[] valuationHistoryCents;
    public long pricePerShareCents;
    public int listedShares;
    public int yourHoldings;
    public boolean tradingBlocked;
    public boolean youAreOwner;

    public PacketMarketDetails() {}
    public PacketMarketDetails(String companyId, String displayName, long valuationCurrentCents, long[] valuationHistoryCents, long pricePerShareCents, int listedShares, int yourHoldings, boolean tradingBlocked, boolean youAreOwner) {
        this.companyId = companyId;
        this.displayName = displayName;
        this.valuationCurrentCents = valuationCurrentCents;
        this.valuationHistoryCents = valuationHistoryCents;
        this.pricePerShareCents = pricePerShareCents;
        this.listedShares = listedShares;
        this.yourHoldings = yourHoldings;
        this.tradingBlocked = tradingBlocked;
        this.youAreOwner = youAreOwner;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.companyId = ByteBufUtils.readUTF8String(buf);
        this.displayName = ByteBufUtils.readUTF8String(buf);
        this.valuationCurrentCents = buf.readLong();
        int historySize = buf.readInt();
        if (historySize < 0) historySize = 0;
        this.valuationHistoryCents = new long[historySize];
        for (int i = 0; i < historySize; i++) {
            this.valuationHistoryCents[i] = buf.readLong();
        }
        this.pricePerShareCents = buf.readLong();
        this.listedShares = buf.readInt();
        this.yourHoldings = buf.readInt();
        this.tradingBlocked = buf.readBoolean();
        this.youAreOwner = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.companyId == null ? "" : this.companyId);
        ByteBufUtils.writeUTF8String(buf, this.displayName == null ? "" : this.displayName);
        buf.writeLong(this.valuationCurrentCents);
        long[] history = this.valuationHistoryCents == null ? new long[0] : this.valuationHistoryCents;
        buf.writeInt(history.length);
        for (long v : history) {
            buf.writeLong(v);
        }
        buf.writeLong(this.pricePerShareCents);
        buf.writeInt(this.listedShares);
        buf.writeInt(this.yourHoldings);
        buf.writeBoolean(this.tradingBlocked);
        buf.writeBoolean(this.youAreOwner);
    }

    public static class Handler implements IMessageHandler<PacketMarketDetails, IMessage> {
        @Override
        public IMessage onMessage(PacketMarketDetails message, MessageContext ctx) {
            IThreadListener thread = net.minecraft.client.Minecraft.getMinecraft();
            thread.addScheduledTask(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                if (mc.currentScreen instanceof com.primebank.client.gui.GuiCompanyDetails) {
                    com.primebank.client.gui.GuiCompanyDetails gui = (com.primebank.client.gui.GuiCompanyDetails) mc.currentScreen;
                    // English: Update the GUI with latest details.
                    // Español: Actualizar la GUI con los últimos detalles.
                    long[] historyCopy = message.valuationHistoryCents == null ? new long[0] : java.util.Arrays.copyOf(message.valuationHistoryCents, message.valuationHistoryCents.length);
                    gui.onDetails(message.displayName, message.valuationCurrentCents, historyCopy, message.pricePerShareCents, message.listedShares, message.yourHoldings, message.tradingBlocked, message.youAreOwner);
                }
            });
            return null;
        }
    }
}
