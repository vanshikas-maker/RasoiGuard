package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RasoiRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val workspaceDao = db.workspaceDao()
    private val inventoryDao = db.inventoryDao()
    private val activityDao = db.activityDao()

    fun getProfile(phone: String): Flow<UserProfile?> = userDao.getProfile(phone)
    suspend fun getProfileOnce(phone: String): UserProfile? = userDao.getProfileOnce(phone)
    suspend fun saveProfile(profile: UserProfile) = userDao.insertOrUpdateProfile(profile)
    suspend fun updateLocationAndTimezone(phone: String, city: String, timezone: String) =
        userDao.updateCityAndTimezone(phone, city, timezone)
    suspend fun updateCurrentWorkspace(phone: String, wsId: String) =
        userDao.updateCurrentWorkspace(phone, wsId)
    suspend fun updatePermissions(phone: String, sms: Boolean, notif: Boolean, camera: Boolean) =
        userDao.updatePermissions(phone, sms, notif, camera)

    fun getWorkspaces(phone: String): Flow<List<RasoiWorkspace>> = workspaceDao.getWorkspaces(phone)
    suspend fun createWorkspace(workspace: RasoiWorkspace) = workspaceDao.insertWorkspace(workspace)
    suspend fun deleteWorkspace(workspaceId: String) = workspaceDao.deleteWorkspace(workspaceId)
    suspend fun getWorkspace(workspaceId: String) = workspaceDao.getWorkspaceById(workspaceId)

    fun getItems(workspaceId: String): Flow<List<InventoryItem>> = inventoryDao.getItemsForWorkspace(workspaceId)
    fun getActiveItems(workspaceId: String): Flow<List<InventoryItem>> = inventoryDao.getActiveItems(workspaceId)

    suspend fun addItem(item: InventoryItem) {
        val existingBatches = inventoryDao.countBatchesForName(item.workspaceId, item.name)
        val finalItem = if (item.batchNumber <= 1 && existingBatches > 0) {
            item.copy(batchNumber = existingBatches + 1)
        } else item
        inventoryDao.insertItem(finalItem)
        activityDao.insertLog(
            ActivityLog(
                workspaceId = finalItem.workspaceId,
                userPhone = finalItem.userPhone,
                date = finalItem.purchaseDate,
                type = "PURCHASE",
                itemName = finalItem.name,
                amount = finalItem.purchasePrice,
                description = "Added ${finalItem.quantity} ${finalItem.unit} of ${finalItem.name} via ${finalItem.source}"
            )
        )
    }

    suspend fun addItems(items: List<InventoryItem>) {
        items.forEach { addItem(it) }
    }

    suspend fun updateItem(item: InventoryItem) = inventoryDao.updateItem(item)
    suspend fun deleteItem(id: String) = inventoryDao.deleteItem(id)

    suspend fun markConsumed(item: InventoryItem) {
        val now = System.currentTimeMillis()
        inventoryDao.markConsumed(item.id, now)
        activityDao.insertLog(
            ActivityLog(
                workspaceId = item.workspaceId,
                userPhone = item.userPhone,
                date = now,
                type = "CONSUMED",
                itemName = item.name,
                amount = item.purchasePrice,
                description = "Consumed ${item.name} (${item.quantity} ${item.unit}) - Zero food waste!"
            )
        )
    }

    suspend fun markExpired(item: InventoryItem) {
        inventoryDao.markExpired(item.id, item.purchasePrice)
        activityDao.insertLog(
            ActivityLog(
                workspaceId = item.workspaceId,
                userPhone = item.userPhone,
                date = System.currentTimeMillis(),
                type = "EXPIRED",
                itemName = item.name,
                amount = item.purchasePrice,
                description = "Expired: ${item.name} (Loss: ₹${item.purchasePrice})"
            )
        )
    }

    fun getActivityLogs(workspaceId: String): Flow<List<ActivityLog>> =
        activityDao.getLogsForWorkspace(workspaceId)
}
