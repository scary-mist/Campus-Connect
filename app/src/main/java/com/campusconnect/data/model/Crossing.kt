package com.campusconnect.data.model

data class Crossing(
    val userId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserPhotoUrl: String = "",
    val otherUserCollege: String = "",
    val otherUserDepartment: String = "",
    val firstCrossedAt: Long = System.currentTimeMillis(),
    val lastCrossedAt: Long = System.currentTimeMillis(),
    val crossingCount: Int = 1,
    val locationName: String = ""
)

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserPhotoUrl: String = "",
    val toUserId: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RequestStatus {
    PENDING, ACCEPTED, DECLINED
}
