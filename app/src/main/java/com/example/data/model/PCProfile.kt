package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pc_profiles")
data class PCProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String = "root",
    val authType: String = "PASSWORD", // "PASSWORD" or "PRIVATE_KEY"
    val password: String = "",
    val privateKey: String = "",
    val isDefault: Boolean = false,
    val sudoPassword: String? = ""
)
