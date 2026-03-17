package com.campusconnect.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.campusconnect.ui.components.LoadingSpinner
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campusconnect.ui.auth.*
import com.campusconnect.ui.chat.*
import com.campusconnect.ui.discover.*
import com.campusconnect.ui.feed.*
import com.campusconnect.ui.profile.*
import com.campusconnect.ui.theme.GradientStart

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Show a loading screen while the ViewModel resolves auth on startup
    if (authState.isAuthChecking) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingSpinner(color = GradientStart)
        }
        return
    }

    // Determine startDestination once (frozen — never changes after NavHost is built)
    val startDestination = remember {
        when {
            !authState.isLoggedIn -> NavRoutes.Login.route
            !authState.isOnboarded -> NavRoutes.Onboarding.route
            else -> NavRoutes.Feed.route
        }
    }

    // Navigate reactively whenever auth state changes AFTER initial setup
    LaunchedEffect(authState.isLoggedIn, authState.isOnboarded) {
        val current = navController.currentBackStackEntry?.destination?.route
        when {
            !authState.isLoggedIn -> {
                // Signed out — go to login and clear everything
                if (current != NavRoutes.Login.route) {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            authState.isLoggedIn && !authState.isOnboarded -> {
                // Logged in but not onboarded
                if (current != NavRoutes.Onboarding.route) {
                    navController.navigate(NavRoutes.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            authState.isLoggedIn && authState.isOnboarded -> {
                // Fully authenticated — move off auth screens
                if (current == NavRoutes.Login.route ||
                    current == NavRoutes.Register.route ||
                    current == NavRoutes.Onboarding.route) {
                    navController.navigate(NavRoutes.Feed.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    // Show loading until we have determined the start destination — handled above

    // Show bottom bar only on main tabs
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        NavRoutes.Feed.route,
        NavRoutes.Discover.route,
        NavRoutes.Conversations.route,
        NavRoutes.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Auth ──
            composable(NavRoutes.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = {
                        navController.navigate(NavRoutes.Register.route)
                    },
                    onLoginSuccess = { isOnboarded ->
                        val dest = if (isOnboarded) NavRoutes.Feed.route else NavRoutes.Onboarding.route
                        navController.navigate(dest) {
                            popUpTo(NavRoutes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.Register.route) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(NavRoutes.Onboarding.route) {
                            popUpTo(NavRoutes.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.Onboarding.route) {
                OnboardingScreen(
                    authViewModel = authViewModel,
                    onOnboardingComplete = {
                        navController.navigate(NavRoutes.Feed.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Feed ──
            composable(NavRoutes.Feed.route) {
                val feedViewModel: FeedViewModel = hiltViewModel()
                FeedScreen(
                    feedViewModel = feedViewModel,
                    onCreatePost = { navController.navigate(NavRoutes.CreatePost.route) },
                    onPostDetail = { postId ->
                        navController.navigate(NavRoutes.PostDetail.createRoute(postId))
                    },
                    onUserProfile = { userId ->
                        navController.navigate(NavRoutes.UserProfile.createRoute(userId))
                    }
                )
            }

            composable(NavRoutes.CreatePost.route) {
                val feedViewModel: FeedViewModel = hiltViewModel()
                CreatePostScreen(
                    feedViewModel = feedViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                NavRoutes.PostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                val feedViewModel: FeedViewModel = hiltViewModel()
                PostDetailScreen(
                    postId = postId,
                    feedViewModel = feedViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Discover ──
            composable(NavRoutes.Discover.route) {
                val discoverViewModel: DiscoverViewModel = hiltViewModel()
                DiscoverScreen(
                    discoverViewModel = discoverViewModel,
                    onFriendRequests = { navController.navigate(NavRoutes.FriendRequests.route) },
                    onUserProfile = { userId ->
                        navController.navigate(NavRoutes.UserProfile.createRoute(userId))
                    }
                )
            }

            composable(NavRoutes.FriendRequests.route) {
                val discoverViewModel: DiscoverViewModel = hiltViewModel()
                FriendRequestsScreen(
                    discoverViewModel = discoverViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Chat ──
            composable(NavRoutes.Conversations.route) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                ConversationsScreen(
                    chatViewModel = chatViewModel,
                    onChatDetail = { conversationId ->
                        navController.navigate(NavRoutes.ChatDetail.createRoute(conversationId))
                    }
                )
            }

            composable(
                NavRoutes.ChatDetail.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                val chatViewModel: ChatViewModel = hiltViewModel()
                ChatScreen(
                    conversationId = conversationId,
                    chatViewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Profile ──
            composable(NavRoutes.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    onEditProfile = { navController.navigate(NavRoutes.EditProfile.route) },
                    onSignOut = {
                        authViewModel.signOut()
                    }
                )
            }

            composable(NavRoutes.EditProfile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                EditProfileScreen(
                    profileViewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                NavRoutes.UserProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                val profileViewModel: ProfileViewModel = hiltViewModel()
                LaunchedEffect(userId) { profileViewModel.loadProfile(userId) }
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    onEditProfile = { },
                    onSignOut = { },
                    isOwnProfile = false
                )
            }
        }
    }
}
