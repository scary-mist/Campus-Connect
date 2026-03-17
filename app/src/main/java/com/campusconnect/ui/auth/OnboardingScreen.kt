package com.campusconnect.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusconnect.ui.theme.GradientStart
import com.campusconnect.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    authViewModel: AuthViewModel,
    onOnboardingComplete: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("1") }
    var bio by remember { mutableStateOf("") }
    var interestInput by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(listOf<String>()) }

    val predefinedInterests = listOf(
        "Coding", "AI/ML", "Web Dev", "Android", "iOS",
        "Data Science", "Cybersecurity", "Cloud", "Blockchain",
        "Competitive Programming", "Open Source", "Hackathons",
        "Photography", "Music", "Sports", "Gaming",
        "Startups", "Finance", "Design", "Writing"
    )

    LaunchedEffect(uiState.isOnboarded) {
        if (uiState.isOnboarded) onOnboardingComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Progress indicator
        @Suppress("DEPRECATION")
        LinearProgressIndicator(
            progress = (currentStep + 1) / 3f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = GradientStart,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Step indicator
            Text(
                text = "Step ${currentStep + 1} of 3",
                style = MaterialTheme.typography.labelLarge,
                color = GradientStart
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (currentStep) {
                0 -> {
                    // Personal Info
                    Text(
                        text = "Tell us about yourself",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Let's set up your profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 3,
                        maxLines = 5,
                        placeholder = { Text("Tell people about yourself...") }
                    )
                }
                1 -> {
                    // Academic Info
                    Text(
                        text = "Academic Details",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your college information",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = college,
                        onValueChange = { college = it },
                        label = { Text("College/University") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year of Study") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                2 -> {
                    // Interests
                    Text(
                        text = "Your Interests",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pick topics you're passionate about",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        predefinedInterests.forEach { interest ->
                            val isSelected = interest in interests
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    interests = if (isSelected) {
                                        interests - interest
                                    } else {
                                        interests + interest
                                    }
                                },
                                label = { Text(interest) },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom interest input
                    OutlinedTextField(
                        value = interestInput,
                        onValueChange = { interestInput = it },
                        label = { Text("Add custom interest") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (interestInput.isNotBlank()) {
                                IconButton(onClick = {
                                    interests = interests + interestInput.trim()
                                    interestInput = ""
                                }) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Add")
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 2) {
                            currentStep++
                        } else {
                            authViewModel.completeOnboarding(
                                name = name,
                                college = college,
                                department = department,
                                year = year.toIntOrNull() ?: 1,
                                bio = bio,
                                interests = interests
                            )
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == 2) SuccessGreen else GradientStart
                    ),
                    enabled = when (currentStep) {
                        0 -> name.isNotBlank()
                        1 -> college.isNotBlank() && department.isNotBlank()
                        else -> interests.isNotEmpty()
                    } && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        LoadingSpinner(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (currentStep == 2) "Get Started!" else "Next",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            if (currentStep == 2) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
