# Architecture Deep Dive — Campus Connect

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │  Login    │  │  Feed    │  │ Discover │  │  Chat    │ │
│  │  Screen   │  │  Screen  │  │  Screen  │  │  Screen  │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
│       │              │              │              │       │
│  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐ │
│  │  Auth    │  │  Feed    │  │ Discover │  │  Chat    │ │
│  │ViewModel │  │ViewModel │  │ViewModel │  │ViewModel │ │
│  └────┬─────┘  └────┬─────┘  └────┴─────┘  └────┬─────┘ │
└───────┼──────────────┼──────────────┼──────────────┼──────┘
        │              │              │              │
┌───────┼──────────────┼──────────────┼──────────────┼──────┐
│       │         Domain Layer        │              │       │
│  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐ │
│  │  Auth    │  │  Feed    │  │ Discover │  │  Chat    │ │
│  │  Repo    │  │  Repo    │  │  Repo    │  │  Repo    │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
└───────┼──────────────┼──────────────┼──────────────┼──────┘
        │              │              │              │
┌───────┴──────────────┴──────────────┴──────────────┴──────┐
│                      Firebase Services                     │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────┐       │
│  │ Firebase │  │   Cloud      │  │   Firebase     │       │
│  │   Auth   │  │  Firestore   │  │   Storage      │       │
│  └──────────┘  └──────────────┘  └────────────────┘       │
└───────────────────────────────────────────────────────────┘
```

## Design Patterns Used

### 1. MVVM (Model-View-ViewModel)
- **View** = Jetpack Compose screens (stateless composables)
- **ViewModel** = holds UI state via `StateFlow`, calls repository methods
- **Model** = data classes + Firebase

**Why MVVM?** Google-recommended for Android. Survives configuration changes. Clean separation of UI and business logic.

### 2. Repository Pattern
Each feature has its own repository:
- `AuthRepository` — Firebase Auth + user profiles + profile photo uploads
- `FeedRepository` — posts, likes, comments, post image uploads
- `DiscoverRepository` — location, crossings, friend requests, friend management
- `ChatRepository` — conversations, messages

**Why?** Abstracts data source from ViewModel. Easy to swap Firebase for Room/REST API. Testable.

**Cross-Repository Usage**: ViewModels can depend on multiple repositories — e.g., `ChatViewModel` uses both `ChatRepository` (for conversations) and `DiscoverRepository` (for `removeFriend`). `FeedViewModel` uses both `FeedRepository` and `AuthRepository` (for friends-only feed filtering).

### 3. Dependency Injection (Hilt)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
    // ...
}
```
**Why Hilt over Koin/Manual?** Compile-time graph validation (catches errors early). Google-endorsed. Works seamlessly with `@HiltViewModel`.

### 4. Observer Pattern (Reactive Streams)
```
Firestore → callbackFlow → Repository → StateFlow → ViewModel → Compose UI
```
Data changes propagate automatically from Firestore to the UI without manual refresh.

**Pull-to-Refresh**: Each list-based screen (Feed, Discover, Chat) wraps its content in a reusable `PullRefreshLayout` that triggers the ViewModel's `refresh()` method. The ViewModel sets `isRefreshing = true`, re-collects the data flow, then resets `isRefreshing = false`. This adds user-initiated refresh on top of the automatic snapshot-driven updates.

### 5. Reactive Navigation (Auth State → Screen)
```
Firebase Auth state → AuthViewModel._uiState → LaunchedEffect in AppNavHost → navigate()
```
Navigation is driven by auth state, not imperative callbacks. When `isLoggedIn` becomes `false` (after signOut), the `LaunchedEffect` automatically navigates to the Login screen and clears the back stack.

## Data Flow Example: Sending a Message

```
1. User taps Send button
2. ChatScreen calls chatViewModel.sendMessage(conversationId, text)
3. ChatViewModel creates a Message object, calls chatRepository.sendMessage()
4. ChatRepository:
   a. Adds message to Firestore: conversations/{id}/messages/{auto}
   b. Updates conversation metadata: lastMessage, lastTimestamp
5. Firestore triggers snapshot listener
6. New message flows through callbackFlow → StateFlow → ChatScreen
7. LazyColumn recomposes with new message
8. Auto-scroll to bottom via LaunchedEffect
```

## Data Flow Example: Creating a Post with Image

```
1. User selects photo via ActivityResultContracts.PickVisualMedia
2. Content URI stored in local state (selectedImageUri)
3. User taps Post → feedViewModel.createPost(content, imageUri, ...)
4. FeedViewModel sets isPosting = true (button shows loading spinner)
5. FeedRepository.uploadPostImage():
   a. Creates ref at postImages/{timestamp}.jpg
   b. ref.putFile(imageUri).await() → uploads to Firebase Storage
   c. ref.downloadUrl.await() → gets CDN-backed download URL
6. Post data class created with imageUrl = downloadURL
7. postsCollection.add(post) → Firestore document created
8. FeedViewModel sets postCreated = true
9. LaunchedEffect in CreatePostScreen observes postCreated → navigates back
10. Feed's snapshot listener picks up new post with imageUrl
11. Coil's AsyncImage loads and renders the image
```

**Key insight**: Navigation back must wait for upload to complete — otherwise the content URI loses access permissions and the upload fails silently.

## Data Flow: Wave Button State Resolution

```
1. DiscoverViewModel loads three data sources in parallel:
   a. crossings/{uid}/people → list of crossed users
   b. friendRequests where fromUserId=uid AND status=PENDING → sentRequestUserIds
   c. users/{uid}.friends → friendIds
2. CrossingCard receives crossing + waveState computed as:
   - "friends" if otherUserId ∈ friendIds
   - "pending" if otherUserId ∈ sentRequestUserIds
   - "wave" otherwise
3. Each state renders a different button (green disabled, gray disabled, purple active)
4. All three sources are real-time (callbackFlow) so buttons update reactively
```

## Firestore Data Model

```
users/
  {uid}/
    name, email, college, department, year, bio, interests[],
    skills[], projects[], linkedinUrl, githubUrl, photoUrl,
    lastGeohash, lastLat, lastLng, lastLocationTimestamp,
    friends[], isOnboarded

posts/
  {postId}/
    authorId, authorName, authorPhotoUrl, content, imageUrl?,
    tags[], likes[], commentCount, timestamp
    comments/
      {commentId}/ → authorId, authorName, content, timestamp

crossings/
  {uid}/
    people/
      {otherUid}/ → crossingCount, firstCrossedAt, lastCrossedAt,
                     otherUserName, otherUserPhotoUrl, otherUserCollege

friendRequests/
  {fromUid}_{toUid}/    ← deterministic ID prevents duplicates
    fromUserId, toUserId, fromUserName, status (PENDING/ACCEPTED/DECLINED),
    timestamp

conversations/
  {conversationId}/
    participants[], participantNames{}, lastMessage, lastTimestamp
    messages/
      {messageId}/ → senderId, senderName, content, timestamp, isRead
```

## Key Architecture Decisions

| Decision | Alternative | Why I Chose This |
|----------|-------------|-----------------|
| Firestore over Realtime DB | Realtime Database | Better querying (compound queries, ordering), typed documents, sub-collections |
| StateFlow over LiveData | LiveData | Thread-safe, better Compose integration, supports operators like `map`, `combine` |
| Foreground Service for location | WorkManager | Need continuous tracking (every 30s), WorkManager has 15-min minimum |
| Geohash over Haversine | Calculate distance for every pair | O(1) query via string prefix vs O(n) for each user pair |
| Sealed class for routes | String constants | Type-safe navigation, compiler catches typos |
| callbackFlow for Firestore | addSnapshotListener directly | Integrates with coroutines ecosystem, proper cancellation |
| Deterministic request IDs | Auto-generated IDs | Prevents duplicate friend requests without extra query |
| Firestore transactions for accept | Simple write | Ensures idempotent acceptance, handles concurrent accepts |
| Reactive auth navigation | Manual navigate() calls | Single source of truth for auth-driven navigation; prevents stale screens after logout |
| Result<T> return type | Exceptions | Explicit error handling in ViewModels, no silent failures |
| Wait for upload before nav | Navigate immediately | Content URI from photo picker loses access after navigation, causing silent upload failure |
| Material 2 `pullRefresh` | Material 3 `PullToRefreshBox` | M3's `PullToRefreshBox` requires BOM 2024.09.00+ which needs Kotlin compiler extension upgrade; M2's `pullRefresh` modifier works with current BOM |
| Reusable `PullRefreshLayout` | Inline pull-refresh per screen | Single wrapper composable in `ui/components/` keeps all screens consistent and DRY |
