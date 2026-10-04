package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DietaryPreference

@Composable
fun AuthScreen(
    onLoginSubmit: (String, () -> Unit, () -> Unit) -> Unit,
    onCompleteSetup: (
        phone: String,
        name: String,
        title: String,
        dietary: String,
        workspaceName: String,
        isCommercial: Boolean,
        smsPerm: Boolean,
        notifPerm: Boolean,
        camPerm: Boolean
    ) -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0: Phone entry, 1: OTP, 2: Step 1 (Name/Title), 3: Step 2 (Diet), 4: Step 3 (Workspace), 5: Step 4 (Permissions)

    var phoneInput by remember { mutableStateOf("9876543210") }
    var otpInput by remember { mutableStateOf("") }
    val simulatedOtp = "2468"

    var nameInput by remember { mutableStateOf("Arjun Sharma") }
    var customTitle by remember { mutableStateOf("Kitchen Director") }
    val titleSuggestions = listOf("Kitchen Director", "Rasoi Boss", "Head Chef", "Pantry Master", "Culinary Curator")

    var selectedDiet by remember { mutableStateOf(DietaryPreference.VEGETARIAN) }

    var workspaceName by remember { mutableStateOf("Household Rasoi") }
    var isCommercialMode by remember { mutableStateOf(false) }

    var smsPermission by remember { mutableStateOf(true) }
    var notifPermission by remember { mutableStateOf(true) }
    var cameraPermission by remember { mutableStateOf(true) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo & Branding Header
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🛡️", fontSize = 32.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RasoiGuard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Smart Kitchen & Zero-Waste Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Step 0: Phone Number Entry
                    if (step == 0) {
                        Text(
                            text = "Enter Mobile Number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "State will automatically resume for existing accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it.take(10) },
                            label = { Text("Phone Number") },
                            prefix = { Text("+91 ") },
                            leadingIcon = { Icon(Icons.Filled.Phone, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("phone_number_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (phoneInput.length >= 10) {
                                    onLoginSubmit(
                                        phoneInput,
                                        { step = 1 }, // New account -> show OTP -> Onboarding
                                        { /* Resumed directly! */ }
                                    )
                                }
                            },
                            enabled = phoneInput.length >= 10,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("verify_phone_button")
                        ) {
                            Text("Continue")
                        }
                    }

                    // Step 1: OTP Verification
                    if (step == 1) {
                        Text(
                            text = "Enter 4-Digit OTP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Code sent to +91 $phoneInput. (Simulated OTP: $simulatedOtp)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { otpInput = it.take(4) },
                            placeholder = { Text("e.g. 2468") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("otp_code_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { step = 2 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("submit_otp_button")
                        ) {
                            Text("Verify & Setup Profile")
                        }
                    }

                    // Step 2: User Name & Custom Title
                    if (step == 2) {
                        Text(
                            text = "Step 1 of 4: Profile & Title",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Your Name *") },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            modifier = Modifier.fillMaxWidth().testTag("setup_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Custom Title") },
                            modifier = Modifier.fillMaxWidth().testTag("setup_title_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Quick Title Suggestions:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            titleSuggestions.take(3).forEach { suggestion ->
                                FilterChip(
                                    selected = customTitle == suggestion,
                                    onClick = { customTitle = suggestion },
                                    label = { Text(suggestion, fontSize = 10.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { step = 3 },
                            enabled = nameInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("next_step_1_button")
                        ) {
                            Text("Next: Dietary Preference")
                        }
                    }

                    // Step 3: Dietary Preference
                    if (step == 3) {
                        Text(
                            text = "Step 2 of 4: Dietary Filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI recipes and zero-waste suggestions are STRICTLY filtered by this.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        DietaryPreference.values().forEach { diet ->
                            Surface(
                                color = if (selectedDiet == diet) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedDiet = diet }
                                    .testTag("dietary_choice_${diet.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = diet.icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = diet.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedDiet == diet) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedDiet == diet) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { step = 4 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("next_step_2_button")
                        ) {
                            Text("Next: Kitchen Mode")
                        }
                    }

                    // Step 4: Kitchen Workspace Setup
                    if (step == 4) {
                        Text(
                            text = "Step 3 of 4: Kitchen Workspace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = workspaceName,
                            onValueChange = { workspaceName = it },
                            label = { Text("Kitchen / Rasoi Name") },
                            modifier = Modifier.fillMaxWidth().testTag("workspace_name_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCommercialMode) "Commercial Kitchen Mode" else "Household Kitchen Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isCommercialMode) "Enables FIFO lot tracking, batch badges & commercial cost efficiency." else "Family pantry management, zero-waste meal prep.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isCommercialMode,
                                onCheckedChange = { isCommercialMode = it },
                                modifier = Modifier.testTag("commercial_mode_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { step = 5 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("next_step_3_button")
                        ) {
                            Text("Next: Native Permissions")
                        }
                    }

                    // Step 5: Native Android Permissions
                    if (step == 5) {
                        Text(
                            text = "Step 4 of 4: Native Device Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure native integrations for seamless receipt scanning & alarm alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // READ_SMS Toggle
                        PermissionRow(
                            title = "READ_SMS Hook",
                            description = "Auto-capture Swiggy, Blinkit, and Zepto order confirmations",
                            checked = smsPermission,
                            onCheckedChange = { smsPermission = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Notification Listener Access Toggle
                        PermissionRow(
                            title = "Notification Listener",
                            description = "Sniff order notifications directly from notification tray",
                            checked = notifPermission,
                            onCheckedChange = { notifPermission = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Camera Access Toggle
                        PermissionRow(
                            title = "Camera Access",
                            description = "Live packaging OCR scanner and bill reader",
                            checked = cameraPermission,
                            onCheckedChange = { cameraPermission = it }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                onCompleteSetup(
                                    phoneInput,
                                    nameInput,
                                    customTitle,
                                    selectedDiet.displayName,
                                    workspaceName,
                                    isCommercialMode,
                                    smsPermission,
                                    notifPermission,
                                    cameraPermission
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("finish_onboarding_button")
                        ) {
                            Text("Launch RasoiGuard")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
