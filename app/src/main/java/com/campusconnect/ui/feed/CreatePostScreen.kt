package com.campusconnect.ui.feed

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.campusconnect.ui.components.LoadingSpinner
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.campusconnect.ui.theme.GradientStart

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    feedViewModel: FeedViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by feedViewModel.uiState.collectAsState()
    var content by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(listOf<String>()) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var linkText by remember { mutableStateOf("") }
    var showLinkDialog by remember { mutableStateOf(false) }

    val availableTags = listOf(
        "Hackathon", "Internship", "Placement", "Clubs",
        "Tech", "Events", "Study Group", "Project",
        "Doubt", "Achievement", "Advice", "Fun"
    )

    // Navigate back only AFTER the post (including image upload) has been created
    LaunchedEffect(uiState.postCreated) {
        if (uiState.postCreated) {
            feedViewModel.clearPostCreated()
            onNavigateBack()
        }
    }

    // Show error as a snackbar if upload fails
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            feedViewModel.clearError()
        }
    }

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    // Link dialog
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Add Link") },
            text = {
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Add", color = GradientStart)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    linkText = ""
                    showLinkDialog = false
                }) { Text("Clear") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            feedViewModel.createPost(
                                content = content,
                                imageUri = selectedImageUri,
                                linkUrl = linkText.takeIf { it.isNotBlank() },
                                tags = selectedTags
                            )
                        },
                        enabled = content.isNotBlank() && !uiState.isPosting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GradientStart)
                    ) {
                        if (uiState.isPosting) {
                            LoadingSpinner(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Post", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                placeholder = {
                    Text(
                        "What's on your mind? Share updates, ask questions, or post about events...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GradientStart,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Selected image preview
            if (selectedImageUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    // Remove image button
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                RoundedCornerShape(50)
                            )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove image",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Link preview chip
            if (linkText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, GradientStart.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, contentDescription = null,
                        tint = GradientStart, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = linkText,
                        style = MaterialTheme.typography.bodySmall,
                        color = GradientStart,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    IconButton(onClick = { linkText = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTags.forEach { tag ->
                    val isSelected = tag in selectedTags
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                        },
                        label = { Text("#$tag") },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GradientStart.copy(alpha = 0.15f),
                            selectedLabelColor = GradientStart
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Attachment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = if (selectedImageUri != null)
                        ButtonDefaults.outlinedButtonColors(containerColor = GradientStart.copy(alpha = 0.1f))
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp),
                        tint = if (selectedImageUri != null) GradientStart else MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo", color = if (selectedImageUri != null) GradientStart else MaterialTheme.colorScheme.onSurface)
                }
                OutlinedButton(
                    onClick = { showLinkDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = if (linkText.isNotBlank())
                        ButtonDefaults.outlinedButtonColors(containerColor = GradientStart.copy(alpha = 0.1f))
                    else ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp),
                        tint = if (linkText.isNotBlank()) GradientStart else MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Link", color = if (linkText.isNotBlank()) GradientStart else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
