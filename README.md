# ⚡ BFIT — The Ultimate Cloud-Connected Fitness Companion

![BFIT Banner](https://img.shields.io/badge/Fitness-Cloud--Powered-00C853?style=for-the-badge&logo=firebase&logoColor=white)
![Build Status](https://img.shields.io/badge/Build-v1.0--Stable-blue?style=for-the-badge)
![AI Powered](https://img.shields.io/badge/AI-Gemini--Pro-red?style=for-the-badge&logo=google-ai&logoColor=white)

BFIT is not just another workout tracker—it's a **Next-Gen AI Fitness Ecosystem**. Originally a local prototype, BFIT has been transformed into a fully cloud-synced, AI-integrated health platform. Whether you're bulking, leaning, or maintaining, BFIT keeps your fitness journey in the palm of your hand.

---

## ✨ Key Features

### 🔐 1. Seamless Cloud Identity
Say goodbye to local-only logins. BFIT now uses **Firebase Authentication** for:
- **Google Sign-In**: One-tap access.
- **Email/Password**: Traditional, secure account management.
- **Cloud Profiles**: Your height, weight, and fitness goals follow you across devices.

### 🤖 2. Gemini AI Fitness Coach
Powered by **Google Gemini Pro**, our AI coach provides:
- **Instant Advice**: Ask about nutrition, form, or motivation.
- **Smart History**: All your chat history is saved to Firestore—never lose a great tip again.
- **Personalized Responses**: The bot knows your goals and tailors advice accordingly.

### 🍎 3. Macro-Aware Food Scanner
Track what you eat without the manual labor using the **OpenFoodFacts API**:
- **Barcode Scanning**: Scan any product to instantly fetch macros.
- **Full Macro Tracking**: Record **Calories, Protein, Carbs, and Fats** in one tap.
- **Daily Logs**: Food logs sync directly to your personal cloud dashboard.

### ☁️ 4. Single Source of Truth
We use a **Hybrid Database Architecture**:
- **Room (Local Cache)**: Lightning-fast offline performance.
- **Firestore (Cloud Provider)**: Constant sync across multiple devices. No progress is ever lost.

### 🛒 5. BFIT Smart Store
Access top-tier supplements with ease:
- **Cloud Catalog**: Browse recommended supplements directly from Firestore.
- **Mock Checkout**: Simulation of a secure purchase flow recorded in your profile.

---

## 🛠️ Tech Stack & Architecture

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin / Java |
| **UI Framework** | Material 3 / XML Layouts |
| **Authentication** | Firebase Auth (Google + Email) |
| **Cloud Backend** | Firebase Firestore |
| **AI Engine** | Google Generative AI (Gemini Pro) |
| **Networking** | Retrofit + OkHttp |
| **Scanning** | ML Kit + CameraX |
| **Data Architecture** | Repository Pattern + Room Cache |

---

## 🚀 Instant Setup (The Best Alternative)
To simplify the "Getting Started" process, we've enabled **Clerk Authentication**. Instead of setting up a unique Firebase project per developer, simply use a shared **Clerk Publishable Key**.

1. **Clone the project**:
   ```bash
   git clone https://github.com/pushpraj20062006-max/BFIT.git
   ```

2. **Configure API Key (No JSON required)**:
   Add your Clerk key to `local.properties`:
   ```properties
   clerk.publishableKey=pk_test_... # Ask the owner for the shared key
   gemini.apiKey=YOUR_GEMINI_PRO_API_KEY
   ```

3. **Build and Run**:
   Sync Gradle and run the app. Use **"Try Demo Mode"** on the login screen to jump in instantly with mock data!

---

## 🏗️ Technical Setup (Individual Developers)
If you prefer to use your own backend for testing:
1. **Clerk setup**: Enable Native API in your Clerk Dashboard and get your Publishable Key.
2. **Firebase setup**: Only required if using Firestore sync features (download `google-services.json`).

## 📸 Glimpse of the App

### 🏠 **Smart Dashboard**
Track your daily calorie and protein progress in real-time with beautiful linear & circular progress indicators.

### 🤳 **AI & Scanner**
Interactive chat bubbles for coaching and a high-performance scanner for quick nutrition logging.

---

## 📝 Contribution

We're always looking to push the boundaries of fitness tech! Feel free to:
- Open **Issues** for bugs or feature requests.
- Submit **Pull Requests** for optimizations or new features.

---

## 📜 License
Distributed under the MIT License. See `LICENSE` for more information.

---

*Fuel your passion. Build your body. **Believe in BFIT.*** 🤘
