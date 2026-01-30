package com.jazwinn.fitnesstracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jazwinn.fitnesstracker.ui.navigation.Screen
import com.jazwinn.fitnesstracker.ui.viewmodel.ProfileViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current
    
    var isEditing by remember { mutableStateOf(false) }

    // State holders
    var name by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var stepGoal by remember { mutableStateOf("") }
    var profilePictureUri by remember { mutableStateOf<String?>(null) }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flag)
            profilePictureUri = uri.toString()
        }
    }

    // Sync state with profile data (reset on cancel or initial load)
    LaunchedEffect(userProfile, isEditing) {
        if (!isEditing) {
            name = userProfile?.name ?: ""
            height = userProfile?.heightCm?.toString() ?: "170"
            weight = userProfile?.weightKg?.toString() ?: "70"
            age = userProfile?.age?.toString() ?: "25"
            stepGoal = userProfile?.dailyStepGoal?.toString() ?: "10000"
            profilePictureUri = userProfile?.profilePictureUri
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Section
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .clickable(enabled = isEditing) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profilePictureUri != null) {
                    AsyncImage(
                        model = profilePictureUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                     if (isEditing) {
                         Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f), CircleShape))
                         Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                     if (isEditing) {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                             Icon(
                                 imageVector = Icons.Default.Edit,
                                 contentDescription = "Add Photo",
                                 tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                 modifier = Modifier.padding(16.dp).size(24.dp)
                             )
                         }
                    }
                }
            }

            // Personal Information
            ProfileSection("Personal Information") {
                ProfileField(
                    label = "Name",
                    value = name,
                    onValueChange = { name = it },
                    isEditing = isEditing,
                    leadingIcon = Icons.Default.Person
                )
                ProfileField(
                    label = "Age",
                    value = age,
                    onValueChange = { age = it },
                    isEditing = isEditing,
                    leadingIcon = Icons.Default.Info, // Reused icon or generic
                    keyboardType = KeyboardType.Number
                )
            }
            
            // Physical Stats
            ProfileSection("Physical Stats") {
                 Row(
                     modifier = Modifier.fillMaxWidth(), 
                     horizontalArrangement = Arrangement.spacedBy(16.dp)
                 ) {
                     Box(modifier = Modifier.weight(1f)) {
                         ProfileField(
                             label = "Height (cm)",
                             value = height,
                             onValueChange = { height = it },
                             isEditing = isEditing,
                             leadingIcon = Icons.Default.Info,
                             keyboardType = KeyboardType.Number
                         )
                     }
                     Box(modifier = Modifier.weight(1f)) {
                         ProfileField(
                             label = "Weight (kg)",
                             value = weight,
                             onValueChange = { weight = it },
                             isEditing = isEditing,
                             leadingIcon = Icons.Default.Info,
                             keyboardType = KeyboardType.Number
                         )
                     }
                 }
            }

            // Goals
            ProfileSection("Goals") {
                 ProfileField(
                    label = "Daily Step Goal",
                    value = stepGoal,
                    onValueChange = { stepGoal = it },
                    isEditing = isEditing,
                    leadingIcon = Icons.Default.Star,
                    keyboardType = KeyboardType.Number
                )
            }
            
            // Action Buttons
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                name,
                                height.toFloatOrNull() ?: 170f,
                                weight.toFloatOrNull() ?: 70f,
                                age.toIntOrNull() ?: 25,
                                stepGoal.toIntOrNull() ?: 10000,
                                profilePictureUri
                            )
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            } else {
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        Card(
             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    if (isEditing) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value.ifEmpty { "-" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

