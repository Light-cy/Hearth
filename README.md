# HEARTH — Shared Household Ledger & Co-Living Vault

> **Split it. Settle it. Done.**  
> A precision financial steward and collaborative pantry manager for shared apartments and co-living households, crafted with a tactile **Glassmorphism** design system on **Jetpack Compose (Material 3)** and backed by a **Node.js + Express + PostgreSQL** synchronization engine.

---

## ✨ Key Highlights

- **Paisa-Exact 64-Bit Integer Arithmetic (`MoneyEngine`)**: Zero floating-point drift. Every PKR amount is parsed, stored, split, and audited in integer paisa (`Long` on Android / `BIGINT` in PostgreSQL).
- **Deterministic Remainder Allocation**: When an equal split cannot be divided evenly (e.g., `PKR 100.00` across 3 residents), `+1 paisa` remainders are allocated deterministically in ascending order of Resident ID (`M01` → `M02` → `M03`).
- **Continuous Zero-Sum Audit**: At every point in time, the sum of all household residents' net balances strictly equals `PKR 0.00`:
  $$\text{Net}_i = \text{Paid}_i - \text{Owed}_i + \text{Sent}_i - \text{Received}_i \quad \implies \quad \sum_{i} \text{Net}_i = 0$$
- **Smart Settle Debt Simplification**: Greedy bipartite debt-simplification algorithm computes the minimum number of peer-to-peer transfers required to settle the entire household to `PKR 0.00` in one tap.
- **Collaborative Pantry & Checkout-to-Expense Pipeline**: Residents queue needed household provisions (`pack`, `dozen`, `bottle`, `bag`) and check out purchased items in a single step—with optional **Sales Tax (%)** and **Discount (PKR)** calculation—automatically recording a shared `Groceries` expense and marking items as `BOUGHT`.
- **Monthly Stewardship Challenge & Category Cap Alerts**: Real-time tracking against the monthly household ceiling (`ON_TRACK` < 80%, `NEAR_LIMIT` 80%–100%, `OVER_BUDGET` > 100%) with automatic warnings whenever any single category exceeds **40%** of the monthly cap.
- **Recurring Monthly Bills Generator**: Mark routine bills (e.g., Fiber Internet, Electricity) as recurring and generate current-month entries in a single tap with idempotency protection.
- **Hybrid Dual-Engine Architecture**: Seamlessly switch between the **On-Device Local Vault** (zero-setup reactive state engine) and **Cloud Sync** (`Retrofit2` + Node.js/PostgreSQL REST API) directly from the Steward Profile screen.

---

## 🎨 Glassmorphism Design System

Hearth features a custom atmospheric UI inspired by modern fintech cards:
- **Atmospheric Canvas**: Radial-illuminated sage-to-oatmeal gradient (`#B4CEBE` → `#DCE6DF` → `#EDEAE2`) that adapts across compact phones (`<360dp`), standard handsets, and tablet/foldable viewports (`840dp+`).
- **Notched Physical Debit Card Holster**: Frosted white outer holster housing a deep emerald-forest card (`#0A2118` → `#16382B`) with a procedural dot-matrix texture, iridescent metallic `hearth` badge, and split-decimal typography (`PKR 250.00`).
- **Translucent Floating Bottom Dock**: Pill-shaped glass navigation bar with an active circular white indicator and quick-action orb.

---

## 🧭 Complete User Flow & Feature Guide

### 1. Onboarding & Household Authentication (`AuthScreens.kt`)
1. **Welcome Screen**:
   - Displays a live preview of the active household vault (**Flat 4B, 3 Gulberg Estate**, invite code `LHR-4B-9872`) with real-time resident balances.
   - **1-Tap Resident Vault Access**: Tap **Ali (`M01`)**, **Ayesha (`M02`)**, or **Bilal (`M03`)** on the Welcome or Sign-In screen to immediately enter the household from that resident's perspective.
   - **Sign In / Create or Join Household**:
     - **Sign In**: Authenticate with email and password (default residents: `ali@hearth.pk`, `ayesha@hearth.pk`, `bilal@hearth.pk` / password: `hearth2026`).
     - **Join Existing Flat**: Register a new resident account using the household invite code (`LHR-4B-9872`). The new resident is assigned the next sequential ID (`M04`, etc.) and immediately appears across all split selectors and balance tables.
     - **Create New Vault**: Founding a new residence assigns the creator as `ADMIN` Steward, generates a fresh invite code, and initializes a clean household ledger.

### 2. Home Dashboard & Stewardship Challenge (`HomeScreen.kt`)
- **Top Bar & Perspective Switcher**:
  - Tap the **Viewing as** pill in the top-right to switch the active resident perspective (`Ali`, `Ayesha`, `Bilal`) on the fly and observe how the hero card and net status copy (*"You are owed PKR 250.00"*, *"You owe PKR 450.00"*, or *"You are all settled up"*) update instantaneously.
- **Overview Tab**:
  - **Resident Hero Card**: Shows total paid, total share, split-decimal net balance, and quick actions (**Manage Ledger** and **+ Settle / Add**).
  - **Monthly Ceiling Widget**: Animated radial progress gauge showing monthly spend (`PKR 4,200.00 / PKR 5,000.00`, `84% USED`, `NEAR LIMIT`) with **Challenge View** and **Adjust Cap** controls.
  - **Roommate Splits**: Interactive glass cards for each co-resident with direct **Settle Now / Pay Split** actions pre-filled from Smart Settle suggestions.
  - **Pantry Run Promo Card**: Live progress bar of stocked vs. pending kitchen provisions.
  - **Recent Activity & Category Warning Banner**: Displays latest shared bills and highlights any category exceeding 40% of the monthly ceiling.
- **Budget & Challenge Tab**:
  - **Monthly Stewardship Challenge Card**: 6-segment illuminated progress bar, 6 category checkpoint nodes (`GRO`, `UTI`, `CLE`, `DIN`, `ENT`, `OTH`), per-category spend breakdown, and the **Generate Monthly Recurring Bills** action button.

### 3. Collaborative Pantry & Checkout (`ShoppingScreen.kt`)
- **Filter & Track Provisions**: Switch between **All**, **Pending**, and **Bought** tabs.
- **Queue New Provision (`+`)**: Add items with custom quantities and standard household units (`pack`, `dozen`, `bottle`, `bag`). Duplicate pending items are automatically prevented.
- **Batch Checkout (`Checkout (N)`)**:
  - Select one or more pending items and open the **Checkout Selected Items** modal sheet.
  - Enter the **Items Subtotal Paid (PKR)**, optionally expand **+ Add Sales Tax % & Discount** for live half-up tax and discount calculation, select the **Paid By** resident and **Split Equally Among** co-residents, and tap **Complete Checkout**.
  - Hearth atomically creates the `Groceries` expense, updates everyone's net balance, and marks the items as `BOUGHT`.

### 4. Household Ledger, Expense Splits & Settlements (`MoneyScreen.kt`)
- **Balances Table Tab**:
  - **Zero-Sum Ledger Verification Banner**: Live audit confirming that all resident net balances sum to `PKR 0.00`.
  - **Smart Settle Suggestions Card**: Shows the minimum transfers needed to clear all debts with **One-Tap Settle** buttons.
  - **Individual Resident Cards**: Full breakdown of `Paid`, `Share`, `Sent`, `Rcvd`, and `Net` for every member.
- **Expenses & Settlements Tab**:
  - Chronological ledger of all shared expenses (with per-member split chips and recurring badges) and recorded settlements.
- **Add Expense Modal (`+ Expense`)**:
  - Supports **Equal Split** (with live per-person preview and remainder note) and **Custom Split** (with live `Allocated vs. Remaining` validation).
  - Supports **Expense Date (`YYYY-MM-DD`)** validation (rejecting future dates) and **Recurring Monthly Bill** templating.
- **Record Settlement Modal (`Settle Up`)**:
  - Select **Payer (Debtor)** and **Recipient (Creditor)**, tap a quick-fill suggestion chip, and record partial or full settlements with strict over-settlement protection.

### 5. Steward Profile & Vault Configuration (`ProfileScreen.kt`)
- **Cloud Sync & Vault Storage**: Toggle between **On-Device Local Vault** and **Cloud-Synced Household Server**, customize the Node.js API Base URL, or tap **Sync Ledger** to restore the default household state.
- **Switch Active Resident**: One-tap resident switcher.
- **Shared Household Vault**: Copy or rotate (`ADMIN` only) the household invite code.
- **Security & Alerts**: Configure expense/budget notifications, update resident login passwords, or sign out to return to the Welcome screen.

---

## 🏗️ Project Architecture

```text
Hearth/
├── app/                                      # Android Client (Kotlin + Jetpack Compose + Material 3)
│   └── src/
│       ├── main/java/com/example/
│       │   ├── MainActivity.kt               # Root navigation & ZeniFloatingBottomDock
│       │   ├── engine/MoneyEngine.kt         # Pure 64-bit paisa math, equal/custom splits & Smart Settle
│       │   ├── model/Models.kt               # Immutable domain models & DTOs
│       │   ├── data/
│       │   │   ├── HearthRepository.kt       # Repository interface & reactive LocalVaultEngine
│       │   │   ├── SessionManager.kt         # Auth session, resident registry & household state
│       │   │   └── remote/                   # HearthApiService & RemoteRetrofitRepository
│       │   ├── di/AppContainer.kt            # Dependency container & Local/Cloud mode switching
│       │   └── ui/
│       │       ├── theme/                    # Color.kt, Type.kt, Theme.kt (Glassmorphism palette)
│       │       ├── components/Components.kt  # GlassPanelCard, DarkForestHeroCard, SplitDecimalMoneyText
│       │       ├── auth/AuthScreens.kt       # WelcomeScreen, LoginScreen, RegisterScreen
│       │       ├── home/HomeScreen.kt        # Overview & Budget Stewardship Challenge tabs
│       │       ├── shopping/ShoppingScreen.kt# Collaborative Pantry & Tax/Discount Checkout sheet
│       │       ├── money/MoneyScreen.kt      # Balances Table, Smart Settle, Expense & Settle sheets
│       │       └── profile/ProfileScreen.kt  # Steward Profile, Cloud Sync toggle & Invite management
│       └── test/java/com/example/            # JUnit4 unit tests for MoneyEngine & LocalVaultEngine
├── server/                                   # Backend API (Node.js + Express + PostgreSQL)
│   ├── engine/money.js                       # Server-side integer paisa engine & zero-sum verifier
│   ├── db/migrations/001_initial_schema.sql  # PostgreSQL BIGINT schema
│   ├── modules/                              # Auth, Household, Members, Shopping, Expenses, Settle, Budget
│   ├── scripts/
│   │   ├── seed.js                           # Database seeder
│   │   └── verify-benchmark.js               # Standalone math & invariant verification script
│   └── test/money-engine.test.js             # Node.js unit test suite
└── docs/
    └── CONTRACT.md                           # Full REST API specification
```

---

## 🚀 Getting Started

### 1. Android Application
1. Open the root `Hearth` directory in **Android Studio (Ladybug / Koala or newer)** with **JDK 17**.
2. Sync Gradle and run the `app` configuration on any Android emulator or physical device (`minSdk 24`, `targetSdk 35`).
3. The app starts in **Local Vault** mode out of the box—no external database setup is required to explore all features, switch residents, log expenses, check out groceries, or settle debts.

### 2. Node.js + PostgreSQL Backend (Optional Cloud Sync)
```bash
cd server
npm install

# Run pure math & ledger invariant unit tests (no Postgres required)
npm test
node scripts/verify-benchmark.js

# To run with PostgreSQL:
cp .env.example .env
npm run migrate
npm run seed
npm run dev
```
Then open **Profile → Cloud Sync & Vault Storage** in the Android app and toggle **Cloud Sync** on (default emulator host URL: `http://10.0.2.2:3000/`).
