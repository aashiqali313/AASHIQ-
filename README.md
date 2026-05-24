# <p align="center"><img src="https://img.shields.io/badge/AASHIQ%2B-Offline%20Learning%20Series-FFB300?style=for-the-badge&logo=android&logoColor=ffffff" alt="AASHIQ+" width="380" /></p>

<p align="center">
  <img src="https://img.shields.io/github/actions/workflow/status/user/repo/android.yml?branch=main&style=flat-square&label=Build%20Status&color=00C853" alt="Build Status"/>
  <img src="https://img.shields.io/badge/Platform-Android-00C853?style=flat-square&logo=android" alt="Platform"/>
  <img src="https://img.shields.io/badge/Language-Kotlin%20%2F%20Compose-7F52FF?style=flat-square&logo=kotlin" alt="Language"/>
  <img src="https://img.shields.io/badge/Architecture-MVVM%20%2F%20Clean-FF6F00?style=flat-square" alt="Architecture"/>
</p>

---

**AASHIQ+** is a state-of-the-art, premium, offline-first native Android education platform. Crafted in modern **Jetpack Compose** and **Kotlin**, AASHIQ+ empowers users to download and store dense, media-rich interactive courses directly on local devices for seamless, latency-free exploration without an internet connection.

---

## 📥 Direct Download & Installation Guide

By default, every code update you push to your GitHub repository automatically triggers a cloud build which compiles a ready-to-run APK. Anyone in the world can fetch and install the app in seconds!

### How to Download your App:

1. **Visit the Actions Tab:**
   Navigating to the **Actions** tab at the top of your GitHub repository page:
   ```
   https://github.com/YOUR_USERNAME/YOUR_REPOSITORY_NAME/actions
   ```
2. **Select the Latest Build:**
   Click on the topmost run (usually titled `"Build Android APK"` or matching your latest commit message).
3. **Download the APK Artifact:**
   Scroll down to the bottom of the run page to finding the **Artifacts** group. Click on **`app-debug`** to download the ZIP file containing your installable APK!
4. **Install on Android:**
   Uncompress the downloaded ZIP, move the `app-debug.apk` to your phone, and open it to install. 

> 💡 **Sharing with Friends:** You can easily make a permanent download link by going to **Releases** on your GitHub page, clicking "Draft a new release", uploading this `app-debug.apk` file, and publishing it. That way, anyone can download your finished app with a single click!

---

## ✨ Features Breakdown

*   🤖 **Edge-to-Edge Visuals**: Fully immersive interface styled with modern Material 3 design systems and responsive layout structures.
*   📺 **Cinematic ExoPlayer Engine**: High-fidelity local video rendering equipped with multi-speed control (`0.5x` to `2.0x`), quick forward/rewind, and seamless volume/brightness swipe gesture overlays.
*   📝 **Dynamic Markdown Notes**: Native interactive markdown viewer for local course attachments featuring a custom paragraph rendering hierarchy, bullets, headers, and quote blocks.
*   💾 **Local Bookmarks Drawer**: Instantly pin your favorite video timestamps inside any lesson and sync them to your collection to resume watching anytime.
*   🎨 **Adaptable Theme Controls**: Full dynamic support of standard system light/dark skins alongside an eye-safe, custom-colored Warm Light theme ensuring text reading comfort across all configurations.

---

## 🛠️ Tech Stack & Architecture

*   **UI System:** Jetpack Compose (Material Design 3)
*   **Language:** Kotlin (100% type-safe)
*   **Media Core:** Google Media3 ExoPlayer
*   **Database:** Room (SQLite local state cache)
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Theme Management:** Dynamic Scheme Adaptation (Dark & Warm Light)

---

## 🚀 Setting Up the Development Workspace

To build, test, and run the project locally on your system using Android Studio:

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY_NAME.git

# Enter the directory
cd YOUR_REPOSITORY_NAME

# Synchronize resources and let Gradle fetch dependencies
# Open the project directory directly inside Android Studio (Java 17 required)
```

Designed with ❤️ for high-performance offline learning. 
