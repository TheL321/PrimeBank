package com.primebank.content.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import com.primebank.core.Money;
import com.primebank.core.accounts.PlayerAccounts;
import com.primebank.core.state.PrimeBankState;
import com.primebank.core.ledger.Ledger;
import com.primebank.content.items.CashUtil;
import com.primebank.PrimeBankMod;

/*
 English: PrimeBank Terminal block placeholder. Shows balance on use.
 Español: Bloque Terminal de PrimeBank provisional. Muestra el saldo al usarse.
*/
public class BlockTerminalPrimeBank extends Block {
    public BlockTerminalPrimeBank() {
        super(Material.IRON);
        setHardness(3.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        // English: Use domain-qualified registry name so blockstates/models resolve (primebank:terminal_primebank).
        // Español: Usar nombre de registro con dominio para que se resuelvan blockstates/modelos (primebank:terminal_primebank).
        setRegistryName(PrimeBankMod.MODID, "terminal_primebank");
        setUnlocalizedName("primebank.terminal_primebank");
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            String accId = PlayerAccounts.ensurePersonal(playerIn.getUniqueID());
            if (playerIn.isSneaking()) {
                long collected = CashUtil.collectAllCurrency((net.minecraft.entity.player.EntityPlayerMP) playerIn);
                if (collected > 0) {
                    Ledger ledger = new Ledger(PrimeBankState.get().accounts());
                    ledger.deposit(accId, collected);
                    long bal = PrimeBankState.get().accounts().get(accId).getBalanceCents();
                    playerIn.sendMessage(new TextComponentTranslation("primebank.terminal.deposit_added", Money.formatUsd(collected), Money.formatUsd(bal)));
                } else {
                    playerIn.sendMessage(new TextComponentTranslation("primebank.terminal.no_cash"));
                }
            } else {
                long bal = PrimeBankState.get().accounts().get(accId).getBalanceCents();
                playerIn.sendMessage(new TextComponentTranslation("primebank.terminal.balance", Money.formatUsd(bal)));
                playerIn.sendMessage(new TextComponentTranslation("primebank.terminal.prompt_deposit"));
            }
        } else {
            // English: Client-side: open terminal menu when not sneaking.
            // Español: Lado cliente: abrir menú del terminal cuando no está agachado.
            if (!playerIn.isSneaking()) {
                PrimeBankMod.PROXY.openTerminalGui(playerIn);
            }
        }
        return true;
    }
}
