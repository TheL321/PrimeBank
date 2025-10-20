package com.primebank.core.time;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/*
 English: Time utilities anchored to the server time zone. Client UIs may convert to local time.
 Español: Utilidades de tiempo ancladas a la zona horaria del servidor. Las UIs de cliente pueden convertir a hora local.
*/
public final class TimeService {
    private static ZoneId serverZone = ZoneId.systemDefault();

    private TimeService() {}

    /*
     English: Get the current server zone.
     Español: Obtener la zona horaria del servidor.
    */
    public static ZoneId serverZone() {
        return serverZone;
    }

    /*
     English: Current server time.
     Español: Hora actual del servidor.
    */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(serverZone);
    }

    /*
     English: Set server zone if provided.
     Español: Establecer la zona del servidor si se provee.
    */
    public static void setServerZone(ZoneId zone) {
        if (zone != null) serverZone = zone;
    }
}
