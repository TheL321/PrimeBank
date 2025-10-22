package com.primebank.content.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.tileentity.TileEntity;

import com.primebank.PrimeBankMod;
import com.primebank.net.PacketPosChargeInitiate;
import com.primebank.net.PacketPosOpenConfigRequest;
import com.primebank.core.accounts.CompanyAccounts;
import com.primebank.content.items.ItemCard;

/*
 English: POS block placeholder for Phase 2. Currently just a metal block with proper registry and name.
 Español: Bloque POS provisional para la Fase 2. Actualmente es solo un bloque metálico con registro y nombre adecuados.
*/
public class BlockPOSPrimeBank extends Block {
    public BlockPOSPrimeBank() {
        super(Material.IRON);
        setHardness(3.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        // English: Use domain-qualified registry name so blockstates/models resolve (primebank:pos_primebank).
        // Español: Usar nombre de registro con dominio para que se resuelvan blockstates/modelos (primebank:pos_primebank).
        setRegistryName(PrimeBankMod.MODID, "pos_primebank");
        setUnlocalizedName("primebank.pos_primebank");
    }

    @Override
    public boolean hasTileEntity(IBlockState state) { return true; }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) { return new TilePosPrimeBank(); }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // English: Server-side: first right-click links POS to the player's default company if not holding a card (avoids buyers linking by accident).
        // Español: Lado servidor: el primer clic derecho enlaza el POS a la empresa por defecto del jugador si no sostiene una tarjeta (evita que compradores la enlacen por accidente).
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (!(te instanceof TilePosPrimeBank)) return true;
            TilePosPrimeBank t = (TilePosPrimeBank) te;
            if (t.companyId == null) {
                ItemStack heldSrv = playerIn.getHeldItem(hand);
                boolean holdingCard = heldSrv != null && heldSrv.getItem() instanceof ItemCard;
                if (!holdingCard) {
                    String companyId = CompanyAccounts.ensureDefault(playerIn.getUniqueID());
                    // English: Ensure a company record exists in the registry for approvals/market.
                    // Español: Asegurar que exista un registro de empresa en el registro para aprobaciones/mercado.
                    com.primebank.core.state.PrimeBankState.get().companies().ensureDefault(playerIn.getUniqueID());
                    t.companyId = companyId;
                    t.markDirty();
                    // English: Show friendly label: display name > owner's username > raw id.
                    // Español: Mostrar etiqueta amigable: nombre visible > usuario del dueño > id crudo.
                    String label = com.primebank.core.state.PrimeBankState.get().getCompanyName(companyId);
                    if (label == null || label.isEmpty()) {
                        if (companyId.startsWith("c:")) {
                            try {
                                String raw = companyId.substring(2);
                                java.util.UUID owner = java.util.UUID.fromString(raw);
                                net.minecraft.entity.player.EntityPlayerMP online = worldIn.getMinecraftServer().getPlayerList().getPlayerByUUID(owner);
                                if (online != null) label = online.getName();
                                else {
                                    com.mojang.authlib.GameProfile gp = worldIn.getMinecraftServer().getPlayerProfileCache().getProfileByUUID(owner);
                                    if (gp != null && gp.getName() != null) label = gp.getName();
                                }
                            } catch (Exception ignored) {}
                        }
                        if (label == null || label.isEmpty()) label = companyId;
                    }
                    playerIn.sendMessage(new TextComponentTranslation("primebank.pos.linked", label));
                    return true;
                }
            } else if (playerIn.isSneaking()) {
                // English: If already linked, inform the player.
                // Español: Si ya está enlazado, informar al jugador.
                // English: Show friendly label for the already linked company.
                // Español: Mostrar etiqueta amigable para la empresa ya enlazada.
                String label = com.primebank.core.state.PrimeBankState.get().getCompanyName(t.companyId);
                if (label == null || label.isEmpty()) {
                    if (t.companyId != null && t.companyId.startsWith("c:")) {
                        try {
                            String raw = t.companyId.substring(2);
                            java.util.UUID owner = java.util.UUID.fromString(raw);
                            net.minecraft.entity.player.EntityPlayerMP online = worldIn.getMinecraftServer().getPlayerList().getPlayerByUUID(owner);
                            if (online != null) label = online.getName();
                            else {
                                com.mojang.authlib.GameProfile gp = worldIn.getMinecraftServer().getPlayerProfileCache().getProfileByUUID(owner);
                                if (gp != null && gp.getName() != null) label = gp.getName();
                            }
                        } catch (Exception ignored) {}
                    }
                    if (label == null || label.isEmpty()) label = t.companyId;
                }
                playerIn.sendMessage(new TextComponentTranslation("primebank.pos.linked.already", label));
                return true;
            }
        }
        // English: Buyer client-side initiates POS charge when right-clicking while holding a card.
        // Español: Cliente comprador inicia el cobro POS al hacer clic derecho sosteniendo una tarjeta.
        if (worldIn.isRemote) {
            ItemStack held = playerIn.getHeldItem(hand);
            if (held != null && held.getItem() instanceof ItemCard) {
                PrimeBankMod.NETWORK.sendToServer(new PacketPosChargeInitiate(pos));
            } else {
                // English: Not holding a card — request to open the POS config GUI (owner-only validation on server).
                // Español: No sosteniendo una tarjeta — solicitar abrir la GUI de configuración del POS (validación de dueño en el servidor).
                PrimeBankMod.NETWORK.sendToServer(new PacketPosOpenConfigRequest(pos));
            }
            return true;
        }
        return true;
    }
}
