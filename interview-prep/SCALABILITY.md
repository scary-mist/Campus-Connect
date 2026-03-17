# Scalability — How to Scale Campus Connect

## Current Architecture → Scale-Ready Architecture

### Current (Single Campus, <1K Users)
```
Mobile App → Firebase Auth → Firestore (single region) → Firebase Storage
                              ↕
                        Location Service (30s updates per user)
```

### Scaled (Multi-Campus, 100K+ Users)

```
Mobile App → Firebase Auth → Firestore (sharded by campus)
     ↓                            ↕
FCM Push ← Cloud Functions ← Pub/Sub Event Bus
                                  ↕
                          Redis (presence/typing)
                                  ↕
                        Cloud Storage + CDN
```

---

## Bottleneck #1: Location Writes (Main Concern)

### Problem
100K users × 1 write/30s = **3,333 writes/second** to `users` collection.

Firestore limit: **10,000 writes/sec per collection** — close to the limit.

### Solutions

1. **Shard by campus/region**
   ```
   campuses/{campusId}/users/{uid}
   ```
   Each campus gets its own user collection → writes distributed across shards.

2. **Reduce write frequency**
   - Increase to 60s intervals (still useful for campus proximity)
   - Don't write if geohash hasn't changed

3. **Separate location collection**
   ```
   locations/{uid} → { geohash, timestamp }
   ```
   Separate from user profiles to avoid heavy writes on user documents.

---

## Bottleneck #2: Proximity Queries

### Problem
For each location update, querying all users with matching geohash → lots of reads.

### Solutions

1. **Cloud Functions trigger**
   ```
   onWrite("locations/{uid}") → find nearby users → create crossings
   ```
   Moves computation to server. Client only writes location; server handles matching.

2. **GeoFire library**
   Purpose-built geospatial indexing for Firebase. Uses precision-based listeners.

3. **Time-scoped queries**
   Only query users whose `lastLocationTimestamp` is within 5 minutes → reduces read set.

---

## Bottleneck #3: Feed Fan-Out

### Problem
When a popular user creates a post, everyone must see it → all followers query for it.

### Solutions

1. **Fan-out on write** (recommended for <100K users)
   When a post is created, Cloud Function writes a reference to each friend's feed collection:
   ```
   feeds/{userId}/posts/{postId} → { postId, timestamp }
   ```
   Each user's feed is a pre-computed collection — read is O(1).

2. **Fan-out on read** (for 100K+ users)
   Popular users' posts are fetched at read time. Combine with cached feed entries.

3. **Pagination with cursors**
   ```kotlin
   postsCollection.orderBy("timestamp").startAfter(lastDoc).limit(20)
   ```

---

## Bottleneck #4: Chat Scalability

### Current: Firestore Snapshot Listeners
Works well up to ~10K concurrent conversations. Each listener is a persistent connection.

### At Scale:
1. **Firebase Cloud Messaging (FCM)** for push notifications instead of always-on listeners
2. **Presence system** via Firebase Realtime Database (optimized for high-frequency reads)
3. **Message pagination** — load last 50 messages, paginate on scroll up
4. **Group chats** — use sub-collections with composite indexes

---

## Estimated Costs at Scale

| Users | Writes/day | Reads/day | Estimated Cost/month |
|-------|-----------|-----------|---------------------|
| 1K | 2.8M | 5M | ~$5 (free tier covers most) |
| 10K | 28M | 50M | ~$50–100 |
| 100K | 280M | 500M | ~$500–1000 |

> **Interview tip**: Always mention cost when discussing scalability. It shows you think about real-world engineering.

---

## Migration Path

| Phase | Change | Effort |
|-------|--------|--------|
| Phase 1 | Add Firestore indexes + query optimization | Low |
| Phase 2 | Extract location to separate collection | Medium |
| Phase 3 | Add Cloud Functions for crossing detection | Medium |
| Phase 4 | Shard by campus | High |
| Phase 5 | Add Redis for presence/typing | High |
| Phase 6 | Custom backend (Go/Node) + PostgreSQL | Very High |
