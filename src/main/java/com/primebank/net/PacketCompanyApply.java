package com.primebank.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.core.company.Company;
import com.primebank.core.state.PrimeBankState;
import com.primebank.persistence.CompanyPersistence;

/*
 English: C2S company application packet with name and description.
 Español: Paquete C2S de solicitud de empresa con nombre y descripción.
*/
public class PacketCompanyApply implements IMessage {
    private String name;
    private String desc;

    public PacketCompanyApply() {}
    public PacketCompanyApply(String name, String desc) { this.name = name; this.desc = desc; }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
        this.desc = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name == null ? "" : this.name);
        ByteBufUtils.writeUTF8String(buf, this.desc == null ? "" : this.desc);
    }

    public static class Handler implements IMessageHandler<PacketCompanyApply, IMessage> {
        @Override
        public IMessage onMessage(PacketCompanyApply message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                java.util.UUID owner = p.getUniqueID();
                com.primebank.core.company.CompanyRegistry reg = PrimeBankState.get().companies();
                Company c = reg.ensureDefault(owner);
                c.name = message.name == null ? null : message.name.trim();
                c.description = message.desc == null ? null : message.desc.trim();
                c.appliedAt = System.currentTimeMillis();
                c.approved = false;
                // English: Update display name mapping so POS and UIs show it immediately.
                // Español: Actualizar el mapeo de nombre visible para que POS y UIs lo muestren de inmediato.
                if (c.name != null && !c.name.isEmpty()) {
                    PrimeBankState.get().setCompanyName(c.id, c.name);
                }
                CompanyPersistence.saveCompany(c);
                // English: Persist snapshot including company display names.
                // Español: Persistir snapshot incluyendo los nombres visibles de empresas.
                com.primebank.persistence.BankPersistence.saveAllAsync();
                p.sendMessage(new net.minecraft.util.text.TextComponentTranslation("primebank.company.apply.submitted"));
            });
            return null;
        }
    }
}
