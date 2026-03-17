# Challenges Faced During Development — Campus Connect

This document captures the real challenges I faced while building Campus Connect, how I diagnosed each issue, and the solutions I implemented. These are excellent talking points for interviews — they demonstrate debugging skills, system-level thinking, and iterative problem solving.

---

## Challenge 1: App Crash on Discover Screen — Location Permissions

### Problem
The app crashed immediately when opening the Discover tab. The crash was caused by trying to start a foreground service and access `FusedLocationProviderClient` without first requesting runtime location permissions.

### Root Cause
Android requires `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions to be granted at runtime (not just declared in the manifest). The `LocationService` also requires `POST_NOTIFICATIONS` permission on Android 13+ for the foreground notification.

### How I Fixed It
- Added runtime permission requests using `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` before starting the service
- Wrapped `startForegroundService()` and `stopService()` in try-catch blocks to gracefully handle cases where the system rejects the service start
- Added null-safety checks for `FusedLocationProviderClient` initialization

### Interview Talking Point
> "I learned that Android's permission model requires careful sequencing — you can't just declare permissions in the manifest. I implemented a graceful degradation pattern where the app functions without location but prompts the user to enable it."

---

## Challenge 2: Duplicate Friend Requests and Inflated Friend Counts

### Problem
Users could tap the "Wave" button multiple times rapidly, creating duplicate friend request documents. When accepted, the same pair could be added to each other's friend lists multiple times, inflating the count.

### Root Cause
Auto-generated Firestore document IDs are unique per write, so each tap created a new document. The acceptance logic didn't check if users were already friends.

### How I Fixed It
- **Deterministic document IDs**: Changed from `collection.add()` to `collection.document("${fromUid}_${toUid}").set()`. Since the ID is always the same for the same pair, repeated writes just overwrite the same document
- **Idempotent acceptance**: Added a guard in the acceptance transaction — `if (fromUserId !in user.friends)` — to skip the update if already friends
- **Request cleanup**: After acceptance, the request document is deleted to prevent re-acceptance

### Interview Talking Point
> "I used deterministic document IDs as a form of natural idempotency — the same operation always targets the same document. Combined with Firestore transactions, this ensures correctness even under concurrent access."

---

## Challenge 3: Feed Showing Posts from All Users Instead of Friends Only

### Problem
Every post from every user appeared in the feed, regardless of whether they were friends. The app should only show posts from the user's friend network.

### Root Cause
The `getPostsFeed()` function was fetching all posts without any filtering.

### How I Fixed It
- Modified `getPostsFeed()` to accept a `visibleUserIds` parameter (friends + self)
- Applied client-side filtering: `allPosts.filter { it.authorId in visibleUserIds }`
- `FeedViewModel` fetches the user's friend list from `AuthRepository` and passes it to the feed query
- Chose client-side filtering because Firestore's `whereIn` has a 30-element limit, which wouldn't scale

### Interview Talking Point
> "I evaluated Firestore's `whereIn` (limited to 30 elements) versus client-side filtering. For a campus app where friend lists could exceed 30, client-side filtering with a broad timestamp-ordered query was the pragmatic choice."

---

## Challenge 4: Stale Users Appearing on Discover Feed

### Problem
The Discover tab showed users who had been inactive for hours or even days. A user who opened the app once in a building and then left would keep appearing for everyone in that area.

### Root Cause
The proximity query (`findNearbyUsers`) had no freshness filter — it returned everyone with a matching geohash, regardless of when they were last active.

### How I Fixed It
- Added a `lastLocationTimestamp` field to each user document, updated with every location write
- Filtered the query results to only include users active within the last 30 minutes
- Added rate-limiting to `recordCrossing()` — the crossing count only increments if the last crossing was more than 5 minutes ago, preventing inflation when two people stay in the same area

### Interview Talking Point
> "I added temporal filtering on top of spatial filtering. Geohashing solves the 'where' problem; the timestamp filter solves the 'when' problem. The 5-minute crossing cooldown prevents count inflation from co-located users."

---

## Challenge 5: Logout Didn't Navigate to Sign-In Screen

### Problem
Tapping the logout button would sign the user out of Firebase, but the app stayed on the Profile screen (showing a loading spinner). It never navigated back to the Login screen.

### Root Cause
There were actually two bugs:
1. The `ProfileScreen` called `profileViewModel.signOut()` which only called Firebase's `auth.signOut()` — but never updated the `AuthViewModel`'s `isLoggedIn` state
2. The `AppNavHost` had a `LaunchedEffect` watching `authState.isLoggedIn` for reactive navigation, but since the state never changed, the navigation never triggered

### How I Fixed It
- Changed the logout flow to call `authViewModel.signOut()` instead of `profileViewModel.signOut()`
- `AuthViewModel.signOut()` properly: (1) calls `authRepository.signOut()`, and (2) resets `_uiState` to `AuthUiState(isAuthChecking = false)` which sets `isLoggedIn = false`
- The `LaunchedEffect` in `AppNavHost` detects `isLoggedIn = false` and navigates to Login with `popUpTo(0) { inclusive = true }` (clearing the entire back stack)

### Interview Talking Point
> "This was a classic 'two sources of truth' bug. The ProfileViewModel was directly calling Firebase sign-out, bypassing the AuthViewModel that controlled navigation state. The fix was making AuthViewModel the single source of truth for auth state, with navigation being a reactive side effect."

---

## Challenge 6: Post Images Not Showing Despite Successful Selection

### Problem
Users could select a photo when creating a post (the preview showed correctly on the Create Post screen), but after posting, the image never appeared in the feed — only the text was visible.

### Root Cause (Phase 1 — Timing)
The `CreatePostScreen` was calling `onNavigateBack()` immediately after `feedViewModel.createPost()`. But `createPost()` launches a background coroutine — the navigation happened before the image upload completed. When the user navigated away, the content URI (`content://media/picker/...`) from the photo picker potentially lost its access permissions.

### Root Cause (Phase 2 — Silent Failure)
The upload error was being silently swallowed by `.getOrNull()`. The post was being created with `imageUrl = null` and no error was shown to the user.

### Root Cause (Phase 3 — Permissions)
Even after fixing the timing, the upload still failed because Firebase Storage security rules were set to deny all writes by default.

### How I Fixed It
1. **Wait for upload**: Added a `postCreated` flag to `FeedUiState`. The Post button triggers `createPost()` which sets `isPosting = true` (showing a loading spinner). Only after the upload succeeds and the post is saved does it set `postCreated = true`. A `LaunchedEffect` in `CreatePostScreen` observes this flag and navigates back only when it's true
2. **Surface errors**: Replaced `.getOrNull()` with explicit `Result.fold()` that logs the error and sets `uiState.error`. Added a `Snackbar` to show the error message to the user
3. **Firebase Storage rules**: Updated security rules to `allow read, write: if request.auth != null;`

### Interview Talking Point
> "This bug taught me three things: (1) Android content URIs have a lifecycle tied to the component that received them, (2) `.getOrNull()` is dangerous for operations that should never fail silently, and (3) always verify cloud service security rules early. The multi-layered root cause — timing, error swallowing, and permissions — is typical of real-world debugging."

---

## Challenge 7: Firestore Composite Index Errors

### Problem
Several Firestore queries failed at runtime with errors like `FAILED_PRECONDITION: The query requires an index`. This affected the chat screen (conversations query), friend requests, and the feed.

### Root Cause
Firestore requires composite indexes for queries that use multiple `where` clauses or combine `where` with `orderBy`. These indexes must be created manually in the Firebase console or via `firestore.indexes.json`.

### How I Fixed It
- Created a `firestore.indexes.json` file with all required composite indexes
- Deployed indexes via Firebase CLI: `firebase deploy --only firestore:indexes`
- Added indexes for: conversations (participants + lastTimestamp), friendRequests (fromUserId + status), friendRequests (toUserId + status), posts (timestamp ordering)

### Interview Talking Point
> "Firestore's indexing requirement is a trade-off for read performance — it pre-computes query results. I learned to watch for `FAILED_PRECONDITION` errors and proactively define indexes for new compound queries."

---

## Challenge 8: CircularProgressIndicator and LinearGradient Crashes

### Problem
The app crashed on certain screens with `IllegalArgumentException` from `CircularProgressIndicator` (zero size) and `LinearGradient` (identical start/end offsets).

### Root Cause
- `CircularProgressIndicator` with a `size(0.dp)` modifier crashes because it can't draw a circle with zero diameter
- `Brush.linearGradient` crashes if `start` and `end` offsets are identical (e.g., both `Offset.Zero`)

### How I Fixed It
- Created a custom `LoadingSpinner` composable with a minimum size guarantee
- Used `Brush.linearGradient(colors = ..., start = Offset.Zero, end = Offset(1000f, 1000f))` to ensure non-identical offsets
- Replaced all direct `CircularProgressIndicator` calls with the safe `LoadingSpinner`

### Interview Talking Point
> "These crashes were caused by edge cases in Compose's drawing system. I created a reusable safe wrapper (`LoadingSpinner`) and replaced all call sites — a pattern of defensive programming that prevents similar crashes across the app."

---

## Challenge 9: Profile Loading Spinner After Sign-Out

### Problem
After signing out, the Profile screen would show a loading spinner indefinitely instead of navigating to login.

### Root Cause
The `ProfileScreen` had a `LaunchedEffect(Unit)` that called `profileViewModel.loadProfile()` on entry. After sign-out, `loadProfile()` found `uid == null` and returned early — but it never set `isLoading = false`, leaving the spinner spinning forever.

### How I Fixed It
- Modified `loadProfile()` to explicitly set `isLoading = false` when `uid` is null
- Changed the logout flow to call `onSignOut()` (which triggers reactive navigation) — so the Profile screen is removed from the navigation stack before the loading state matters

### Interview Talking Point
> "This was a lesson in making every code path set a terminal state. The 'early return' pattern is convenient but dangerous if it skips state cleanup. I now always ensure that loading flags are reset regardless of the exit path."

---

## Challenge 10: Chat ViewModel Needing Multiple Repositories

### Problem
The "Remove Friend" feature requires removing the friend from both users' friend lists, deleting the conversation, and cleaning up request documents. This logic lives in `DiscoverRepository`, but the UI trigger is in the Chat screen which uses `ChatViewModel`.

### Root Cause
Initially, each ViewModel was tied 1:1 to a single repository. Cross-cutting features like "remove friend" don't fit neatly into one domain.

### How I Fixed It
- Modified `ChatViewModel` to accept both `ChatRepository` and `DiscoverRepository` via Hilt constructor injection
- Added a `removeFriend(friendUid: String)` method to `ChatViewModel` that delegates to `discoverRepository.removeFriend()`
- Hilt handles the multi-dependency injection automatically

### Interview Talking Point
> "This challenge taught me that real-world features often cut across clean architectural boundaries. The solution was simple — a ViewModel can depend on multiple repositories. Hilt made this trivial since it manages the entire dependency graph."

---

## Summary: Key Lessons Learned

| # | Challenge | Core Lesson |
|---|-----------|-------------|
| 1 | Location permission crash | Always request runtime permissions before using protected APIs |
| 2 | Duplicate friend requests | Use deterministic IDs + transactions for idempotent operations |
| 3 | Feed showing all posts | Choose between server-side and client-side filtering based on constraints |
| 4 | Stale users in Discover | Combine spatial filtering (geohash) with temporal filtering (timestamp) |
| 5 | Logout not navigating | Single source of truth for auth state; reactive navigation |
| 6 | Post images not showing | Content URI lifecycle; never swallow errors silently; verify cloud rules |
| 7 | Firestore index errors | Pre-define composite indexes for compound queries |
| 8 | Drawing crashes | Validate inputs to drawing APIs; create safe wrapper composables |
| 9 | Loading spinner stuck | Every code path must reach a terminal state (set isLoading = false) |
| 10 | Cross-cutting features | ViewModels can depend on multiple repositories; Hilt makes this easy |
| 11 | Pull-to-refresh BOM compatibility | Material 2 + Material 3 can coexist; reusable wrapper components keep code DRY |

---

## Challenge 11: Pull-to-Refresh — Material 3 BOM Compatibility

### Problem
The app's list screens (Feed, Discover, Chat) had no pull-to-refresh, so users couldn't manually trigger a data reload. The natural choice was Material 3's `PullToRefreshBox`, but it wasn't available in the project's Compose BOM (`2024.01.00`).

### Root Cause
`PullToRefreshBox` was introduced in Material3 1.3.0, which requires Compose BOM `2024.09.00`+. Upgrading the BOM would require upgrading the Kotlin compiler extension from `1.5.8` and potentially AGP — a cascading dependency chain.

### How I Fixed It
- Added `androidx.compose.material:material` (Material 2) as a dependency — it provides the `pullRefresh` modifier and `PullRefreshIndicator` which works with the current BOM
- Created a reusable `PullRefreshLayout` composable in `ui/components/` that wraps any content with pull-refresh functionality
- Added `isRefreshing: Boolean` to each ViewModel's UiState and a public `refresh()` method that sets `isRefreshing = true` and re-triggers data loading
- Kept `isRefreshing` separate from `isLoading` so initial load shows a full-screen spinner while refreshes show only the pull indicator

### Interview Talking Point
> "I learned that Material 2 and Material 3 can coexist in the same project — you don't have to go all-or-nothing. By pulling in just the `pullRefresh` API from Material 2, I avoided a risky BOM upgrade while still delivering the feature. The reusable wrapper pattern kept it DRY across three screens."
