package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PCProfile
import com.example.data.model.QuickCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteControlDao {

    // Profiles
    @Query("SELECT * FROM pc_profiles ORDER BY name ASC")
    fun getProfiles(): Flow<List<PCProfile>>

    @Query("SELECT * FROM pc_profiles WHERE isDefault = 1 LIMIT 1")
    fun getDefaultProfile(): Flow<PCProfile?>

    @Query("SELECT * FROM pc_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfileSync(): PCProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PCProfile): Long

    @Update
    suspend fun updateProfile(profile: PCProfile)

    @Delete
    suspend fun deleteProfile(profile: PCProfile)

    @Query("UPDATE pc_profiles SET isDefault = 0")
    suspend fun clearActiveProfiles()

    // Commands
    @Query("SELECT * FROM quick_commands ORDER BY category ASC, name ASC")
    fun getCommands(): Flow<List<QuickCommand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: QuickCommand)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommands(commands: List<QuickCommand>)

    @Delete
    suspend fun deleteCommand(command: QuickCommand)

    @Query("SELECT COUNT(*) FROM quick_commands")
    suspend fun getCommandsCount(): Int
}
