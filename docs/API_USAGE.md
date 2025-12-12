# PrimeBank API Usage Guide

This guide explains how to interact with PrimeBank from another server-side Forge mod.

## Overview

The `PrimeBankAPI` allows you to secure read and modify player bank balances. It handles thread safety, transaction logging, and locking for you.

**Requirement**: Your code utilizing this API must run on the **Server Side**. Client-side calls will throw a `PrimeBankSecurityException`.

## getting Started

1.  Add the PrimeBank mod jar to your project's dependencies (e.g., in `libs/` or via a maven repository if available).
2.  Access the API entry point:

```java
import com.primebank.api.PrimeBankAPI;

PrimeBankAPI bank = PrimeBankAPI.getInstance();
```

## Methods

### 1. Get Balance

Retrieves the current balance in cents.

```java
import java.util.UUID;

UUID playerUUID = ...; // The player's unique ID
try {
    long balanceCents = bank.getBalance(playerUUID);
    System.out.println("Balance: " + (balanceCents / 100.0) + " USD");
} catch (PrimeBankException e) {
    // Account not found or other error
    System.err.println("Error: " + e.getMessage());
}
```

### 2. Deposit

Adds funds to an account.

```java
import com.primebank.api.PrimeBankResult;

long amount = 500; // 5.00 USD
String source = "VendingMachine"; // Who is depositing effectively
String desc = "Soda Purchase Refund"; // Context

PrimeBankResult result = bank.deposit(playerUUID, amount, source, desc);
if (result == PrimeBankResult.SUCCESS) {
    // Done
} else if (result == PrimeBankResult.ACCOUNT_NOT_FOUND) {
    // Handle error
}
```

### 3. Withdraw

Removes funds from an account.

```java
long price = 150; // 1.50 USD
PrimeBankResult result = bank.withdraw(playerUUID, price, "VendingMachine", "Soda Purchase");

if (result == PrimeBankResult.SUCCESS) {
    // Dispense item
} else if (result == PrimeBankResult.INSUFFICIENT_FUNDS) {
    // Tell player they are broke
}
```

### 4. Transfer

Moves funds between two players securely.

```java
UUID sellerUUID = ...;
PrimeBankResult result = bank.transfer(playerUUID, sellerUUID, price, "P2P Trade", "Diamond Sword");

if (result == PrimeBankResult.SUCCESS) {
    // Trade successful
}
```

## Error Codes (`PrimeBankResult`)

| Code | Description |
| :--- | :--- |
| `SUCCESS` | Operation completed successfully. |
| `ACCOUNT_NOT_FOUND` | The player (or company) does not have a bank account properly initialized. |
| `INSUFFICIENT_FUNDS` | The source account does not have enough money. |
| `INVALID_AMOUNT` | The amount was <= 0. |
| `INTERNAL_ERROR` | An unexpected error occurred (check server logs). |

## Security & Concurrency

- **Thread Safety**: The API uses strict locking (`AccountLockManager`). You can call these methods from any thread.
- **Atomicity**: Transfers are atomic. If a crash happens mid-transaction (extremely rare due to locking), the operation either fully completes or fails before formatting changes.
- **Side**: Always check `if (!world.isRemote)` or verify you are on the server before calling.

## Example: Vending Machine

```java
public void onPlayerBuySoda(EntityPlayer player) {
    if (player.world.isRemote) return; // Server only!

    PrimeBankAPI api = PrimeBankAPI.getInstance();
    long price = 200; // $2.00

    PrimeBankResult res = api.withdraw(player.getUniqueID(), price, "MyMod:Vending", "Soda");
    
    if (res == PrimeBankResult.SUCCESS) {
        // Give soda
        player.sendMessage(new TextComponentString("Enjoy your drink!"));
    } else {
        player.sendMessage(new TextComponentString("Transaction failed: " + res));
    }
}
```
