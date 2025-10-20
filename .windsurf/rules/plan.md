---
trigger: manual
---


# PrimeBank Development Plan (Forge 1.12.2)

## Locked-in rules
- **[Time]** Real days. Company “Day 0” = approval timestamp. First valuation at the end of real day 8; thereafter every 7 real days.
- **[Market]** Primary and P2P allowed. Fees: 2.5% buyer + 5% seller/issuer, total 7.5% to PrimeBank central account. Trading blocked while V == 0.
- **[Loans]** Down payment Option B: downPayment = requestedAmount × p (0%–80%). If approved, down payment reduces principal. Installments every 3 real days. APR annual; per-period rate = APR / (365/3). Fixed installment schedule; allow early payoff without penalty. On due date, if insufficient funds, capture partial payment and mark delinquent (no late fee in v1).
- **[Transfers]** If amount > 50% of sender’s starting balance, apply 2% fee to sender (to central bank).
- **[POS]** 95% to seller (company account), 5% to central bank.
- **[Cards]** No corporate-authorized lists in v1. Owner-only, optional cashback benefit type; admin-configurable templates/overlays.
- **[Persistence]** world/primebank/... per-world. JSON snapshots + async I/O; authoritative state in memory with locks.
- **[Admins]** UUIDs defined in serverconfig/primebank.toml and OP fallback. Central bank controlled by admins.
- **[Identity]** Works fully with online_mode=false. Use server-provided GameProfile UUID (offline UUIDs supported). Store by UUID; log “offline mode” flag.
- **[Currency]** USD cents (long). Half-up rounding.
- **[Time zone]** Scheduling uses server local time zone (likely UTC−3). Client UI converts server timestamps to client local time for display.

---

## Architecture

- **Core modules**
  - **Account & Ledger**: Accounts (personal, company, central), balances in cents, atomic operations with locks.
  - **Transactions**: Typed records (deposit, withdraw, transfer, POS, loan, market, fees).
  - **Scheduler**: Real-time timers for weekly valuations and 3-day installments; catch-up on world load.
  - **Persistence**: In-memory state + async JSON snapshots in world/primebank/... with rotation for logs.
  - **Config**: serverconfig/primebank.toml for fees, APR defaults, limits, admin UUIDs.

- **Game content**
  - **Items**: Cash denominations (1¢, 5¢, 10¢, 25¢, 50¢, $1 coins; $1, $5, $10, $20, $50, $100 bills); Card item (NBT: cardId, ownerUUID, accountId, revoked, type, cashbackBps).
  - **Blocks/TEs**: TerminalPrimeBank (opens GUI; server authority), POS (bound to companyId).
  - **GUIs/Containers**: Registration, account menu, deposit/withdraw, transfer, company application, admin panel, POS checkout, market (listings, portfolio), loans.

- **Networking (SimpleNetworkWrapper)**
  - C2S: RegisterAccount, DepositRequest, WithdrawRequest, TransferRequest, CompanyApply, POSChargeInitiate, POSChargeRespond, LoanApply, LoanAdminDecision, MarketPlaceOrder, MarketCancelOrder, AdminSetConfig.
  - S2C: OpenGui, BalanceUpdate, Notification, MarketDataUpdate, POSChargePrompt, POSResult, LoanUpdate, CompanyApprovalUpdate.

- **Market**
  - 101 total shares per company; max 50 simultaneously on sale; majority rule: owner must keep ≥51 after any operation.
  - Valuation: Week 1 V1 = 6 × salesWeek1; Week n≥2 Vn = (6 × salesWeekN + 2 × V(n−1)) / 3.
  - Price: floor(V / 101) cents. Trading disabled if V == 0.

- **Data model (JSON snapshots)**
  - world/primebank/users/<uuid>.json: profile, hashed PIN (PBKDF2-HMAC-SHA256, salt, iter), personal account, holdings map, linked companies, loan IDs.
  - world/primebank/companies/<companyId>.json: owner UUID, name/desc, approved, createdAt, valuation current/history (last 26), shares state, weekly sales accumulator, company accountId.
  - world/primebank/loans/<loanId>.json: borrower UUID, principal, downPayment, APR, installments, nextDueAt, status, history.
  - world/primebank/cards/<cardId>.json: owner UUID, accountId, type, cashback bps, revoked.
  - world/primebank/pos/<posId>.json: bound companyId, location, createdAt.
  - world/primebank/logs/*.jsonl: rotated append-only for audits.

---

## Development phases

### Phase 1 — Core banking, cash, transfers
- **Scope**
  - Terminal block + GUI skeleton (registration, account selector).
  - Personal accounts; central bank account; admin list.
  - Cash items and denominations; deposit/withdraw with 2-phase server-authoritative mutation.
  - Transfers with 2% fee if amount > 50% of sender’s starting balance; full history entries.
  - Persistence in world/primebank/..., async I/O, log rotation.
  - Config: defaults (POS 5%, Market 2.5%+5%, Loans downPayment default 20% within 0–80%, APR 12%).
  - Scheduler foundation and global locks.
  - Offline mode full support.

- **Deliverables**
  - Packages: com.primebank.core (accounts, ledger, persistence, scheduler, config), com.primebank.content (items, blocks, TEs), com.primebank.net, com.primebank.client.gui.
  - GUIs: Register, AccountMenu, DepositWithdraw, Transfer.
  - Tests: dupe prevention, server restart persistence, fee threshold correctness, offline mode player accounts.

- **Acceptance**
  - No item dupes under lag or inventory edge cases.
  - All actions server-authoritative; balances consistent across relog/restart.
  - Config reload applies to new operations (not retroactive).

### Phase 2 — Companies, POS, cards
- **Scope**
  - Company application and admin approval; company accounts; Day 0 timestamp per company.
  - POS block + handshake: seller sets amount; buyer sees prompt; timeout/abort safe.
  - Cards: owner-only, NBT with cardId, overlay templates, optional cashback; admin create/edit card types.
  - POS settlement: buyer account/card → 95% seller company, 5% central bank; apply card cashback if any.
  - Admin panel basics: approve companies, manage card types, inspect central bank inflows, change configs.

- **Deliverables**
  - GUIs: CompanyApply, AdminPanel (companies/cards/config summary), POSCheckout.
  - Net messages: POS initiation/prompt/decision, card management.
  - Tests: POS under concurrent payments; breaking/replacing POS; card revocation; cashback correctness.

- **Acceptance**
  - POS transactions atomic; no double-charge or undercharge.
  - Companies appear selectable for owners; only approved companies usable in POS.

### Phase 3 — Market (primary), valuation
- **Scope**
  - Weekly valuation engine (server time zone); sales accumulator per company; first valuation at Day 8; trading locked until V > 0.
  - Primary market: owner lists up to min(50, ownerShares − 51) shares; buyers purchase; fees 2.5% buyer, 5% issuer to central bank.
  - Market UI: listings, company detail with valuation graph (last 26 weeks), buy flow with fee breakdown.

- **Deliverables**
  - Services: ValuationService, MarketPrimaryService.
  - GUIs: MarketHome, CompanyDetails, BuyDialog.
  - Tests: time-based valuation correctness across restarts; blocking when V==0; majority rule enforcement.

- **Acceptance**
  - No listing exceeds constraints; majority never violated; price = floor(V/101) cents; graphs reflect history.

### Phase 4 — Market (secondary) and loans
- **Scope**
  - Secondary P2P orders: seller posts blocks of 1+ shares; partial fills allowed; fees 2.5% buyer + 5% seller to central bank.
  - Portfolio UI with realized/unrealized P/L, positions.
  - Loans end-to-end: application with down payment B (0–80%), admin approval (can edit p/APR/terms), disbursement, amortization every 3 real days, partial capture on insufficient funds, early payoff, rejection refunds 100% down payment.

- **Deliverables**
  - Services: OrderBookService (simple FIFO or price-time, v1: price must equal current price), LoanService.
  - GUIs: Portfolio, SellDialog, LoanApply, LoanAdmin.
  - Tests: P2P fee routing, partial fills, down payment checks, installment schedule, delinquency marking and recovery.

- **Acceptance**
  - Order matching respects fees and constraints; holdings accurate.
  - Loans: exact per-period interest, rounding audited, nextDue reschedules correctly; refunds on rejection.

---

## Detailed tasks per phase (ready for AI)

- **Common groundwork**
  - Implement AccountLockManager (per-account/company reentrant locks).
  - Implement Money utility (cents ops, rounding).
  - Implement TimeService reading server zone; expose conversion helpers for client UI.

- **Persistence**
  - In-memory registries: AccountRegistry, UserRegistry, CompanyRegistry, LoanRegistry, CardRegistry, POSRegistry.
  - JSON serializer/deserializer; snapshot queue; rotation for logs/*.jsonl at size/time thresholds.

- **Networking**
  - Define packet IDs, side directions, validation on server; rate-limit sensitive C2S messages.

- **GUI/UX**
  - Clear breakdown of amounts, fees, and results in all flows.
  - Client-side caches for market graphs; invalidate on MarketDataUpdate.

- **Admin/config**
  - serverconfig/primebank.toml keys:
    - market.buyerFeeBps=250, market.sellerFeeBps=500 
    - pos.bankFeeBps=500 
    - loans.defaultAPR=0.12, loans.downPaymentDefaultBps=2000, loans.downPaymentMinBps=0, loans.downPaymentMaxBps=8000 
    - loans.installmentDays=3 
    - security.allowOfflineMode=true 
    - admins=[uuid1, uuid2, ...] 
  - Commands: /primebank reload, /primebank admin (open panel), optional /primebank export.

---

## Testing and QA

- **Unit**
  - Money math, fee thresholds, rounding, amortization schedule.
  - Valuation formula and time windows.

- **Integration**
  - Deposit/withdraw under lag; POS handshake race conditions.
  - Market listing constraints; majority rule; JSON snapshot recovery after crash.

- **Multiplayer**
  - Concurrent deposits to same account; POS multi-client; order fills racing.
  - Offline mode players joining with same name across sessions (log warnings; accounts bound to UUID).

- **Performance**
  - No per-tick heavy loops; schedulers operate on due lists.
  - Async I/O throughput; backoff on disk saturation.

---

## Acceptance criteria summary

- **Consistency**: All money moves are atomic, logged, and persist across restarts.
- **Security**: PIN with PBKDF2 + salt; owner-only cards; admin-only actions through UUID/OP.
- **Compliance**: Fees and thresholds exactly as specified; market/loan rules enforced.
- **Offline mode**: Entire feature set operational with online_mode=false.
- **UX**: GUIs show precise fee breakdowns, timestamps converted to client locale, and actionable errors.

---

## Proposed directory/package structure
- com.primebank.PrimeBankMod (mod entry, proxies, registry)
- com.primebank.core (accounts, ledger, money, locks, config, scheduler)
- com.primebank.persistence (registries, json, snapshots, logs)
- com.primebank.market (valuation, primary, secondary, portfolio)
- com.primebank.loans (applications, schedules, payments)
- com.primebank.content.items (cash, cards)
- com.primebank.content.blocks (terminal, pos)
- com.primebank.net (packets)
- com.primebank.client.gui (screens)
- com.primebank.admin (panel, config UI, commands)

---
All strings must be translated to both English and Spanish