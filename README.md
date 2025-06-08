# 🔒 TimeVault – Secure Your Future Moments

**TimeVault** is a secure and intelligent data storage app that allows users to lock memories, files, and personal information inside a digital vault. The unique feature? Users set a future unlock time — and until that moment arrives, the data remains completely inaccessible.

> **"Lock it today. Unlock it tomorrow."**

---

## 🚀 Features

### 🔐 Authentication
- Firebase Email/Password Login & Signup
- Auto-login using `SharedPreferences`

### 📁 Vault System
- Create vaults with a title, unlock time, and custom password
- Upload **images, PDFs, and videos**
- View vaults as **Locked** or **Unlocked**
- **Search** by Vault Name or Vault ID  
> ⚠️ Vaults can only be unlocked **after unlock time** and **with correct password**

### 🔔 Notifications
- **FCM push notifications** when a vault unlocks
- Notifications can be deleted inside the app
- Notifications sent via backend automation

### ⚙️ App Settings
- Toggle between **Light**, **Dark**, or **System Default** themes
- View current app version
- Edit profile details
- Change / Forgot password
- Log out
- Help Center & Privacy Policy dialog

### ⏱️ Time Unlock Automation (Node.js + GitHub Actions)
- Periodically runs every 15 mins or 1 hour
- Uses Firebase Admin SDK to check unlock time
- If unlock time has passed:
  - Updates vault’s status (`isUnlocked: true`)
  - Sends **Email (via NodeMailer)**
  - Sends **Push Notification (via FCM)**
- Fully server-side: **no dependency on client timers**

---

## 🛠️ Tech Stack

| Layer         | Tools & Technologies |
|---------------|-----------------------|
| **Frontend**  | Android (Kotlin + XML), MaterialDatePicker, Lottie |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Backend**   | Firebase Auth, Firestore, Realtime DB, Node.js (GitHub Actions) |
| **Storage**   | Firestore (Vaults), Realtime DB (Users/Notifications), SharedPreferences, Cloudinary (media) |
| **UI/UX**     | CardView, RecyclerView, Bottom Sheet, AlertDialog, Floating Button, Pop-up Menu |

---

## 📦 Setup & Installation

### 🔧 Prerequisites

- Android Studio (latest version recommended)
- Firebase project with `google-services.json`
- [Cloudinary Account](https://cloudinary.com)

---

### 🛠️ Steps to Run Locally

1. **Clone the Repository**
   ```bash
   git clone https://github.com/Alok-kumar2024/TimeVault-App.git
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Click `Open` > Select the project folder

3. **Firebase Setup**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your project → Settings
   - Download `google-services.json`
   - In Android Studio:  
     - Switch to **Project View** (Top-left corner)
     - Paste `google-services.json` into the `app/` folder

4. **Cloudinary Setup**
   - Sign up at [cloudinary.com](https://cloudinary.com)
   - In Dashboard, get:
     - `cloud_name`
     - `api_key`
     - `api_secret`

   - In `CloudinaryUploadWorker.kt`, replace:
     ```kotlin
     val cloudinary = Cloudinary(
         ObjectUtils.asMap(
             "cloud_name", "your_cloud_name",
             "api_key", "your_api_key",
             "api_secret", "your_api_secret"
         )
     )
     ```

   - In `EditProfile_Activity.kt`, inside `uploadToCloudinary`:
     ```kotlin
     val cloudName = "your_cloud_name"
     val apiKey = "your_api_key"
     val apiSecret = "your_api_secret"
     ```

---

## 🖼️ Screenshots

<div style="display: flex; flex-wrap: wrap; gap: 10px;">
  <img src="App-TimeVault-SS/SplashScreenActivity.jpg" alt="Splash Screen Activity" width="200"/>
  <img src="App-TimeVault-SS/SignInFragment.jpg" alt="Sign In Fragment" width="200"/>
  <img src="App-TimeVault-SS/SigbnUpFragment.jpg" alt="Sign Up Fragment" width="200"/>
  <img src="App-TimeVault-SS/Home_Fragment.jpg" alt="Home Fragment" width="200"/>
  <img src="App-TimeVault-SS/VaultFragment.jpg" alt="Vault Fragment" width="200"/>
  <img src="App-TimeVault-SS/PopUpMenu.jpg" alt="Pop Up Menu" width="200"/>
  <img src="App-TimeVault-SS/Notification_BottomSheetDialog.jpg" alt="Notification Bottom Sheet Dialog" width="200"/>
  <img src="App-TimeVault-SS/VaultCreationActivity.jpg" alt="Vault Creation Activity" width="200"/>
  <img src="App-TimeVault-SS/SettingsActivity.jpg" alt="Settings Activity" width="200"/>
  <img src="App-TimeVault-SS/Theme_AlertDialog.jpg" alt="Theme Alert Dialog" width="200"/>
  <img src="App-TimeVault-SS/EditProfileActivity.jpg" alt="Edit Profile Activity" width="200"/>
  <img src="App-TimeVault-SS/ChangePasswordActivity.jpg" alt="Change Password Activity" width="200"/>
  <img src="App-TimeVault-SS/ForgotPasswordAlertDialog.jpg" alt="Forgot Password Alert Dialog" width="200"/>
  <img src="App-TimeVault-SS/LogOutAlertDialog.jpg" alt="Log Out Alert Dialog" width="200"/>
</div>


---

## 🤝 Contribution Guide

We welcome all contributions! 🛠️✨

### 📌 Steps to Contribute

1. **Fork** this repository
2. **Create a branch** (e.g. `feature/add-vault-export`)
3. **Commit** your changes with clear messages
4. **Push** to your fork
5. **Open a Pull Request** with a description

---

## 📄 License

A license for this project has not been chosen yet.

Until a license is added, contributions are welcome. Assume to be open sourced.

---

## 👨‍💻 Developer

**Alok Kumar**  
📧 Email: [02kumaralok@gmail.com](mailto:02kumaralok@gmail.com)  
🔗 LinkedIn: [linkedin.com/in/alok-kumar-3953a1321](https://www.linkedin.com/in/alok-kumar-3953a1321)  
💻 Project Repository: [TimeVault on GitHub](https://github.com/Alok-kumar2024/TimeVault-App)

---
