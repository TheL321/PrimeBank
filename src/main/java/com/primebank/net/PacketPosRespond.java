package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.core.accounts.PlayerAccounts;
import com.primebank.core.ledger.Ledger;
import com.primebank.core.state.PrimeBankState;

/*
 English: C2S packet with the client's POS decision (accepted/cancelled), amount in cents, and companyId.
 Español: Paquete C2S con la decisión del cliente en el POS (aceptado/cancelado), monto en centavos y companyId.
*/
public class PacketPosRespond implements IMessage {
    public boolean accepted;
    public long cents;
    public String companyId;

    public PacketPosRespond() {
    }

    public PacketPosRespond(boolean accepted, long cents, String companyId) {
        this.accepted = accepted;
        this.cents = cents;
        this.companyId = companyId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.accepted = buf.readBoolean();
        this.cents = buf.readLong();
        this.companyId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.accepted);
        buf.writeLong(this.cents);
        ByteBufUtils.writeUTF8String(buf, this.companyId == null ? "" : this.companyId);
    }

    public static class Handler implements IMessageHandler<PacketPosRespond, IMessage> {
        @Override
        public IMessage onMessage(PacketPosRespond message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                if (!message.accepted) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                            "primebank.pos.result.cancelled"));
                    return;
                }

                // English: SECURITY FIX: Validate client response against server-side stored
                // pending charge.
                // Español: CORRECCIÓN DE SEGURIDAD: Validar respuesta del cliente contra cargo
                // pendiente almacenado en el servidor.
                com.primebank.core.state.PrimeBankState.PendingPosCharge expected = com.primebank.core.state.PrimeBankState
                        .get().getPendingPosCharge(p.getUniqueID());

                if (expected == null) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                            "primebank.pos.error.no_pending"));
                    return;
                }

                if (!expected.companyId.equals(message.companyId)) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                            "primebank.pos.error.company_mismatch"));
                    com.primebank.core.state.PrimeBankState.get().clearPendingPosCharge(p.getUniqueID());
                    return;
                }

                if (expected.cents != message.cents) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                            "primebank.pos.error.amount_mismatch"));
                    com.primebank.core.state.PrimeBankState.get().clearPendingPosCharge(p.getUniqueID());
                    return;
                }

                // English: Clear the pending charge now that we've validated it.
                // Español: Limpiar el cargo pendiente ahora que lo hemos validado.
                com.primebank.core.state.PrimeBankState.get().clearPendingPosCharge(p.getUniqueID());

                // English: Require company approval before allowing POS checkout.
                // Español: Requerir aprobación de la empresa antes de permitir el cobro POS.
                if (message.companyId == null
                        || !com.primebank.core.state.PrimeBankState.get().companies().isApproved(message.companyId)) {
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                            "primebank.pos.error.company_not_approved"));
                    return;
                }
                // English: Ensure buyer personal account exists and perform POS charge split
                // 95/5. Use the server's expected amount, not the client's.
                // Español: Asegurar cuenta personal del comprador y ejecutar el cobro POS 95/5.
                // Usar el monto esperado del servidor, no el del cliente.
                String buyerId = PlayerAccounts.ensurePersonal(p.getUniqueID());
                Ledger ledger = new Ledger(PrimeBankState.get().accounts());
                Ledger.TransferResult res = ledger.posCharge(buyerId, expected.companyId, expected.cents);
                if (res.success) {
                    // English: Add gross sale to company's weekly sales accumulator (for
                    // valuation).
                    // Español: Agregar la venta bruta al acumulador semanal de la empresa (para
                    // valoración).
                    com.primebank.core.state.PrimeBankState.get().companies().addSales(expected.companyId,
                            expected.cents);

                    // English: SECURITY FIX: Save company data immediately to prevent sales data
                    // loss on crash.
                    // Español: CORRECCIÓN DE SEGURIDAD: Guardar datos de empresa inmediatamente
                    // para prevenir pérdida de ventas en crash.
                    com.primebank.core.company.Company company = com.primebank.core.state.PrimeBankState.get()
                            .companies().get(message.companyId);
                    if (company != null) {
                        com.primebank.persistence.CompanyPersistence.saveCompany(company);
                    }
                    // English: Inform buyer with details (to merchant and central fee).
                    // Español: Informar al comprador con detalles (al comerciante y comisión al
                    // central).
                    long toCompany = com.primebank.core.Money.multiplyBps(expected.cents, 9500);
                    long toCentral = com.primebank.core.Money.add(expected.cents, -toCompany);
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                            "primebank.pos.result.accepted.details",
                            com.primebank.core.Money.formatUsd(toCompany),
                            com.primebank.core.Money.formatUsd(toCentral)));

                    // English: Apply global cashback if configured, paid from central to buyer.
                    // Español: Aplicar cashback global si está configurado, pagado desde el central
                    // al comprador.
                    int cbBps = com.primebank.core.state.PrimeBankState.get().getGlobalCashbackBps();
                    boolean cashbackEnabled = com.primebank.core.config.PrimeBankConfig.CASHBACK_ENABLED;
                    if (cashbackEnabled && cbBps > 0) {
                        long cashback = com.primebank.core.Money.multiplyBps(expected.cents, cbBps);
                        Ledger.OpResult cb = ledger.applyCashbackToBuyer(buyerId, cashback);
                        if (cb.success && cashback > 0) {
                            p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                                    "primebank.pos.result.cashback", com.primebank.core.Money.formatUsd(cashback)));
                        }
                    }

                    // English: Notify merchant (owner of companyId) if online.
                    // Español: Notificar al comerciante (dueño de companyId) si está en línea.
                    try {
                        if (message.companyId != null && message.companyId.startsWith("c:")) {
                            String raw = message.companyId.substring(2);
                            java.util.UUID owner = java.util.UUID.fromString(raw);
                            net.minecraft.entity.player.EntityPlayerMP merchant = p.getServerWorld()
                                    .getMinecraftServer().getPlayerList().getPlayerByUUID(owner);
                            if (merchant != null) {
                                merchant.sendMessage(new net.minecraft.util.text.TextComponentTranslation(
                                        "primebank.pos.notify.merchant.received",
                                        com.primebank.core.Money.formatUsd(toCompany),
                                        p.getName()));
                            }
                            // Use NotificationHelper for consistent notification
                            com.primebank.util.NotificationHelper.notifyPosSale(p.getServerWorld().getMinecraftServer(),
                                    p.getUniqueID(), owner, expected.companyId, expected.cents);
                        }
                    } catch (Exception ignored) {
                    }

                    // English: Optionally clear the company's pending POS amount after a successful
                    // sale.
                    // Español: Opcionalmente limpiar el monto POS pendiente de la empresa tras una
                    // venta exitosa.
                    if (com.primebank.core.config.PrimeBankConfig.POS_AUTOCLEAR_PENDING_AFTER_SALE) {
                        com.primebank.core.state.PrimeBankState.get().clearPendingCharge(expected.companyId);
                        com.primebank.persistence.BankPersistence.saveAllAsync();
                    }
                } else {
                    String key = "primebank.transfer.error." + res.code;
                    p.sendMessage(new net.minecraft.util.text.TextComponentTranslation(key));
                }
            });
            return null;
        }
    }
}
