package com.campusconnect.data.repository

import com.campusconnect.data.model.Conversation
import com.campusconnect.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Get all conversations for a user, ordered by last message time.
     */
    fun getConversations(uid: String): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .whereArrayContains("participants", uid)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Don't crash the app — emit empty list so UI shows "no conversations" state
                    // The most common cause is a missing Firestore composite index.
                    android.util.Log.e("ChatRepository", "getConversations failed: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val conversations = snapshot?.documents?.mapNotNull {
                    it.toObject(Conversation::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get or create a conversation between two users.
     */
    suspend fun getOrCreateConversation(
        currentUid: String,
        otherUid: String,
        currentName: String,
        otherName: String,
        currentPhoto: String,
        otherPhoto: String
    ): Result<String> {
        return try {
            // Check if conversation already exists
            val existing = firestore.collection("conversations")
                .whereArrayContains("participants", currentUid)
                .get()
                .await()

            val existingConvo = existing.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.contains(otherUid) == true
            }

            if (existingConvo != null) {
                Result.success(existingConvo.id)
            } else {
                // Create new conversation
                val conversation = Conversation(
                    participants = listOf(currentUid, otherUid),
                    participantNames = mapOf(currentUid to currentName, otherUid to otherName),
                    participantPhotos = mapOf(currentUid to currentPhoto, otherUid to otherPhoto),
                    lastTimestamp = System.currentTimeMillis()
                )
                val docRef = firestore.collection("conversations").add(conversation).await()
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get messages for a conversation with real-time updates.
     */
    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatRepository", "getMessages failed: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(Message::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Send a message in a conversation.
     */
    suspend fun sendMessage(conversationId: String, message: Message): Result<Unit> {
        return try {
            // Add message
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .add(message)
                .await()

            // Update conversation metadata
            firestore.collection("conversations")
                .document(conversationId)
                .update(mapOf(
                    "lastMessage" to message.content,
                    "lastSenderId" to message.senderId,
                    "lastTimestamp" to message.timestamp
                ))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
