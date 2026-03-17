package com.campusconnect.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    // Academic info
    val college: String = "",
    val department: String = "",
    val year: Int = 1,
    // Social info
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val friends: List<String> = emptyList(),
    // Professional info
    val skills: List<String> = emptyList(),
    val projects: List<String> = emptyList(),
    val experience: String = "",
    val linkedinUrl: String = "",
    val githubUrl: String = "",
    val resumeUrl: String = "",
    // Location tracking
    @get:PropertyName("isLocationEnabled")
    @set:PropertyName("isLocationEnabled")
    var isLocationEnabled: Boolean = false,
    val lastGeohash: String = "",
    val lastLat: Double = 0.0,
    val lastLng: Double = 0.0,
    val lastLocationTimestamp: Long = 0L,
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("isOnboarded")
    @set:PropertyName("isOnboarded")
    var isOnboarded: Boolean = false
)
