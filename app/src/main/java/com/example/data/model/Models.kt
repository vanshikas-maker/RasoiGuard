package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class DietaryPreference(val displayName: String, val icon: String) {
    VEGETARIAN("Vegetarian", "🌱"),
    NON_VEGETARIAN("Non-Vegetarian", "🍗"),
    EGGETARIAN("Eggetarian", "🥚"),
    VEGAN("Vegan", "🥬"),
    JAIN("Jain (No Onion/Garlic)", "🪷")
}

enum class WorkspaceType(val displayName: String) {
    HOUSEHOLD("Household Rasoi"),
    COMMERCIAL("Commercial Rasoi")
}

enum class ExpiryLevel(val label: String, val colorHex: Long) {
    SAFE("Safe (>3 Days)", 0xFF10B981),
    WARNING("Warning (48 Hours)", 0xFFF59E0B),
    CRITICAL("Critical (24 Hours)", 0xFFEF4444),
    EXPIRED("Expired", 0xFF991B1B)
}

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val phoneNumber: String,
    val name: String = "Kitchen Director",
    val customTitle: String = "Kitchen Director",
    val dietaryPreference: String = DietaryPreference.VEGETARIAN.name,
    val currentWorkspaceId: String = "default_ws",
    val city: String = "Ghaziabad, UP",
    val timezone: String = "Asia/Kolkata (IST - UTC+5:30)",
    val smsPermissionGranted: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val isOnboarded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workspaces")
data class RasoiWorkspace(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userPhone: String,
    val name: String,
    val type: String = WorkspaceType.HOUSEHOLD.name,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val userPhone: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val purchasePrice: Double,
    val purchaseDate: Long = System.currentTimeMillis(),
    val expiryDate: Long,
    val status: String = "ACTIVE", // ACTIVE, CONSUMED, EXPIRED
    val batchNumber: Int = 1,
    val source: String = "MANUAL", // MANUAL, SMS, OCR, WHATSAPP
    val sourceDetails: String = "",
    val consumedDate: Long? = null,
    val wastageLoss: Double = 0.0
) {
    fun getExpiryLevel(currentTime: Long = System.currentTimeMillis()): ExpiryLevel {
        if (status == "EXPIRED") return ExpiryLevel.EXPIRED
        val diffMs = expiryDate - currentTime
        val hoursRemaining = diffMs / (1000 * 60 * 60)
        return when {
            diffMs < 0 -> ExpiryLevel.EXPIRED
            hoursRemaining <= 24 -> ExpiryLevel.CRITICAL
            hoursRemaining <= 72 -> ExpiryLevel.WARNING
            else -> ExpiryLevel.SAFE
        }
    }

    fun hoursLeft(currentTime: Long = System.currentTimeMillis()): Long {
        val diff = expiryDate - currentTime
        return (diff / (1000 * 60 * 60)).coerceAtLeast(0)
    }

    fun daysLeft(currentTime: Long = System.currentTimeMillis()): Long {
        val diff = expiryDate - currentTime
        return (diff / (1000 * 60 * 60 * 24))
    }
}

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val userPhone: String,
    val date: Long = System.currentTimeMillis(),
    val type: String, // PURCHASE, CONSUMED, EXPIRED, ALERT
    val itemName: String,
    val amount: Double,
    val description: String
)
