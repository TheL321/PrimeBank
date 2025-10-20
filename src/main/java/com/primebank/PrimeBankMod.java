package com.primebank;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.primebank.core.config.PrimeBankConfig;
import com.primebank.core.state.PrimeBankState;
import com.primebank.commands.CommandPrimeBank;
import com.primebank.persistence.BankPersistence;
import com.primebank.persistence.PersistencePaths;
import java.io.File;

/*
 English: PrimeBank mod entry point. Initializes logger, network channel, and loads default config.
 Español: Punto de entrada del mod PrimeBank. Inicializa el logger, el canal de red y carga la configuración por defecto.
*/
@Mod(modid = PrimeBankMod.MODID, name = PrimeBankMod.NAME, version = PrimeBankMod.VERSION, acceptedMinecraftVersions = "[1.12.2]")
public class PrimeBankMod {
    public static final String MODID = "primebank";
    public static final String NAME = "PrimeBank";
    public static final String VERSION = "1.0";

    public static SimpleNetworkWrapper NETWORK;
    public static Logger LOGGER = LogManager.getLogger(NAME);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        /*
         English: Pre-initialization phase. Set up logging and networking, and apply default settings.
         Español: Fase de preinicialización. Configura el registro y la red, y aplica ajustes por defecto.
        */
        LOGGER = event.getModLog();
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        PrimeBankConfig.reloadDefaults();
        /*
         English: Initialize global state (registries, central bank account).
         Español: Inicializa el estado global (registros, cuenta del banco central).
        */
        PrimeBankState.get().init();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        /*
         English: Initialization phase. Reserved for registries, GUIs, and packet registration in later phases.
         Español: Fase de inicialización. Reservado para registros, interfaces y paquetes en fases posteriores.
        */
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        /*
         English: Server starting: set world directory, load snapshots, and register /primebank command.
         Español: Inicio del servidor: establece directorio del mundo, carga snapshots y registra el comando /primebank.
        */
        File worldDir = event.getServer().getEntityWorld().getSaveHandler().getWorldDirectory();
        PersistencePaths.setWorldDir(worldDir);
        BankPersistence.loadAll();
        event.registerServerCommand(new CommandPrimeBank());
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        /*
         English: Server stopping: save snapshot to disk.
         Español: Parada del servidor: guarda snapshot en disco.
        */
        BankPersistence.saveAllBlocking();
    }
}
