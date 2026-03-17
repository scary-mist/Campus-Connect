package com.campusconnect.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.campusconnect.ui.components.LoadingSpinner
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campusconnect.data.model.User
import com.campusconnect.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    isOwnProfile: Boolean = true
) {
    val uiState by profileViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Reload profile every time this screen comes into view (e.g. returning from edit)
    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = onEditProfile) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                        IconButton(onClick = { onSignOut() }) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner(color = GradientStart)
            }
        } else {
            val user = uiState.user ?: return@Scaffold

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                            colors = listOf(GradientStart, GradientMid, GradientEnd)
                        )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.photoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = user.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = user.name.take(1).uppercase(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "${user.department} • ${user.college}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = "${user.friends.size}", label = "Friends")
                    StatItem(value = "${user.interests.size}", label = "Interests")
                    StatItem(value = "${user.skills.size}", label = "Skills")
                }

                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GradientStart
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Social", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Professional", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Outlined.Work, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )
                }

                // Tab content
                when (selectedTab) {
                    0 -> SocialTab(user)
                    1 -> ProfessionalTab(user)
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = GradientStart
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialTab(user: User) {
    Column(modifier = Modifier.padding(24.dp)) {
        // Bio
        if (user.bio.isNotEmpty()) {
            ProfileSection(icon = Icons.Outlined.Info, title = "About") {
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Academic Info
        ProfileSection(icon = Icons.Outlined.School, title = "Academic") {
            InfoRow(label = "College", value = user.college)
            InfoRow(label = "Department", value = user.department)
            InfoRow(label = "Year", value = "Year ${user.year}")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Interests
        if (user.interests.isNotEmpty()) {
            ProfileSection(icon = Icons.Outlined.Interests, title = "Interests") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    user.interests.forEach { interest ->
                        AssistChip(
                            onClick = { },
                            label = { Text(interest) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = GradientStart.copy(alpha = 0.1f),
                                labelColor = GradientStart
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfessionalTab(user: User) {
    Column(modifier = Modifier.padding(24.dp)) {
        // Skills
        if (user.skills.isNotEmpty()) {
            ProfileSection(icon = Icons.Outlined.Code, title = "Skills") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    user.skills.forEach { skill ->
                        AssistChip(
                            onClick = { },
                            label = { Text(skill) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = GradientMid.copy(alpha = 0.1f),
                                labelColor = GradientMid
                            ),
                            border = null
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Experience
        if (user.experience.isNotEmpty()) {
            ProfileSection(icon = Icons.Outlined.Work, title = "Experience") {
                Text(
                    text = user.experience,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Projects
        if (user.projects.isNotEmpty()) {
            ProfileSection(icon = Icons.Outlined.Folder, title = "Projects") {
                user.projects.forEach { project ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = project,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Links
        ProfileSection(icon = Icons.Outlined.Link, title = "Links") {
            val uriHandler = LocalUriHandler.current
            if (user.linkedinUrl.isNotEmpty()) {
                LinkRow(
                    label = "LinkedIn",
                    url = user.linkedinUrl,
                    onClick = {
                        val url = if (user.linkedinUrl.startsWith("http")) user.linkedinUrl else "https://${user.linkedinUrl}"
                        uriHandler.openUri(url)
                    }
                )
            }
            if (user.githubUrl.isNotEmpty()) {
                LinkRow(
                    label = "GitHub",
                    url = user.githubUrl,
                    onClick = {
                        val url = if (user.githubUrl.startsWith("http")) user.githubUrl else "https://${user.githubUrl}"
                        uriHandler.openUri(url)
                    }
                )
            }
            if (user.linkedinUrl.isEmpty() && user.githubUrl.isEmpty()) {
                Text(
                    text = "No links added yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProfileSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = GradientStart,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LinkRow(label: String, url: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = GradientStart,
            textDecoration = TextDecoration.Underline
        )
    }
}
