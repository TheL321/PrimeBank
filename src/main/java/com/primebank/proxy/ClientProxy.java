package com.primebank.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.primebank.client.gui.GuiTerminalMenu;
import com.primebank.net.Net;

/*
 English: Client-side proxy. Handles GUI opening and other client-only logic.
 Español: Proxy del lado del cliente. Maneja la apertura de GUIs y otra lógica exclusiva del cliente.
*/
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    
    @Override
    public void openTerminalGui(EntityPlayer player) {
        /*
         English: Open the terminal menu GUI on the client.
         Español: Abrir la GUI del menú del terminal en el cliente.
        */
        Minecraft.getMinecraft().displayGuiScreen(new GuiTerminalMenu());
    }

    @Override
    public void registerClientPackets() {
        /*
         English: Register client-only packet handlers (S2C) on the client side.
         Español: Registrar manejadores de paquetes solo de cliente (S2C) en el lado del cliente.
        */
        Net.registerForClient();
    }
}
