package com.primebank.net;

import net.minecraftforge.fml.relauncher.Side;

import com.primebank.PrimeBankMod;

/*
 English: Network registration helper.
 Español: Ayudante para el registro de mensajes de red.
*/
public final class Net {
    private static int ID = 0;
    private Net() {}

    public static void register() {
        // English: Register C2S POS initiate packet.
        // Español: Registrar paquete C2S de inicio de cobro POS.
        PrimeBankMod.NETWORK.registerMessage(PacketPosChargeInitiate.Handler.class, PacketPosChargeInitiate.class, ID++, Side.SERVER);
    }
}
