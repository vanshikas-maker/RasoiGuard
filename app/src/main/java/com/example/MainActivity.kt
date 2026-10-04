package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RasoiViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RasoiGuardApp()
            }
        }
    }
}

@Composable
fun RasoiGuardApp(viewModel: RasoiViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State collections
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val currentWorkspace by viewModel.currentWorkspace.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val financialStats by viewModel.financialStats.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val activeRecipe by viewModel.activeRecipe.collectAsStateWithLifecycle()
    val uiNotice by viewModel.uiNotice.collectAsStateWithLifecycle()

    // Navigation and Modal States
    var currentNavDestination by remember { mutableStateOf("INVENTORY") } // INVENTORY or CALENDAR
    var showAddItemsModal by remember { mutableStateOf(false) }
    var showAddWorkspaceDialog by remember { mutableStateOf(false) }
    var showAiChatDrawer by remember { mutableStateOf(false) }
    var showWeeklyReportDialog by remember { mutableStateOf(false) }

    // Real Native Device Permission Launchers
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        viewModel.updatePermissions(sms = isGranted, notif = true, camera = hasCameraPermission)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        viewModel.updatePermissions(sms = hasSmsPermission, notif = true, camera = isGranted)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Display Toast / Notice in Snackbar
    LaunchedEffect(uiNotice) {
        uiNotice?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotice()
        }
    }

    // If not authenticated, render Auth & Onboarding Flow
    if (userProfile == null || !userProfile!!.isOnboarded) {
        AuthScreen(
            onLoginSubmit = { phone, onNeedsOnboarding, onResumed ->
                viewModel.loginWithPhone(phone, onNeedsOnboarding, onResumed)
            },
            onCompleteSetup = { phone, name, title, dietary, wsName, isCommercial, sms, notif, cam ->
                viewModel.completeOnboarding(
                    phone = phone,
                    name = name,
                    title = title,
                    dietary = dietary,
                    workspaceName = wsName,
                    isCommercial = isCommercial,
                    smsPerm = sms,
                    notifPerm = notif,
                    camPerm = cam
                )
            }
        )
        return
    }

    val isCommercial = currentWorkspace?.type == "COMMERCIAL"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HeaderBar(
                profile = userProfile,
                currentWorkspace = currentWorkspace,
                allWorkspaces = workspaces,
                onSelectWorkspace = { wsId -> viewModel.selectWorkspace(wsId) },
                onAddNewWorkspace = { showAddWorkspaceDialog = true },
                onUpdateLocation = { city, tz -> viewModel.updateLocationAndTimezone(city, tz) },
                onOpenWeeklyReport = { showWeeklyReportDialog = true },
                onTestAlarm = { viewModel.triggerAlarmTest() },
                onLogout = { viewModel.logout() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentNavDestination == "INVENTORY",
                    onClick = { currentNavDestination = "INVENTORY" },
                    icon = {
                        Icon(
                            imageVector = if (currentNavDestination == "INVENTORY") Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = "Pantry Inventory"
                        )
                    },
                    label = { Text("Pantry Inventory") },
                    modifier = Modifier.testTag("nav_inventory")
                )

                NavigationBarItem(
                    selected = currentNavDestination == "CALENDAR",
                    onClick = { currentNavDestination = "CALENDAR" },
                    icon = {
                        Icon(
                            imageVector = if (currentNavDestination == "CALENDAR") Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar & Spend"
                        )
                    },
                    label = { Text("Calendar & Spend") },
                    modifier = Modifier.testTag("nav_calendar")
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Floating AI Rasoi Assistant Button
                ExtendedFloatingActionButton(
                    onClick = { showAiChatDrawer = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("floating_ai_assistant_button")
                ) {
                    Text(text = "👨‍🍳", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rasoi Assistant",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Primary "+ Add Items" Floating Action Button
                ExtendedFloatingActionButton(
                    onClick = { showAddItemsModal = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("main_add_items_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Items")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Items",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNavDestination) {
                "INVENTORY" -> {
                    InventoryScreen(
                        items = inventoryItems,
                        isCommercial = isCommercial,
                        onOpenAddModal = { showAddItemsModal = true },
                        onMarkConsumed = { item -> viewModel.markConsumed(item) },
                        onMarkExpired = { item -> viewModel.markExpired(item) },
                        onShowRecipe = { item -> viewModel.showRecipeForItem(item) },
                        onDeleteItem = { item -> viewModel.deleteItem(item) }
                    )
                }
                "CALENDAR" -> {
                    CalendarScreen(
                        items = inventoryItems,
                        logs = activityLogs,
                        stats = financialStats,
                        onOpenAddModal = { showAddItemsModal = true },
                        onOpenWeeklyReport = { showWeeklyReportDialog = true }
                    )
                }
            }
        }
    }

    // Modal: Weekly Efficiency Report Visual Dashboard
    if (showWeeklyReportDialog) {
        WeeklyEfficiencyReportDialog(
            items = inventoryItems,
            onDismiss = { showWeeklyReportDialog = false }
        )
    }

    // Modal: Ingest Items (4 Tabs)
    AddItemsModal(
        isOpen = showAddItemsModal,
        onDismiss = { showAddItemsModal = false },
        hasSmsPermission = hasSmsPermission,
        hasCameraPermission = hasCameraPermission,
        onRequestSmsPermission = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
        onRequestCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onAddManualItem = { name, cat, qty, unit, price, shelfLife ->
            viewModel.addManualItem(name, cat, qty, unit, price, shelfLife)
        },
        onAddParsedDrafts = { drafts, source, details ->
            viewModel.addParsedDrafts(drafts, source, details)
        }
    )

    // Modal: Add New Workspace Dialog
    if (showAddWorkspaceDialog) {
        var newWsName by remember { mutableStateOf("") }
        var newIsCommercial by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddWorkspaceDialog = false },
            title = { Text("+ Add New Rasoi Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newWsName,
                        onValueChange = { newWsName = it },
                        label = { Text("Kitchen Name *") },
                        placeholder = { Text("e.g. Household Kitchen 2, Bakery Section") },
                        modifier = Modifier.fillMaxWidth().testTag("new_workspace_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Commercial Mode (FIFO)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enables batch codes & cost efficiency analysis",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = newIsCommercial,
                            onCheckedChange = { newIsCommercial = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newWsName.isNotBlank()) {
                            viewModel.createNewWorkspace(newWsName, newIsCommercial)
                            showAddWorkspaceDialog = false
                        }
                    },
                    enabled = newWsName.isNotBlank(),
                    modifier = Modifier.testTag("save_new_workspace_button")
                ) {
                    Text("Create Workspace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkspaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Zero-Waste Recipe Dialog
    activeRecipe?.let { recipe ->
        ZeroWasteRecipeDialog(
            recipe = recipe,
            onDismiss = { viewModel.closeRecipe() }
        )
    }

    // Drawer: AI Chef Chat Assistant
    val expiringItems = inventoryItems
        .filter { it.status == "ACTIVE" }
        .sortedBy { it.expiryDate }
        .map { "${it.name} (${it.hoursLeft()}h)" }

    AiChatDrawer(
        isOpen = showAiChatDrawer,
        onClose = { showAiChatDrawer = false },
        messages = chatMessages,
        isThinking = isAiThinking,
        expiringItemNames = expiringItems,
        dietaryPreference = userProfile?.dietaryPreference ?: "Vegetarian",
        onSendMessage = { text -> viewModel.sendChatMessage(text) }
    )
}
