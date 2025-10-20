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
        // English: Register S2C POS prompt and C2S POS response packets.
        // Español: Registrar paquetes S2C de aviso POS y C2S de respuesta POS.
        PrimeBankMod.NETWORK.registerMessage(PacketPosPrompt.Handler.class, PacketPosPrompt.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketPosRespond.Handler.class, PacketPosRespond.class, ID++, Side.SERVER);
    }
}
