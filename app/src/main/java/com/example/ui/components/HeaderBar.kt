package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RasoiWorkspace
import com.example.data.model.UserProfile
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    profile: UserProfile?,
    currentWorkspace: RasoiWorkspace?,
    allWorkspaces: List<RasoiWorkspace>,
    onSelectWorkspace: (String) -> Unit,
    onAddNewWorkspace: () -> Unit,
    onUpdateLocation: (String, String) -> Unit,
    onOpenWeeklyReport: () -> Unit,
    onTestAlarm: () -> Unit,
    onLogout: () -> Unit
) {
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("Good day") }

    // Live clock updating every second
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        while (true) {
            val now = Calendar.getInstance()
            currentTimeString = timeFormat.format(now.time)
            currentDateString = dateFormat.format(now.time)
            val hour = now.get(Calendar.HOUR_OF_DAY)
            greeting = when (hour) {
                in 4..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                else -> "Good evening"
            }
            delay(1000)
        }
    }

    var showWorkspaceDropdown by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Row: Brand & Multi-Rasoi Switcher & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Workspace Switcher Chip
                Box {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("workspace_switcher_button")
                            .clickable { showWorkspaceDropdown = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (currentWorkspace?.type == "COMMERCIAL") Icons.Filled.Storefront else Icons.Filled.Kitchen,
                                contentDescription = "Kitchen type",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentWorkspace?.name ?: "Household Rasoi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentWorkspace?.type == "COMMERCIAL") "Commercial FIFO" else "Household Pantry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Switch workspace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showWorkspaceDropdown,
                        onDismissRequest = { showWorkspaceDropdown = false }
                    ) {
                        Text(
                            text = "SWITCH RASOI WORKSPACE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        allWorkspaces.forEach { ws ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (ws.type == "COMMERCIAL") Icons.Filled.Storefront else Icons.Filled.Kitchen,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (ws.id == currentWorkspace?.id) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = ws.name,
                                                fontWeight = if (ws.id == currentWorkspace?.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = if (ws.type == "COMMERCIAL") "Commercial Mode" else "Household Mode",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectWorkspace(ws.id)
                                    showWorkspaceDropdown = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+ Add New Rasoi",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            onClick = {
                                showWorkspaceDropdown = false
                                onAddNewWorkspace()
                            }
                        )
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenWeeklyReport,
                        modifier = Modifier.testTag("header_weekly_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = "Weekly Efficiency Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onTestAlarm,
                        modifier = Modifier.testTag("test_alarm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Test Expiry Alarms",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenuDropdown = true },
                            modifier = Modifier.testTag("header_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Menu options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Weekly Efficiency Report") },
                                leadingIcon = { Icon(Icons.Outlined.BarChart, null) },
                                onClick = {
                                    showMenuDropdown = false
                                    onOpenWeeklyReport()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Change City / Timezone") },
                                leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                                onClick = {
                                    showMenuDropdown = false
                                    showLocationDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Logout & Switch Phone") },
                                leadingIcon = { Icon(Icons.Outlined.Logout, null) },
                                onClick = {
                                    showMenuDropdown = false
                                    onLogout()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Greeting & Live Clock Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dynamic Greeting with Title & User Name
                        val title = profile?.customTitle?.ifBlank { "Kitchen Director" } ?: "Kitchen Director"
                        val userName = profile?.name?.ifBlank { "Chef" } ?: "Chef"

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$greeting, $title $userName!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Interactive Location and Timezone Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showLocationDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Place,
                                    contentDescription = "Location",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${profile?.city ?: "Ghaziabad, UP"} • ${profile?.dietaryPreference ?: "Vegetarian"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Live Clock Badge
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentTimeString.ifBlank { "Live Clock" },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = currentDateString,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Location & Timezone Selector Dialog
    if (showLocationDialog) {
        var cityInput by remember { mutableStateOf(profile?.city ?: "Ghaziabad, UP") }
        var selectedTz by remember { mutableStateOf(profile?.timezone ?: "Asia/Kolkata (IST - UTC+5:30)") }

        val timezones = listOf(
            "Asia/Kolkata (IST - UTC+5:30)",
            "Asia/Dubai (GST - UTC+4:00)",
            "UTC (GMT +0:00)",
            "America/New_York (EST - UTC-5:00)",
            "Europe/London (BST - UTC+1:00)",
            "Asia/Singapore (SGT - UTC+8:00)"
        )

        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Kitchen Location & Timezone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("City / Region") },
                        leadingIcon = { Icon(Icons.Filled.LocationCity, null) },
                        modifier = Modifier.fillMaxWidth().testTag("city_input_field")
                    )

                    Text(
                        text = "Timezone",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    timezones.forEach { tz ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTz = tz }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTz == tz,
                                onClick = { selectedTz = tz }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tz,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (selectedTz == tz) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateLocation(cityInput, selectedTz)
                        showLocationDialog = false
                    },
                    modifier = Modifier.testTag("save_location_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
