# 🎓 Campus Connect

A **campus social networking** Android app built with **Kotlin + Jetpack Compose** that combines:
- **Proximity discovery** — discover students you cross paths with on campus
- **Posting** — share updates, achievements, and opportunities
- **Real-time chat** — message friends and connections
- **Dual profiles** — social + professional information in one place

## 📱 Screenshots

> *Run the app on an emulator/device to see the full UI*

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **DI** | Hilt (Dagger) |
| **Navigation** | Compose Navigation |
| **Backend** | Firebase (Auth, Firestore, Storage) |
| **Location** | Google Play Services Location + Geohashing |
| **Image Loading** | Coil |
| **State Management** | ViewModel + StateFlow |

## 📂 Project Structure

```
com.campusconnect/
├── di/                      # Hilt dependency injection modules
├── data/
│   ├── model/               # Data classes (User, Post, Message, Crossing)
│   └── repository/          # Firebase repository implementations
├── ui/
│   ├── theme/               # Material 3 theme, colors, typography
│   ├── navigation/          # NavHost, routes, bottom navigation
│   ├── auth/                # Login, Register, Onboarding
│   ├── profile/             # Profile view/edit (social + professional)
│   ├── feed/                # Social feed, create post, comments
│   ├── discover/            # Proximity discovery, friend requests
│   └── chat/                # Conversations list, real-time chat
├── service/                 # Location foreground service + GeoHash
└── CampusConnectApp.kt      # @HiltAndroidApp application class
```

## 🚀 Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Steps

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd CampusConnect
   ```

2. **Firebase Setup**
   - Create a project at [Firebase Console](https://console.firebase.google.com)
   - Enable **Authentication** (Email/Password)
   - Enable **Cloud Firestore**
   - Enable **Firebase Storage**
   - Download `google-services.json` and place it in `app/`

3. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and click **Run**.

## ✨ Features

### 🔐 Authentication & Onboarding
- Email/password sign-up and sign-in via Firebase Auth
- 3-step onboarding: personal info → academic details → interest selection

### 👤 Dual Profile System
- **Social tab**: Bio, interests, friends list, academic info
- **Professional tab**: Skills, projects, experience, LinkedIn/GitHub links
- Edit all fields in a dedicated edit screen

### 📍 Proximity Discovery (Happn-like)
- Foreground service tracks location with geohash encoding
- Detects when two users are within ~1.2km (campus range)
- Shows crossing cards with "Wave 👋" action to send friend requests
- Tracks crossing count and timestamps

### 📝 Social Feed (LinkedIn-like)
- Create posts with text, images, and tags
- Like posts (with real-time count updates via Firestore transactions)
- Comment system with nested sub-collection
- Tag-based filtering (#hackathon, #placement, #clubs)

### 💬 Real-time Chat
- Conversations list with last message preview
- Real-time messaging via Firestore snapshot listeners
- Styled message bubbles (sent vs. received)
- Auto-scroll to latest messages

