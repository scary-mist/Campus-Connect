# Project Overview — Campus Connect

## 30-Second Pitch
> "I built **Campus Connect**, an Android app using **Kotlin + Jetpack Compose** that helps college students network on campus. It has a **Happn-like proximity discovery** feature — when two students physically cross paths, they appear on each other's 'Discover' feed and can connect. It also has a **LinkedIn-like posting feed** with photo uploads and **pull-to-refresh** for sharing updates and opportunities, **real-time chat** with friend management, and a **dual social + professional profile**. The backend uses **Firebase (Auth, Firestore, Storage)** with a **geohash-based location system** for efficient proximity queries."

## 2-Minute Pitch
> "The problem I identified is that students on large campuses often sit next to someone in class or the library but never actually connect. Existing apps like LinkedIn are too professional, and Instagram is too casual.
>
> So I built **Campus Connect** — a social networking app specifically for campus life. It has 5 key feature areas:
>
> **1. Proximity Discovery**: Inspired by Happn, a foreground service tracks your location and encodes it as a geohash. When another user has a matching geohash prefix (within ~1.2km), a 'crossing' is recorded. You see a feed of people you've crossed paths with, along with how many times. The feed intelligently filters out inactive users (last seen > 30 min ago) and rate-limits crossing counts (5-min cooldown) to keep the data meaningful.
>
> **2. Smart Friend Request System (Wave)**: The crossing card shows context-aware buttons — 'Wave' to send a request, 'Pending' if you've already sent one, or 'Friends' if you're connected. Requests use deterministic document IDs to prevent duplicates, and acceptance uses Firestore transactions for idempotent operation. Upon acceptance, a chat conversation is automatically created so users can start messaging immediately.
>
> **3. Social Feed**: Users can create posts with text, photos (uploaded to Firebase Storage), links, and hashtags. Posts support likes (via Firestore transactions for concurrent writes) and a comment system using sub-collections. The feed is friends-only — each user sees only posts from their friend network. I also added **pull-to-refresh** on the Feed, Discover, and Chat screens using a reusable `PullRefreshLayout` component.
>
> **4. Real-time Chat with Friend Management**: After connecting, users can chat in real-time using Firestore snapshot listeners. There's also the ability to remove friends directly from the chat list, which atomically cleans up both users' friend lists, the conversation, and any request documents.
>
> **5. Dual Profile**: Each profile has a Social tab (bio, interests) and Professional tab (skills, projects, LinkedIn/GitHub links) — bridging casual and professional networking. LinkedIn and GitHub URLs are clickable and open directly in the browser with automatic `https://` normalization. Users can also upload profile photos to Firebase Storage.
>
> Architecturally, I used **MVVM + Clean Architecture** with **Hilt DI** and **Compose Navigation**. The app is fully reactive — StateFlow drives the UI, and Firestore snapshot listeners provide real-time data. Auth state changes (login/logout) are handled reactively via a `LaunchedEffect` that observes the auth state and automatically navigates to the appropriate screen."

## Why This Tech Stack?

| Choice | Why |
|--------|-----|
| **Kotlin** | Industry standard for Android; null safety, coroutines, data classes |
| **Jetpack Compose** | Modern declarative UI; less boilerplate than XML; better state handling |
| **Material 3** | Latest design system; dynamic theming; accessibility built-in |
| **Hilt** | Official DI solution from Google; compile-time validation; integrates with ViewModels |
| **Firebase Auth** | Fast auth setup; handles edge cases (password reset, email verification) |
| **Firestore** | Real-time sync (no need for WebSockets); offline support; scales automatically |
| **Firebase Storage** | CDN-backed file storage for profile photos and post images |
| **Geohashing** | O(1) proximity queries instead of O(n) distance calculations |
| **Coil** | Kotlin-first image loading; Compose integration; lightweight |
| **Material 2 pullRefresh** | Used for custom pull-to-refresh component; Material 3 `PullToRefreshBox` requires newer BOM |
| **StateFlow** | Thread-safe state holder; lifecycle-aware; better than LiveData for Compose |

## What Makes This Project Stand Out for Interviews

1. **Real-world system design** — location tracking, proximity detection, real-time messaging
2. **Concurrency handling** — Firestore transactions for likes, friend acceptance; deterministic IDs to prevent duplicates
3. **Android services** — foreground service with notification for location tracking
4. **Clean architecture** — separation of concerns with repository pattern
5. **State machine thinking** — Wave button shows Wave/Pending/Friends based on relationship status
6. **Reusable components** — `PullRefreshLayout` wrapping Material 2's `pullRefresh` API; `LoadingSpinner` wrapping `CircularProgressIndicator`; `LinkRow` for clickable URLs with auto `https://` normalization
7. **Multiple feature verticals** — auth, social, messaging, location, photo uploads — each with its own complexity
8. **Error handling** — proper error surfacing (e.g., upload failure shown via Snackbar), `Result<T>` pattern throughout
9. **Reactive navigation** — auth state drives navigation via `LaunchedEffect`, not imperative callbacks
