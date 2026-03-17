# Technical Deep Dives — Campus Connect

## 1. Geohashing — How Proximity Discovery Works

### The Problem
Given N users with (lat, lng) coordinates, find all pairs of users within ~1.2km of each other.

**Naive approach**: For each user, compute Haversine distance to every other user → O(n²) comparisons per update.

### The Solution: Geohashing

Geohashing is a **space-filling curve** that maps 2D coordinates to a 1D string.

```
Latitude:  30.7603    →    Geohash: "ttncq7"
Longitude: 76.7838

Precision levels:
  1 char  ≈ 5,000 km
  2 chars ≈ 1,250 km
  3 chars ≈ 156 km
  4 chars ≈ 39 km
  5 chars ≈ 5 km
  6 chars ≈ 1.2 km   ← Campus level
  7 chars ≈ 153 m
  8 chars ≈ 38 m
```

### How the Algorithm Works

```
1. Input: lat = 30.7603, lng = 76.7838

2. Interleave binary of lat/lng:
   - Longitude range [-180, 180] → binary subdivision
   - Latitude range [-90, 90] → binary subdivision
   - Alternate bits: lng, lat, lng, lat...

3. Group into 5-bit chunks, map to Base32:
   [01101] [10100] [01110] [01100] [10001] [10111]
      t        t        n        c        q        7

4. Result: "ttncq7"
```

### Firestore Query
```kotlin
val prefix = geohash.take(6)  // "ttncq7"
val endPrefix = "ttncq8"      // next alphabetical

firestore.collection("users")
    .whereGreaterThanOrEqualTo("lastGeohash", prefix)
    .whereLessThan("lastGeohash", endPrefix)
    .get()
```

This is a **single indexed range query** — O(log n) in Firestore, massively faster than checking every user.

### Boundary Problem & Solution
Two people 10 meters apart could have different 6-char geohash prefixes if they're on a tile boundary.

**Solution**: Query the 8 neighboring geohash tiles as well. This is a constant 9 queries total, still O(log n) each.

### Stale User Filtering & Rate-Limiting
To keep the Discover feed meaningful:
```kotlin
// Only show users active in the last 30 minutes
val cutoff = System.currentTimeMillis() - (30 * 60 * 1000)
val activeUsers = nearbyUsers.filter {
    (it.getLong("lastLocationTimestamp") ?: 0) > cutoff
}

// Rate-limit crossings: only increment if last crossing > 5 min ago
firestore.runTransaction { transaction ->
    val lastCrossedAt = doc.getLong("lastCrossedAt") ?: 0
    if (System.currentTimeMillis() - lastCrossedAt > 5 * 60 * 1000) {
        transaction.update(ref, "crossingCount", currentCount + 1)
        transaction.update(ref, "lastCrossedAt", System.currentTimeMillis())
    }
}
```

---

## 2. Firestore Transactions — Concurrent Like Handling

### The Problem
Two users click "Like" on the same post at the exact same time. Without transactions:
```
User A reads: likes = ["user1", "user2"]
User B reads: likes = ["user1", "user2"]
User A writes: likes = ["user1", "user2", "userA"]
User B writes: likes = ["user1", "user2", "userB"]  ← Overwrites A's like!
```

### The Solution: Firestore Transactions
```kotlin
firestore.runTransaction { transaction ->
    val snapshot = transaction.get(docRef)        // 1. Read inside transaction
    val likes = snapshot.toObject(Post::class.java)?.likes?.toMutableList()
    if (userId in likes) likes.remove(userId)     // 2. Toggle
    else likes.add(userId)
    transaction.update(docRef, "likes", likes)     // 3. Write
}
```

Firestore automatically retries if the document changed between read and write. It guarantees **serializable isolation**.

### How Firestore Transactions Work Internally
1. Client reads the document and notes the **version**
2. Client sends the write with the expected version
3. Server checks: if version matches → apply. Otherwise → reject and client retries
4. This is **optimistic concurrency control** — no locks, retry on conflict

---

## 3. Friend Request System — Deterministic IDs & Idempotent Acceptance

### The Problem
Without safeguards, a user could send multiple friend requests by tapping "Wave" repeatedly, or acceptance could run twice and corrupt data.

### Solution 1: Deterministic Document IDs
```kotlin
val requestId = "${fromUserId}_${toUserId}"
firestore.collection("friendRequests").document(requestId).set(request)
```
Since the document ID is deterministic (`userA_userB`), calling `set()` multiple times is naturally idempotent — it overwrites the same document instead of creating duplicates.

### Solution 2: Idempotent Acceptance via Transaction
```kotlin
firestore.runTransaction { transaction ->
    val user1 = transaction.get(userRef1).toObject(User::class.java)
    val user2 = transaction.get(userRef2).toObject(User::class.java)

    // Only add if not already friends (idempotent guard)
    if (user1 != null && fromUserId !in (user1.friends)) {
        transaction.update(userRef1, "friends", user1.friends + fromUserId)
    }
    if (user2 != null && toUserId !in (user2.friends)) {
        transaction.update(userRef2, "friends", user2.friends + toUserId)
    }
}
// Then DELETE the request document (cleanup)
firestore.collection("friendRequests").document(requestId).delete()
```

### Solution 3: Context-Aware Button States (Wave → Pending → Friends)
```kotlin
// DiscoverViewModel loads three real-time data sources
val crossings: Flow<List<Crossing>>             // who you've crossed
val sentRequests: Flow<List<FriendRequest>>      // outbound pending waves
val friendIds: Set<String>                       // current friend list

// CrossingCard resolves button state
val waveState = when {
    otherUserId in friendIds -> "friends"         // green ✓ button
    otherUserId in sentRequestUserIds -> "pending" // gray ⏳ button
    else -> "wave"                                 // purple 👋 button
}
```

---

## 4. Kotlin Coroutines + callbackFlow — Bridging Firebase

### The Problem
Firebase uses callback-based APIs (listeners), but our app uses Kotlin Flows (coroutines).

### The Solution: callbackFlow
```kotlin
fun getPostsFeed(visibleUserIds: List<String>): Flow<List<Post>> = callbackFlow {
    val listener = postsCollection
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val allPosts = snapshot?.documents?.mapNotNull {
                it.toObject(Post::class.java)?.copy(id = it.id)
            } ?: emptyList()
            // Client-side filter: show only posts from friends
            val filtered = if (visibleUserIds.isNotEmpty()) {
                allPosts.filter { it.authorId in visibleUserIds }
            } else allPosts
            trySend(filtered)
        }
    awaitClose { listener.remove() }
}
```

**Key concepts:**
- `callbackFlow` creates a `Flow` backed by a channel
- `trySend()` pushes values into the flow (non-suspending)
- `awaitClose` is called when the flow collector cancels — here we remove the listener
- This prevents memory leaks from orphaned listeners
- Friends-only filtering is done client-side to keep the Firestore query simple

### Data Flow Pipeline
```
Firestore SnapshotListener
    → callbackFlow (trySend)
        → Flow<List<Post>>
            → ViewModel collects → updates MutableStateFlow
                → Compose collects StateFlow → Recomposition
```

---

## 5. Android Foreground Service — Location Tracking

### Why a Foreground Service?
Starting from Android 8 (API 26), background location access is heavily restricted. You MUST use a foreground service with a visible notification for continuous location tracking.

### Service Lifecycle
```
1. User toggles "Discovery" ON
2. DiscoverScreen sends Intent(ACTION_START) to LocationService
3. Service calls startForeground(notification) — shows persistent notification
4. FusedLocationProvider requests updates every 30s
5. Each update → compute geohash → write to Firestore → check nearby users
6. User toggles OFF → Intent(ACTION_STOP) → stopSelf()
7. onDestroy() removes location callback and cancels coroutine scope
```

### Error Handling for Location
```kotlin
try {
    val intent = Intent(context, LocationService::class.java).apply {
        action = LocationService.ACTION_START
    }
    context.startForegroundService(intent)
} catch (e: Exception) {
    Log.e("LocationService", "Failed to start: ${e.message}")
}
```
The service start/stop is wrapped in try-catch to handle cases where the system denies the foreground service (e.g., battery optimization, missing permissions).

### Why FusedLocationProvider over LocationManager?
- **Battery efficient**: Google Play Services optimizes GPS, Wi-Fi, and cell triangulation
- **PRIORITY_BALANCED_POWER_ACCURACY**: Uses Wi-Fi/cell (not GPS) unless available
- **Batching**: Can batch multiple updates to reduce wake-ups

---

## 6. Image Upload — Firebase Storage + Content URI Lifecycle

### The Problem
Users select photos via Android's photo picker (`ActivityResultContracts.PickVisualMedia`), which returns a `content://` URI. Uploading to Firebase Storage must complete **before** navigating away from the screen, because the content URI may lose access permissions.

### The Solution: Async Upload with Completion Signal
```kotlin
// FeedViewModel
fun createPost(content: String, imageUri: Uri?, ...) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isPosting = true)

        // Upload image (must complete before navigation!)
        var uploadedImageUrl: String? = null
        if (imageUri != null) {
            val result = feedRepository.uploadPostImage(imageUri)
            result.fold(
                onSuccess = { url -> uploadedImageUrl = url },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        error = "Image upload failed: ${e.message}"
                    )
                    return@launch  // Abort post creation
                }
            )
        }

        // Create post with image URL
        val post = Post(imageUrl = uploadedImageUrl, ...)
        feedRepository.createPost(post)
        _uiState.value = _uiState.value.copy(postCreated = true)
    }
}

// CreatePostScreen — navigate back ONLY after upload completes
LaunchedEffect(uiState.postCreated) {
    if (uiState.postCreated) {
        feedViewModel.clearPostCreated()
        onNavigateBack()
    }
}
```

### Why This Matters
If you navigate away immediately after calling `createPost()`:
1. The coroutine continues in `viewModelScope`
2. But the content URI (e.g., `content://media/picker/...`) may be revoked
3. `ref.putFile(uri)` fails silently → post created without image
4. The `getOrNull()` pattern swallows the error

**Lesson learned**: Always surface upload errors via `Result<T>` and wait for completion before navigation.

---

## 7. Jetpack Compose — State Management & Recomposition

### State Architecture
```
@HiltViewModel
class FeedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FeedUiState())  // Private, mutable
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()  // Public, immutable
}

@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val state by viewModel.uiState.collectAsState()  // Triggers recomposition
    // Compose reads `state.posts` — only recomposes if `posts` changed
}
```

### Why StateFlow > LiveData for Compose?
| Feature | StateFlow | LiveData |
|---------|-----------|----------|
| Thread safety | ✅ Safe from any thread | ❌ Must set from main thread |
| Initial value | ✅ Always has a value | ❌ Can be null |
| Operators | ✅ map, filter, combine | ❌ Limited transformations |
| Compose integration | ✅ collectAsState() | ⚠️ observeAsState() (needs lifecycle) |

### LazyColumn Performance
```kotlin
LazyColumn {
    items(posts, key = { it.id }) { post ->  // `key` enables smart item reuse
        PostCard(post = post)
    }
}
```
The `key` parameter lets Compose identify items across recompositions. If a post is added/removed, only the affected items recompose — not the entire list.

---

## 8. Reactive Auth Navigation

### The Problem
When a user signs out, the app should immediately navigate to the Login screen. But if the logout button calls `navController.navigate()` directly alongside `signOut()`, there's a race condition — the screen may try to load data with a null user before navigation completes.

### The Solution: Declarative, State-Driven Navigation
```kotlin
// AppNavHost.kt
LaunchedEffect(authState.isLoggedIn, authState.isOnboarded) {
    when {
        !authState.isLoggedIn -> {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
        authState.isLoggedIn && !authState.isOnboarded -> {
            navController.navigate(NavRoutes.Onboarding.route) { ... }
        }
    }
}

// ProfileScreen logout — just update state, navigation is automatic
IconButton(onClick = { onSignOut() })  // calls authViewModel.signOut()

// AuthViewModel.signOut()
fun signOut() {
    authRepository.signOut()
    _uiState.value = AuthUiState(isAuthChecking = false)  // isLoggedIn = false
}
```
The navigation is **reactive**: changing `isLoggedIn` is the single cause, and navigation is the effect. No imperative `navigate()` calls scattered across screens.

---

## 9. Remove Friend — Atomic Cleanup

### The Operation
Removing a friend requires cleaning up **four** data locations:
1. Remove from current user's `friends[]` array
2. Remove from other user's `friends[]` array
3. Delete the conversation between them
4. Delete any friend request documents between them

### Implementation
```kotlin
suspend fun removeFriend(currentUid: String, friendUid: String): Result<Unit> {
    // Step 1+2: Atomic friend removal via transaction
    firestore.runTransaction { transaction ->
        val user1 = transaction.get(userRef1).toObject(User::class.java)
        val user2 = transaction.get(userRef2).toObject(User::class.java)
        transaction.update(userRef1, "friends", user1.friends.filter { it != friendUid })
        transaction.update(userRef2, "friends", user2.friends.filter { it != currentUid })
    }.await()

    // Step 3: Find and delete conversation
    val convos = firestore.collection("conversations")
        .whereArrayContains("participants", currentUid).get().await()
    convos.documents.forEach { doc ->
        if ((doc.get("participants") as? List<*>)?.contains(friendUid) == true) {
            doc.reference.delete().await()
        }
    }

    // Step 4: Cleanup request documents (both directions)
    firestore.collection("friendRequests").document("${currentUid}_${friendUid}").delete()
    firestore.collection("friendRequests").document("${friendUid}_${currentUid}").delete()
}
```

### UI: Confirm Before Destructive Action
The remove friend option appears as a three-dot menu (⋮) on each conversation item with a confirmation `AlertDialog` before proceeding.

---

## 10. Pull-to-Refresh — Reusable Component with BOM Compatibility

### The Problem
The app's list-based screens (Feed, Discover, Chat) relied solely on Firestore snapshot listeners for data updates. Users had no way to manually trigger a refresh, which felt unresponsive especially when opening the app after being away.

### The BOM Compatibility Challenge
Material 3's `PullToRefreshBox` requires Compose BOM `2024.09.00`+ (Material3 1.3.0+), which requires a newer Kotlin compiler extension than the project's current `1.5.8`. Upgrading the BOM would cascade into upgrading Kotlin, AGP, and potentially break other dependencies.

### The Solution: Material 2's `pullRefresh` Modifier
Instead of upgrading the entire dependency chain, I used Material 2's `pullRefresh` API (available via `androidx.compose.material:material`) and created a reusable wrapper:

```kotlin
// PullRefreshLayout.kt — reusable component
@Composable
fun PullRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing, onRefresh = onRefresh
    )
    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        content()
        PullRefreshIndicator(
            refreshing = isRefreshing, state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = GradientStart
        )
    }
}
```

### ViewModel Pattern: `isRefreshing` + `refresh()`
Each ViewModel exposes `isRefreshing` in its UiState and a public `refresh()` method:
```kotlin
data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,  // drives PullRefreshIndicator
    // ...
)

fun refresh() {
    _uiState.value = _uiState.value.copy(isRefreshing = true)
    loadPosts() // re-collects the Flow; resets isRefreshing on completion
}
```

### Key Design Decisions
1. **`isRefreshing` vs `isLoading`**: Separate flags so the full-screen loading spinner only shows on initial load, while the pull indicator shows on subsequent refreshes
2. **Reusable component**: `PullRefreshLayout` lives in `ui/components/` and wraps any scrollable content — DRY across 3 screens
3. **Material 2 + Material 3 coexistence**: The project uses Material 3 for all UI, but adds Material 2 _only_ for the `pullRefresh` modifier — both libraries can coexist in the same project
