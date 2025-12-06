# 🏦 PrimeBank

**A comprehensive banking, economy, and stock market mod for Minecraft servers.**

PrimeBank introduces a realistic economic system featuring physical currency, digital bank accounts, credit cards, point-of-sale (POS) terminals for shops, and a dynamic stock market where players can list their own companies.

> [!NOTE]
> This mod provides a robust alternative to plugin-based economies, fully integrated into the game with custom blocks and items.

---

## ✨ Key Features

- **💰 Personal Banking**: Every player gets a secure bank account.
- **💵 Physical Currency**: Coins and bills that can be deposited/withdrawn ($0.01 to $100).
- **💳 PrimeBank Cards**: Secure cards for cashless payments at player-owned shops.
- **🏪 POS System**: Shop owners can place POS terminals to charge customers easily.
- **🏢 Company System**: Players can create companies, get them approved by admins, and manage business funds separately.
- **📈 Stock Market**: Approved companies can list shares. Share prices update dynamically based on real sales activity!
- **🔒 Secure Transactions**: Majority ownership rules prevent hostile takeovers.

---

## 📚 Documentation

For a complete guide on how to use the mod, see the **User Guide**:

- [📖 User Guide (English)](docs/USER_GUIDE_EN.md)
- [📖 Guía de Usuario (Español)](docs/USER_GUIDE_ES.md)

---

## 📥 Installation

1. Install **Minecraft Forge** for your version.
2. Download the `PrimeBank-x.x.x.jar`.
3. Place the jar file in your server's `mods` folder.
4. Restart the server.

---

## 🛠️ Compilation (For Developers)

To build PrimeBank from the source code:

### Prerequisites
- JDK 8 or higher
- Git

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/TheL321/PrimeBank.git
   cd PrimeBank
   ```

2. Setup the workspace (optional but recommended for IDEs):
   ```bash
   # Windows
   gradlew setupDecompWorkspace
   
   # Linux/Mac
   ./gradlew setupDecompWorkspace
   ```

3. Build the mod:
   ```bash
   # Windows
   gradlew build
   
   # Linux/Mac
   ./gradlew build
   ```

4. The compiled jar file will be in `build/libs/`.

---

## 🤖 AI Disclosure

**Transparency Notice:**
This mod was developed with the assistance of Artificial Intelligence tools. AI was used to help generate code structures, optimize logic, translate documentation, and design features. All code has been reviewed and tested to ensure functionality and security.

---

*PrimeBank - Making the Minecraft Economy Real!*
