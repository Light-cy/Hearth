# Hisaab Mobile & Backend Connection Guide

This guide explains how the backend runs, how the Android app is architected, and the step-by-step instructions for running the app on a physical mobile phone connected to your laptop.

---

## 1. How the Backend & Frontend Are Architected

### A. The Android App (Dual-Mode Design)
When installed on any device (emulator or physical phone), the app is completely self-reliant out-of-the-box:
- **Default (Local Demo Engine):** The app contains an embedded in-memory database and pure Kotlin money engine pre-seeded with the hackathon benchmark (Ali, Ayesha, Bilal, 4 expenses, 84% Near Limit budget, and Smart Settle). It works with **zero internet or external server required**.
- **Live Retrofit Mode:** Under **Profile Screen $\to$ Server Settings**, you can toggle **"Connect to Live Backend"** and enter any backend URL. The app will immediately re-route all API requests (auth, shopping, expenses, balances, budget, settle) to your live Node.js server.
- **Fail-Safe Offline Banner:** If your laptop backend goes down mid-session, the app displays a non-intrusive warning banner with a **Retry** button rather than crashing.

### B. The Node.js + PostgreSQL Backend (`/server`)
- Runs Express on port `4000` (or `PORT` in `.env`).
- Connects to a PostgreSQL database via `DATABASE_URL` or standard PG environment variables (`PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`).
- Serves the frozen REST contract from `/docs/CONTRACT.md`:
  - `POST /api/auth/register`, `/login`, `/refresh`, `/logout`, `/password`
  - `GET /api/household`, `POST /api/household/invite/rotate`, `GET /api/members`
  - `GET /api/shopping`, `POST /api/shopping`, `POST /api/shopping/checkout`
  - `GET /api/expenses`, `POST /api/expenses`, `GET /api/budget`, `GET /api/budget/categories`
  - `GET /api/balances`, `GET /api/balances/simplify`, `POST /api/settle`, `GET /api/settlements`
  - `GET /api/health` (used by the mobile app to test connectivity before saving settings)

---

## 2. Running on a Physical Mobile Phone via Your Laptop

Because a physical phone has its own network stack, entering `http://localhost:4000` on the phone looks inside the phone itself, not your laptop. You can connect using either **Option A (USB Cable)** or **Option B (Same Wi-Fi Network)**.

---

### Option A: USB Tethering / `adb reverse` (Most Reliable & Recommended)

This option works even without a local Wi-Fi router and is unaffected by college or venue firewall restrictions (as noted in the Master Plan).

1. **Connect Phone via USB:**
   - Plug your Android phone into your laptop.
   - Enable **Developer Options** and **USB Debugging** on the phone.
2. **Reverse the Port on your Laptop Terminal:**
   ```bash
   adb reverse tcp:4000 tcp:4000
   ```
   *(This routes any request sent to `localhost:4000` on the phone directly into port 4000 on your laptop).*
3. **Start the Backend on your Laptop:**
   ```bash
   cd server
   npm install
   npm run migrate
   npm run seed
   npm start
   ```
4. **Configure the App on your Phone:**
   - Open **Hisaab** on the phone.
   - Go to **Profile** (top right avatar / tab).
   - In **Server Connection Settings**, toggle **Connect to Live Backend**.
   - Set Server URL: `http://localhost:4000` (or `http://127.0.0.1:4000`).
   - Tap **Test Connection & Save**. It will show `"Connected to Hisaab Backend API"`.

---

### Option B: Local Wi-Fi Network (Wireless)

Use this if your phone and laptop are connected to the same Wi-Fi router (or mobile hotspot).

1. **Find your Laptop's Local IP Address:**
   - **Mac / Linux:** Run `ifconfig` or `ip a` (e.g. `192.168.1.15` or `192.168.100.42`).
   - **Windows:** Run `ipconfig` in Command Prompt (look for IPv4 Address).
2. **Start the Backend on your Laptop:**
   ```bash
   cd server
   npm start
   ```
   *(The server listens on `0.0.0.0:4000`, so it accepts connections from any device on your local network).*
3. **Configure the App on your Phone:**
   - Open **Hisaab** on the phone.
   - Go to **Profile $\to$ Server Connection Settings**.
   - Toggle **Connect to Live Backend**.
   - Set Server URL: `http://<YOUR_LAPTOP_IP>:4000` (e.g., `http://192.168.1.15:4000`).
   - Tap **Test Connection & Save**.

---

## 3. How to Install the App APK on your Mobile Phone

1. **From AI Studio:**
   - In the top right / menu, click **Export $\to$ Download APK / AAB**.
   - Copy the generated `.apk` file to your phone and install it (allow "Install from unknown sources" if prompted).
2. **From Terminal (via ADB):**
   ```bash
   gradle :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 4. Summary & Verification Matrix

| Method | Server URL to enter in App | Requirement | Best For |
| :--- | :--- | :--- | :--- |
| **Local Demo Mode** | *(No URL needed)* | None (works offline) | Immediate evaluation & oral defense fallback |
| **USB (`adb reverse`)** | `http://localhost:4000` | USB cable + `adb reverse tcp:4000 tcp:4000` | Hackathons, unstable Wi-Fi |
| **Wi-Fi LAN** | `http://<LAPTOP_IP>:4000` | Laptop and phone on same Wi-Fi | Cable-free demonstration |
| **Android Emulator** | `http://10.0.2.2:4000` | Running in standard Android Studio emulator | Laptop-only testing |
