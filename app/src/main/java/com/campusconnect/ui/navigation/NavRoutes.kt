package com.campusconnect.ui.navigation

/**
 * Sealed class representing all navigation routes in the app.
 * Using sealed classes for type safety in navigation.
 */
sealed class NavRoutes(val route: String) {
    // Auth
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object Onboarding : NavRoutes("onboarding")

    // Main tabs
    object Feed : NavRoutes("feed")
    object Discover : NavRoutes("discover")
    object Conversations : NavRoutes("conversations")
    object Profile : NavRoutes("profile")

    // Detail screens
    object CreatePost : NavRoutes("create_post")
    object PostDetail : NavRoutes("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object ChatDetail : NavRoutes("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object EditProfile : NavRoutes("edit_profile")
    object FriendRequests : NavRoutes("friend_requests")
    object UserProfile : NavRoutes("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
}
