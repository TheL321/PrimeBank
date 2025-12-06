# PrimeBank User Guide

> A complete banking, market, and payment system for Minecraft servers

---

## 📖 Table of Contents

1. [Introduction](#-introduction)
2. [Getting Started](#-getting-started)
3. [For Players](#-for-players)
   - [Your Bank Account](#your-bank-account)
   - [Using Physical Money](#using-physical-money)
   - [The PrimeBank Card](#the-primebank-card)
   - [Using the Terminal](#using-the-primebank-terminal)
   - [Making Payments with POS](#making-payments-with-pos)
   - [Sending Money to Other Players](#sending-money-to-other-players)
   - [Transaction History](#transaction-history)
4. [For Business Owners](#-for-business-owners)
   - [Creating a Company](#creating-a-company)
   - [Managing Your Company](#managing-your-company)
   - [Setting Up a POS Terminal](#setting-up-a-pos-terminal)
   - [Receiving Payments](#receiving-payments)
   - [The Stock Market](#the-stock-market)
5. [For Server Administrators](#-for-server-administrators)
   - [Installation](#installation)
   - [Configuration](#configuration)
   - [Admin Commands](#admin-commands)
   - [Discord Integration](#discord-integration)
6. [Command Reference](#-command-reference)
7. [Understanding Fees](#-understanding-fees)
8. [Tips & Best Practices](#-tips--best-practices)
9. [Frequently Asked Questions](#-frequently-asked-questions)

---

## 🌟 Introduction

**PrimeBank** is a complete economic system mod for Minecraft servers that adds:

- 💰 **Personal bank accounts** for every player
- 💵 **Physical currency** (coins and bills) you can hold and trade
- 💳 **PrimeBank Cards** for cashless payments
- 🏪 **POS (Point of Sale) terminals** for shops and businesses
- 🏢 **Companies** that players can create and manage
- 📈 **A Stock Market** where players can buy and sell company shares

Think of it like having a real bank inside Minecraft! You can save money, pay for things, run a business, and even invest in other players' companies.

---

## 🚀 Getting Started

### What You Get When You Start

When you first join a server with PrimeBank, you automatically get:

1. **A Personal Bank Account** - Your money is safely stored here
2. **Starting Balance** - The server may give you starting money (depends on server settings)

### The Basics

- Your **balance** is stored in **cents** (100 cents = $1.00)
- You can have physical money (coins/bills) in your inventory OR digital money in your account
- To pay at shops, you need a **PrimeBank Card**

---

## 👤 For Players

### Your Bank Account

Your bank account is where your digital money is stored safely. Unlike physical coins in your inventory, money in your account:

- ✅ Won't be lost when you die
- ✅ Can't be stolen by other players
- ✅ Can be used for card payments
- ✅ Keeps a history of your transactions

#### Checking Your Balance

**Command:** `/pb balance` or `/primebank balance`

This shows you how much money you have in your account.

**Example output:**
```
Balance: $150.25
```

---

### Using Physical Money

PrimeBank includes physical coins and bills that you can find, trade, or receive:

| Item | Value |
|------|-------|
| 1 Cent Coin | $0.01 |
| 5 Cent Coin | $0.05 |
| 10 Cent Coin | $0.10 |
| 25 Cent Coin | $0.25 |
| 50 Cent Coin | $0.50 |
| $1 Bill | $1.00 |
| $5 Bill | $5.00 |
| $10 Bill | $10.00 |
| $20 Bill | $20.00 |
| $50 Bill | $50.00 |
| $100 Bill | $100.00 |

#### Depositing Physical Money

You have two options:

**Option 1: Using a Terminal (Recommended)**
1. Find a **PrimeBank Terminal** block
2. **Sneak + Right-click** on it
3. All physical money in your inventory will be deposited automatically!

**Option 2: Using Commands**
```
/pb deposit <amount>
```
Example: `/pb deposit 50` deposits $50.00 of physical currency from your inventory.

#### Withdrawing Money as Physical Currency

```
/pb withdraw <amount>
```
Example: `/pb withdraw 25` gives you $25.00 in coins/bills.

> [!TIP]
> When withdrawing, the mod automatically gives you the most efficient combination of bills and coins.

---

### The PrimeBank Card

The **PrimeBank Card** is your key to cashless payments. You need it to pay at POS terminals.

#### How It Works

1. **Get a Card** - Cards can be crafted or given by the server
2. **First Use Links It** - The first time you use a card, it becomes linked to your account
3. **Only You Can Use It** - Once linked, only you can pay with that specific card

#### Card Information

When you hover over your card, you'll see:
- **Owner** - Who the card belongs to (or "Not linked" if new)
- **Card ID** - A unique identifier for this specific card

> [!IMPORTANT]
> If someone gives you their card, you won't be able to use it for payments - cards are personal!

---

### Using the PrimeBank Terminal

The **Terminal** is a special block that serves as your connection to your bank account.

#### What You Can Do at a Terminal

| Action | How to Do It |
|--------|--------------|
| **Check Balance** | Right-click the terminal |
| **Deposit All Cash** | Sneak + Right-click |
| **Open Menu** | Right-click (opens GUI with more options) |

#### Terminal Menu Options

When you right-click a terminal (without sneaking), a menu appears:

1. **Merchant Charge** - For business owners to charge customers
2. **Apply for Company** - Start your own business
3. **Open Market** - View and trade company stocks

---

### Making Payments with POS

POS (Point of Sale) terminals are how shops charge customers.

#### How to Pay at a POS

1. **Hold your PrimeBank Card** in your hand
2. **Right-click** the POS terminal
3. A confirmation popup will appear showing:
   - The merchant's name
   - The amount to pay
4. Click **Confirm** to pay or **Cancel** to decline

#### What Happens When You Pay

- The money is deducted from your bank account
- The merchant receives the payment (minus a small fee)
- You may receive **Cashback** (if enabled by the server)
- Both you and the merchant get notifications

> [!TIP]
> Always check the amount before confirming! Make sure you're paying the right price.

---

### Sending Money to Other Players

You can send money directly to other players without meeting them.

#### Transfer Command

```
/pb transfer <playername> <amount>
```

**Example:**
```
/pb transfer Steve 100
```
This sends $100.00 to Steve.

#### What You Should Know

- The money comes from your bank account (not physical currency)
- The recipient gets a notification
- There may be a small transfer fee (depends on server settings)
- Both sender and receiver see the transaction in their history

---

### Transaction History

Keep track of your financial activity!

**Command:** `/pb history`

This shows your last 20 transactions, including:

| Transaction Type | Description |
|-----------------|-------------|
| **Deposit** | Money added to your account |
| **Withdrawal** | Money taken out as physical currency |
| **Transfer Sent** | Money you sent to another player |
| **Transfer Received** | Money you received from another player |
| **Market Buy** | Shares you purchased |
| **Market Sale** | Shares you sold |
| **POS Payment** | Payment you made at a shop |
| **POS Receipt** | Payment you received at your shop |
| **Cashback Received** | Bonus money from card payments |
| **Fee** | Processing fees |

---

## 🏢 For Business Owners

### Creating a Company

Want to start your own business? Here's how!

#### Step 1: Apply for a Company

1. Go to a **PrimeBank Terminal**
2. Right-click to open the menu
3. Select **"Apply for Company"**
4. Fill in:
   - **Company Name** - Your business's full name
   - **Ticker** - A short code (2-8 letters/numbers, like "SHOP" or "FARM1")
   - **Description** - Optional description of what you do

#### Step 2: Wait for Approval

- A server administrator must approve your company
- Once approved, you become the company owner
- Your company gets its own bank account

> [!NOTE]
> The approval process helps prevent spam and ensures quality businesses on the server.

---

### Managing Your Company

Once your company is approved, you have several management commands:

#### Viewing Your Companies

```
/pb mycompanies
```
Shows all companies you own and their balances.

#### Checking Company Balance

```
/pb mycompanybalance
```
Shows your primary company's balance.

#### Withdrawing From Your Company

```
/pb companywithdraw <company> <amount>
```

**Example:**
```
/pb companywithdraw SHOP 500
```
Withdraws $500 from your company "SHOP" to your personal account.

#### Changing Company Name

```
/pb setcompanyname <new name>
```
or to clear it:
```
/pb setcompanyname clear
```

#### Changing Company Ticker

```
/pb setcompanyticker <TICKER>
```
The ticker must be 2-8 characters, letters and numbers only, uppercase.

---

### Setting Up a POS Terminal

POS terminals let customers pay at your shop.

#### Step 1: Place the POS Block

Place a **PrimeBank POS** block where customers can reach it.

#### Step 2: Link It to Your Company

1. Make sure you're NOT holding a card
2. Right-click the POS block
3. Select your company from the list

#### Step 3: Set the Price

1. Sneak + Right-click the POS
2. Enter the price you want to charge
3. Click OK

Now customers can pay by right-clicking with their card!

> [!TIP]
> You can have multiple POS terminals for different products at different prices, all linked to the same company.

---

### Receiving Payments

When a customer pays at your POS:

1. ✅ Money goes to your **company account** (not personal)
2. ✅ You receive a chat notification
3. ✅ A small fee goes to the Central Bank

#### Fee Structure

| Fee Type | Amount | Who Pays |
|----------|--------|----------|
| POS Fee | 5% | Deducted from merchant payment |

**Example:** Customer pays $100
- You receive: $95 (after 5% fee)
- Central Bank receives: $5

---

### The Stock Market

PrimeBank includes a stock market where company shares can be traded!

#### Understanding Shares

When your company is approved, you receive **101 shares** of your company. These represent ownership:

- 🔒 **You must keep at least 51 shares** (majority ownership)
- 📊 You can sell up to 50 shares to investors
- 💰 Share price is based on your company's **valuation**

#### Opening the Market

1. Go to a Terminal
2. Right-click to open the menu
3. Select **"Open Market"**

#### Listing Shares for Sale

```
/pb marketlist <number of shares> <company>
```

**Example:**
```
/pb marketlist 10 SHOP
```
Lists 10 shares of your company "SHOP" for sale.

> [!WARNING]
> Trading is blocked until your company has its first valuation (based on sales activity).

#### How Valuation Works

Your company's value (and share price) is calculated based on:

- 📊 Weekly sales through POS terminals
- 📈 A rolling average of the last 7 days
- 🎯 The formula creates a fair market price

**Share Price = Company Valuation ÷ 101**

#### Buying Shares

```
/pb marketbuy <company> <shares>
```

**Example:**
```
/pb marketbuy FARM1 5
```
Buys 5 shares of company "FARM1".

#### Market Fees

| Fee | Amount | Who Pays |
|-----|--------|----------|
| Buyer Fee | 2.5% | Added to purchase price |
| Seller Fee | 5% | Deducted from sale proceeds |

---

## 🔧 For Server Administrators

### Installation

1. **Download** the PrimeBank mod JAR file
2. **Place** it in your server's `mods/` folder
3. **Restart** your server
4. **Configure** settings as needed (see below)

#### Requirements

- Minecraft Forge (version compatible with the mod)
- Java 8 or higher

### Configuration

Configuration file location:
```
<server>/serverconfig/primebank.toml
```

#### Available Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `discord_webhook_url` | Discord webhook for transaction logs | empty (disabled) |

#### Fee Constants (In Code)

These are currently set in the code and may become configurable in future versions:

| Fee | Value | Description |
|-----|-------|-------------|
| `MARKET_BUYER_FEE_BPS` | 250 (2.5%) | Fee on stock purchases |
| `MARKET_SELLER_FEE_BPS` | 500 (5%) | Fee on stock sales |
| `POS_BANK_FEE_BPS` | 500 (5%) | Fee on POS transactions |

### Admin Commands

All admin commands require **OP level 2** or higher.

#### Approving Companies

```
/pb adminapprove <company>
```
Approves a pending company application.

#### Setting Global Cashback

```
/pb cashback <bps>
```
Sets the cashback percentage in basis points (100 bps = 1%).

**Example:** `/pb cashback 100` gives 1% cashback on all card payments.

#### Central Bank Balance

```
/pb centralbalance
```
Shows the Central Bank's accumulated fees.

#### Withdraw from Central Bank

```
/pb centralwithdraw <amount>
```
Withdraws funds from the Central Bank.

#### Reload Configuration

```
/pb reload
```
Reloads configuration from disk.

### Discord Integration

PrimeBank can send transaction logs to a Discord channel via webhook.

#### Setup

1. Create a webhook in your Discord server
2. Add to `serverconfig/primebank.toml`:
```toml
discord_webhook_url = "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL"
```
3. Reload the mod or restart the server

#### What Gets Logged

- All transfers between players
- POS transactions
- Market trades
- Large transactions

---

## 📋 Command Reference

### Player Commands

| Command | Description |
|---------|-------------|
| `/pb balance` | Check your account balance |
| `/pb history` | View last 20 transactions |
| `/pb deposit <amount>` | Deposit physical currency |
| `/pb withdraw <amount>` | Withdraw as physical currency |
| `/pb transfer <player> <amount>` | Send money to a player |

### Company Commands

| Command | Description |
|---------|-------------|
| `/pb mycompanies` | List your companies |
| `/pb mycompanybalance` | Check company balance |
| `/pb companywithdraw <company> <amount>` | Withdraw from company |
| `/pb setcompanyname <name\|clear>` | Set/clear company name |
| `/pb setcompanyticker <TICKER\|clear>` | Set/clear company ticker |

### Market Commands

| Command | Description |
|---------|-------------|
| `/pb marketlist <shares> <company>` | List shares for sale |
| `/pb marketbuy <company> <shares>` | Buy shares |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/pb adminapprove <company>` | Approve a company |
| `/pb cashback <bps>` | Set cashback rate |
| `/pb centralbalance` | Check central bank |
| `/pb centralwithdraw <amount>` | Withdraw from central bank |
| `/pb reload` | Reload config |

---

## 💸 Understanding Fees

PrimeBank uses fees to create a realistic economy and fund the Central Bank.

### Fee Quick Reference

| Transaction Type | Buyer/Sender Pays | Seller/Receiver Pays |
|-----------------|-------------------|---------------------|
| **Player Transfer** | May have fee | Nothing |
| **POS Payment** | Nothing extra | 5% deducted |
| **Stock Purchase** | 2.5% added | 5% deducted |

### Where Do Fees Go?

All fees go to the **Central Bank**, which is controlled by server administrators. They can:

- Redistribute it as events/prizes
- Fund server projects
- Leave it as economic regulation

---

## 💡 Tips & Best Practices

### For Players

1. **Always deposit your cash** - Money in your account is safer than physical currency
2. **Keep your card safe** - It's linked to your account and can't be transferred
3. **Check prices before paying** - Always verify the amount at POS terminals
4. **Use transfers for large amounts** - Safer than carrying physical currency
5. **Check your history regularly** - Catch any suspicious transactions early

### For Business Owners

1. **Set clear prices** - Make sure customers know what they're paying for
2. **Withdraw regularly** - Move company profits to your personal account
3. **Only sell shares you can afford to lose** - Keep majority ownership!
4. **Build sales for higher valuation** - More POS sales = higher company value
5. **Name your company clearly** - Good names attract more customers

### For Administrators

1. **Approve companies carefully** - Verify the player is serious about running a business
2. **Monitor the Central Bank** - Redistribute fees to keep the economy healthy
3. **Enable Discord logging** - Track large transactions for anti-fraud
4. **Set reasonable cashback** - Too high and it inflates the economy
5. **Communicate with players** - Explain the economic system to new players

---

## ❓ Frequently Asked Questions

### General Questions

**Q: What happens to my money if I die?**
A: Money in your account is safe! Only physical currency in your inventory can be lost.

**Q: Can I have multiple bank accounts?**
A: No, each player has one personal account. Companies have separate accounts.

**Q: What's the maximum balance?**
A: The system uses long integers, so theoretically trillions of dollars.

### Card Questions

**Q: I lost my card. Can I get a new one?**
A: Yes! Get a new card from the server/crafting. It will link to your account when you first use it.

**Q: Can I give my card to someone else?**
A: The physical item can be given, but they won't be able to use it for payments.

### Business Questions

**Q: How long does company approval take?**
A: Depends on your server administrators. Ask them if it's taking too long.

**Q: Why can't I sell more than 50 shares?**
A: This limit ensures the market stays active and prevents flooding.

**Q: My company shows "Trading blocked" - why?**
A: Your company needs sales activity to get a valuation. Start selling through POS!

### Technical Questions

**Q: Where is my data stored?**
A: In the server's world folder under `primebank/` directory.

**Q: Is there a backup system?**
A: The mod maintains data files that can be backed up with normal world backups.

---

## 📞 Need Help?

If you have issues:

1. Check this guide again for your answer
2. Ask server administrators
3. Report bugs on the mod's GitHub page

---

*PrimeBank - Making Minecraft Economy Real!*
