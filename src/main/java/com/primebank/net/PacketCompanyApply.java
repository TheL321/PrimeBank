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
    private String shortName;

    public PacketCompanyApply() {}
    public PacketCompanyApply(String name, String desc, String shortName) {
        this.name = name;
        this.desc = desc;
        this.shortName = shortName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.name = ByteBufUtils.readUTF8String(buf);
        this.desc = ByteBufUtils.readUTF8String(buf);
        this.shortName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.name == null ? "" : this.name);
        ByteBufUtils.writeUTF8String(buf, this.desc == null ? "" : this.desc);
        ByteBufUtils.writeUTF8String(buf, this.shortName == null ? "" : this.shortName);
    }

    public static class Handler implements IMessageHandler<PacketCompanyApply, IMessage> {
        @Override
        public IMessage onMessage(PacketCompanyApply message, MessageContext ctx) {
            IThreadListener thread = ctx.getServerHandler().player.getServerWorld();
            thread.addScheduledTask(() -> {
                EntityPlayerMP p = ctx.getServerHandler().player;
                java.util.UUID owner = p.getUniqueID();
                com.primebank.core.company.CompanyRegistry reg = PrimeBankState.get().companies();
                // English: Use the default company for the first application; create a new unique ID for subsequent applications.
                // Español: Usar la empresa por defecto para la primera solicitud; crear un ID único para solicitudes posteriores.
                String defaultId = com.primebank.core.accounts.CompanyAccounts.defaultCompanyId(owner);
                Company existingDefault = reg.get(defaultId);
                boolean defaultIsEmpty = (existingDefault == null)
                    || ((existingDefault.name == null || existingDefault.name.trim().isEmpty())
                        && (existingDefault.shortName == null || existingDefault.shortName.trim().isEmpty())
                        && (existingDefault.description == null || existingDefault.description.trim().isEmpty())
                        && !existingDefault.approved);
                Company c = defaultIsEmpty ? reg.ensureDefault(owner) : reg.createNew(owner);
                // English: Ensure an account exists for this company id (needed for ledger/markets).
                // Español: Asegurar que exista una cuenta para este id de empresa (necesario para ledger/mercados).
                if (!PrimeBankState.get().accounts().exists(c.id)) {
                    PrimeBankState.get().accounts().create(c.id, com.primebank.core.accounts.AccountType.COMPANY, owner, 0L);
                }
                c.name = message.name == null ? null : message.name.trim();
                c.description = message.desc == null ? null : message.desc.trim();
                c.shortName = message.shortName == null ? null : message.shortName.trim();
                c.appliedAt = System.currentTimeMillis();
                c.approved = false;
                // English: Update display name mapping so POS and UIs show it immediately.
                // Español: Actualizar el mapeo de nombre visible para que POS y UIs lo muestren de inmediato.
                if (c.name != null && !c.name.isEmpty()) {
                    PrimeBankState.get().setCompanyName(c.id, c.name);
                }
                if (c.shortName != null && !c.shortName.isEmpty()) {
                    // English: Also set the ticker-style short name.
                    // Español: También establecer el nombre corto tipo ticker.
                    PrimeBankState.get().setCompanyShortName(c.id, c.shortName);
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
