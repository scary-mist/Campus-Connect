# 🚀 Android Interview Killer Roadmap
### Kotlin + Jetpack Compose + Everything You Need to Dominate

---

## 📋 How to Use This Roadmap

- **Timeline**: 4–6 weeks if you study 3–4 hours/day
- **Priority**: Each section is marked 🔴 (must-know), 🟡 (frequently asked), or 🟢 (bonus points)
- **Practice**: After each section, explain the concept out loud in 2 minutes — that's your interview rehearsal
- **Your project**: Campus Connect covers most of these — always tie answers back to it

---

## Phase 1: Kotlin Mastery (Week 1)
> _"Every single Android interview starts with Kotlin fundamentals"_

### 🔴 Core Language (Day 1–2)
- [ ] **Null Safety** — `?`, `!!`, `?.`, `?:` (Elvis), `let`, `run`, `also`, `apply`
  - _Interview Q_: "How does Kotlin handle nulls at compile time vs Java?"
  - _Campus Connect tie-in_: `val uid = authRepository.currentUser?.uid ?: return`
- [ ] **Data Classes** — `copy()`, destructuring, `equals`/`hashCode` auto-generation
  - _Tie-in_: `Post`, `User`, `Message`, `Crossing` — all data classes
- [ ] **Sealed Classes / Sealed Interfaces** — exhaustive `when`, type-safe hierarchies
  - _Tie-in_: `NavRoutes` sealed class for type-safe navigation
- [ ] **Extension Functions** — adding functionality without inheritance
- [ ] **Higher-Order Functions & Lambdas** — `map`, `filter`, `fold`, `let`, `apply`
- [ ] **Scope Functions** — know when to use `let` vs `apply` vs `also` vs `run` vs `with`

### 🔴 Coroutines & Async (Day 3–4)
- [ ] **Coroutine Basics** — `suspend`, `launch`, `async`, `withContext`
- [ ] **Dispatchers** — `Main`, `IO`, `Default` — when to use each
  - _Interview Q_: "Why can't you do network calls on Main?"
- [ ] **viewModelScope** — auto-cancellation on ViewModel clear
  - _Tie-in_: Every ViewModel uses `viewModelScope.launch { ... }`
- [ ] **Structured Concurrency** — parent-child relationships, cancellation propagation
- [ ] **Exception Handling** — `try-catch` in coroutines, `CoroutineExceptionHandler`

### 🔴 Flows (Day 5–6)
- [ ] **Flow basics** — cold streams, `flow { emit() }`, `collect`
- [ ] **StateFlow** — hot stream, always has value, thread-safe
  - _Tie-in_: `_uiState = MutableStateFlow(UiState())`
- [ ] **SharedFlow** — for events (one-shot), replay cache
- [ ] **callbackFlow** — bridging callback APIs to Flows
  - _Tie-in_: All Firestore listeners wrapped in `callbackFlow`
- [ ] **Flow operators** — `map`, `filter`, `combine`, `flatMapLatest`, `debounce`
- [ ] **StateFlow vs LiveData** — know the comparison table cold

### 🟡 Advanced Kotlin (Day 7)
- [ ] **Generics** — `in`, `out`, variance, `reified`
- [ ] **Inline functions** — why `inline` helps with lambdas
- [ ] **Delegation** — `by lazy`, `by viewModels()`, property delegates
- [ ] **Result\<T\>** — functional error handling pattern
  - _Tie-in_: All repository methods return `Result<T>`
- [ ] **Collections** — `Sequence` vs `List`, lazy evaluation

### 📚 Resources
- [Kotlin Koans](https://play.kotlinlang.org/koans) (interactive exercises)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Roman Elizarov's talks on YouTube](https://www.youtube.com/results?search_query=roman+elizarov+coroutines) (coroutines inventor)

---

## Phase 2: Jetpack Compose (Week 2)
> _"This is THE skill companies are hiring for right now"_

### 🔴 Compose Fundamentals (Day 1–2)
- [ ] **Declarative vs Imperative UI** — why Compose over XML
  - _Interview Q_: "Explain the difference and why Google moved to Compose"
- [ ] **Composable functions** — `@Composable`, remember, state
- [ ] **Recomposition** — when and why composables re-execute
  - _Key concept_: Compose only recomposes what changed
- [ ] **State** — `remember`, `mutableStateOf`, `rememberSaveable`
- [ ] **State Hoisting** — stateless composables, lifting state up
- [ ] **Side Effects** — `LaunchedEffect`, `DisposableEffect`, `SideEffect`
  - _Tie-in_: `LaunchedEffect(authState.isLoggedIn)` for reactive navigation

### 🔴 Compose UI Components (Day 3–4)
- [ ] **Layouts** — `Column`, `Row`, `Box`, `Scaffold`, `Surface`
- [ ] **Lists** — `LazyColumn`, `LazyRow`, `items()`, `key` parameter
  - _Interview Q_: "Why does `key` matter in LazyColumn?"
- [ ] **Material 3** — `Card`, `Button`, `TextField`, `TopAppBar`, `BottomNavBar`
- [ ] **Modifiers** — ordering matters! `padding` vs `background` order changes result
- [ ] **Theming** — `MaterialTheme`, custom color schemes, typography
- [ ] **Navigation** — `NavHost`, `composable()`, passing arguments, back stack

### 🟡 Advanced Compose (Day 5–6)
- [ ] **Animations** — `animate*AsState`, `AnimatedVisibility`, `animateContentSize`
- [ ] **Custom Layouts** — `Layout` composable, measure/place
- [ ] **Performance** — `derivedStateOf`, stable types, `@Immutable`
- [ ] **Testing** — `composeTestRule`, `onNodeWithText`, `performClick`
- [ ] **Compose + ViewModel** — `hiltViewModel()`, `collectAsState()`
  - _Tie-in_: `val uiState by viewModel.uiState.collectAsState()`

### 📚 Resources
- [Official Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)
- [Compose Pathway (Google Codelab)](https://developer.android.com/courses/pathways/compose)
- [Philipp Lackner YouTube](https://www.youtube.com/@PhilippLackner) (best Compose tutorials)

---

## Phase 3: Android Architecture (Week 3)
> _"This separates juniors from seniors in interviews"_

### 🔴 MVVM Pattern (Day 1–2)
- [ ] **ViewModel** — survives config changes, `viewModelScope`
- [ ] **UiState pattern** — single data class for screen state
  - _Tie-in_: `FeedUiState(posts, isLoading, isRefreshing, isPosting, postCreated, error)`
- [ ] **Unidirectional Data Flow (UDF)** — events go up, state comes down
- [ ] **Repository Pattern** — abstracts data sources
  - _Tie-in_: 4 repositories in Campus Connect
- [ ] **MVVM vs MVI vs MVP** — know the trade-offs

### 🔴 Dependency Injection (Day 3)
- [ ] **Why DI?** — testability, decoupling, lifecycle management
- [ ] **Hilt** — `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`
  - _Tie-in_: `AppModule` provides Firebase singletons
- [ ] **Hilt Scopes** — `@Singleton`, `@ViewModelScoped`, `@ActivityScoped`
- [ ] **Multi-repo ViewModels** — one ViewModel, multiple repositories
  - _Tie-in_: `ChatViewModel` uses both `ChatRepository` and `DiscoverRepository`

### 🔴 Android Components (Day 4–5)
- [ ] **Activity Lifecycle** — `onCreate`, `onResume`, `onPause`, `onDestroy`
- [ ] **Services** — Foreground vs Background vs Bound
  - _Tie-in_: `LocationService` foreground service
- [ ] **Permissions** — runtime vs install-time, permission launchers
  - _Tie-in_: Location permission request flow
- [ ] **Intents** — explicit vs implicit, share intents
- [ ] **Notifications** — channels, builders, PendingIntent

### 🟡 Data Layer (Day 6–7)
- [ ] **Room Database** — entities, DAOs, migrations (offline caching)
- [ ] **DataStore** — replacing SharedPreferences
- [ ] **Retrofit + OkHttp** — REST API calls (even if you use Firebase, know this)
- [ ] **Repository Pattern** — combining Room + Retrofit for offline-first
- [ ] **Paging 3** — infinite scroll with `PagingSource`

### 📚 Resources
- [Guide to App Architecture](https://developer.android.com/topic/architecture)
- [Hilt Codelab](https://developer.android.com/codelabs/android-hilt)
- [Now in Android (Google's reference app)](https://github.com/android/nowinandroid)

---

## Phase 4: Firebase & Backend (Week 4)
> _"You built with Firebase — own it and explain it deeply"_

### 🔴 Firestore (Day 1–2)
- [ ] **Document Model** — documents, collections, sub-collections
- [ ] **CRUD Operations** — `add`, `set`, `update`, `delete`, `get`
- [ ] **Queries** — `where`, `orderBy`, `limit`, compound queries
- [ ] **Real-time Listeners** — `addSnapshotListener`, offline persistence
- [ ] **Transactions** — optimistic concurrency, retry behavior
  - _Tie-in_: Likes, friend acceptance, crossing rate-limiting
- [ ] **Batch Writes** — atomic multi-document operations
- [ ] **Indexes** — composite indexes, `FAILED_PRECONDITION` errors
  - _Tie-in_: `firestore.indexes.json`
- [ ] **Security Rules** — `request.auth`, `resource.data`

### 🔴 Firebase Auth (Day 3)
- [ ] **Email/Password** — `createUser`, `signIn`, `signOut`
- [ ] **Auth State Listener** — `AuthStateListener`
- [ ] **Google Sign-In** — OAuth flow (know conceptually even if not implemented)

### 🟡 Firebase Storage (Day 4)
- [ ] **Upload/Download** — `putFile`, `downloadUrl`
  - _Tie-in_: Profile photos, post images
- [ ] **Storage Rules** — `request.auth != null`
- [ ] **Content URI Lifecycle** — why URIs can lose permissions
  - _Tie-in_: The image upload bug you debugged!

### 🟢 Cloud Functions & FCM (Day 5)
- [ ] **Cloud Functions** — serverless triggers (onWrite, onCall)
- [ ] **FCM Push Notifications** — server-to-device messaging
- [ ] **When to use what** — Firestore listeners vs FCM vs polling

### 📚 Resources
- [Firebase for Android](https://firebase.google.com/docs/android/setup)
- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Firebase YouTube Channel](https://www.youtube.com/firebase)

---

## Phase 5: System Design & DSA (Week 5)
> _"For mid-senior roles and top companies"_

### 🔴 System Design for Mobile (Day 1–3)
- [ ] **Design a Chat App** — Firestore/WebSocket, message ordering, presence
- [ ] **Design a Social Feed** — fan-out-on-write vs fan-out-on-read
- [ ] **Design a Location-based App** — geohashing, spatial indexing
  - _Tie-in_: You literally built this!
- [ ] **Caching Strategies** — LRU, offline-first, cache invalidation
- [ ] **Pagination** — cursor-based vs offset-based
- [ ] **API Design** — RESTful principles, even if you use Firebase
- [ ] **Rate Limiting** — for friend requests, location updates
  - _Tie-in_: 5-min crossing cooldown

### 🟡 Data Structures & Algorithms (Day 4–7)
- [ ] **Arrays & Strings** — two pointers, sliding window, hashing
- [ ] **LinkedList** — reversal, cycle detection, merge
- [ ] **Trees** — BST, traversals, BFS/DFS
- [ ] **Graphs** — BFS, DFS, shortest path (Dijkstra)
- [ ] **Sorting** — quicksort, mergesort, complexity analysis
- [ ] **Dynamic Programming** — memoization, tabulation (top 10 patterns)
- [ ] **Time/Space Complexity** — Big O analysis for every answer

### 📚 Resources
- [Grokking the System Design Interview](https://www.designgurus.io/course/grokking-the-system-design-interview)
- [LeetCode](https://leetcode.com) — focus on Medium problems
- [NeetCode](https://neetcode.io) — structured problem sets with video explanations
- [System Design Primer (GitHub)](https://github.com/donnemartin/system-design-primer)

---

## Phase 6: Interview Execution (Week 6)
> _"Knowing the content is 50%. Presenting it is the other 50%."_

### 🔴 Mock Interview Practice (Daily)
- [ ] **STAR Method** for behavioral: Situation → Task → Action → Result
- [ ] **Project walkthrough**: Practice the 30-second and 2-minute pitch until it's natural
- [ ] **Challenge stories**: Practice telling 3–4 challenge stories from CHALLENGES_FACED.md (including the pull-to-refresh BOM compatibility story)
- [ ] **Whiteboard coding**: Solve 2 LeetCode mediums/day, explain your thought process aloud
- [ ] **System design mock**: Design a feature (chat, feed, notification) in 30 minutes

### 🔴 Common Android Interview Questions
- [ ] "Explain the Activity lifecycle and how it relates to ViewModel"
- [ ] "What's the difference between StateFlow and LiveData?"
- [ ] "How does Compose decide what to recompose?"
- [ ] "Explain the repository pattern and why you'd use it"
- [ ] "How do Firestore transactions work internally?"
- [ ] "What is geohashing and why would you use it?"
- [ ] "How do you prevent memory leaks in Android?"
- [ ] "Explain Hilt and how dependency injection works"
- [ ] "How would you scale your app to 100K users?"
- [ ] "What was the hardest bug you faced and how did you fix it?"

### 🟡 Behavioral Questions
- [ ] "Tell me about a time you had to debug a difficult issue" → Image upload bug
- [ ] "How do you handle conflicting requirements?" → Privacy vs discovery
- [ ] "Describe a technical decision you made and the trade-offs" → Geohash vs Haversine
- [ ] "How do you learn new technologies?" → Your experience building Campus Connect

---

## 🗓️ Weekly Study Schedule

| Day | Morning (1.5h) | Afternoon (1.5h) | Evening (1h) |
|-----|----------------|-------------------|---------------|
| **Mon** | Kotlin/Compose concept study | Build/modify a mini feature | 2 LeetCode problems |
| **Tue** | Architecture deep dive | Campus Connect code walkthrough | 2 LeetCode problems |
| **Wed** | Firebase & backend concepts | Practice interview Q&A aloud | 2 LeetCode problems |
| **Thu** | System design topic | Design a feature on paper | 2 LeetCode problems |
| **Fri** | Review weak areas | Mock interview with a friend | Read engineering blog posts |
| **Sat** | Full mock interview (2h) | Review & fix gaps | Rest |
| **Sun** | Review Campus Connect code | Prepare stories/pitches | Light review |

---

## 🎯 The "Kill It" Checklist — Before Your Interview

- [ ] I can explain Campus Connect in 30 seconds without pausing
- [ ] I can explain MVVM + Repository + Hilt in 2 minutes
- [ ] I can draw the Firestore data model on a whiteboard
- [ ] I can explain geohashing from scratch with an example
- [ ] I can explain 3 challenges I faced and how I solved them
- [ ] I can explain Compose recomposition and state management
- [ ] I can write a ViewModel with StateFlow on a whiteboard
- [ ] I can explain Firestore transactions and why they're needed
- [ ] I can solve a Medium LeetCode in 25 minutes
- [ ] I can design a simplified chat/feed system in 30 minutes
- [ ] I can explain the difference between coroutines, threads, and callbacks
- [ ] I can talk about trade-offs (not just "what I used" but "why I chose it over alternatives")

---

## 💡 Pro Tips

1. **Always tie back to Campus Connect** — interviewers love concrete examples. Don't say "I know about transactions" — say "I used transactions in my project to handle concurrent likes"

2. **Show debugging skills** — the image upload bug and logout navigation bug are GOLD for interviews. They show you can think about system-level interactions

3. **Quantify everything** — "O(1) proximity queries", "30-second intervals", "4 repositories", "15+ screens", "sub-second message delivery"

4. **Mention trade-offs** — "I chose client-side filtering over Firestore's whereIn because of the 30-element limit" shows maturity

5. **Show growth** — "Initially I was swallowing errors with getOrNull(), but I learned to surface them via Result<T> and Snackbar" shows self-improvement

6. **Know what you DON'T know** — "For production, I'd add Room for offline caching and Cloud Functions for push notifications" shows self-awareness

7. **Code confidently** — Practice writing ViewModels, Composables, and repository methods by hand. The syntax should flow naturally.

---

_Good luck! You've built something real — now go show them._ 🔥
