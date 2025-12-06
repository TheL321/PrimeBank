# Guía de Usuario de PrimeBank

> Un sistema completo de banca, mercado y pagos para servidores de Minecraft

---

## 📖 Tabla de Contenidos

1. [Introducción](#-introducción)
2. [Primeros Pasos](#-primeros-pasos)
3. [Para Jugadores](#-para-jugadores)
   - [Tu Cuenta Bancaria](#tu-cuenta-bancaria)
   - [Usando Dinero Físico](#usando-dinero-físico)
   - [La Tarjeta PrimeBank](#la-tarjeta-primebank)
   - [Usando la Terminal](#usando-la-terminal-primebank)
   - [Haciendo Pagos con POS](#haciendo-pagos-con-pos)
   - [Enviando Dinero a Otros Jugadores](#enviando-dinero-a-otros-jugadores)
   - [Historial de Transacciones](#historial-de-transacciones)
4. [Para Dueños de Negocios](#-para-dueños-de-negocios)
   - [Creando una Empresa](#creando-una-empresa)
   - [Administrando tu Empresa](#administrando-tu-empresa)
   - [Configurando una Terminal POS](#configurando-una-terminal-pos)
   - [Recibiendo Pagos](#recibiendo-pagos)
   - [El Mercado de Valores](#el-mercado-de-valores)
5. [Para Administradores de Servidor](#-para-administradores-de-servidor)
   - [Instalación](#instalación)
   - [Configuración](#configuración)
   - [Comandos de Admin](#comandos-de-admin)
   - [Integración con Discord](#integración-con-discord)
6. [Referencia de Comandos](#-referencia-de-comandos)
7. [Entendiendo las Comisiones](#-entendiendo-las-comisiones)
8. [Consejos y Mejores Prácticas](#-consejos-y-mejores-prácticas)
9. [Preguntas Frecuentes](#-preguntas-frecuentes)

---

## 🌟 Introducción

**PrimeBank** es un mod de sistema económico completo para servidores de Minecraft que añade:

- 💰 **Cuentas bancarias personales** para cada jugador
- 💵 **Moneda física** (monedas y billetes) que puedes tener e intercambiar
- 💳 **Tarjetas PrimeBank** para pagos sin efectivo
- 🏪 **Terminales POS (Punto de Venta)** para tiendas y negocios
- 🏢 **Empresas** que los jugadores pueden crear y administrar
- 📈 **Un Mercado de Valores** donde los jugadores pueden comprar y vender acciones

¡Piénsalo como tener un banco real dentro de Minecraft! Puedes ahorrar dinero, pagar por cosas, manejar un negocio e incluso invertir en las empresas de otros jugadores.

---

## 🚀 Primeros Pasos

### Lo Que Recibes al Empezar

Cuando te unes por primera vez a un servidor con PrimeBank, automáticamente obtienes:

1. **Una Cuenta Bancaria Personal** - Tu dinero se guarda aquí de forma segura
2. **Saldo Inicial** - El servidor puede darte dinero inicial (depende de la configuración del servidor)

### Lo Básico

- Tu **saldo** se guarda en **centavos** (100 centavos = $1.00)
- Puedes tener dinero físico (monedas/billetes) en tu inventario O dinero digital en tu cuenta
- Para pagar en tiendas, necesitas una **Tarjeta PrimeBank**

---

## 👤 Para Jugadores

### Tu Cuenta Bancaria

Tu cuenta bancaria es donde tu dinero digital se guarda de forma segura. A diferencia de las monedas físicas en tu inventario, el dinero en tu cuenta:

- ✅ No se perderá cuando mueras
- ✅ No puede ser robado por otros jugadores
- ✅ Se puede usar para pagos con tarjeta
- ✅ Mantiene un historial de tus transacciones

#### Consultando tu Saldo

**Comando:** `/pb balance` o `/primebank balance`

Esto te muestra cuánto dinero tienes en tu cuenta.

**Ejemplo de salida:**
```
Saldo: $150.25
```

---

### Usando Dinero Físico

PrimeBank incluye monedas y billetes físicos que puedes encontrar, intercambiar o recibir:

| Ítem | Valor |
|------|-------|
| Moneda de 1 Centavo | $0.01 |
| Moneda de 5 Centavos | $0.05 |
| Moneda de 10 Centavos | $0.10 |
| Moneda de 25 Centavos | $0.25 |
| Moneda de 50 Centavos | $0.50 |
| Billete de $1 | $1.00 |
| Billete de $5 | $5.00 |
| Billete de $10 | $10.00 |
| Billete de $20 | $20.00 |
| Billete de $50 | $50.00 |
| Billete de $100 | $100.00 |

#### Depositando Dinero Físico

Tienes dos opciones:

**Opción 1: Usando una Terminal (Recomendado)**
1. Encuentra un bloque de **Terminal PrimeBank**
2. **Agáchate + Clic derecho** sobre ella
3. ¡Todo el dinero físico en tu inventario se depositará automáticamente!

**Opción 2: Usando Comandos**
```
/pb deposit <cantidad>
```
Ejemplo: `/pb deposit 50` deposita $50.00 de moneda física de tu inventario.

#### Retirando Dinero como Moneda Física

```
/pb withdraw <cantidad>
```
Ejemplo: `/pb withdraw 25` te da $25.00 en monedas/billetes.

> [!TIP]
> Al retirar, el mod automáticamente te da la combinación más eficiente de billetes y monedas.

---

### La Tarjeta PrimeBank

La **Tarjeta PrimeBank** es tu llave para pagos sin efectivo. La necesitas para pagar en terminales POS.

#### Cómo Funciona

1. **Obtén una Tarjeta** - Las tarjetas pueden ser fabricadas o dadas por el servidor
2. **El Primer Uso la Vincula** - La primera vez que uses una tarjeta, se vincula a tu cuenta
3. **Solo Tú Puedes Usarla** - Una vez vinculada, solo tú puedes pagar con esa tarjeta específica

#### Información de la Tarjeta

Cuando pasas el cursor sobre tu tarjeta, verás:
- **Dueño** - A quién pertenece la tarjeta (o "Sin vincular" si es nueva)
- **ID de Tarjeta** - Un identificador único para esta tarjeta específica

> [!IMPORTANT]
> Si alguien te da su tarjeta, no podrás usarla para pagos - ¡las tarjetas son personales!

---

### Usando la Terminal PrimeBank

La **Terminal** es un bloque especial que sirve como tu conexión a tu cuenta bancaria.

#### Qué Puedes Hacer en una Terminal

| Acción | Cómo Hacerla |
|--------|--------------|
| **Ver Saldo** | Clic derecho en la terminal |
| **Depositar Todo el Efectivo** | Agáchate + Clic derecho |
| **Abrir Menú** | Clic derecho (abre GUI con más opciones) |

#### Opciones del Menú de la Terminal

Cuando haces clic derecho en una terminal (sin agacharte), aparece un menú:

1. **Cobro del Comerciante** - Para que dueños de negocios cobren a clientes
2. **Solicitar Empresa** - Inicia tu propio negocio
3. **Abrir Mercado** - Ver y comerciar acciones de empresas

---

### Haciendo Pagos con POS

Las terminales POS (Punto de Venta) son la forma en que las tiendas cobran a los clientes.

#### Cómo Pagar en un POS

1. **Sostén tu Tarjeta PrimeBank** en tu mano
2. **Clic derecho** en la terminal POS
3. Aparecerá una ventana de confirmación mostrando:
   - El nombre del comerciante
   - La cantidad a pagar
4. Haz clic en **Confirmar** para pagar o **Cancelar** para rechazar

#### Qué Sucede Cuando Pagas

- El dinero se deduce de tu cuenta bancaria
- El comerciante recibe el pago (menos una pequeña comisión)
- Puedes recibir **Cashback** (si está habilitado por el servidor)
- Tanto tú como el comerciante reciben notificaciones

> [!TIP]
> ¡Siempre verifica la cantidad antes de confirmar! Asegúrate de que estás pagando el precio correcto.

---

### Enviando Dinero a Otros Jugadores

Puedes enviar dinero directamente a otros jugadores sin encontrarte con ellos.

#### Comando de Transferencia

```
/pb transfer <nombre_jugador> <cantidad>
```

**Ejemplo:**
```
/pb transfer Steve 100
```
Esto envía $100.00 a Steve.

#### Lo Que Debes Saber

- El dinero viene de tu cuenta bancaria (no moneda física)
- El destinatario recibe una notificación
- Puede haber una pequeña comisión de transferencia (depende de la configuración del servidor)
- Tanto el remitente como el receptor ven la transacción en su historial

---

### Historial de Transacciones

¡Mantén un registro de tu actividad financiera!

**Comando:** `/pb history`

Esto muestra tus últimas 20 transacciones, incluyendo:

| Tipo de Transacción | Descripción |
|---------------------|-------------|
| **Depósito** | Dinero añadido a tu cuenta |
| **Retiro** | Dinero sacado como moneda física |
| **Transferencia Enviada** | Dinero que enviaste a otro jugador |
| **Transferencia Recibida** | Dinero que recibiste de otro jugador |
| **Compra Mercado** | Acciones que compraste |
| **Venta Mercado** | Acciones que vendiste |
| **Pago POS** | Pago que hiciste en una tienda |
| **Cobro POS** | Pago que recibiste en tu tienda |
| **Cashback Recibido** | Dinero de bonificación por pagos con tarjeta |
| **Comisión** | Comisiones de procesamiento |

---

## 🏢 Para Dueños de Negocios

### Creando una Empresa

¿Quieres empezar tu propio negocio? ¡Así es cómo!

#### Paso 1: Solicitar una Empresa

1. Ve a una **Terminal PrimeBank**
2. Haz clic derecho para abrir el menú
3. Selecciona **"Solicitar Empresa"**
4. Completa:
   - **Nombre de la Empresa** - El nombre completo de tu negocio
   - **Ticker** - Un código corto (2-8 letras/números, como "TIENDA" o "GRANJA1")
   - **Descripción** - Descripción opcional de lo que haces

#### Paso 2: Esperar Aprobación

- Un administrador del servidor debe aprobar tu empresa
- Una vez aprobada, te conviertes en el dueño de la empresa
- Tu empresa obtiene su propia cuenta bancaria

> [!NOTE]
> El proceso de aprobación ayuda a prevenir spam y asegura negocios de calidad en el servidor.

---

### Administrando tu Empresa

Una vez que tu empresa está aprobada, tienes varios comandos de administración:

#### Viendo tus Empresas

```
/pb mycompanies
```
Muestra todas las empresas que posees y sus saldos.

#### Verificando el Saldo de la Empresa

```
/pb mycompanybalance
```
Muestra el saldo de tu empresa principal.

#### Retirando de tu Empresa

```
/pb companywithdraw <empresa> <cantidad>
```

**Ejemplo:**
```
/pb companywithdraw TIENDA 500
```
Retira $500 de tu empresa "TIENDA" a tu cuenta personal.

#### Cambiando el Nombre de la Empresa

```
/pb setcompanyname <nuevo nombre>
```
o para borrarlo:
```
/pb setcompanyname clear
```

#### Cambiando el Ticker de la Empresa

```
/pb setcompanyticker <TICKER>
```
El ticker debe tener 2-8 caracteres, solo letras y números, en mayúsculas.

---

### Configurando una Terminal POS

Las terminales POS permiten que los clientes paguen en tu tienda.

#### Paso 1: Coloca el Bloque POS

Coloca un bloque **POS PrimeBank** donde los clientes puedan alcanzarlo.

#### Paso 2: Vincúlalo a tu Empresa

1. Asegúrate de que NO estás sosteniendo una tarjeta
2. Haz clic derecho en el bloque POS
3. Selecciona tu empresa de la lista

#### Paso 3: Establece el Precio

1. Agáchate + Clic derecho en el POS
2. Ingresa el precio que quieras cobrar
3. Haz clic en OK

¡Ahora los clientes pueden pagar haciendo clic derecho con su tarjeta!

> [!TIP]
> Puedes tener múltiples terminales POS para diferentes productos a diferentes precios, todos vinculados a la misma empresa.

---

### Recibiendo Pagos

Cuando un cliente paga en tu POS:

1. ✅ El dinero va a tu **cuenta de empresa** (no personal)
2. ✅ Recibes una notificación en el chat
3. ✅ Una pequeña comisión va al Banco Central

#### Estructura de Comisiones

| Tipo de Comisión | Cantidad | Quién Paga |
|------------------|----------|------------|
| Comisión POS | 5% | Se deduce del pago al comerciante |

**Ejemplo:** El cliente paga $100
- Tú recibes: $95 (después de 5% de comisión)
- El Banco Central recibe: $5

---

### El Mercado de Valores

¡PrimeBank incluye un mercado de valores donde se pueden comerciar acciones de empresas!

#### Entendiendo las Acciones

Cuando tu empresa es aprobada, recibes **101 acciones** de tu empresa. Estas representan la propiedad:

- 🔒 **Debes mantener al menos 51 acciones** (propiedad mayoritaria)
- 📊 Puedes vender hasta 50 acciones a inversores
- 💰 El precio de la acción se basa en la **valoración** de tu empresa

#### Abriendo el Mercado

1. Ve a una Terminal
2. Haz clic derecho para abrir el menú
3. Selecciona **"Abrir Mercado"**

#### Listando Acciones para la Venta

```
/pb marketlist <número de acciones> <empresa>
```

**Ejemplo:**
```
/pb marketlist 10 TIENDA
```
Lista 10 acciones de tu empresa "TIENDA" para la venta.

> [!WARNING]
> El comercio está bloqueado hasta que tu empresa tenga su primera valoración (basada en la actividad de ventas).

#### Cómo Funciona la Valoración

El valor de tu empresa (y el precio de la acción) se calcula basándose en:

- 📊 Ventas semanales a través de terminales POS
- 📈 Un promedio móvil de los últimos 7 días
- 🎯 La fórmula crea un precio de mercado justo

**Precio por Acción = Valoración de la Empresa ÷ 101**

#### Comprando Acciones

```
/pb marketbuy <empresa> <acciones>
```

**Ejemplo:**
```
/pb marketbuy GRANJA1 5
```
Compra 5 acciones de la empresa "GRANJA1".

#### Comisiones del Mercado

| Comisión | Cantidad | Quién Paga |
|----------|----------|------------|
| Comisión Comprador | 2.5% | Se añade al precio de compra |
| Comisión Vendedor | 5% | Se deduce de los ingresos de la venta |

---

## 🔧 Para Administradores de Servidor

### Instalación

1. **Descarga** el archivo JAR del mod PrimeBank
2. **Colócalo** en la carpeta `mods/` de tu servidor
3. **Reinicia** tu servidor
4. **Configura** las opciones según sea necesario (ver abajo)

#### Requisitos

- Minecraft Forge (versión compatible con el mod)
- Java 8 o superior

### Configuración

Ubicación del archivo de configuración:
```
<servidor>/serverconfig/primebank.toml
```

#### Configuraciones Disponibles

| Configuración | Descripción | Por Defecto |
|---------------|-------------|-------------|
| `discord_webhook_url` | Webhook de Discord para logs de transacciones | vacío (deshabilitado) |

#### Constantes de Comisiones (En el Código)

Estas están actualmente establecidas en el código y pueden volverse configurables en futuras versiones:

| Comisión | Valor | Descripción |
|----------|-------|-------------|
| `MARKET_BUYER_FEE_BPS` | 250 (2.5%) | Comisión en compras de acciones |
| `MARKET_SELLER_FEE_BPS` | 500 (5%) | Comisión en ventas de acciones |
| `POS_BANK_FEE_BPS` | 500 (5%) | Comisión en transacciones POS |

### Comandos de Admin

Todos los comandos de admin requieren **nivel de OP 2** o superior.

#### Aprobando Empresas

```
/pb adminapprove <empresa>
```
Aprueba una solicitud de empresa pendiente.

#### Estableciendo Cashback Global

```
/pb cashback <bps>
```
Establece el porcentaje de cashback en puntos básicos (100 bps = 1%).

**Ejemplo:** `/pb cashback 100` da 1% de cashback en todos los pagos con tarjeta.

#### Saldo del Banco Central

```
/pb centralbalance
```
Muestra las comisiones acumuladas del Banco Central.

#### Retirar del Banco Central

```
/pb centralwithdraw <cantidad>
```
Retira fondos del Banco Central.

#### Recargar Configuración

```
/pb reload
```
Recarga la configuración desde el disco.

### Integración con Discord

PrimeBank puede enviar logs de transacciones a un canal de Discord vía webhook.

#### Configuración

1. Crea un webhook en tu servidor de Discord
2. Añade a `serverconfig/primebank.toml`:
```toml
discord_webhook_url = "https://discord.com/api/webhooks/TU_URL_DE_WEBHOOK"
```
3. Recarga el mod o reinicia el servidor

#### Qué Se Registra

- Todas las transferencias entre jugadores
- Transacciones POS
- Operaciones del mercado
- Transacciones grandes

---

## 📋 Referencia de Comandos

### Comandos de Jugador

| Comando | Descripción |
|---------|-------------|
| `/pb balance` | Ver el saldo de tu cuenta |
| `/pb history` | Ver últimas 20 transacciones |
| `/pb deposit <cantidad>` | Depositar moneda física |
| `/pb withdraw <cantidad>` | Retirar como moneda física |
| `/pb transfer <jugador> <cantidad>` | Enviar dinero a un jugador |

### Comandos de Empresa

| Comando | Descripción |
|---------|-------------|
| `/pb mycompanies` | Listar tus empresas |
| `/pb mycompanybalance` | Ver saldo de la empresa |
| `/pb companywithdraw <empresa> <cantidad>` | Retirar de la empresa |
| `/pb setcompanyname <nombre\|clear>` | Establecer/borrar nombre de empresa |
| `/pb setcompanyticker <TICKER\|clear>` | Establecer/borrar ticker de empresa |

### Comandos del Mercado

| Comando | Descripción |
|---------|-------------|
| `/pb marketlist <acciones> <empresa>` | Listar acciones para venta |
| `/pb marketbuy <empresa> <acciones>` | Comprar acciones |

### Comandos de Admin

| Comando | Descripción |
|---------|-------------|
| `/pb adminapprove <empresa>` | Aprobar una empresa |
| `/pb cashback <bps>` | Establecer tasa de cashback |
| `/pb centralbalance` | Ver banco central |
| `/pb centralwithdraw <cantidad>` | Retirar del banco central |
| `/pb reload` | Recargar config |

---

## 💸 Entendiendo las Comisiones

PrimeBank usa comisiones para crear una economía realista y financiar el Banco Central.

### Referencia Rápida de Comisiones

| Tipo de Transacción | Comprador/Remitente Paga | Vendedor/Receptor Paga |
|---------------------|--------------------------|------------------------|
| **Transferencia entre Jugadores** | Puede tener comisión | Nada |
| **Pago POS** | Nada extra | Se deduce 5% |
| **Compra de Acciones** | Se añade 2.5% | Se deduce 5% |

### ¿A Dónde Van las Comisiones?

Todas las comisiones van al **Banco Central**, que está controlado por los administradores del servidor. Ellos pueden:

- Redistribuirlo como eventos/premios
- Financiar proyectos del servidor
- Dejarlo como regulación económica

---

## 💡 Consejos y Mejores Prácticas

### Para Jugadores

1. **Siempre deposita tu efectivo** - El dinero en tu cuenta es más seguro que la moneda física
2. **Mantén tu tarjeta segura** - Está vinculada a tu cuenta y no puede ser transferida
3. **Verifica los precios antes de pagar** - Siempre verifica la cantidad en las terminales POS
4. **Usa transferencias para cantidades grandes** - Más seguro que cargar moneda física
5. **Revisa tu historial regularmente** - Detecta cualquier transacción sospechosa temprano

### Para Dueños de Negocios

1. **Establece precios claros** - Asegúrate de que los clientes sepan qué están pagando
2. **Retira regularmente** - Mueve las ganancias de la empresa a tu cuenta personal
3. **Solo vende acciones que puedas permitirte perder** - ¡Mantén la propiedad mayoritaria!
4. **Construye ventas para mayor valoración** - Más ventas POS = Mayor valor de la empresa
5. **Nombra tu empresa claramente** - Buenos nombres atraen más clientes

### Para Administradores

1. **Aprueba empresas cuidadosamente** - Verifica que el jugador sea serio sobre manejar un negocio
2. **Monitorea el Banco Central** - Redistribuye las comisiones para mantener la economía saludable
3. **Habilita el logging de Discord** - Rastrea transacciones grandes para anti-fraude
4. **Establece un cashback razonable** - Muy alto y infla la economía
5. **Comunícate con los jugadores** - Explica el sistema económico a los nuevos jugadores

---

## ❓ Preguntas Frecuentes

### Preguntas Generales

**P: ¿Qué pasa con mi dinero si muero?**
R: ¡El dinero en tu cuenta está seguro! Solo la moneda física en tu inventario puede perderse.

**P: ¿Puedo tener múltiples cuentas bancarias?**
R: No, cada jugador tiene una cuenta personal. Las empresas tienen cuentas separadas.

**P: ¿Cuál es el saldo máximo?**
R: El sistema usa enteros largos, así que teóricamente billones de dólares.

### Preguntas sobre Tarjetas

**P: Perdí mi tarjeta. ¿Puedo obtener una nueva?**
R: ¡Sí! Obtén una nueva tarjeta del servidor/fabricación. Se vinculará a tu cuenta cuando la uses por primera vez.

**P: ¿Puedo darle mi tarjeta a otra persona?**
R: El ítem físico puede ser dado, pero no podrán usarlo para pagos.

### Preguntas sobre Negocios

**P: ¿Cuánto tiempo tarda la aprobación de la empresa?**
R: Depende de los administradores de tu servidor. Pregúntales si está tardando mucho.

**P: ¿Por qué no puedo vender más de 50 acciones?**
R: Este límite asegura que el mercado se mantenga activo y previene la saturación.

**P: Mi empresa muestra "Comercio bloqueado" - ¿por qué?**
R: Tu empresa necesita actividad de ventas para obtener una valoración. ¡Comienza a vender a través del POS!

### Preguntas Técnicas

**P: ¿Dónde se almacenan mis datos?**
R: En la carpeta del mundo del servidor bajo el directorio `primebank/`.

**P: ¿Hay un sistema de respaldo?**
R: El mod mantiene archivos de datos que pueden respaldarse con los respaldos normales del mundo.

---

## 📞 ¿Necesitas Ayuda?

Si tienes problemas:

1. Revisa esta guía de nuevo para encontrar tu respuesta
2. Pregunta a los administradores del servidor
3. Reporta bugs en la página de GitHub del mod

---

*PrimeBank - ¡Haciendo la Economía de Minecraft Real!*
