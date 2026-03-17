package com.campusconnect.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.campusconnect.ui.components.LoadingSpinner
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.campusconnect.ui.theme.GradientEnd
import com.campusconnect.ui.theme.GradientStart
import com.campusconnect.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val user = uiState.user

    // Photo picker launcher — no permissions needed (PickVisualMedia is the modern API)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { profileViewModel.uploadPhoto(it) }
    }

    // Fields start empty and get populated once user data loads from Firestore.
    // Using remember alone would freeze the value at composition time (when user=null).
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("1") }
    var skillsText by remember { mutableStateOf("") }
    var projectsText by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var githubUrl by remember { mutableStateOf("") }

    // Populate fields once user data arrives (runs only when user transitions from null → non-null)
    LaunchedEffect(user) {
        user?.let {
            name = it.name
            bio = it.bio
            college = it.college
            department = it.department
            year = it.year.toString()
            skillsText = it.skills.joinToString(", ")
            projectsText = it.projects.joinToString("\n")
            experience = it.experience
            linkedinUrl = it.linkedinUrl
            githubUrl = it.githubUrl
        }
    }

    // Navigate back only AFTER a successful save (not immediately)
    var saveTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isSaving, saveTriggered) {
        if (saveTriggered && !uiState.isSaving) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val updates = mapOf(
                                "name" to name,
                                "bio" to bio,
                                "college" to college,
                                "department" to department,
                                "year" to (year.toIntOrNull() ?: 1),
                                "skills" to skillsText.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                "projects" to projectsText.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
                                "experience" to experience,
                                "linkedinUrl" to linkedinUrl,
                                "githubUrl" to githubUrl
                            )
                            profileViewModel.updateProfile(updates)
                            saveTriggered = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            LoadingSpinner(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Profile Photo ──────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(GradientStart, GradientEnd))
                        )
                        .clickable(enabled = !uiState.isUploadingPhoto) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isUploadingPhoto) {
                        LoadingSpinner(color = androidx.compose.ui.graphics.Color.White, size = 36.dp, strokeWidth = 3.dp)
                    } else if (!user?.photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user?.photoUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = (user?.name ?: name).take(1).uppercase().ifEmpty { "?" },
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        )
                    }
                }
                // Camera badge
                if (!uiState.isUploadingPhoto) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = (-8).dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GradientStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = if (uiState.isUploadingPhoto) "Uploading photo..." else "Tap to change photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Personal section
            SectionHeader("Personal Info")


            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
                maxLines = 4
            )

            // Academic section
            SectionHeader("Academic Info")

            OutlinedTextField(
                value = college,
                onValueChange = { college = it },
                label = { Text("College/University") },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year") },
                    modifier = Modifier.width(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            // Professional section
            SectionHeader("Professional Info")

            OutlinedTextField(
                value = skillsText,
                onValueChange = { skillsText = it },
                label = { Text("Skills (comma-separated)") },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Kotlin, Android, Firebase, etc.") }
            )

            OutlinedTextField(
                value = experience,
                onValueChange = { experience = it },
                label = { Text("Experience") },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text("Internships, TA positions, etc.") }
            )

            OutlinedTextField(
                value = projectsText,
                onValueChange = { projectsText = it },
                label = { Text("Projects (one per line)") },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 3,
                maxLines = 6
            )

            // Links section
            SectionHeader("Links")

            OutlinedTextField(
                value = linkedinUrl,
                onValueChange = { linkedinUrl = it },
                label = { Text("LinkedIn URL") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = githubUrl,
                onValueChange = { githubUrl = it },
                label = { Text("GitHub URL") },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = GradientStart,
        modifier = Modifier.padding(top = 8.dp)
    )
}
