package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.RasoiRepository
import com.example.service.GeminiAiService
import com.example.service.NotificationHelper
import com.example.service.ParsedItemDraft
import com.example.service.SmsReader
import com.example.service.ZeroWasteRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FinancialStats(
    val totalSpend: Double = 0.0,
    val wastageLoss: Double = 0.0,
    val efficiencyPercent: Int = 100,
    val activeItemCount: Int = 0,
    val consumedCount: Int = 0,
    val expiredCount: Int = 0
)

class RasoiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RasoiRepository(AppDatabase.getDatabase(application))
    private val notificationHelper = NotificationHelper(application)

    // Current authenticated phone number (null if not logged in)
    private val _currentPhone = MutableStateFlow<String?>(null)
    val currentPhone: StateFlow<String?> = _currentPhone.asStateFlow()

    // Current User Profile
    val userProfile: StateFlow<UserProfile?> = _currentPhone.flatMapLatest { phone ->
        if (phone.isNullOrBlank()) flowOf(null)
        else repository.getProfile(phone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Workspaces
    val workspaces: StateFlow<List<RasoiWorkspace>> = _currentPhone.flatMapLatest { phone ->
        if (phone.isNullOrBlank()) flowOf(emptyList())
        else repository.getWorkspaces(phone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current active workspace ID
    private val _selectedWorkspaceId = MutableStateFlow<String?>(null)
    val selectedWorkspaceId: StateFlow<String?> = _selectedWorkspaceId.asStateFlow()

    val currentWorkspace: StateFlow<RasoiWorkspace?> = combine(workspaces, _selectedWorkspaceId) { wsList, selId ->
        wsList.find { it.id == selId } ?: wsList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Inventory items for active workspace
    val inventoryItems: StateFlow<List<InventoryItem>> = currentWorkspace.flatMapLatest { ws ->
        if (ws == null) flowOf(emptyList())
        else repository.getItems(ws.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Activity logs for active workspace
    val activityLogs: StateFlow<List<ActivityLog>> = currentWorkspace.flatMapLatest { ws ->
        if (ws == null) flowOf(emptyList())
        else repository.getActivityLogs(ws.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial Metrics
    val financialStats: StateFlow<FinancialStats> = inventoryItems.map { items ->
        var totalSpend = 0.0
        var wasteLoss = 0.0
        var activeCount = 0
        var consumedCount = 0
        var expiredCount = 0

        for (item in items) {
            totalSpend += item.purchasePrice
            when (item.status) {
                "ACTIVE" -> activeCount++
                "CONSUMED" -> consumedCount++
                "EXPIRED" -> {
                    expiredCount++
                    wasteLoss += if (item.wastageLoss > 0) item.wastageLoss else item.purchasePrice
                }
            }
        }

        val eff = if (totalSpend <= 0.0) 100
        else (((totalSpend - wasteLoss) / totalSpend) * 100).toInt().coerceIn(0, 100)

        FinancialStats(
            totalSpend = totalSpend,
            wastageLoss = wasteLoss,
            efficiencyPercent = eff,
            activeItemCount = activeCount,
            consumedCount = consumedCount,
            expiredCount = expiredCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialStats())

    // Active AI Chat Drawer
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "AI",
                text = "Namaste! I'm RasoiGuard AI. I monitor your active pantry expiry dates and recommend delicious zero-waste recipes. How can I help today?"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // Active recipe preview dialog
    private val _activeRecipe = MutableStateFlow<ZeroWasteRecipe?>(null)
    val activeRecipe: StateFlow<ZeroWasteRecipe?> = _activeRecipe.asStateFlow()

    // UI Toast / Alert message
    private val _uiNotice = MutableStateFlow<String?>(null)
    val uiNotice: StateFlow<String?> = _uiNotice.asStateFlow()

    fun clearNotice() { _uiNotice.value = null }

    // AUTH & RESUME FLOW
    fun loginWithPhone(phone: String, onNeedsOnboarding: () -> Unit, onResumed: () -> Unit) {
        val cleanPhone = phone.trim().replace(" ", "").replace("+91", "")
        _currentPhone.value = cleanPhone

        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getProfileOnce(cleanPhone)
            if (existing != null && existing.isOnboarded) {
                _selectedWorkspaceId.value = existing.currentWorkspaceId
                launch(Dispatchers.Main) { onResumed() }
            } else {
                launch(Dispatchers.Main) { onNeedsOnboarding() }
            }
        }
    }

    fun completeOnboarding(
        phone: String,
        name: String,
        title: String,
        dietary: String,
        workspaceName: String,
        isCommercial: Boolean,
        smsPerm: Boolean,
        notifPerm: Boolean,
        camPerm: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultWsId = UUID.randomUUID().toString()
            val initialWorkspace = RasoiWorkspace(
                id = defaultWsId,
                userPhone = phone,
                name = workspaceName.ifBlank { if (isCommercial) "Commercial Rasoi" else "Household Rasoi" },
                type = if (isCommercial) WorkspaceType.COMMERCIAL.name else WorkspaceType.HOUSEHOLD.name,
                isDefault = true
            )
            repository.createWorkspace(initialWorkspace)

            val profile = UserProfile(
                phoneNumber = phone,
                name = name.ifBlank { "Kitchen Director" },
                customTitle = title.ifBlank { "Kitchen Director" },
                dietaryPreference = dietary,
                currentWorkspaceId = defaultWsId,
                smsPermissionGranted = smsPerm,
                notificationListenerEnabled = notifPerm,
                cameraPermissionGranted = camPerm,
                isOnboarded = true
            )
            repository.saveProfile(profile)
            _selectedWorkspaceId.value = defaultWsId
            _currentPhone.value = phone
        }
    }

    fun selectWorkspace(workspaceId: String) {
        _selectedWorkspaceId.value = workspaceId
        val phone = _currentPhone.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCurrentWorkspace(phone, workspaceId)
        }
    }

    fun createNewWorkspace(name: String, isCommercial: Boolean) {
        val phone = _currentPhone.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val newWs = RasoiWorkspace(
                id = UUID.randomUUID().toString(),
                userPhone = phone,
                name = name.ifBlank { "New Rasoi" },
                type = if (isCommercial) WorkspaceType.COMMERCIAL.name else WorkspaceType.HOUSEHOLD.name
            )
            repository.createWorkspace(newWs)
            _selectedWorkspaceId.value = newWs.id
            repository.updateCurrentWorkspace(phone, newWs.id)
            _uiNotice.value = "Created workspace: ${newWs.name}"
        }
    }

    fun updateLocationAndTimezone(city: String, timezone: String) {
        val phone = _currentPhone.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLocationAndTimezone(phone, city, timezone)
        }
    }

    fun updatePermissions(sms: Boolean, notif: Boolean, camera: Boolean) {
        val phone = _currentPhone.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePermissions(phone, sms, notif, camera)
        }
    }

    // INVENTORY CRUD
    fun addManualItem(
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        price: Double,
        shelfLifeDays: Int
    ) {
        val ws = currentWorkspace.value ?: return
        val phone = _currentPhone.value ?: return
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, shelfLifeDays)

        val item = InventoryItem(
            workspaceId = ws.id,
            userPhone = phone,
            name = name.trim(),
            category = category,
            quantity = quantity,
            unit = unit,
            purchasePrice = price,
            expiryDate = cal.timeInMillis,
            source = "MANUAL",
            sourceDetails = "Manual Entry"
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.addItem(item)
            _uiNotice.value = "Added ${item.name} to ${ws.name}"
        }
    }

    fun addParsedDrafts(drafts: List<ParsedItemDraft>, source: String, details: String) {
        val ws = currentWorkspace.value ?: return
        val phone = _currentPhone.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val items = drafts.map { draft ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, draft.shelfLifeDays)
                InventoryItem(
                    workspaceId = ws.id,
                    userPhone = phone,
                    name = draft.name,
                    category = draft.category,
                    quantity = draft.quantity,
                    unit = draft.unit,
                    purchasePrice = draft.price,
                    expiryDate = cal.timeInMillis,
                    source = source,
                    sourceDetails = details
                )
            }
            repository.addItems(items)
            _uiNotice.value = "Synced ${items.size} items from $source into ${ws.name}!"
        }
    }

    fun markConsumed(item: InventoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markConsumed(item)
            _uiNotice.value = "Zero-Waste Win! Marked ${item.name} as consumed."
        }
    }

    fun markExpired(item: InventoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markExpired(item)
            _uiNotice.value = "Recorded ₹${item.purchasePrice} wastage loss for ${item.name}."
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteItem(item.id)
            _uiNotice.value = "Removed ${item.name}."
        }
    }

    fun triggerAlarmTest() {
        notificationHelper.sendTestAlarm("RasoiGuard native notification engine active. Expiry alarms ready!")
        _uiNotice.value = "Sent test push notification to system tray!"
    }

    fun checkAndNotifyExpiryAlerts() {
        val active = inventoryItems.value.filter { it.status == "ACTIVE" }
        for (item in active) {
            val lvl = item.getExpiryLevel()
            if (lvl == ExpiryLevel.CRITICAL || lvl == ExpiryLevel.WARNING) {
                notificationHelper.sendExpiryAlert(item)
                break
            }
        }
    }

    // AI ASSISTANT CHAT
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val currentList = _chatMessages.value.toMutableList()
        val userMsg = ChatMessage(sender = "USER", text = userText.trim())
        currentList.add(userMsg)
        _chatMessages.value = currentList
        _isAiThinking.value = true

        val active = inventoryItems.value.filter { it.status == "ACTIVE" }
        val dietary = userProfile.value?.dietaryPreference ?: "Vegetarian"
        val wsName = currentWorkspace.value?.name ?: "Household Rasoi"

        viewModelScope.launch(Dispatchers.IO) {
            val answer = GeminiAiService.askAssistant(userText, active, dietary, wsName)
            val updated = _chatMessages.value.toMutableList()
            updated.add(ChatMessage(sender = "AI", text = answer))
            _chatMessages.value = updated
            _isAiThinking.value = false
        }
    }

    fun showRecipeForItem(item: InventoryItem) {
        val dietary = userProfile.value?.dietaryPreference ?: "Vegetarian"
        val recipe = GeminiAiService.getZeroWasteRecipeForItem(item, dietary)
        _activeRecipe.value = recipe
    }

    fun closeRecipe() {
        _activeRecipe.value = null
    }

    fun logout() {
        _currentPhone.value = null
        _selectedWorkspaceId.value = null
    }
}
