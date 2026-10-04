package com.example.data.local

import androidx.room.*
import com.example.data.model.ActivityLog
import com.example.data.model.InventoryItem
import com.example.data.model.RasoiWorkspace
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles WHERE phoneNumber = :phone LIMIT 1")
    fun getProfile(phone: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getProfileOnce(phone: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Query("UPDATE user_profiles SET city = :city, timezone = :timezone WHERE phoneNumber = :phone")
    suspend fun updateCityAndTimezone(phone: String, city: String, timezone: String)

    @Query("UPDATE user_profiles SET currentWorkspaceId = :workspaceId WHERE phoneNumber = :phone")
    suspend fun updateCurrentWorkspace(phone: String, workspaceId: String)

    @Query("UPDATE user_profiles SET smsPermissionGranted = :sms, notificationListenerEnabled = :notif, cameraPermissionGranted = :camera WHERE phoneNumber = :phone")
    suspend fun updatePermissions(phone: String, sms: Boolean, notif: Boolean, camera: Boolean)
}

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces WHERE userPhone = :phone ORDER BY createdAt ASC")
    fun getWorkspaces(phone: String): Flow<List<RasoiWorkspace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: RasoiWorkspace)

    @Query("DELETE FROM workspaces WHERE id = :workspaceId")
    suspend fun deleteWorkspace(workspaceId: String)

    @Query("SELECT * FROM workspaces WHERE id = :workspaceId LIMIT 1")
    suspend fun getWorkspaceById(workspaceId: String): RasoiWorkspace?
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE workspaceId = :workspaceId ORDER BY expiryDate ASC")
    fun getItemsForWorkspace(workspaceId: String): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE workspaceId = :workspaceId AND status = 'ACTIVE' ORDER BY expiryDate ASC")
    fun getActiveItems(workspaceId: String): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>)

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("UPDATE inventory_items SET status = 'CONSUMED', consumedDate = :time WHERE id = :id")
    suspend fun markConsumed(id: String, time: Long)

    @Query("UPDATE inventory_items SET status = 'EXPIRED', wastageLoss = :loss WHERE id = :id")
    suspend fun markExpired(id: String, loss: Double)

    @Query("SELECT COUNT(*) FROM inventory_items WHERE workspaceId = :workspaceId AND name = :name")
    suspend fun countBatchesForName(workspaceId: String, name: String): Int
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_logs WHERE workspaceId = :workspaceId ORDER BY date DESC")
    fun getLogsForWorkspace(workspaceId: String): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ActivityLog>)
}

@Database(
    entities = [UserProfile::class, RasoiWorkspace::class, InventoryItem::class, ActivityLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rasoi_guard_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
