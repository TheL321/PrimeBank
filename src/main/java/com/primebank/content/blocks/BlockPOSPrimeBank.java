package com.primebank.content.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import com.primebank.PrimeBankMod;
import com.primebank.net.PacketPosChargeInitiate;

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
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // English: Skeleton interaction. Client sends a C2S packet to initiate a POS charge of $1.00 as placeholder.
        // Español: Interacción esqueleto. El cliente envía un paquete C2S para iniciar un cobro POS de $1.00 como placeholder.
        if (worldIn.isRemote) {
            long cents = 100L;
            PrimeBankMod.NETWORK.sendToServer(new PacketPosChargeInitiate(cents));
        } else {
            // English: Server acknowledges for now.
            // Español: El servidor lo reconoce por ahora.
            playerIn.sendMessage(new TextComponentTranslation("primebank.pos.init.sent"));
        }
        return true;
    }
}
