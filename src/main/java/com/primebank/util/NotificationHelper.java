package com.primebank.util;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import com.primebank.core.Money;

/*
 English: Helper for sending transaction notifications to players.
 Español: Ayudante para enviar notificaciones de transacciones a los jugadores.
*/
public class NotificationHelper {

    /*
     * English: Notify recipient of a transfer.
     * Español: Notificar al destinatario de una transferencia.
     */
    public static void notifyTransfer(MinecraftServer server, UUID from, UUID to, long amountCents) {
        if (server == null || to == null)
            return;
        EntityPlayerMP recipient = server.getPlayerList().getPlayerByUUID(to);
        if (recipient != null) {
            String senderName = "Unknown";
            if (from != null) {
                EntityPlayerMP sender = server.getPlayerList().getPlayerByUUID(from);
                if (sender != null) {
                    senderName = sender.getName();
                } else {
                    // Try cache
                    com.mojang.authlib.GameProfile gp = server.getPlayerProfileCache().getProfileByUUID(from);
                    if (gp != null && gp.getName() != null)
                        senderName = gp.getName();
                }
            }
            recipient.sendMessage(new TextComponentTranslation("primebank.notify.transfer.received",
                    Money.formatUsd(amountCents), senderName));
        }
    }

    /*
     * English: Notify seller of a market share sale.
     * Español: Notificar al vendedor de una venta de acciones en el mercado.
     */
    public static void notifyMarketSale(MinecraftServer server, UUID buyer, UUID seller, String companyName, int shares,
            long totalCents) {
        if (server == null || seller == null)
            return;
        EntityPlayerMP sellerPlayer = server.getPlayerList().getPlayerByUUID(seller);
        if (sellerPlayer != null) {
            String buyerName = "Unknown";
            if (buyer != null) {
                EntityPlayerMP buyerPlayer = server.getPlayerList().getPlayerByUUID(buyer);
                if (buyerPlayer != null) {
                    buyerName = buyerPlayer.getName();
                } else {
                    com.mojang.authlib.GameProfile gp = server.getPlayerProfileCache().getProfileByUUID(buyer);
                    if (gp != null && gp.getName() != null)
                        buyerName = gp.getName();
                }
            }
            sellerPlayer.sendMessage(new TextComponentTranslation("primebank.notify.market.sold", shares, companyName,
                    buyerName, Money.formatUsd(totalCents)));
        }
    }

    /*
     * English: Notify merchant of a POS sale.
     * Español: Notificar al comerciante de una venta POS.
     */
    public static void notifyPosSale(MinecraftServer server, UUID buyer, UUID merchant, String companyName,
            long amountCents) {
        if (server == null || merchant == null)
            return;
        EntityPlayerMP merchantPlayer = server.getPlayerList().getPlayerByUUID(merchant);
        if (merchantPlayer != null) {
            String buyerName = "Unknown";
            if (buyer != null) {
                EntityPlayerMP buyerPlayer = server.getPlayerList().getPlayerByUUID(buyer);
                if (buyerPlayer != null) {
                    buyerName = buyerPlayer.getName();
                } else {
                    com.mojang.authlib.GameProfile gp = server.getPlayerProfileCache().getProfileByUUID(buyer);
                    if (gp != null && gp.getName() != null)
                        buyerName = gp.getName();
                }
            }
            merchantPlayer.sendMessage(new TextComponentTranslation("primebank.notify.pos.received",
                    Money.formatUsd(amountCents), buyerName, companyName));
        }
    }
}
