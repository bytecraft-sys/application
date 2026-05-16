package com.yourapp.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.application.ui.theme.ApplicationTheme
import com.yourapp.data.local.UserProfile

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profileState by viewModel.profileState.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                UiEvent.NavigateToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    ProfileScreen(
        profileState = profileState,
        validationErrors = validationErrors,
        onBack = onBack,
        onStartEditing = viewModel::startEditing,
        onCancelEditing = viewModel::cancelEditing,
        onUpdateField = viewModel::updateField,
        onToggleTrait = viewModel::toggleTrait,
        onSaveProfile = viewModel::saveProfile,
        onLogout = viewModel::logout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileState: ProfileState,
    validationErrors: ProfileValidationErrors,
    onBack: () -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onUpdateField: (ProfileField, String) -> Unit,
    onToggleTrait: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var lastProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(profileState) {
        when (profileState) {
            is ProfileState.Viewing -> lastProfile = profileState.profile
            is ProfileState.Editing -> lastProfile = profileState.profile
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (profileState) {
                ProfileState.Loading -> LoadingContent(modifier = Modifier.fillMaxSize())
                is ProfileState.Error -> ErrorContent(
                    message = profileState.message,
                    modifier = Modifier.fillMaxSize(),
                )
                is ProfileState.Viewing,
                is ProfileState.Editing,
                ProfileState.Saving -> {
                    val profile = when (profileState) {
                        is ProfileState.Viewing -> profileState.profile
                        is ProfileState.Editing -> profileState.profile
                        ProfileState.Saving -> lastProfile
                    }

                    if (profile == null) {
                        LoadingContent(modifier = Modifier.fillMaxSize())
                    } else {
                        ProfileContent(
                            profile = profile,
                            isEditing = profileState is ProfileState.Editing || profileState is ProfileState.Saving,
                            isSaving = profileState is ProfileState.Saving,
                            validationErrors = validationErrors,
                            onStartEditing = onStartEditing,
                            onCancelEditing = onCancelEditing,
                            onUpdateField = onUpdateField,
                            onToggleTrait = onToggleTrait,
                            onSaveProfile = onSaveProfile,
                            onLogoutClick = { showLogoutDialog = true },
                        )
                    }
                }
            }

            if (profileState is ProfileState.Saving) {
                SavingOverlay()
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = {
                Text("Your chat history will remain on this device. You will need to complete setup again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) {
                    Text(
                        text = "Log out",
                        color = LogoutRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    isEditing: Boolean,
    isSaving: Boolean,
    validationErrors: ProfileValidationErrors,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onUpdateField: (ProfileField, String) -> Unit,
    onToggleTrait: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileHeader(profile = profile)
        Spacer(modifier = Modifier.height(24.dp))
        if (isEditing) {
            EditProfileSection(
                profile = profile,
                validationErrors = validationErrors,
                isSaving = isSaving,
                onUpdateField = onUpdateField,
                onToggleTrait = onToggleTrait,
                onCancelEditing = onCancelEditing,
                onSaveProfile = onSaveProfile,
            )
        } else {
            ProfileInfoCard(
                profile = profile,
                onStartEditing = onStartEditing,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        LogoutButton(onClick = onLogoutClick)
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(ProfileTeal.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ProfileTeal)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialsFor(profile.name),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = profile.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = profile.phone,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ProfileInfoCard(
    profile: UserProfile,
    onStartEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(18.dp),
    ) {
        InfoRow(
            icon = Icons.Default.Person,
            label = "Full Name",
            value = profile.name,
        )
        ProfileRowDivider()
        InfoRow(
            icon = Icons.Default.Cake,
            label = "Age",
            value = profile.age.toString(),
        )
        ProfileRowDivider()
        InfoRow(
            icon = Icons.Default.Phone,
            label = "Phone",
            value = profile.phone,
        )
        ProfileRowDivider()
        InfoRow(
            icon = Icons.Default.Psychology,
            label = "Personality Traits",
            value = profile.selectedTraits.joinToString(", ").ifBlank { "None selected" },
        )
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedButton(
            onClick = onStartEditing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Edit Profile")
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ProfileTeal.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ProfileTeal,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProfileRowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileSection(
    profile: UserProfile,
    validationErrors: ProfileValidationErrors,
    isSaving: Boolean,
    onUpdateField: (ProfileField, String) -> Unit,
    onToggleTrait: (String) -> Unit,
    onCancelEditing: () -> Unit,
    onSaveProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileTextField(
            value = profile.name,
            onValueChange = { onUpdateField(ProfileField.NAME, it) },
            label = "Name",
            error = validationErrors.name,
            enabled = !isSaving,
        )
        ProfileTextField(
            value = profile.age.takeIf { it > 0 }?.toString().orEmpty(),
            onValueChange = { onUpdateField(ProfileField.AGE, it) },
            label = "Age",
            error = validationErrors.age,
            enabled = !isSaving,
            keyboardType = KeyboardType.Number,
        )
        ProfileTextField(
            value = profile.phone,
            onValueChange = { onUpdateField(ProfileField.PHONE, it) },
            label = "Phone",
            error = validationErrors.phone,
            enabled = !isSaving,
            keyboardType = KeyboardType.Phone,
        )
        Text(
            text = "Personality Traits",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileTraits.forEach { trait ->
                val selected = trait in profile.selectedTraits
                SuggestionChip(
                    onClick = {
                        if (!isSaving) {
                            onToggleTrait(trait)
                        }
                    },
                    label = {
                        Text(
                            text = trait,
                            color = if (selected) Color.White else Color.Unspecified,
                        )
                    },
                    colors = if (selected) {
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = ProfileTeal,
                        )
                    } else {
                        SuggestionChipDefaults.suggestionChipColors()
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancelEditing,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSaveProfile,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = ProfileTeal),
                modifier = Modifier.weight(1f),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        isError = error != null,
        singleLine = true,
        supportingText = {
            if (error != null) {
                Text(error)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ProfileTeal,
            cursorColor = ProfileTeal,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, LogoutRed),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = LogoutRed,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Log out of this device",
            color = LogoutRed,
        )
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = ProfileTeal)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SavingOverlay(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = ProfileTeal)
    }
}

private fun initialsFor(name: String): String {
    return name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercase() }
        .ifBlank { "U" }
}

private val ProfileTraits = listOf(
    "Curious",
    "Calm",
    "Analytical",
    "Creative",
    "Empathetic",
    "Direct",
    "Humorous",
    "Introverted",
)

private val ProfileTeal = Color(0xFF1D9E75)
private val LogoutRed = Color(0xFFE24B4A)

@Preview(showBackground = true)
@Composable
fun ProfileScreenViewingPreview() {
    ApplicationTheme {
        ProfileScreen(
            profileState = ProfileState.Viewing(PreviewProfile),
            validationErrors = ProfileValidationErrors(),
            onBack = {},
            onStartEditing = {},
            onCancelEditing = {},
            onUpdateField = { _, _ -> },
            onToggleTrait = {},
            onSaveProfile = {},
            onLogout = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenEditingPreview() {
    ApplicationTheme {
        ProfileScreen(
            profileState = ProfileState.Editing(PreviewProfile),
            validationErrors = ProfileValidationErrors(phone = "Phone must be exactly 10 digits"),
            onBack = {},
            onStartEditing = {},
            onCancelEditing = {},
            onUpdateField = { _, _ -> },
            onToggleTrait = {},
            onSaveProfile = {},
            onLogout = {},
        )
    }
}

private val PreviewProfile = UserProfile(
    id = "local-user",
    name = "Rahul Sharma",
    age = 31,
    phone = "9876543210",
    selectedTraits = listOf("Curious", "Analytical", "Calm"),
    createdAt = 0L,
    lastSyncedAt = 0L,
)
