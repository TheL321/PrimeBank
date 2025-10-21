-------------------------------------------
Source installation information for modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access 
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "unrenamed" MCP source code (aka
srgnames) - this means that you will not be able to read them directly against
normal code.

Source pack installation information:

Standalone source installation
==============================

See the Forge Documentation online for more detailed instructions:
http://mcforge.readthedocs.io/en/latest/gettingstarted/

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: You're left with a choice.
If you prefer to use Eclipse:
1. Run the following command: "gradlew genEclipseRuns" (./gradlew genEclipseRuns if you are on Mac/Linux)
2. Open Eclipse, Import > Existing Gradle Project > Select Folder 
   or run "gradlew eclipse" to generate the project.
(Current Issue)
4. Open Project > Run/Debug Settings > Edit runClient and runServer > Environment
5. Edit MOD_CLASSES to show [modid]%%[Path]; 2 times rather then the generated 4.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: "gradlew genIntellijRuns" (./gradlew genIntellijRuns if you are on Mac/Linux)
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can run "gradlew --refresh-dependencies" to refresh the local cache. "gradlew clean" to reset everything {this does not affect your code} and then start the processs again.

Should it still not work, 
Refer to #ForgeGradle on EsperNet for more information about the gradle environment.
or the Forge Project Discord discord.gg/UvedJ9m

Forge source installation
=========================
MinecraftForge ships with this code and installs it as part of the forge
installation process, no further action is required on your part.

LexManos' Install Video
=======================
https://www.youtube.com/watch?v=8VEdtQLuLO0&feature=youtu.be

For more details update more often refer to the Forge Forums:
http://www.minecraftforge.net/forum/index.php/topic,14048.0.html

====================================
PrimeBank Usage / Uso de PrimeBank
====================================

English: The PrimeBank mod provides simple banking commands and currency items. Amounts are in cents (long). Commands support tab-completion for subcommands, usernames (online), and common amounts.
Español: El mod PrimeBank provee comandos bancarios simples y objetos de moneda. Los montos están en centavos (long). Los comandos soportan autocompletado para subcomandos, usuarios (en línea) y montos comunes.

Commands / Comandos:
- /primebank balance
  English: Show your account balance.
  Español: Muestra tu saldo de cuenta.

- /primebank depositcents <cents>
  English: Converts currency items from your inventory into balance. If you lack enough currency items to cover the amount, the command fails.
  Español: Convierte objetos de moneda de tu inventario en saldo. Si no tienes suficiente moneda para cubrir el monto, el comando falla.

- /primebank withdrawcents <cents>
  English: Withdraws currency items into your inventory using denominations. If inventory is full, overflow is dropped near you.
  Español: Retira objetos de moneda a tu inventario usando denominaciones. Si el inventario está lleno, el excedente se suelta cerca de ti.

- /primebank transfercents <username|uuid> <cents>
  English: Transfers cents to another player by username (online) or UUID. Tab-completion suggests online usernames. Offline UUIDs are supported.
  Español: Transfiere centavos a otro jugador por nombre (en línea) o UUID. El autocompletado sugiere nombres en línea. Se soportan UUIDs offline.

- /primebank reload
  English: Reloads configuration/state as supported by the server.
  Español: Recarga configuración/estado según lo soportado por el servidor.

Notes / Notas:
- English: All amounts are integers in cents to avoid float rounding.
  Español: Todos los montos son enteros en centavos para evitar redondeos de flotantes.
- English: Currency items appear in the PrimeBank creative tab.
  Español: Los objetos de moneda aparecen en la pestaña creativa de PrimeBank.
