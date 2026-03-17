# Interview Q&A — Campus Connect

## 🔹 General / Behavioral

### Q1: Tell me about a project you've built.
**A:** "I built Campus Connect, a campus social networking app using Kotlin + Jetpack Compose. It combines Happn-like proximity discovery — where you see people you've physically crossed paths with — with a LinkedIn-style posting feed with photo uploads, real-time chat with friend management, and dual social/professional profiles. The backend is Firebase with geohash-based location queries. I also built a smart friend request system with context-aware button states and handled several challenging real-world bugs around concurrent data access and content URI lifecycle management."

### Q2: Why did you choose to build this project?
**A:** "I noticed that students on large campuses often sit near the same people in class or the library but never actually connect. Existing platforms don't solve this — LinkedIn is too professional, Instagram too casual. I wanted to combine the social aspect with professional networking, and the proximity feature adds a unique real-world component."

### Q3: What was the most challenging part?
**A:** "There were several challenging aspects. First, the proximity discovery — tracking location efficiently, encoding it as geohashes, and filtering out stale users. Second, the friend request system — I had to prevent duplicate requests using deterministic document IDs, ensure idempotent acceptance with Firestore transactions, and build a reactive button that shows Wave/Pending/Friends states. Third, the image upload flow — I learned that Android content URIs from the photo picker can lose access permissions when navigating away from the screen, so I had to restructure the flow to wait for upload completion before navigation."

### Q4: What would you do differently if you restarted?
**A:** "I'd add a proper domain/use-case layer with interfaces for better testability. I'd also implement an offline-first strategy with Room as a local cache, add Firebase Cloud Messaging for push notifications, and build a more robust error-handling framework from the start rather than adding it incrementally."

---

## 🔹 Architecture & Design

### Q5: Explain your app architecture.
**A:** "MVVM with Clean Architecture concepts. Compose Screens observe StateFlow from ViewModels. ViewModels call Repository methods. Repositories abstract Firebase operations and expose Kotlin Flows via callbackFlow. Hilt handles dependency injection across all layers. Navigation is reactive — auth state changes automatically drive screen navigation via LaunchedEffect."

### Q6: Why MVVM over MVI or MVP?
**A:** "MVVM is the Google-recommended pattern for Android. The ViewModel survives configuration changes natively, and StateFlow maps perfectly to Compose's reactive model. MVI would add unnecessary complexity for this scope. MVP requires manual lifecycle management."

### Q7: How does Hilt work in your project?
**A:** "I annotate the Application class with `@HiltAndroidApp` and Activity with `@AndroidEntryPoint`. An `AppModule` provides Firebase singletons (Auth, Firestore, Storage). ViewModels use `@HiltViewModel` and get repositories via constructor injection. Some ViewModels depend on multiple repositories — for example, ChatViewModel uses both ChatRepository and DiscoverRepository for the remove friend feature. Hilt generates the dependency graph at compile time."

### Q8: How do you handle navigation?
**A:** "I use Compose Navigation with a sealed class `NavRoutes` for type-safe routes. The `AppNavHost` hosts all routes. The start destination is determined by auth state — if not logged in, show Login; if not onboarded, show Onboarding; otherwise, show Feed. Navigation is reactive: a `LaunchedEffect` observes `authState.isLoggedIn` and automatically navigates to the Login or Feed screen when auth state changes. This prevents issues like stale screens after logout."

### Q9: How do you manage state?
**A:** "Each ViewModel has a private `MutableStateFlow<UiState>` and exposes a public immutable `StateFlow`. Compose screens collect this flow and recompose on changes. Some state objects track multiple concerns — for example, `FeedUiState` has `posts`, `isLoading`, `isRefreshing`, `isPosting`, `postCreated`, and `error`. The `isRefreshing` flag was added to support pull-to-refresh — it's separate from `isLoading` so the pull indicator shows during manual refresh while the full-screen spinner only shows on initial load. For the post creation flow, I use a `postCreated` flag in the UI state to signal navigation after async upload completes. For profile links, I use `LocalUriHandler` with a reusable `LinkRow` composable that handles URL normalization (auto-prepending `https://`) and opens links in the browser."

---

## 🔹 Firebase & Backend

### Q10: Why Firebase over a custom backend?
**A:** "Firebase provides real-time sync out of the box, so chat messages and feed updates appear instantly. It also handles auth, file storage (for profile and post photos), and offline caching. For a campus-scale app, Firestore's auto-scaling is sufficient, and it let me focus on the app rather than server infrastructure."

### Q11: How do you handle data consistency?
**A:** "For likes, I use Firestore's `runTransaction` to atomically read the current likes array, add/remove the user ID, and write it back. For friend requests, I use deterministic document IDs (`{fromUid}_{toUid}`) to prevent duplicate requests without needing an extra query. Friend acceptance uses a transaction to idempotently add both users to each other's friend lists, check if they're already friends, and then delete the request document. The remove friend operation also uses a transaction to atomically update both users."

### Q12: Explain your Firestore data model.
**A:** "Users are in a top-level `users` collection keyed by UID, with a `friends[]` array. Posts are in `posts` with comments as sub-collections and an optional `imageUrl` field pointing to Firebase Storage. Conversations use `conversations/{id}/messages/{id}` for nested real-time listening. Crossings use `crossings/{uid}/people/{otherUid}` for per-user queries. Friend requests use deterministic IDs like `{fromUid}_{toUid}` to prevent duplicates."

### Q13: How do you handle real-time updates?
**A:** "Firestore's `addSnapshotListener` fires whenever the queried data changes. I wrap it in Kotlin's `callbackFlow`, which bridges callback-based APIs to the coroutines world. When the Flow collector cancels, `awaitClose` removes the listener — preventing memory leaks. I use this for the feed, chat messages, conversations list, incoming friend requests, and sent pending requests."

### Q14: How do you handle offline mode?
**A:** "Firestore has built-in offline persistence. Writes are queued locally and synced when the device reconnects. For a production version, I'd add Room as a structured local cache and implement a sync strategy."

### Q15: How do you handle image uploads?
**A:** "Images (profile photos and post photos) are uploaded to Firebase Storage and the resulting CDN-backed download URL is stored in Firestore. The key challenge was that the photo picker returns a content:// URI that can lose access permissions when the picker's screen is navigated away. I solved this by making the post creation screen wait for the upload to complete (via a `postCreated` flag in the UI state) before navigating back. Upload failures are surfaced to the user via a Snackbar."

---

## 🔹 Proximity Discovery (Geohashing)

### Q16: How does the Happn-like feature work?
**A:** "A foreground service requests location updates every 30 seconds. Each location is encoded into a 7-character geohash. The geohash is written to the user's Firestore document. To find nearby users, I query for users whose geohash shares a 6-character prefix (≈1.2km radius). I also filter out users who haven't been active in the last 30 minutes to keep the feed fresh. Each match is recorded as a 'crossing' — but I rate-limit the count increment to once per 5 minutes to prevent inflation from users standing in the same place."

### Q17: What is geohashing and why use it?
**A:** "Geohashing encodes a (lat, lng) pair into a Base32 string where each character narrows the area. Two nearby locations share a common prefix. This lets me do proximity queries with a simple string range query in Firestore instead of computing Haversine distance for every user pair. It reduces complexity from O(n) to O(1) per query."

### Q18: What are the limitations of geohashing?
**A:** "The main issue is **boundary effects** — two points that are physically close can have different geohash prefixes if they're on opposite sides of a geohash tile boundary. The solution is to also query the 8 neighboring geohash tiles. Another limitation is that geohash cells are rectangles, not circles."

### Q19: Why a foreground service instead of WorkManager?
**A:** "WorkManager's minimum interval is 15 minutes, which is too infrequent for crossing detection — a student could walk past you and leave in 5 minutes. A foreground service allows 30-second updates. The trade-off is showing a persistent notification, which I handle with a subtle 'Discovering people nearby...' notification."

### Q20: How do you handle battery optimization?
**A:** "I use `PRIORITY_BALANCED_POWER_ACCURACY` instead of high accuracy, set a 50-meter minimum displacement filter, and 30-second intervals. Users can toggle discovery on/off. I also wrap the service start in try-catch to handle cases where the system denies the foreground service."

---

## 🔹 Friend Request & Social Features

### Q21: How does the Wave/friend request system work?
**A:** "When a user taps 'Wave' on a crossing card, a friend request document is created with a deterministic ID. The crossing card then shows 'Pending' instead of 'Wave', using a real-time listener on sent pending requests. If the other user accepts, my listener detects the request deletion and the friends list update, switching the button to 'Friends'. If they decline, the request is deleted and the button goes back to 'Wave' — all reactively, no polling."

### Q22: How do you prevent duplicate friend requests?
**A:** "I use deterministic document IDs: `{fromUserId}_{toUserId}`. Since Firestore's `set()` is idempotent on the same document path, calling it multiple times just overwrites the same document. I also check for existing requests in both directions before creating."

### Q23: How does the remove friend feature work?
**A:** "It's a four-step atomic cleanup: (1) remove from both users' friends arrays using a Firestore transaction, (2) delete the conversation between them, (3) delete any friend request documents. The UI uses a three-dot menu with a confirmation dialog before proceeding. The conversation list updates automatically via the Firestore snapshot listener."

### Q24: How does the friends-only feed work?
**A:** "The FeedViewModel first fetches the current user's profile to get their friend list. It passes `visibleUserIds` (friends + self) to the FeedRepository. The repository queries all posts ordered by timestamp but filters client-side to only include posts from visible users. I chose client-side filtering over a Firestore `whereIn` because `whereIn` has a 30-element limit."

---

## 🔹 Chat System

### Q25: How does real-time chat work?
**A:** "Messages are stored in `conversations/{id}/messages` sub-collection, ordered by timestamp. I attach a `snapshotListener` via `callbackFlow` that fires on every new message. The Flow emits the full message list, which StateFlow holds, and the Compose LazyColumn recomposes. A `LaunchedEffect` auto-scrolls to the latest message."

### Q26: How do you handle finding/creating conversations?
**A:** "When a user wants to chat with someone (or when they accept a friend request), I first query conversations where `participants` array contains their UID, then filter client-side for the other user. If no conversation exists, I create one with both UIDs and their profile info. When a friend request is accepted, the conversation is created automatically as part of the acceptance flow so users can start chatting immediately."

### Q27: How would you add typing indicators?
**A:** "I'd add a `typingUsers` map field to the conversation document. When a user starts typing, update the field with their UID and a timestamp. The snapshot listener would pick this up instantly. Clear the field on send or after a 3-second debounce."

---

## 🔹 Performance & Optimization

### Q28: How do you optimize the feed?
**A:** "I limit the initial query to 50 posts via `.limit(50)`. Posts are ordered by timestamp descending. The feed only shows posts from friends (client-side filter). Images are loaded lazily by Coil with memory caching. I also added pull-to-refresh using a reusable `PullRefreshLayout` component that wraps Material 2's `pullRefresh` modifier — this lets users swipe down to re-fetch data on all list screens. For pagination, I'd use Firestore's `startAfter()` cursor."

### Q29: How do you prevent memory leaks?
**A:** "Firestore listeners are wrapped in `callbackFlow` with `awaitClose` that removes the listener on cancellation. ViewModels scope coroutines to `viewModelScope` which cancels on ViewModel clear. The LocationService cancels its `CoroutineScope` in `onDestroy()`."

### Q30: How does Compose optimize recompositions?
**A:** "Compose only recomposes composables whose inputs changed. I pass stable types (data classes, primitive types) to composables. LazyColumn uses `key` to efficiently add/remove items. I use `remember` for expensive calculations."

---

## 🔹 Security & Edge Cases

### Q31: How do you handle user privacy?
**A:** "Location data is only collected when the user explicitly enables discovery. Exact coordinates are never exposed to other users — only the fact that a crossing occurred. Users can toggle location tracking on/off at any time."

### Q32: What Firestore security rules would you set?
**A:** "Users can only read/write their own profile. Posts are readable by any authenticated user but only deletable by the author. Messages are readable only by conversation participants. Crossings are readable only by the owning user. Firebase Storage allows read/write by any authenticated user for profile and post photos."

### Q33: How do you handle network failures?
**A:** "Firestore's offline persistence queues writes automatically. For UI feedback, I return `Result<T>` from repository methods — ViewModels inspect the result and set error messages in the UI state. For image uploads specifically, errors are surfaced via Snackbar on the create post screen."

---

## 🔹 Scalability (System Design Round)

### Q34: How would you scale this to 100K+ users?
**A:** "Key points:
1. **Replace foreground service with push-based**: Cloud Functions trigger on location write to check nearby users
2. **Shard Firestore**: Partitioned collections by campus/region
3. **CDN for media**: Firebase Storage already uses Google CDN
4. **Denormalize data**: Store author name/photo in each post to avoid extra reads
5. **Cloud Functions for fan-out**: When a post is created, create feed entries for followers"

### Q35: What would a v2 look like?
**A:** "Events calendar (campus events with RSVP), study group matching, anonymous Q&A board, push notifications via FCM, image compression pipeline, dark/light theme toggle, end-to-end chat encryption, message read receipts, and story/status feature with 24-hour expiry."

### Q36: How did you implement pull-to-refresh, and why did you use Material 2's API instead of Material 3's?
**A:** "I needed pull-to-refresh on Feed, Discover, and Chat screens. Material 3's `PullToRefreshBox` requires Compose BOM 2024.09.00+, which would need upgrading the Kotlin compiler extension and potentially break other dependencies. Instead, I used Material 2's `pullRefresh` modifier, which works with the current BOM, and created a reusable `PullRefreshLayout` composable that wraps any content with a pull-refresh indicator. Each ViewModel exposes an `isRefreshing` state (separate from `isLoading`) and a `refresh()` method. This keeps initial loading separate from manual refreshes — the full-screen spinner shows only on first load, while the pull indicator shows on swipe-down refresh."
