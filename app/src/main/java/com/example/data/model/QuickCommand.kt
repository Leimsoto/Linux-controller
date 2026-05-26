package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_commands")
data class QuickCommand(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int = 0, // 0 means global/available for all profiles
    val name: String,
    val commandCode: String,
    val iconName: String = "code", // Map to logical icons e.g. "volume", "power", "tv", "search", "terminal"
    val category: String = "General"
)
