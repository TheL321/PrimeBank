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
     English: Friendly company label for chat: display name if set; otherwise owner's username (if resolvable); otherwise raw id.
     Español: Etiqueta amigable de empresa para chat: nombre visible si existe; de lo contrario, nombre del dueño (si se puede resolver); en su defecto id crudo.
    */
    private String companyLabel(MinecraftServer server, String companyId) {
        String disp = PrimeBankState.get().getCompanyName(companyId);
        String ticker = PrimeBankState.get().getCompanyShortName(companyId);
        // English: If a display name exists, annotate with ticker when available.
        // Español: Si existe nombre visible, anotarlo con ticker cuando esté disponible.
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
                if (online != null) return online.getName();
                com.mojang.authlib.GameProfile gp = server.getPlayerProfileCache().getProfileByUUID(owner);
                if (gp != null && gp.getName() != null) return gp.getName();
            } catch (Exception ignored) {}
        }
        String base = companyId;
        if (ticker != null && !ticker.trim().isEmpty()) {
            base = String.format("%s (%s)", base, ticker.trim());
        }
        return base;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        // English: Update usage to advertise ticker support; adminapprove now takes companyId|TICKER.
        // Español: Actualizar uso para anunciar soporte de ticker; adminapprove ahora recibe companyId|TICKER.
        return "/primebank <balance|deposit <d>|withdraw <d>|transfer <player|uuid> <d>|depositcents <c>|withdrawcents <c>|transfercents <player|uuid> <c>|mycompanybalance|setcompanyname <name|clear>|setcompanyticker <ticker|clear>|adminapprove <companyId|TICKER>|setcashbackbps <bps>|marketlist <shares> <companyId|TICKER>|marketbuy <companyId|TICKER> <shares>|reload>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentTranslation("primebank.usage", getUsage(sender)));
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
            case "setcompanyname": {
                // English: Set or clear the display name for the player's default company.
                // Español: Establecer o limpiar el nombre visible de la empresa por defecto del jugador.
                String companyId = CompanyAccounts.ensureDefault(me);
                if (args.length < 2) {
                    String current = PrimeBankState.get().getCompanyName(companyId);
                    String currentTicker = PrimeBankState.get().getCompanyShortName(companyId);
                    String label = companyLabel(server, companyId);
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.current", label, current == null ? "" : current));
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.short_current", label, currentTicker == null ? "" : currentTicker));
                    break;
                }
                String arg1 = args[1];
                if ("clear".equalsIgnoreCase(arg1)) {
                    PrimeBankState.get().setCompanyName(companyId, null);
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.cleared"));
                } else {
                    String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
                    PrimeBankState.get().setCompanyName(companyId, name);
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.set", name));
                }
                com.primebank.persistence.BankPersistence.saveAllAsync();
                break;
            }
            case "setcompanyticker": {
                // English: Set or clear the short ticker for the player's default company.
                // Español: Establecer o limpiar el ticker corto para la empresa por defecto del jugador.
                String companyId = CompanyAccounts.ensureDefault(me);
                if (args.length < 2) {
                    String label = companyLabel(server, companyId);
                    String currentTicker = PrimeBankState.get().getCompanyShortName(companyId);
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.short_current", label, currentTicker == null ? "" : currentTicker));
                    break;
                }
                String arg1 = args[1];
                if ("clear".equalsIgnoreCase(arg1)) {
                    PrimeBankState.get().setCompanyShortName(companyId, null);
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.short_cleared"));
                } else {
                    String ticker = arg1.trim().toUpperCase();
                    if (ticker.length() < 2 || ticker.length() > 8 || !ticker.matches("[A-Z0-9]+")) {
                        sender.sendMessage(new TextComponentTranslation("primebank.company.apply.bad_short"));
                        break;
                    }
                    PrimeBankState.get().setCompanyShortName(companyId, ticker);
                    sender.sendMessage(new TextComponentTranslation("primebank.company.name.short_set", ticker));
                }
                com.primebank.persistence.BankPersistence.saveAllAsync();
                break;
            }
            case "mycompanybalance": {
                // English: Show the executing player's default company account balance (seller proceeds).
                // Español: Mostrar el saldo de la cuenta de empresa por defecto del jugador (ingresos del vendedor).
                String companyId = CompanyAccounts.ensureDefault(me);
                long bal = PrimeBankState.get().accounts().get(companyId).getBalanceCents();
                String label = companyLabel(server, companyId);
                sender.sendMessage(new TextComponentTranslation("primebank.company.balance", label, Money.formatUsd(bal)));
                break;
            }
            case "deposit": {
                // English: Handle deposits specified in whole dollars by converting to cents.
                // Español: Manejar depósitos especificados en dólares completos convirtiéndolos a centavos.
                if (args.length < 2) { sender.sendMessage(new TextComponentTranslation("primebank.missing_dollars")); return; }
                long dollars = parseLongArg(args[1]);
                if (dollars <= 0) { sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero")); return; }
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
                if (args.length < 2) { sender.sendMessage(new TextComponentTranslation("primebank.missing_cents")); return; }
                long cents = parseLongArg(args[1]);
                if (cents <= 0) { sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero")); return; }
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
                // English: Handle withdrawals in whole dollars, returning currency items worth the amount.
                // Español: Manejar retiros en dólares completos, devolviendo ítems de moneda por el monto.
                if (args.length < 2) { sender.sendMessage(new TextComponentTranslation("primebank.missing_dollars")); return; }
                long dollars = parseLongArg(args[1]);
                if (dollars <= 0) { sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero")); return; }
                long cents = dollarsToCents(dollars);
                Ledger.OpResult r = ledger.withdraw(myAcc, cents);
                String key = r.success ? "primebank.withdraw.ok" : ("primebank.withdraw.error." + r.code);
                if (r.success) {
                    sender.sendMessage(new TextComponentTranslation(key, Money.formatUsd(cents)));
                } else {
                    sender.sendMessage(new TextComponentTranslation(key));
                }
                if (r.success) {
                    CashUtil.giveCurrency(player, cents);
                }
                BankPersistence.saveAllAsync();
                break;
            }
            case "withdrawcents": {
                if (args.length < 2) { sender.sendMessage(new TextComponentTranslation("primebank.missing_cents")); return; }
                long cents = parseLongArg(args[1]);
                Ledger.OpResult r = ledger.withdraw(myAcc, cents);
                String key = r.success ? "primebank.withdraw.ok" : ("primebank.withdraw.error." + r.code);
                if (r.success) {
                    // English: Include withdrawn amount in the success message.
                    // Español: Incluir el monto retirado en el mensaje de éxito.
                    sender.sendMessage(new TextComponentTranslation(key, Money.formatUsd(cents)));
                } else {
                    sender.sendMessage(new TextComponentTranslation(key));
                }
                if (r.success) {
                    // English: Give the withdrawn amount back as currency items to the player's inventory.
                    // Español: Entregar el monto retirado como ítems de moneda al inventario del jugador.
                    CashUtil.giveCurrency(player, cents);
                }
                BankPersistence.saveAllAsync();
                break;
            }
            case "transfer": {
                // English: Transfer whole dollars between personal accounts, applying fees when configured.
                // Español: Transferir dólares completos entre cuentas personales, aplicando comisiones cuando existan.
                if (args.length < 3) { sender.sendMessage(new TextComponentTranslation("primebank.missing_args")); return; }
                UUID to;
                try {
                    to = UUID.fromString(args[1]);
                } catch (IllegalArgumentException ex) {
                    to = resolveUsernameToUuid(server, args[1]);
                }
                String toAcc = PlayerAccounts.ensurePersonal(to);
                long dollars = parseLongArg(args[2]);
                if (dollars <= 0) { sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero")); return; }
                long cents = dollarsToCents(dollars);
                Ledger.TransferResult tr = ledger.transfer(myAcc, toAcc, cents);
                if (tr.success) {
                    if (tr.feeApplied) sender.sendMessage(new TextComponentTranslation("primebank.transfer.ok_fee", Money.formatUsd(tr.feeCents)));
                    else sender.sendMessage(new TextComponentTranslation("primebank.transfer.ok"));
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.transfer.error." + tr.code));
                }
                BankPersistence.saveAllAsync();
                break;
            }
            case "transfercents": {
                if (args.length < 3) { sender.sendMessage(new TextComponentTranslation("primebank.missing_args")); return; }
                // English: Accept username or UUID for the recipient. Keep UUID support.
                // Español: Aceptar nombre de usuario o UUID para el destinatario. Mantener soporte de UUID.
                UUID to;
                try {
                    to = UUID.fromString(args[1]);
                } catch (IllegalArgumentException ex) {
                    to = resolveUsernameToUuid(server, args[1]);
                }
                String toAcc = PlayerAccounts.ensurePersonal(to);
                long cents = parseLongArg(args[2]);
                Ledger.TransferResult tr = ledger.transfer(myAcc, toAcc, cents);
                if (tr.success) {
                    if (tr.feeApplied) sender.sendMessage(new TextComponentTranslation("primebank.transfer.ok_fee", Money.formatUsd(tr.feeCents)));
                    else sender.sendMessage(new TextComponentTranslation("primebank.transfer.ok"));
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.transfer.error." + tr.code));
                }
                BankPersistence.saveAllAsync();
                break;
            }
            case "reload": {
                // English: Reset and reload per-world data (accounts, companies) to avoid cross-world leakage.
                // Español: Reiniciar y recargar datos por mundo (cuentas, empresas) para evitar fugas entre mundos.
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
                // English: Admin-only: approve a company by ID or TICKER and grant majority shares to its owner.
                // Español: Solo admin: aprobar una empresa por ID o TICKER y otorgar acciones mayoritarias a su dueño.
                if (!com.primebank.core.admin.AdminService.isAdmin(me, server, sender)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.not_admin"));
                    break;
                }
                if (args.length < 2) { sender.sendMessage(new TextComponentTranslation("primebank.missing_args")); break; }
                String ident = args[1];
                // English: Accept either canonical id or ticker; resolve to company id.
                // Español: Aceptar id canónico o ticker; resolver al id de la empresa.
                String cid = com.primebank.core.state.PrimeBankState.get().resolveCompanyIdentifier(ident);
                if (cid == null) cid = ident;
                com.primebank.core.company.Company c = com.primebank.core.state.PrimeBankState.get().companies().get(cid);
                if (c == null) { sender.sendMessage(new TextComponentTranslation("primebank.admin.company.not_found")); break; }
                c.approved = true;
                c.approvedAt = System.currentTimeMillis();
                if (c.ownerUuid != null) {
                    String ownerKey = c.ownerUuid.toString();
                    int cur = c.holdings.getOrDefault(ownerKey, 0);
                    if (cur < 101) c.holdings.put(ownerKey, 101);
                }
                com.primebank.persistence.CompanyPersistence.saveCompany(c);
                sender.sendMessage(new TextComponentTranslation("primebank.admin.company.approved", companyLabel(server, cid)));
                break;
            }
            case "setcashbackbps": {
                // English: Admin-only: set global cashback in basis points for POS purchases (credited to buyer from central).
                // Español: Solo admin: establecer cashback global en puntos básicos para compras POS (acreditado al comprador desde el central).
                if (!com.primebank.core.admin.AdminService.isAdmin(me, server, sender)) {
                    sender.sendMessage(new TextComponentTranslation("primebank.admin.not_admin"));
                    break;
                }
                if (args.length < 2) { sender.sendMessage(new TextComponentTranslation("primebank.missing_args")); break; }
                int bps;
                try { bps = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage(new TextComponentTranslation("primebank.error.bad_number", args[1])); break; }
                if (bps < 0) bps = 0;
                com.primebank.core.state.PrimeBankState.get().setGlobalCashbackBps(bps);
                BankPersistence.saveAllAsync();
                sender.sendMessage(new TextComponentTranslation("primebank.admin.cashback.set", bps));
                break;
            }
            case "marketlist": {
                // English: Owner lists shares for sale on primary market.
                // Español: El dueño lista acciones para la venta en mercado primario.
                if (args.length < 3) { sender.sendMessage(new TextComponentTranslation("primebank.missing_args")); break; }
                int shares;
                try { shares = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage(new TextComponentTranslation("primebank.error.bad_number", args[1])); break; }
                if (shares <= 0) { sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero")); break; }
                java.util.UUID owner = me;
                // English: Accept ticker or id for the company argument; now mandatory.
                // Español: Aceptar ticker o id para el argumento de empresa; ahora obligatorio.
                String resolved = com.primebank.core.state.PrimeBankState.get().resolveCompanyIdentifier(args[2]);
                String companyId = resolved != null ? resolved : args[2];
                com.primebank.market.MarketPrimaryService.Result r = com.primebank.market.MarketPrimaryService.get().listShares(owner, companyId, shares);
                if (r.ok) {
                    sender.sendMessage(new TextComponentTranslation("primebank.market.list.ok", shares, companyLabel(server, companyId)));
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.market.list.error." + r.error));
                }
                break;
            }
            case "marketbuy": {
                // English: Buyer purchases shares from company's listed inventory.
                // Español: Comprador adquiere acciones del inventario listado de la empresa.
                if (args.length < 3) { sender.sendMessage(new TextComponentTranslation("primebank.missing_args")); break; }
                String companyIdIn = args[1];
                // English: Accept ticker or id for target company.
                // Español: Aceptar ticker o id para la empresa objetivo.
                String companyId = com.primebank.core.state.PrimeBankState.get().resolveCompanyIdentifier(companyIdIn);
                if (companyId == null) companyId = companyIdIn;
                int shares;
                try { shares = Integer.parseInt(args[2]); } catch (NumberFormatException e) { sender.sendMessage(new TextComponentTranslation("primebank.error.bad_number", args[2])); break; }
                if (shares <= 0) { sender.sendMessage(new TextComponentTranslation("primebank.amount_le_zero")); break; }
                java.util.UUID buyer = me;
                // Precompute display price for feedback (best-effort).
                com.primebank.core.company.Company c = com.primebank.core.state.PrimeBankState.get().companies().get(companyId);
                long pps = (c == null) ? 0L : (c.valuationCurrentCents / 101L);
                long gross = pps * shares;
                com.primebank.market.MarketPrimaryService.Result r = com.primebank.market.MarketPrimaryService.get().buyShares(buyer, companyId, shares);
                if (r.ok) {
                    sender.sendMessage(new TextComponentTranslation("primebank.market.buy.ok", shares, companyLabel(server, companyId), com.primebank.core.Money.formatUsd(pps), com.primebank.core.Money.formatUsd(gross)));
                } else {
                    sender.sendMessage(new TextComponentTranslation("primebank.market.buy.error." + r.error));
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
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        // English: Provide tab-completion for subcommands and usernames for transfercents.
        // Español: Proveer autocompletado para subcomandos y nombres de usuario para transfercents.

        // English: Suggest subcommands when typing the first argument.
        // Español: Sugerir subcomandos al escribir el primer argumento.
        if (args.length <= 1) {
            String[] subs = new String[] { "balance", "deposit", "withdraw", "transfer", "depositcents", "withdrawcents", "transfercents", "mycompanybalance", "setcompanyname", "reload" };
            return CommandBase.getListOfStringsMatchingLastWord(args, subs);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "transfer":
            case "transfercents": {
                if (args.length == 2) {
                    // English: Suggest online player usernames for the recipient.
                    // Español: Sugerir nombres de jugadores en línea para el destinatario.
                    return CommandBase.getListOfStringsMatchingLastWord(args, server.getPlayerList().getOnlinePlayerNames());
                } else if (args.length == 3) {
                    // English: Suggest common dollar or cent amounts depending on subcommand.
                    // Español: Sugerir montos comunes en dólares o centavos según el subcomando.
                    String[] suggestions = sub.equals("transfercents") ? new String[] { "1", "5", "10", "25", "50", "100", "500", "1000" }
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
                    // Español: Sugerir montos comunes en dólares o centavos para los comandos depositar/retirar.
                    String[] suggestions = sub.endsWith("cents") ? new String[] { "1", "5", "10", "25", "50", "100", "500", "1000" }
                            : new String[] { "1", "5", "10", "20", "50", "100" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, suggestions);
                }
                break;
            }
            case "setcompanyname": {
                if (args.length == 2) {
                    String[] opts = new String[] { "clear" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, opts);
                }
                break;
            }
            case "setcompanyticker": {
                if (args.length == 2) {
                    String[] opts = new String[] { "clear" };
                    return CommandBase.getListOfStringsMatchingLastWord(args, opts);
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
            // Español: Clave localizada para error de número inválido; el argumento se inserta con %s.
            throw new CommandException("primebank.error.bad_number", s);
        }
    }

    /*
     English: Resolve a username to a UUID using online players, profile cache, and offline fallback.
     Español: Resolver un nombre de usuario a UUID usando jugadores en línea, caché de perfiles y fallback offline.
    */
    private UUID resolveUsernameToUuid(MinecraftServer server, String username) throws CommandException {
        // 1) Online players
        net.minecraft.entity.player.EntityPlayerMP online = server.getPlayerList().getPlayerByUsername(username);
        if (online != null) return online.getUniqueID();

        // 2) Profile cache (players seen before)
        com.mojang.authlib.GameProfile gp = server.getPlayerProfileCache().getGameProfileForUsername(username);
        if (gp != null && gp.getId() != null) return gp.getId();

        // 3) Offline mode fallback: deterministic offline UUID
        if (!server.isServerInOnlineMode()) {
            return java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        // 4) Not found
        throw new CommandException("primebank.error.player_not_found", username);
    }

    /*
     English: Convert whole dollars to cents, guarding against overflow.
     Español: Convertir dólares enteros a centavos, protegiendo contra overflow.
    */
    private long dollarsToCents(long dollars) throws CommandException {
        try {
            return Math.multiplyExact(dollars, 100L);
        } catch (ArithmeticException ex) {
            throw new CommandException("primebank.error.bad_number", Long.toString(dollars));
        }
    }
}
