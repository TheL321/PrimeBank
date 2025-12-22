package com.primebank.net;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.FMLCommonHandler;
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

    /*
     * English: No-Op handler for S2C packets on the server side. This class is safe
     * to
     * load on the server because it doesn't reference any client-only classes.
     * Español: Manejador sin operación para paquetes S2C en el lado del servidor.
     * Esta
     * clase es segura de cargar en el servidor porque no referencia clases
     * exclusivas del cliente.
     */
    public static class NoOpClientHandler<REQ extends IMessage> implements IMessageHandler<REQ, IMessage> {
        @Override
        public IMessage onMessage(REQ message, MessageContext ctx) {
            return null; // No-op
        }
    }

    public static void init() {
        ID = 0; // Reset ID on init to be safe, though usually called once.

        // English: Register all packets in a deterministic order.
        // Español: Registrar todos los paquetes en un orden determinista.

        // C2S Packets (Always register real handler on server side).
        registerC2S(PacketPosChargeInitiate.Handler.class, PacketPosChargeInitiate.class);
        registerC2S(PacketPosRespond.Handler.class, PacketPosRespond.class);
        registerC2S(PacketSetPendingCharge.Handler.class, PacketSetPendingCharge.class);
        registerC2S(PacketPosOpenConfigRequest.Handler.class, PacketPosOpenConfigRequest.class);
        registerC2S(PacketPosSetPrice.Handler.class, PacketPosSetPrice.class);
        registerC2S(PacketPosSetCompany.Handler.class, PacketPosSetCompany.class);
        registerC2S(PacketTerminalOpenChargeRequest.Handler.class, PacketTerminalOpenChargeRequest.class);
        registerC2S(PacketCompanyApply.Handler.class, PacketCompanyApply.class);
        registerC2S(PacketMarketDetailsRequest.Handler.class, PacketMarketDetailsRequest.class);
        registerC2S(PacketMarketBuy.Handler.class, PacketMarketBuy.class);
        registerC2S(PacketMarketHomeRequest.Handler.class, PacketMarketHomeRequest.class);

        // S2C Packets (Register Real handler on Client, No-Op on Server).
        // English: On dedicated server, the real handler classes reference GuiScreen
        // which
        // doesn't exist. We must use NoOpClientHandler on server to avoid
        // ClassNotFound.
        // Español: En servidor dedicado, las clases de handler reales referencian
        // GuiScreen
        // que no existe. Debemos usar NoOpClientHandler en servidor para evitar
        // ClassNotFound.
        registerS2C(PacketPosPrompt.class);
        registerS2C(PacketOpenPosConfig.class);
        registerS2C(PacketOpenPosSelectCompany.class);
        registerS2C(PacketOpenTerminalSelectCompany.class);
        registerS2C(PacketMarketDetails.class);
        registerS2C(PacketMarketHomeList.class);
    }

    private static <REQ extends IMessage, REPLY extends IMessage> void registerC2S(
            Class<? extends IMessageHandler<REQ, REPLY>> handler, Class<REQ> messageType) {
        PrimeBankMod.NETWORK.registerMessage(handler, messageType, ID++, Side.SERVER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <REQ extends IMessage> void registerS2C(Class<REQ> messageType) {
        // English: On SERVER physical side, register NoOpClientHandler. On CLIENT,
        // register nothing here;
        // the client will register the real handlers via PROXY.registerClientPackets().
        // Actually, we need to register on BOTH sides to ensure the ID mapping is
        // consistent.
        // On SERVER: Register with NoOpClientHandler for Side.CLIENT (so ID is mapped).
        // On CLIENT: This same code runs and will also register with NoOpClientHandler.
        // Then
        // ClientProxy.registerClientPackets() can re-register the real handlers? No,
        // that breaks ID.
        //
        // CORRECT APPROACH: Detect physical side and register appropriate handler.
        // Español: En el lado físico SERVER, registrar NoOpClientHandler. En CLIENT,
        // registrar el real aquí.
        // Necesitamos registrar en AMBOS lados para asegurar que el mapeo de ID sea
        // consistente.
        Side physicalSide = FMLCommonHandler.instance().getSide();
        if (physicalSide == Side.SERVER) {
            // English: On dedicated server, use NoOp handler to avoid loading client
            // classes.
            // Español: En servidor dedicado, usar handler NoOp para evitar cargar clases de
            // cliente.
            PrimeBankMod.NETWORK.registerMessage(new NoOpClientHandler<REQ>(), messageType, ID++, Side.CLIENT);
        } else {
            // English: On client (including integrated server), register real handler.
            // The real handler classes will be loaded via reflection by the proxy.
            // Español: En cliente (incluyendo servidor integrado), registrar handler real.
            // Las clases de handler reales serán cargadas por reflexión vía el proxy.
            ID++; // Increment ID to keep mapping consistent, but let ClientProxy do the actual
                  // registration.
        }
    }

    /*
     * English: Client-side registration of S2C packet handlers. Called by
     * ClientProxy.
     * Español: Registro del lado cliente de handlers S2C. Llamado por ClientProxy.
     */
    public static void registerClientS2CHandlers() {
        // English: These IDs must match the server-side registration order.
        // The S2C packets start after all C2S packets.
        // Español: Estos IDs deben coincidir con el orden de registro del servidor.
        // Los paquetes S2C comienzan después de todos los C2S.
        int s2cStartId = 11; // 11 C2S packets registered (IDs 0-10)
        PrimeBankMod.NETWORK.registerMessage(PacketPosPrompt.Handler.class, PacketPosPrompt.class, s2cStartId++,
                Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketOpenPosConfig.Handler.class, PacketOpenPosConfig.class, s2cStartId++,
                Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketOpenPosSelectCompany.Handler.class, PacketOpenPosSelectCompany.class,
                s2cStartId++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketOpenTerminalSelectCompany.Handler.class,
                PacketOpenTerminalSelectCompany.class, s2cStartId++, Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketMarketDetails.Handler.class, PacketMarketDetails.class, s2cStartId++,
                Side.CLIENT);
        PrimeBankMod.NETWORK.registerMessage(PacketMarketHomeList.Handler.class, PacketMarketHomeList.class,
                s2cStartId++, Side.CLIENT);
    }
}
