# 🏦 PrimeBank

**Un mod completo de banca, economía y mercado de valores para servidores de Minecraft.**

PrimeBank introduce un sistema económico realista que incluye moneda física, cuentas bancarias digitales, tarjetas de crédito, terminales de punto de venta (POS) para tiendas, y un mercado de valores dinámico donde los jugadores pueden listar sus propias empresas.

> [!NOTE]
> Este mod ofrece una alternativa robusta a las economías basadas en plugins, totalmente integrada en el juego con bloques y objetos personalizados.

---

## ✨ Características Principales

- **💰 Banca Personal**: Cada jugador obtiene una cuenta bancaria segura.
- **💵 Moneda Física**: Monedas y billetes que se pueden depositar/retirar ($0.01 a $100).
- **💳 Tarjetas PrimeBank**: Tarjetas seguras para pagos sin efectivo en tiendas de jugadores.
- **🏪 Sistema POS**: Los dueños de tiendas pueden colocar terminales POS para cobrar a los clientes fácilmente.
- **🏢 Sistema de Empresas**: Los jugadores pueden crear empresas, obtener aprobación de admins, y administrar fondos del negocio por separado.
- **📈 Mercado de Valores**: ¡Las empresas aprobadas pueden listar acciones. Los precios de las acciones se actualizan dinámicamente basados en la actividad real de ventas!
- **🔒 Transacciones Seguras**: Reglas de propiedad mayoritaria previenen tomas hostiles de empresas.

---

## 📚 Documentación

Para una guía completa sobre cómo usar el mod, consulta la **Guía de Usuario**:

- [📖 User Guide (English)](docs/USER_GUIDE_EN.md)
- [📖 Guía de Usuario (Español)](docs/USER_GUIDE_ES.md)

---

## 📥 Instalación

1. Instala **Minecraft Forge** para tu versión.
2. Descarga el archivo `PrimeBank-x.x.x.jar`.
3. Coloca el archivo jar en la carpeta `mods` de tu servidor.
4. Reinicia el servidor.

---

## 🛠️ Compilación (Para Desarrolladores)

Para construir PrimeBank desde el código fuente:

### Requisitos
- JDK 8 o superior
- Git

### Pasos de Construcción

1. Clona el repositorio:
   ```bash
   git clone https://github.com/TheL321/PrimeBank.git
   cd PrimeBank
   ```

2. Configura el espacio de trabajo (opcional pero recomendado para IDEs):
   ```bash
   # Windows
   gradlew setupDecompWorkspace
   
   # Linux/Mac
   ./gradlew setupDecompWorkspace
   ```

3. Compila el mod:
   ```bash
   # Windows
   gradlew build
   
   # Linux/Mac
   ./gradlew build
   ```

4. El archivo jar compilado estará en `build/libs/`.

---

## 🤖 Declaración de IA

**Aviso de Transparencia:**
Este mod fue desarrollado con la asistencia de herramientas de Inteligencia Artificial. La IA se utilizó para ayudar a generar estructuras de código, optimizar la lógica, traducir documentación y diseñar características. Todo el código ha sido revisado y probado para asegurar su funcionalidad y seguridad.

---

*PrimeBank - ¡Haciendo la Economía de Minecraft Real!*
