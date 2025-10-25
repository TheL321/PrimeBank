package com.primebank.net;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.primebank.PrimeBankMod;

/*
 English: Network registration helper.
 Español: Ayudante para el registro de mensajes de red.
*/
public final class Net {
    private static int ID = 0;
    private Net() {}

    /*
     English: No-op handler used on the server when registering client-bound packets to create the class->id mapping without loading client GUI classes.
     Español: Manejador no-op usado en el servidor al registrar paquetes hacia el cliente para crear el mapeo clase->id sin cargar clases GUI del cliente.
    */
    public static class NoopClientHandler<T extends IMessage> implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext ctx) { return null; }
    }

    public static void registerForServer() {
        // English: C2S-only registrations; safe on dedicated server.
        // Español: Registros solo C2S; seguros en servidor dedicado.
        PrimeBankMod.NETWORK.registerMessage(PacketPosChargeInitiate.Handler.class, PacketPosChargeInitiate.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketPosRespond.Handler.class, PacketPosRespond.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketSetPendingCharge.Handler.class, PacketSetPendingCharge.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketPosOpenConfigRequest.Handler.class, PacketPosOpenConfigRequest.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketPosSetPrice.Handler.class, PacketPosSetPrice.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketPosSetCompany.Handler.class, PacketPosSetCompany.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketTerminalOpenChargeRequest.Handler.class, PacketTerminalOpenChargeRequest.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketCompanyApply.Handler.class, PacketCompanyApply.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketMarketDetailsRequest.Handler.class, PacketMarketDetailsRequest.class, ID++, Side.SERVER);
        PrimeBankMod.NETWORK.registerMessage(PacketMarketBuy.Handler.class, PacketMarketBuy.class, ID++, Side.SERVER);

        // English: Also register client-bound packets with a server-safe no-op handler so the server knows their discriminators when sending.
        // Español: También registrar paquetes dirigidos al cliente con un manejador no-op para que el servidor conozca sus discriminadores al enviarlos.
        PrimeBankMod.NETWORK.registerMessage(new NoopClientHandler<PacketPosPrompt>(), PacketPosPrompt.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(new NoopClientHandler<PacketOpenPosConfig>(), PacketOpenPosConfig.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(new NoopClientHandler<PacketOpenPosSelectCompany>(), PacketOpenPosSelectCompany.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(new NoopClientHandler<PacketOpenTerminalSelectCompany>(), PacketOpenTerminalSelectCompany.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(new NoopClientHandler<PacketMarketDetails>(), PacketMarketDetails.class, ID++, Side.CLIENT);
    }

    public static void registerForClient() {
        // English: S2C-only registrations; only call on client to avoid loading GUI classes on server.
        // Español: Registros solo S2C; llamar solo en cliente para evitar cargar clases GUI en el servidor.
        PrimeBankMod.NETWORK.registerMessage(PacketPosPrompt.Handler.class, PacketPosPrompt.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketOpenPosConfig.Handler.class, PacketOpenPosConfig.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketOpenPosSelectCompany.Handler.class, PacketOpenPosSelectCompany.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketOpenTerminalSelectCompany.Handler.class, PacketOpenTerminalSelectCompany.class, ID++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketMarketDetails.Handler.class, PacketMarketDetails.class, ID++, Side.CLIENT);
    }
}


