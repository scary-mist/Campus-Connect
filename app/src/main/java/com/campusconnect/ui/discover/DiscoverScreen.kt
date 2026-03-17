package com.campusconnect.ui.discover

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.campusconnect.data.model.Crossing
import com.campusconnect.data.model.FriendRequest
import com.campusconnect.data.repository.AuthRepository
import com.campusconnect.data.repository.DiscoverRepository
import com.campusconnect.service.LocationService
import com.campusconnect.ui.components.PullRefreshLayout
import com.campusconnect.ui.theme.GradientEnd
import com.campusconnect.ui.theme.GradientMid
import com.campusconnect.ui.theme.GradientStart
import com.campusconnect.ui.theme.SuccessGreen
import com.campusconnect.ui.theme.WarningAmber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val crossings: List<Crossing> = emptyList(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val sentRequestUserIds: Set<String> = emptySet(),
    val friendIds: Set<String> = emptySet(),
    val isLocationEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUser?.uid

    init {
        loadData()
    }

    private fun loadData() {
        val uid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            discoverRepository.getCrossings(uid).collect { crossings ->
                _uiState.value = _uiState.value.copy(crossings = crossings, isLoading = false, isRefreshing = false)
            }
        }

        viewModelScope.launch {
            discoverRepository.getIncomingRequests(uid).collect { requests ->
                _uiState.value = _uiState.value.copy(incomingRequests = requests)
            }
        }

        // Track outbound pending requests (so we can show "Pending" on the wave button)
        viewModelScope.launch {
            discoverRepository.getSentPendingRequests(uid).collect { sentRequests ->
                _uiState.value = _uiState.value.copy(
                    sentRequestUserIds = sentRequests.map { it.toUserId }.toSet()
                )
            }
        }

        // Track current friends list (so we can show "Friends" on the wave button)
        viewModelScope.launch {
            val profile = authRepository.getUserProfile(uid).getOrNull()
            _uiState.value = _uiState.value.copy(
                friendIds = profile?.friends?.toSet() ?: emptySet()
            )
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadData()
    }

    fun sendWave(crossing: Crossing) {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch

            // Don't send if already friends or if there's a pending request
            if (discoverRepository.areAlreadyFriends(uid, crossing.otherUserId)) return@launch
            if (discoverRepository.hasPendingRequest(uid, crossing.otherUserId)) return@launch

            val profileResult = authRepository.getUserProfile(uid)
            val profile = profileResult.getOrNull() ?: return@launch

            val request = FriendRequest(
                fromUserId = uid,
                fromUserName = profile.name,
                fromUserPhotoUrl = profile.photoUrl,
                toUserId = crossing.otherUserId
            )
            discoverRepository.sendFriendRequest(request)
        }
    }

    fun acceptRequest(request: FriendRequest) {
        viewModelScope.launch {
            discoverRepository.acceptFriendRequest(request)
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            discoverRepository.declineFriendRequest(requestId)
        }
    }

    fun setLocationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isLocationEnabled = enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    discoverViewModel: DiscoverViewModel,
    onFriendRequests: () -> Unit,
    onUserProfile: (String) -> Unit
) {
    val uiState by discoverViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            // Permission granted — start the service
            try {
                val intent = Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                }
                context.startForegroundService(intent)
                discoverViewModel.setLocationEnabled(true)
            } catch (e: Exception) {
                android.util.Log.e("DiscoverScreen", "Failed to start location: ${e.message}")
                discoverViewModel.setLocationEnabled(false)
            }
        } else {
            discoverViewModel.setLocationEnabled(false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Discover", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onFriendRequests) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Friend Requests")
                    }
                    // Show count badge as a separate chip next to the icon
                    if (uiState.incomingRequests.isNotEmpty()) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.offset(x = (-20).dp, y = 8.dp)
                        ) {
                            Text("${uiState.incomingRequests.size}")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullRefreshLayout(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { discoverViewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Location toggle card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isLocationEnabled)
                            SuccessGreen.copy(alpha = 0.1f)
                        else
                            WarningAmber.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (uiState.isLocationEnabled) Icons.Default.LocationOn else Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = if (uiState.isLocationEnabled) SuccessGreen else WarningAmber,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (uiState.isLocationEnabled) "Discovery Active" else "Discovery Paused",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (uiState.isLocationEnabled) "Finding people near you..."
                                else "Enable to discover people nearby",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isLocationEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // Check if permission already granted
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val hasCoarse = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasFine || hasCoarse) {
                                        // Already have permission — start service
                                        try {
                                            val intent = Intent(context, LocationService::class.java).apply {
                                                action = LocationService.ACTION_START
                                            }
                                            context.startForegroundService(intent)
                                            discoverViewModel.setLocationEnabled(true)
                                        } catch (e: Exception) {
                                            android.util.Log.e("DiscoverScreen", "Failed: ${e.message}")
                                            discoverViewModel.setLocationEnabled(false)
                                        }
                                    } else {
                                        // Request permission — callback will start service if granted
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                } else {
                                    // Turning off — stop service
                                    try {
                                        val intent = Intent(context, LocationService::class.java).apply {
                                            action = LocationService.ACTION_STOP
                                        }
                                        context.startService(intent)
                                    } catch (_: Exception) { }
                                    discoverViewModel.setLocationEnabled(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SuccessGreen,
                                checkedTrackColor = SuccessGreen.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Section header
            item {
                Text(
                    "People You've Crossed Paths With",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.crossings.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No crossings yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Walk around campus to discover people!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(uiState.crossings, key = { it.otherUserId }) { crossing ->
                val waveState = when {
                    crossing.otherUserId in uiState.friendIds -> "friends"
                    crossing.otherUserId in uiState.sentRequestUserIds -> "pending"
                    else -> "wave"
                }
                CrossingCard(
                    crossing = crossing,
                    waveState = waveState,
                    onWave = { discoverViewModel.sendWave(crossing) },
                    onViewProfile = { onUserProfile(crossing.otherUserId) }
                )
            }
        }
        }
    }
}

@Composable
fun CrossingCard(
    crossing: Crossing,
    waveState: String = "wave",
    onWave: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onViewProfile() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(GradientStart, GradientMid)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (crossing.otherUserPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = crossing.otherUserPhotoUrl,
                        contentDescription = crossing.otherUserName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = crossing.otherUserName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = crossing.otherUserName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = crossing.otherUserCollege.ifEmpty { "Campus" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = GradientStart
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${crossing.crossingCount}x crossed",
                        style = MaterialTheme.typography.labelSmall,
                        color = GradientStart,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Wave / Pending / Friends button
            when (waveState) {
                "friends" -> {
                    Button(
                        onClick = { },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = SuccessGreen.copy(alpha = 0.2f),
                            disabledContentColor = SuccessGreen
                        )
                    ) {
                        Text("✓", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Friends", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                "pending" -> {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("⏳", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pending", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = onWave,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GradientStart)
                    ) {
                        Text("👋", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wave", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
