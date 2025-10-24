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
        PrimeBankMod.NETWORK.registerMessage(PacketSetPendingCharge.Handler.class, PacketSetPendingCharge.class, ID++, Side.SERVER);
        // English: Per-POS config packets: open request (C2S), open GUI (S2C), and set price (C2S).
        // Español: Paquetes de configuración por POS: solicitud de apertura (C2S), abrir GUI (S2C) y establecer precio (C2S).
        PrimeBankMod.NETWORK.registerMessage(PacketPosOpenConfigRequest.Handler.class, PacketPosOpenConfigRequest.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketOpenPosConfig.Handler.class, PacketOpenPosConfig.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketPosSetPrice.Handler.class, PacketPosSetPrice.class, ID++, Side.SERVER);
        // English: POS company selection flow.
        // Español: Flujo de selección de empresa para POS.
        PrimeBankMod.NETWORK.registerMessage(PacketOpenPosSelectCompany.Handler.class, PacketOpenPosSelectCompany.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketPosSetCompany.Handler.class, PacketPosSetCompany.class, ID++, Side.SERVER);
        // English: Terminal company selection before setting global POS price via terminal.
        // Español: Selección de empresa en el terminal antes de fijar precio POS global desde el terminal.
        PrimeBankMod.NETWORK.registerMessage(PacketTerminalOpenChargeRequest.Handler.class, PacketTerminalOpenChargeRequest.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketOpenTerminalSelectCompany.Handler.class, PacketOpenTerminalSelectCompany.class, ID++, Side.CLIENT);
        // English: Company application packet (C2S).
        // Español: Paquete de solicitud de empresa (C2S).
        PrimeBankMod.NETWORK.registerMessage(PacketCompanyApply.Handler.class, PacketCompanyApply.class, ID++, Side.SERVER);
        // English: Market details request (C2S) and response (S2C); market buy (C2S).
        // Español: Solicitud de detalles de mercado (C2S) y respuesta (S2C); compra en mercado (C2S).
        PrimeBankMod.NETWORK.registerMessage(PacketMarketDetailsRequest.Handler.class, PacketMarketDetailsRequest.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketMarketDetails.Handler.class, PacketMarketDetails.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketMarketBuy.Handler.class, PacketMarketBuy.class, ID++, Side.SERVER);
    }
}

