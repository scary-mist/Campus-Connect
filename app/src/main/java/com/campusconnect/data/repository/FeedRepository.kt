package com.campusconnect.data.repository

import android.net.Uri
import com.campusconnect.data.model.Comment
import com.campusconnect.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val postsCollection = firestore.collection("posts")

    /**
     * Get posts feed filtered to only show posts from a given set of users
     * (the current user + their friends). Uses client-side filtering to avoid
     * needing a composite Firestore index.
     */
    fun getPostsFeed(visibleUserIds: List<String> = emptyList()): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FeedRepository", "getPostsFeed failed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val allPosts = snapshot?.documents?.mapNotNull {
                    it.toObject(Post::class.java)?.copy(id = it.id)
                } ?: emptyList()

                // Client-side filter: show only posts from visible users
                val filtered = if (visibleUserIds.isNotEmpty()) {
                    allPosts.filter { it.authorId in visibleUserIds }
                } else {
                    allPosts
                }
                trySend(filtered)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createPost(post: Post): Result<String> {
        return try {
            val docRef = postsCollection.add(post).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String, userId: String): Result<Unit> {
        return try {
            val docRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val post = snapshot.toObject(Post::class.java) ?: return@runTransaction
                val likes = post.likes.toMutableList()
                if (userId in likes) likes.remove(userId) else likes.add(userId)
                transaction.update(docRef, "likes", likes)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = postsCollection.document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FeedRepository", "getComments failed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull {
                    it.toObject(Comment::class.java)?.copy(id = it.id)
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(postId: String, comment: Comment): Result<Unit> {
        return try {
            postsCollection.document(postId).collection("comments").add(comment).await()
            // Increment comment count
            val docRef = postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentCount = snapshot.getLong("commentCount") ?: 0
                transaction.update(docRef, "commentCount", currentCount + 1)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            postsCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload an image for a post to Firebase Storage.
     * Returns the download URL.
     */
    suspend fun uploadPostImage(imageUri: Uri, postId: String = System.currentTimeMillis().toString()): Result<String> {
        return try {
            android.util.Log.d("FeedRepository", "uploadPostImage: starting upload for $imageUri")
            val ref = storage.reference.child("postImages/$postId.jpg")
            ref.putFile(imageUri).await()
            android.util.Log.d("FeedRepository", "uploadPostImage: file uploaded, getting download URL")
            val url = ref.downloadUrl.await().toString()
            android.util.Log.d("FeedRepository", "uploadPostImage: success, url=$url")
            Result.success(url)
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "uploadPostImage FAILED: ${e.message}", e)
            Result.failure(e)
        }
    }
}
