package com.primebank.commands;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

import com.mojang.authlib.GameProfile;

import com.primebank.PrimeBankMod;
import com.primebank.core.Money;
import com.primebank.core.accounts.PlayerAccounts;
import com.primebank.core.accounts.CompanyAccounts;
import com.primebank.core.ledger.Ledger;
import com.primebank.core.state.PrimeBankState;
import com.primebank.persistence.BankPersistence;
import com.primebank.persistence.PersistencePaths;
import com.primebank.content.items.CashUtil;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.math.BlockPos;

/*
 English: Root command for PrimeBank (temporary test operations for Phase 1).
 Español: Comando raíz para PrimeBank (operaciones de prueba temporales para la Fase 1).
*/
public class CommandPrimeBank extends CommandBase {
    @Override
    public String getName() {
        return "primebank";
    }

    /*
     * English: Friendly company label for chat: display name if set; otherwise
     * owner's username (if resolvable); otherwise raw id.
     * Español: Etiqueta amigable de empresa para chat: nombre visible si existe; de
     * lo contrario, nombre del dueño (si se puede resolver); en su defecto id
     * crudo.
     */
    private String companyLabel(MinecraftServer server, String companyId) {
        String disp = PrimeBankState.get().getCompanyName(companyId);
        String ticker = PrimeBankState.get().getCompanyShortName(companyId);
        // English: If a display name exists, annotate with ticker when available.
        // Español: Si existe nombre visible, anotarlo con ticker cuando esté
        // disponible.
        if (disp != null && !disp.isEmpty()) {
            if (ticker != null && !ticker.trim().isEmpty()) {
                return String.format("%s (%s)", disp, ticker.trim());
            }
            return disp;
        }
        if (companyId != null && companyId.startsWith("c:")) {
            try {
                String raw = companyId.substring(2);
                java.util.UUID owner = java.util.UUID.fromString(raw);
                net.minecraft.entity.player.EntityPlayerMP online = server.getPlayerList().getPlayerByUUID(owner);
                if (online != null)
                    return online.getName();
                com.mojang.authlib.GameProfile gp = server.getPlayerProfileCache().getProfileByUUID(owner);
                if (gp != null && gp.getName() != null)
                    return gp.getName();
            } catch (Exception ignored) {
            }
        }
        String base = companyId;
        if (ticker != null && !ticker.trim().isEmpty()) {
            base = String.format("%s (%s)", base, ticker.trim());
        }
        return base;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        // English: Update usage to advertise ticker support; adminapprove now takes
        // companyId|TICKER.
        // Español: Actualizar uso para anunciar soporte de ticker; adminapprove ahora
        // recibe companyId|TICKER.
        return "/primebank <balance|history|deposit <d>|withdraw <d>|transfer <player|uuid> <d>|depositcents <c>|withdrawcents <c>|transfercents <player|uuid> <c>|mycompanybalance [companyId|TICKER]|mycompanies|companywithdraw <companyId|TICKER> <d>|setcompanyname [for <companyId|TICKER>] <name|clear>|setcompanyticker [for <companyId|TICKER>] <ticker|clear>|adminapprove <companyId|TICKER>|setcashbackbps <bps>|marketlist <shares> <companyId|TICKER>|marketbuy <companyId|TICKER> <shares>|centralbalance|centralwithdraw <d>|reload>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            if (sender instanceof EntityPlayerMP) {
                sendHelp(server, sender);
            } else {
                sender.sendMessage(new TextComponentTranslation("primebank.usage", getUsage(sender)));
            }
            return;
        }
        String sub = args[0].toLowerCase();
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentTranslation("primebank.player_only"));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        UUID me = player.getUniqueID();
        String myAcc = PlayerAccounts.ensurePersonal(me);
        Ledger ledger = new Ledger(PrimeBankState.get().accounts());

        switch (sub) {
            case "balance": {
                long bal = PrimeBankState.get().accounts().get(myAcc).getBalanceCents();
                sender.sendMessage(new TextComponentTranslation("primebank.balance", Money.formatUsd(bal)));
                break;
            }
            case "history": {
                com.primebank.core.accounts.Account acc = PrimeBankState.get().accounts().get(myAcc);
                java.util.List<com.primebank.core.accounts.Account.TransactionRecord> hist = acc.getHistory();
                if (hist.isEmpty()) {
                    sender.sendMessage(new TextComponentTranslation("primebank.history.empty"));
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.history.header"));
                    for (com.primebank.core.accounts.Account.TransactionRecord r : hist) {
                        sender.sendMessage(new TextComponentTranslation("primebank.history.line",
                                r.timestamp,
                                new TextComponentTranslation("primebank.history.type." + r.type),
                                Money.formatUsd(r.amount),
                                r.description));
                    }
                }
                break;
            }
            case "setcompanyname": {
                // English: Set the display name for an owned company. Supports selecting a specific company when a player owns multiple companies.
                // Español: Establecer el nombre visible para una empresa propia. Soporta seleccionar una empresa específica cuando el jugador tiene múltiples empresas.
                String companyId = CompanyAccounts.ensureDefault(me);
                int nameStartIndex = 1;

                // English: Optional selector form: /pb setcompanyname for <company> <name|clear>
                // Español: Forma opcional: /pb setcompanyname for <empresa> <nombre|clear>
                if (args.length >= 2 && "for".equalsIgnoreCase(args[1])) {
                    if (args.length < 3) {
                        sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                        break;
                    }
                    String ident = args[2];
                    String resolved = PrimeBankState.get().resolveCompanyIdentifier(ident);
                    String targetCompanyId = resolved != null ? resolved : ident;

                    com.primebank.core.company.Company c = PrimeBankState.get().companies().get(targetCompanyId);
                    if (c == null) {
                        sender.sendMessage(new TextComponentTranslation("primebank.admin.company.not_found"));
                        break;
                    }
                    if (!me.equals(c.ownerUuid)) {
                        sender.sendMessage(new TextComponentTranslation("primebank.company.not_owner"));
                        break;
                    }

                    companyId = targetCompanyId;
                    nameStartIndex = 3;
                }

                if (args.length <= nameStartIndex) {
                    String current = PrimeBankState.get().getCompanyName(companyId);
                    String currentTicker = PrimeBankState.get().getCompanyShortName(companyId);
                    String label = companyLabel(server, companyId);
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.current", label,
                            current == null ? "" : current));
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.short_current", label,
                            currentTicker == null ? "" : currentTicker));
                    break;
                }

                String arg1 = args[nameStartIndex];
                if ("clear".equalsIgnoreCase(arg1)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_name"));
                    break;
                }
                String name = String.join(" ", java.util.Arrays.copyOfRange(args, nameStartIndex, args.length))
                        .trim();
                if (name.isEmpty()) {
                    sender.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_name"));
                    break;
                }
                if (!PrimeBankState.get().setCompanyName(companyId, name)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_name"));
                    break;
                }
                sender.sendMessage(new TextComponentTranslation("primebank.company.name.set", name));
                com.primebank.persistence.BankPersistence.saveAllAsync();
                break;
            }
            case "setcompanyticker": {
                // English: Set the ticker for an owned company; supports selecting a specific company when a player owns multiple.
                // Español: Establecer el ticker para una empresa propia; soporta seleccionar una empresa específica cuando el jugador tiene múltiples.
                String companyId = CompanyAccounts.ensureDefault(me);
                int tickerIndex = 1;

                // English: Optional selector form: /pb setcompanyticker for <company> <ticker|clear>
                // Español: Forma opcional: /pb setcompanyticker for <empresa> <ticker|clear>
                if (args.length >= 2 && "for".equalsIgnoreCase(args[1])) {
                    if (args.length < 4) {
                        // English: Need at least: for <company> <ticker|clear>
                        // Español: Se necesita al menos: for <empresa> <ticker|clear>
                        sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                        break;
                    }
                    String ident = args[2];
                    String resolved = PrimeBankState.get().resolveCompanyIdentifier(ident);
                    String targetCompanyId = resolved != null ? resolved : ident;

                    com.primebank.core.company.Company c = PrimeBankState.get().companies().get(targetCompanyId);
                    if (c == null) {
                        sender.sendMessage(new TextComponentTranslation("primebank.admin.company.not_found"));
                        break;
                    }
                    if (!me.equals(c.ownerUuid)) {
                        sender.sendMessage(new TextComponentTranslation("primebank.company.not_owner"));
                        break;
                    }

                    companyId = targetCompanyId;
                    tickerIndex = 3;
                }

                if (args.length <= tickerIndex) {
                    String label = companyLabel(server, companyId);
                    String currentTicker = PrimeBankState.get().getCompanyShortName(companyId);
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.short_current", label,
                            currentTicker == null ? "" : currentTicker));
                    break;
                }

                String arg1 = args[tickerIndex];
                if ("clear".equalsIgnoreCase(arg1)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_short"));
                    break;
                }
                String ticker = arg1.trim().toUpperCase();
                if (ticker.length() < 2 || ticker.length() > 8 || !ticker.matches("[A-Z0-9]+")) {
                    sender.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_short"));
                    break;
                }
                if (!PrimeBankState.get().setCompanyShortName(companyId, ticker)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_short"));
                    break;
                }
                sender.sendMessage(new TextComponentTranslation("primebank.company.name.short_set", ticker));
                com.primebank.persistence.BankPersistence.saveAllAsync();
                break;
            }
            case "mycompanybalance": {
                // English: Show company account balance. Supports selecting which company to
                // view when a player owns multiple.
                // Español: Mostrar el saldo de la cuenta de empresa. Soporta seleccionar qué
                // empresa ver cuando el jugador posee múltiples.
                String companyId = CompanyAccounts.ensureDefault(me);
                if (args.length >= 2) {
                    String ident = args[1];
                    String resolved = PrimeBankState.get().resolveCompanyIdentifier(ident);
                    String targetCompanyId = resolved != null ? resolved : ident;
                    com.primebank.core.company.Company c = PrimeBankState.get().companies().get(targetCompanyId);
                    if (c == null) {
                        sender.sendMessage(new TextComponentTranslation("primebank.admin.company.not_found"));
                        break;
                    }
                    if (!me.equals(c.ownerUuid)) {
                        sender.sendMessage(new TextComponentTranslation("primebank.company.not_owner"));
                        break;
                    }
                    companyId = targetCompanyId;
                }
                long bal = PrimeBankState.get().accounts().get(companyId).getBalanceCents();
                String label = companyLabel(server, companyId);
                sender.sendMessage(
                        new TextComponentTranslation("primebank.company.balance", label, Money.formatUsd(bal)));
                break;
            }
            case "mycompanies": {
                // English: List all companies owned by the player with their balances.
                // Español: Listar todas las empresas propiedad del jugador con sus saldos.
                java.util.Collection<com.primebank.core.company.Company> all = PrimeBankState.get().companies().all();
                boolean found = false;
                sender.sendMessage(new TextComponentTranslation("primebank.company.list.header"));
                for (com.primebank.core.company.Company c : all) {
                    if (c.ownerUuid != null && c.ownerUuid.equals(me)) {
                        found = true;
                        long bal = PrimeBankState.get().accounts().get(c.id).getBalanceCents();
                        String label = companyLabel(server, c.id);
                        sender.sendMessage(new TextComponentTranslation("primebank.company.list.item", label,
                                Money.formatUsd(bal)));
                    }
                }
                if (!found) {
                    sender.sendMessage(new TextComponentTranslation("primebank.company.list.none"));
                }
                break;
            }
            case "companywithdraw": {
                // English: Withdraw from a specific company owned by the player.
                // Español: Retirar de una empresa específica propiedad del jugador.
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                    break;
                }
                String ident = args[1];
                String companyId = PrimeBankState.get().resolveCompanyIdentifier(ident);
                if (companyId == null)
                    companyId = ident;

                com.primebank.core.company.Company c = PrimeBankState.get().companies().get(companyId);
                if (c == null) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.company.not_found"));
                    break;
                }
                if (!me.equals(c.ownerUuid)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.company.not_owner"));
                    break;
                }

                long dollars = parseLongArg(args[2]);
                if (dollars <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    break;
                }
                long cents = dollarsToCents(dollars);

                // English: SECURITY FIX: Save BEFORE giving items to prevent money duplication
                // on crash.
                // Español: CORRECCIÓN DE SEGURIDAD: Guardar ANTES de entregar ítems para
                // prevenir duplicación en caso de crash.
                Ledger.OpResult r = ledger.withdraw(companyId, cents);
                String key = r.success ? "primebank.withdraw.ok" : ("primebank.withdraw.error." + r.code);
                if (r.success) {
                    // Step 1: Withdraw from ledger (already done above)
                    // Step 2: Save bank data IMMEDIATELY with blocking save
                    BankPersistence.saveAllBlocking();
                    // Step 3: Give items to player
                    sender.sendMessage(new TextComponentTranslation(key, Money.formatUsd(cents)));
                    CashUtil.giveCurrency(player, cents);
                    // Step 4: Force player data save to minimize item loss on crash
                    server.getPlayerList().saveAllPlayerData();
                } else {
                    sender.sendMessage(new TextComponentTranslation(key));
                }
                break;
            }
            case "deposit": {
                // English: Handle deposits specified in whole dollars by converting to cents.
                // Español: Manejar depósitos especificados en dólares completos convirtiéndolos
                // a centavos.
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_dollars"));
                    return;
                }
                long dollars = parseLongArg(args[1]);
                if (dollars <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    return;
                }
                long cents = dollarsToCents(dollars);
                boolean spent = CashUtil.spendCurrency(player, cents);
                if (!spent) {
                    sender.sendMessage(new TextComponentTranslation("primebank.deposit.not_enough_currency"));
                } else {
                    Ledger.OpResult r = ledger.deposit(myAcc, cents);
                    String key = r.success ? "primebank.deposit.ok" : ("primebank.deposit.error." + r.code);
                    sender.sendMessage(new TextComponentTranslation(key, Money.formatUsd(cents)));
                    BankPersistence.saveAllAsync();
                }
                break;
            }
            case "depositcents": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_cents"));
                    return;
                }
                long cents = parseLongArg(args[1]);
                if (cents <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    return;
                }
                boolean spent = CashUtil.spendCurrency(player, cents);
                if (!spent) {
                    sender.sendMessage(new TextComponentTranslation("primebank.deposit.not_enough_currency"));
                } else {
                    Ledger.OpResult r = ledger.deposit(myAcc, cents);
                    String key = r.success ? "primebank.deposit.ok" : ("primebank.deposit.error." + r.code);
                    sender.sendMessage(new TextComponentTranslation(key, Money.formatUsd(cents)));
                    BankPersistence.saveAllAsync();
                }
                break;
            }
            case "withdraw": {
                // English: Handle withdrawals in whole dollars, returning currency items worth
                // the amount.
                // Español: Manejar retiros en dólares completos, devolviendo ítems de moneda
                // por el monto.
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_dollars"));
                    return;
                }
                long dollars = parseLongArg(args[1]);
                if (dollars <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    return;
                }
                long cents = dollarsToCents(dollars);
                // English: SECURITY FIX: Save BEFORE giving items to prevent money duplication
                // on crash.
                // Español: CORRECCIÓN DE SEGURIDAD: Guardar ANTES de entregar ítems para
                // prevenir duplicación en caso de crash.
                Ledger.OpResult r = ledger.withdraw(myAcc, cents);
                String key = r.success ? "primebank.withdraw.ok" : ("primebank.withdraw.error." + r.code);
                if (r.success) {
                    // Step 1: Withdraw from ledger (already done above)
                    // Step 2: Save bank data IMMEDIATELY with blocking save
                    BankPersistence.saveAllBlocking();
                    // Step 3: Give items to player
                    sender.sendMessage(new TextComponentTranslation(key, Money.formatUsd(cents)));
                    CashUtil.giveCurrency(player, cents);
                    // Step 4: Force player data save to minimize item loss on crash
                    server.getPlayerList().saveAllPlayerData();
                } else {
                    sender.sendMessage(new TextComponentTranslation(key));
                }
                break;
            }
            case "withdrawcents": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_cents"));
                    return;
                }
                long cents = parseLongArg(args[1]);
                if (cents <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    return;
                }
                // English: SECURITY FIX: Save BEFORE giving items to prevent money duplication
                // on crash.
                // Español: CORRECCIÓN DE SEGURIDAD: Guardar ANTES de entregar ítems para
                // prevenir duplicación en caso de crash.
                Ledger.OpResult r = ledger.withdraw(myAcc, cents);
                String key = r.success ? "primebank.withdraw.ok" : ("primebank.withdraw.error." + r.code);
                if (r.success) {
                    // Step 1: Withdraw from ledger (already done above)
                    // Step 2: Save bank data IMMEDIATELY with blocking save
                    BankPersistence.saveAllBlocking();
                    // Step 3: Give items to player with success message
                    sender.sendMessage(new TextComponentTranslation(key, Money.formatUsd(cents)));
                    CashUtil.giveCurrency(player, cents);
                    // Step 4: Force player data save to minimize item loss on crash
                    server.getPlayerList().saveAllPlayerData();
                } else {
                    sender.sendMessage(new TextComponentTranslation(key));
                }
                break;
            }
            case "transfer": {
                // English: Transfer whole dollars between personal accounts, applying fees when
                // configured.
                // Español: Transferir dólares completos entre cuentas personales, aplicando
                // comisiones cuando existan.
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                    return;
                }
                UUID to;
                try {
                    to = UUID.fromString(args[1]);
                } catch (IllegalArgumentException ex) {
                    to = resolveUsernameToUuid(server, args[1]);
                }
                String toAcc = PlayerAccounts.ensurePersonal(to);
                long dollars = parseLongArg(args[2]);
                if (dollars <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    return;
                }
                long cents = dollarsToCents(dollars);
                Ledger.TransferResult tr = ledger.transfer(myAcc, toAcc, cents);
                if (tr.success) {
                    if (tr.feeApplied)
                        sender.sendMessage(new TextComponentTranslation("primebank.transfer.ok_fee",
                                Money.formatUsd(tr.feeCents)));
                    else
                        sender.sendMessage(new TextComponentTranslation("primebank.transfer.ok"));
                    com.primebank.util.NotificationHelper.notifyTransfer(server, me, to, cents);
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.transfer.error." + tr.code));
                }
                BankPersistence.saveAllAsync();
                break;
            }
            case "transfercents": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                    return;
                }
                // English: Accept username or UUID for the recipient. Keep UUID support.
                // Español: Aceptar nombre de usuario o UUID para el destinatario. Mantener
                // soporte de UUID.
                UUID to;
                try {
                    to = UUID.fromString(args[1]);
                } catch (IllegalArgumentException ex) {
                    to = resolveUsernameToUuid(server, args[1]);
                }
                String toAcc = PlayerAccounts.ensurePersonal(to);
                long cents = parseLongArg(args[2]);
                if (cents <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    return;
                }
                Ledger.TransferResult tr = ledger.transfer(myAcc, toAcc, cents);
                if (tr.success) {
                    if (tr.feeApplied)
                        sender.sendMessage(new TextComponentTranslation("primebank.transfer.ok_fee",
                                Money.formatUsd(tr.feeCents)));
                    else
                        sender.sendMessage(new TextComponentTranslation("primebank.transfer.ok"));
                    com.primebank.util.NotificationHelper.notifyTransfer(server, me, to, cents);
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.transfer.error." + tr.code));
                }
                BankPersistence.saveAllAsync();
                break;
            }
            case "reload": {
                // English: Reset and reload per-world data (accounts, companies) to avoid
                // cross-world leakage.
                // Español: Reiniciar y recargar datos por mundo (cuentas, empresas) para evitar
                // fugas entre mundos.
                // English: Admin-only (OP level 2 required).
                // Español: Solo admin (requiere OP nivel 2).
                if (!com.primebank.core.admin.AdminService.isAdmin(me, server, sender)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.not_admin"));
                    break;
                }
                File worldDir = server.getEntityWorld().getSaveHandler().getWorldDirectory();
                PersistencePaths.setWorldDir(worldDir);
                com.primebank.core.state.PrimeBankState.get().resetForNewWorld();
                BankPersistence.loadAll();
                com.primebank.persistence.CompanyPersistence.loadAll();
                com.primebank.core.state.PrimeBankState.get().ensureCentralAccount();
                sender.sendMessage(new TextComponentTranslation("primebank.reload.ok"));
                break;
            }
            case "adminapprove": {
                // English: Admin-only: approve a company by ID or TICKER and grant majority
                // shares to its owner.
                // Español: Solo admin: aprobar una empresa por ID o TICKER y otorgar acciones
                // mayoritarias a su dueño.
                if (!com.primebank.core.admin.AdminService.isAdmin(me, server, sender)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.not_admin"));
                    break;
                }
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                    break;
                }
                String ident = args[1];
                // English: Accept either canonical id or ticker; resolve to company id.
                // Español: Aceptar id canónico o ticker; resolver al id de la empresa.
                String cid = com.primebank.core.state.PrimeBankState.get().resolveCompanyIdentifier(ident);
                if (cid == null)
                    cid = ident;
                com.primebank.core.company.Company c = com.primebank.core.state.PrimeBankState.get().companies()
                        .get(cid);
                if (c == null) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.company.not_found"));
                    break;
                }
                c.approved = true;
                c.approvedAt = System.currentTimeMillis();
                if (c.ownerUuid != null) {
                    String ownerKey = c.ownerUuid.toString();
                    int cur = c.holdings.getOrDefault(ownerKey, 0);
                    if (cur < 101)
                        c.holdings.put(ownerKey, 101);
                }
                com.primebank.persistence.CompanyPersistence.saveCompany(c);
                sender.sendMessage(
                        new TextComponentTranslation("primebank.admin.company.approved", companyLabel(server, cid)));
                break;
            }
            case "setcashbackbps": {
                // English: Admin-only: set global cashback in basis points for POS purchases
                // (credited to buyer from central).
                // Español: Solo admin: establecer cashback global en puntos básicos para
                // compras POS (acreditado al comprador desde el central).
                if (!com.primebank.core.admin.AdminService.isAdmin(me, server, sender)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.not_admin"));
                    break;
                }
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                    break;
                }
                int bps;
                try {
                    bps = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponentTranslation("primebank.error.bad_number", args[1]));
                    break;
                }
                if (bps < 0)
                    bps = 0;
                com.primebank.core.state.PrimeBankState.get().setGlobalCashbackBps(bps);
                BankPersistence.saveAllAsync();
                sender.sendMessage(new TextComponentTranslation("primebank.admin.cashback.set", bps));
                if (!com.primebank.core.config.PrimeBankConfig.CASHBACK_ENABLED) {
                    // English: Inform admin that cashback is disabled by config even though BPS changed.
                    // Español: Informar al admin que el cashback está deshabilitado por configuración aunque se cambie el BPS.
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.cashback.disabled"));
                }
                break;
            }
            case "marketlist": {
                // English: Owner lists shares for sale on primary market.
                // Español: El dueño lista acciones para la venta en mercado primario.
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                    break;
                }
                int shares;
                try {
                    shares = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponentTranslation("primebank.error.bad_number", args[1]));
                    break;
                }
                if (shares <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    break;
                }
                java.util.UUID owner = me;
                // English: Accept ticker or id for the company argument; now mandatory.
                // Español: Aceptar ticker o id para el argumento de empresa; ahora obligatorio.
                String resolved = com.primebank.core.state.PrimeBankState.get().resolveCompanyIdentifier(args[2]);
                String companyId = resolved != null ? resolved : args[2];
                com.primebank.market.MarketPrimaryService.Result r = com.primebank.market.MarketPrimaryService.get()
                        .listShares(owner, companyId, shares);
                if (r.ok) {
                    sender.sendMessage(new TextComponentTranslation("primebank.market.list.ok", shares,
                            companyLabel(server, companyId)));
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.market.list.error." + r.error));
                }
                break;
            }
            case "marketbuy": {
                // English: Buyer purchases shares from company's listed inventory.
                // Español: Comprador adquiere acciones del inventario listado de la empresa.
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_args"));
                    break;
                }
                String companyIdIn = args[1];
                // English: Accept ticker or id for target company.
                // Español: Aceptar ticker o id para la empresa objetivo.
                String companyId = com.primebank.core.state.PrimeBankState.get().resolveCompanyIdentifier(companyIdIn);
                if (companyId == null)
                    companyId = companyIdIn;
                int shares;
                try {
                    shares = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponentTranslation("primebank.error.bad_number", args[2]));
                    break;
                }
                if (shares <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    break;
                }
                java.util.UUID buyer = me;
                // Precompute display price for feedback (best-effort).
                com.primebank.core.company.Company c = com.primebank.core.state.PrimeBankState.get().companies()
                        .get(companyId);
                long pps = (c == null) ? 0L : (c.valuationCurrentCents / 101L);
                long gross = pps * shares;
                com.primebank.market.MarketPrimaryService.Result r = com.primebank.market.MarketPrimaryService.get()
                        .buyShares(server, buyer, companyId, shares);
                if (r.ok) {
                    sender.sendMessage(new TextComponentTranslation("primebank.market.buy.ok", shares,
                            companyLabel(server, companyId), com.primebank.core.Money.formatUsd(pps),
                            com.primebank.core.Money.formatUsd(gross)));
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.market.buy.error." + r.error));
                }
                break;
            }
            case "centralbalance": {
                // English: Admin-only: check central bank balance.
                // Español: Solo admin: consultar saldo del banco central.
                if (!com.primebank.core.admin.AdminService.isAdmin(me, server, sender)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.not_admin"));
                    break;
                }
                com.primebank.core.accounts.Account central = PrimeBankState.get().ensureCentralAccount();
                long bal = central.getBalanceCents();
                sender.sendMessage(
                        new TextComponentTranslation("primebank.admin.central.balance", Money.formatUsd(bal)));
                break;
            }
            case "centralwithdraw": {
                // English: Admin-only: withdraw from central bank.
                // Español: Solo admin: retirar del banco central.
                if (!com.primebank.core.admin.AdminService.isAdmin(me, server, sender)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.not_admin"));
                    break;
                }
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentTranslation("primebank.missing_dollars"));
                    break;
                }
                long dollars = parseLongArg(args[1]);
                if (dollars <= 0) {
                    sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero"));
                    break;
                }
                long cents = dollarsToCents(dollars);

                // English: SECURITY FIX: Save BEFORE giving items to prevent money duplication
                // on crash.
                // Español: CORRECCIÓN DE SEGURIDAD: Guardar ANTES de entregar ítems para
                // prevenir duplicación en caso de crash.
                Ledger.OpResult r = ledger.centralWithdraw(player.getName(), cents);
                if (r.success) {
                    // Step 1: Withdraw from ledger (already done above)
                    // Step 2: Save bank data IMMEDIATELY with blocking save
                    BankPersistence.saveAllBlocking();
                    // Step 3: Give items to player
                    CashUtil.giveCurrency(player, cents);
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.central.withdraw.ok",
                            Money.formatUsd(cents)));
                    // Step 4: Force player data save to minimize item loss on crash
                    server.getPlayerList().saveAllPlayerData();
                } else {
                    sender.sendMessage(
                            new TextComponentTranslation("primebank.admin.central.withdraw.error.insufficient"));
                }
                break;
            }
            case "apistress": {
                if (!com.primebank.core.admin.AdminService.isAdmin(me, server, sender)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.not_admin"));
                    break;
                }
                if (args.length < 3) {
                    sender.sendMessage(
                            new TextComponentString("Usage: /pb apistress <mode> <count>. Modes: 1=churn, 2=transfer"));
                    break;
                }
                int mode = Integer.parseInt(args[1]);
                int count = Integer.parseInt(args[2]);

                final com.primebank.api.PrimeBankAPI api = com.primebank.api.PrimeBankAPI.getInstance();
                final UUID u1 = me;

                sender.sendMessage(new TextComponentString(
                        "Starting API Stress Test Mode " + mode + " with " + count + " cycles..."));

                if (mode == 1) {
                    // Churn: Deposit 100, Withdraw 100 concurrently
                    long initial = api.getBalance(u1);
                    java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger(0);

                    Thread t1 = new Thread(() -> {
                        for (int i = 0; i < count; i++) {
                            if (api.deposit(u1, 100, "StressTest", "Churn+")
                                    .equals(com.primebank.api.PrimeBankResult.SUCCESS) == false)
                                errors.incrementAndGet();
                        }
                    });
                    Thread t2 = new Thread(() -> {
                        for (int i = 0; i < count; i++) {
                            // Retry logic for withdraw if insufficient funds happens due to race (shouldn't
                            // if started with buffer, but we might hit transient state if withdraw beats
                            // deposit)
                            // Actually, 100 cents is small. Let's assume player has some balance.
                            // To be safe, we loop until success or limit? No, just try.
                            // If we want net zero change, every successful deposit must be matched by a
                            // withdraw.
                            // Implementing simplistic: we assume starting balance is sufficient to cover
                            // transient dips or we tolerate failures but count matches.
                            // Better: Just verify consistency.
                            api.withdraw(u1, 100, "StressTest", "Churn-");
                        }
                    });
                    t1.start();
                    t2.start();

                    new Thread(() -> {
                        try {
                            t1.join();
                            t2.join();
                            long fin = api.getBalance(u1);
                            long diff = fin - initial;
                            sender.sendMessage(new TextComponentString("Stress Test Finished."));
                            sender.sendMessage(new TextComponentString(
                                    "Initial: " + initial + ", Final: " + fin + ", Diff: " + diff));
                            sender.sendMessage(new TextComponentString("Deposit Failures: " + errors.get()));
                            if (diff == 0)
                                sender.sendMessage(new TextComponentString("SUCCESS: Balance conserved."));
                            else
                                sender.sendMessage(new TextComponentString("FAILURE: Balance leaked!"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else if (mode == 2) {
                    // Deadlock test: Mutual transfers
                    // Need a second user. We'll use Central Bank as second user if we can resolve
                    // its UUID?
                    // Central account is special, has no UUID.
                    // We need another UUID. Let's make a dummy one.
                    UUID u2 = UUID.randomUUID();
                    // Ensure u2 exists and has funds
                    api.deposit(u2, 1000000, "Setup", "StressTest");
                    api.deposit(u1, 1000000, "Setup", "StressTest");

                    long start1 = api.getBalance(u1);
                    long start2 = api.getBalance(u2);

                    Thread tA = new Thread(() -> {
                        for (int i = 0; i < count; i++) {
                            api.transfer(u1, u2, 10, "Stress", "A->B");
                        }
                    });
                    Thread tB = new Thread(() -> {
                        for (int i = 0; i < count; i++) {
                            api.transfer(u2, u1, 10, "Stress", "B->A");
                        }
                    });
                    tA.start();
                    tB.start();

                    new Thread(() -> {
                        try {
                            tA.join();
                            tB.join();
                            long end1 = api.getBalance(u1);
                            long end2 = api.getBalance(u2);
                            long totalStart = start1 + start2;
                            long totalEnd = end1 + end2;

                            sender.sendMessage(new TextComponentString("Deadlock Test Finished."));
                            sender.sendMessage(
                                    new TextComponentString("Total Start: " + totalStart + ", Total End: " + totalEnd));
                            if (totalStart == totalEnd)
                                sender.sendMessage(new TextComponentString("SUCCESS: Mass conserved."));
                            else
                                sender.sendMessage(new TextComponentString("FAILURE: Mass changed!"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                break;
            }
            default:
                sender.sendMessage(new TextComponentTranslation("primebank.unknown_subcommand"));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> getAliases() {
        return java.util.Collections.singletonList("pb");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            BlockPos targetPos) {
        // English: Provide tab-completion for subcommands and usernames for
        // transfercents.
        // Español: Proveer autocompletado para subcomandos y nombres de usuario para
        // transfercents.

        // English: Suggest subcommands when typing the first argument.
        // Español: Sugerir subcomandos al escribir el primer argumento.
        if (args.length <= 1) {
            String[] subs = new String[] { "balance", "history", "deposit", "withdraw", "transfer", "depositcents",
                    "withdrawcents", "transfercents", "mycompanybalance", "mycompanies", "companywithdraw",
                    "setcompanyname", "setcompanyticker", "marketlist", "marketbuy", "adminapprove",
                    "setcashbackbps", "centralbalance", "centralwithdraw", "reload", "apistress" };
            return CommandBase.getListOfStringsMatchingLastWord(args, subs);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "transfer":
            case "transfercents": {
                if (args.length == 2) {
                    // English: Suggest online player usernames for the recipient.
                    // Español: Sugerir nombres de jugadores en línea para el destinatario.
                    return CommandBase.getListOfStringsMatchingLastWord(args,
                            server.getPlayerList().getOnlinePlayerNames());
                } else if (args.length == 3) {
                    // English: Suggest common dollar or cent amounts depending on subcommand.
                    // Español: Sugerir montos comunes en dólares o centavos según el subcomando.
                    String[] suggestions = sub.equals("transfercents")
                            ? new String[] { "1", "5", "10", "25", "50", "100", "500", "1000" }
                            : new String[] { "1", "5", "10", "20", "50", "100" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, suggestions);
                }
                break;
            }
            case "deposit":
            case "withdraw":
            case "depositcents":
            case "withdrawcents": {
                if (args.length == 2) {
                    // English: Suggest common dollar or cent amounts for deposit/withdraw commands.
                    // Español: Sugerir montos comunes en dólares o centavos para los comandos
                    // depositar/retirar.
                    String[] suggestions = sub.endsWith("cents")
                            ? new String[] { "1", "5", "10", "25", "50", "100", "500", "1000" }
                            : new String[] { "1", "5", "10", "20", "50", "100" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, suggestions);
                }
                break;
            }
            case "setcompanyname": {
                if (args.length == 2) {
                    String[] opts = new String[] { "clear", "for" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, opts);
                }
                if (args.length == 3 && "for".equalsIgnoreCase(args[1])) {
                    // English: Suggest owned companies (tickers or ids).
                    // Español: Sugerir empresas propias (tickers o ids).
                    if (sender instanceof EntityPlayerMP) {
                        EntityPlayerMP p = (EntityPlayerMP) sender;
                        java.util.List<String> owned = new java.util.ArrayList<>();
                        for (com.primebank.core.company.Company c : PrimeBankState.get().companies().all()) {
                            if (c.ownerUuid != null && c.ownerUuid.equals(p.getUniqueID())) {
                                if (c.shortName != null)
                                    owned.add(c.shortName);
                                else
                                    owned.add(c.id);
                            }
                        }
                        return CommandBase.getListOfStringsMatchingLastWord(args, owned);
                    }
                }
                if (args.length == 4 && "for".equalsIgnoreCase(args[1])) {
                    String[] opts = new String[] { "clear" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, opts);
                }
                break;
            }
            case "setcompanyticker": {
                if (args.length == 2) {
                    String[] opts = new String[] { "clear", "for" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, opts);
                }
                if (args.length == 3 && "for".equalsIgnoreCase(args[1])) {
                    // English: Suggest owned companies (tickers or ids).
                    // Español: Sugerir empresas propias (tickers o ids).
                    if (sender instanceof EntityPlayerMP) {
                        EntityPlayerMP p = (EntityPlayerMP) sender;
                        java.util.List<String> owned = new java.util.ArrayList<>();
                        for (com.primebank.core.company.Company c : PrimeBankState.get().companies().all()) {
                            if (c.ownerUuid != null && c.ownerUuid.equals(p.getUniqueID())) {
                                if (c.shortName != null)
                                    owned.add(c.shortName);
                                else
                                    owned.add(c.id);
                            }
                        }
                        return CommandBase.getListOfStringsMatchingLastWord(args, owned);
                    }
                }
                if (args.length == 4 && "for".equalsIgnoreCase(args[1])) {
                    String[] opts = new String[] { "clear" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, opts);
                }
                break;
            }
            case "mycompanybalance": {
                if (args.length == 2) {
                    // English: Suggest owned companies (tickers or ids).
                    // Español: Sugerir empresas propias (tickers o ids).
                    if (sender instanceof EntityPlayerMP) {
                        EntityPlayerMP p = (EntityPlayerMP) sender;
                        java.util.List<String> owned = new java.util.ArrayList<>();
                        for (com.primebank.core.company.Company c : PrimeBankState.get().companies().all()) {
                            if (c.ownerUuid != null && c.ownerUuid.equals(p.getUniqueID())) {
                                if (c.shortName != null)
                                    owned.add(c.shortName);
                                else
                                    owned.add(c.id);
                            }
                        }
                        return CommandBase.getListOfStringsMatchingLastWord(args, owned);
                    }
                }
                break;
            }
            case "companywithdraw": {
                if (args.length == 2) {
                    // English: Suggest owned companies (tickers or ids).
                    // Español: Sugerir empresas propias (tickers o ids).
                    if (sender instanceof EntityPlayerMP) {
                        EntityPlayerMP p = (EntityPlayerMP) sender;
                        java.util.List<String> owned = new java.util.ArrayList<>();
                        for (com.primebank.core.company.Company c : PrimeBankState.get().companies().all()) {
                            if (c.ownerUuid != null && c.ownerUuid.equals(p.getUniqueID())) {
                                if (c.shortName != null)
                                    owned.add(c.shortName);
                                else
                                    owned.add(c.id);
                            }
                        }
                        return CommandBase.getListOfStringsMatchingLastWord(args, owned);
                    }
                } else if (args.length == 3) {
                    String[] suggestions = new String[] { "1", "5", "10", "20", "50", "100" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, suggestions);
                }
                break;
            }
            default:
                break;
        }

        return java.util.Collections.emptyList();
    }

    private long parseLongArg(String s) throws CommandException {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            // English: Localized bad number error key; argument is injected via %s.
            // Español: Clave localizada para error de número inválido; el argumento se
            // inserta con %s.
            throw new CommandException("primebank.error.bad_number", s);
        }
    }

    /*
     * English: Resolve a username to a UUID using online players, profile cache,
     * and offline fallback.
     * Español: Resolver un nombre de usuario a UUID usando jugadores en línea,
     * caché de perfiles y fallback offline.
     */
    private UUID resolveUsernameToUuid(MinecraftServer server, String username) throws CommandException {
        // 1) Online players
        net.minecraft.entity.player.EntityPlayerMP online = server.getPlayerList().getPlayerByUsername(username);
        if (online != null)
            return online.getUniqueID();

        // 2) Profile cache (players seen before)
        com.mojang.authlib.GameProfile gp = server.getPlayerProfileCache().getGameProfileForUsername(username);
        if (gp != null && gp.getId() != null)
            return gp.getId();

        // 3) Offline mode fallback: deterministic offline UUID
        if (!server.isServerInOnlineMode()) {
            return java.util.UUID
                    .nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        // 4) Not found
        throw new CommandException("primebank.error.player_not_found", username);
    }

    private void sendHelp(MinecraftServer server, ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6PrimeBank Commands:§r"));
        sender.sendMessage(new TextComponentString("§e-- Basic --§r"));
        sender.sendMessage(new TextComponentString(" /pb balance"));
        sender.sendMessage(new TextComponentString(" /pb history"));
        sender.sendMessage(new TextComponentString(" /pb deposit <amount>"));
        sender.sendMessage(new TextComponentString(" /pb withdraw <amount>"));
        sender.sendMessage(new TextComponentString(" /pb transfer <player> <amount>"));

        sender.sendMessage(new TextComponentString("§e-- Company --§r"));
        sender.sendMessage(new TextComponentString(" /pb mycompanybalance [company]"));
        sender.sendMessage(new TextComponentString(" /pb mycompanies"));
        sender.sendMessage(new TextComponentString(" /pb companywithdraw <company> <amount>"));
        sender.sendMessage(new TextComponentString(" /pb setcompanyname [for <company>] <name|clear>"));
        sender.sendMessage(new TextComponentString(" /pb setcompanyticker [for <company>] <ticker|clear>"));

        sender.sendMessage(new TextComponentString("§e-- Market --§r"));
        sender.sendMessage(new TextComponentString(" /pb marketlist <shares> <company>"));
        sender.sendMessage(new TextComponentString(" /pb marketbuy <company> <shares>"));

        if ((sender instanceof EntityPlayerMP)
                && com.primebank.core.admin.AdminService.isAdmin(((EntityPlayerMP) sender).getUniqueID(), server,
                        sender)) {
            sender.sendMessage(new TextComponentString("§c-- Admin --§r"));
            sender.sendMessage(new TextComponentString(" /pb adminapprove <company>"));
            sender.sendMessage(new TextComponentString(" /pb reload"));
        }
    }

    /*
     * English: Convert whole dollars to cents, guarding against overflow.
     * Español: Convertir dólares enteros a centavos, protegiendo contra overflow.
     */
    private long dollarsToCents(long dollars) throws CommandException {
        try {
            return Math.multiplyExact(dollars, 100L);
        } catch (ArithmeticException ex) {
            throw new CommandException("primebank.error.bad_number", Long.toString(dollars));
        }
    }
}
