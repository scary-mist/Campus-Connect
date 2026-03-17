package com.campusconnect.data.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
