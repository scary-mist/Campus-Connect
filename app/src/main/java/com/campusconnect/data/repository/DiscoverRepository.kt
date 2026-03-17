package com.campusconnect.data.repository

import com.campusconnect.data.model.Crossing
import com.campusconnect.data.model.FriendRequest
import com.campusconnect.data.model.RequestStatus
import com.campusconnect.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoverRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Update user's current location in Firestore.
     * Uses geohashing for efficient proximity queries.
     */
    suspend fun updateLocation(uid: String, lat: Double, lng: Double, geohash: String): Result<Unit> {
        return try {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "lastLat" to lat,
                    "lastLng" to lng,
                    "lastGeohash" to geohash,
                    "lastLocationTimestamp" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Find nearby users based on geohash prefix matching.
     * Only returns users who have been active in the last 30 minutes.
     */
    suspend fun findNearbyUsers(currentUid: String, geohash: String): Result<List<User>> {
        return try {
            val prefix = geohash.take(6)
            val endPrefix = prefix.substring(0, prefix.length - 1) +
                    (prefix.last() + 1)

            // Only show users active in the last 30 minutes
            val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000)

            val snapshot = firestore.collection("users")
                .whereGreaterThanOrEqualTo("lastGeohash", prefix)
                .whereLessThan("lastGeohash", endPrefix)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.takeIf { user ->
                    user.uid != currentUid && user.lastLocationTimestamp > thirtyMinutesAgo
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a crossing event between two users.
     * Rate-limited: only increments count if last crossing was >5 minutes ago.
     */
    suspend fun recordCrossing(crossing: Crossing): Result<Unit> {
        return try {
            val crossingRef = firestore.collection("crossings")
                .document(crossing.userId)
                .collection("people")
                .document(crossing.otherUserId)

            firestore.runTransaction { transaction ->
                val existing = transaction.get(crossingRef)
                if (existing.exists()) {
                    val lastCrossedAt = existing.getLong("lastCrossedAt") ?: 0
                    val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
                    if (lastCrossedAt < fiveMinutesAgo) {
                        val currentCount = existing.getLong("crossingCount") ?: 0
                        transaction.update(crossingRef, mapOf(
                            "crossingCount" to currentCount + 1,
                            "lastCrossedAt" to System.currentTimeMillis()
                        ))
                    }
                } else {
                    transaction.set(crossingRef, crossing)
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all crossings for a user, sorted by most recent.
     */
    fun getCrossings(uid: String): Flow<List<Crossing>> = callbackFlow {
        val listener = firestore.collection("crossings")
            .document(uid)
            .collection("people")
            .orderBy("lastCrossedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("DiscoverRepo", "getCrossings failed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val crossings = snapshot?.documents?.mapNotNull {
                    it.toObject(Crossing::class.java)
                } ?: emptyList()
                trySend(crossings)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Check if a pending friend request already exists between two users.
     */
    suspend fun hasPendingRequest(fromUid: String, toUid: String): Boolean {
        return try {
            val existing = firestore.collection("friendRequests")
                .whereEqualTo("fromUserId", fromUid)
                .whereEqualTo("toUserId", toUid)
                .whereEqualTo("status", RequestStatus.PENDING.name)
                .get()
                .await()
            existing.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if two users are already friends.
     */
    suspend fun areAlreadyFriends(uid1: String, uid2: String): Boolean {
        return try {
            val user = firestore.collection("users").document(uid1).get().await()
                .toObject(User::class.java)
            user?.friends?.contains(uid2) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Send a friend request (wave).
     * Uses deterministic doc ID to prevent duplicates.
     */
    suspend fun sendFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            // Use deterministic ID: fromUser_toUser — prevents multiple requests
            val docId = "${request.fromUserId}_${request.toUserId}"
            firestore.collection("friendRequests").document(docId).set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get incoming friend requests for a user.
     */
    fun getIncomingRequests(uid: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection("friendRequests")
            .whereEqualTo("toUserId", uid)
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("DiscoverRepo", "getIncomingRequests failed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull {
                    it.toObject(FriendRequest::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get outbound pending friend requests (waves sent by the user that are still pending).
     * Real-time listener so the UI updates when a request is accepted/declined.
     */
    fun getSentPendingRequests(uid: String): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection("friendRequests")
            .whereEqualTo("fromUserId", uid)
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull {
                    it.toObject(FriendRequest::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Remove a friend: remove from both users' friends lists and delete their conversation.
     */
    suspend fun removeFriend(currentUid: String, friendUid: String): Result<Unit> {
        return try {
            val userRef1 = firestore.collection("users").document(currentUid)
            val userRef2 = firestore.collection("users").document(friendUid)

            // Remove from each other's friends list
            firestore.runTransaction { transaction ->
                val user1 = transaction.get(userRef1).toObject(com.campusconnect.data.model.User::class.java)
                val user2 = transaction.get(userRef2).toObject(com.campusconnect.data.model.User::class.java)

                if (user1 != null) {
                    transaction.update(userRef1, "friends", user1.friends.filter { it != friendUid })
                }
                if (user2 != null) {
                    transaction.update(userRef2, "friends", user2.friends.filter { it != currentUid })
                }
                null
            }.await()

            // Delete any conversation between them
            val convos = firestore.collection("conversations")
                .whereArrayContains("participants", currentUid)
                .get().await()
            convos.documents.forEach { doc ->
                val participants = doc.get("participants") as? List<*>
                if (participants?.contains(friendUid) == true) {
                    doc.reference.delete().await()
                }
            }

            // Delete any friend request docs between them
            val req1Id = "${currentUid}_${friendUid}"
            val req2Id = "${friendUid}_${currentUid}"
            try { firestore.collection("friendRequests").document(req1Id).delete().await() } catch (_: Exception) {}
            try { firestore.collection("friendRequests").document(req2Id).delete().await() } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accept a friend request: add each user to the other's friends list,
     * create a chat conversation, then DELETE the request document.
     * Idempotent — checks if already friends before adding.
     */
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            val userRef1 = firestore.collection("users").document(request.fromUserId)
            val userRef2 = firestore.collection("users").document(request.toUserId)

            // Step 1: Add friends (idempotent)
            val users = firestore.runTransaction { transaction ->
                val user1 = transaction.get(userRef1).toObject(User::class.java)
                val user2 = transaction.get(userRef2).toObject(User::class.java)

                if (user1 != null && user2 != null) {
                    if (!user1.friends.contains(request.toUserId)) {
                        transaction.update(userRef1, "friends", user1.friends + request.toUserId)
                    }
                    if (!user2.friends.contains(request.fromUserId)) {
                        transaction.update(userRef2, "friends", user2.friends + request.fromUserId)
                    }
                }
                Pair(user1, user2)
            }.await()

            // Step 2: Create a conversation so they can chat
            val user1 = users.first
            val user2 = users.second
            if (user1 != null && user2 != null) {
                // Check if conversation already exists
                val existing = firestore.collection("conversations")
                    .whereArrayContains("participants", request.fromUserId)
                    .get().await()
                val alreadyExists = existing.documents.any { doc ->
                    val participants = doc.get("participants") as? List<*>
                    participants?.contains(request.toUserId) == true
                }

                if (!alreadyExists) {
                    val conversation = mapOf(
                        "participants" to listOf(request.fromUserId, request.toUserId),
                        "participantNames" to mapOf(
                            request.fromUserId to user1.name,
                            request.toUserId to user2.name
                        ),
                        "participantPhotos" to mapOf(
                            request.fromUserId to user1.photoUrl,
                            request.toUserId to user2.photoUrl
                        ),
                        "lastMessage" to "You are now connected! 👋",
                        "lastTimestamp" to System.currentTimeMillis()
                    )
                    firestore.collection("conversations").add(conversation).await()
                }
            }

            // Step 3: Delete the request doc so it disappears from the list
            firestore.collection("friendRequests").document(request.id).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("friendRequests").document(requestId)
                .update("status", RequestStatus.DECLINED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
