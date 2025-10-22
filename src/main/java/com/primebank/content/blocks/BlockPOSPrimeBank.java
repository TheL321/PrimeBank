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
                    t.companyId = companyId;
                    t.markDirty();
                    playerIn.sendMessage(new TextComponentTranslation("primebank.pos.linked", companyId));
                    return true;
                }
            } else if (playerIn.isSneaking()) {
                // English: If already linked, inform the player.
                // Español: Si ya está enlazado, informar al jugador.
                playerIn.sendMessage(new TextComponentTranslation("primebank.pos.linked.already", t.companyId));
                return true;
            }
        }
        // English: Buyer client-side initiates POS charge when right-clicking while holding a card.
        // Español: Cliente comprador inicia el cobro POS al hacer clic derecho sosteniendo una tarjeta.
        if (worldIn.isRemote) {
            ItemStack held = playerIn.getHeldItem(hand);
            if (held != null && held.getItem() instanceof ItemCard) {
                PrimeBankMod.NETWORK.sendToServer(new PacketPosChargeInitiate(pos));
            }
            return true;
        }
        return true;
    }
}
