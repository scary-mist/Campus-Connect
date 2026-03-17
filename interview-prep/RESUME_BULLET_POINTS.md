# Resume Bullet Points — Campus Connect

## Option 1: Concise (2 bullets)
```
• Built Campus Connect, a campus social networking Android app using Kotlin, Jetpack Compose, and
  Firebase with Happn-like proximity discovery (geohash-based), LinkedIn-style posting with photo
  uploads, real-time chat, and a smart friend request system with reactive button states

• Implemented MVVM + Clean Architecture with Hilt DI, Firestore transactions for concurrent data
  handling, Firebase Storage for media uploads, and a foreground service for battery-optimized
  background location tracking
```

## Option 2: Detailed (4 bullets)
```
• Developed Campus Connect, a full-stack Android app (Kotlin/Jetpack Compose) enabling campus-wide
  social networking with 5 core features: proximity discovery, smart friend requests, social feed
  with photo uploads, real-time chat with friend management, and dual profiles

• Engineered a Happn-like proximity system using geohash encoding for O(1) location queries with
  stale-user filtering (30-min inactivity) and rate-limited crossing counts (5-min cooldown)

• Built an idempotent friend request system using deterministic Firestore document IDs and
  transactions, with context-aware UI states (Wave/Pending/Friends) driven by real-time listeners

• Architected using MVVM pattern with Hilt DI, reactive auth-driven navigation via LaunchedEffect,
  Firebase Storage integration for image uploads with proper content URI lifecycle management, and
  pull-to-refresh on list screens using a reusable component wrapping Material 2's pullRefresh API
```

## Option 3: For System Design-Focused Roles (Goldman Sachs, etc.)
```
• Designed and implemented Campus Connect, an Android social platform featuring geohash-based
  proximity detection (O(1) spatial queries), Firestore transactions for idempotent operations
  (friend acceptance, concurrent likes), and reactive real-time communication via snapshot
  listeners and Kotlin Flows

• Applied scalable architectural patterns: deterministic document IDs to prevent duplicate writes,
  client-side feed filtering for friends-only visibility, foreground service with battery-optimized
  location tracking (50m displacement filter, balanced power priority), reactive state-driven
  navigation, pull-to-refresh with reusable PullRefreshLayout component, and atomic multi-document
  cleanup for friend removal
```

## Keywords to Highlight
When discussing in interviews, make sure to name-drop these technologies naturally:
- **Kotlin** (coroutines, StateFlow, sealed classes, data classes, Result<T>)
- **Jetpack Compose** (declarative UI, recomposition, LazyColumn, LaunchedEffect)
- **MVVM** + **Clean Architecture**
- **Hilt** (dependency injection, compile-time graph, multi-repo ViewModels)
- **Firebase** (Auth, Firestore, Storage)
- **Geohashing** (spatial indexing, proximity queries, stale filtering)
- **Firestore Transactions** (optimistic concurrency control, idempotent operations)
- **Foreground Service** (Android system service, notification channel)
- **callbackFlow** (bridging callbacks to coroutines)
- **Compose Navigation** (type-safe routing, reactive navigation)
- **Pull-to-refresh** (Material 2 `pullRefresh` modifier, reusable composable wrapper, `isRefreshing` state)
- **Content URI lifecycle** (photo picker, access permissions, async upload)
- **Deterministic IDs** (preventing duplicate writes without extra queries)

## How to Describe Your Contribution
Since this is a solo project, you can say:
- "I independently designed, architected, and built..."
- "I made key technical decisions, including choosing geohashing over Haversine distance..."
- "I optimized for battery life by using balanced location priority with displacement filters..."
- "I debugged and resolved a subtle issue where content URIs from the photo picker lost access permissions during navigation..."
- "I designed a state machine for the Wave button that reactively updates between three states..."

## Metrics You Can Quote
- **5 core features**: proximity discovery, friend requests, social feed, chat, dual profile
- **15+ screens** with animations and transitions
- **4 repositories**, **4+ ViewModels**, **clean separation of concerns**
- **Real-time**: sub-second message delivery via Firestore listeners
- **30-second** location update interval with 50m displacement filter
- **O(1)** proximity queries via geohash prefix matching
- **3-state** context-aware Wave button (Wave → Pending → Friends)
- **Idempotent** friend acceptance using deterministic IDs + transactions
- **Pull-to-refresh** on 3 screens via reusable `PullRefreshLayout` component
- **Firebase Storage** integration for profile photos and post images
