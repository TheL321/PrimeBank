package com.primebank.proxy;

import net.minecraft.entity.player.EntityPlayer;

/*
 English: Common proxy for server and client. Handles logic that's safe on both sides.
 Español: Proxy común para servidor y cliente. Maneja lógica segura en ambos lados.
*/
public class CommonProxy {
    
    /*
     English: Open terminal GUI. On server, this does nothing. Client proxy overrides this.
     Español: Abrir GUI del terminal. En el servidor, no hace nada. El proxy cliente sobreescribe esto.
    */
    public void openTerminalGui(EntityPlayer player) {
        // English: No-op on server side / Español: Sin operación en el lado del servidor
    }

    /*
     English: Register client-only network handlers. Server does nothing here.
     Español: Registrar manejadores de red solo de cliente. El servidor no hace nada aquí.
    */
    public void registerClientPackets() {
        // English: No-op on server side / Español: Sin operación en el lado del servidor
    }
}
