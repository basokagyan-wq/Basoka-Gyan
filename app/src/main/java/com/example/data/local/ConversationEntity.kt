package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val intentName: String? = null,
    val intentTarget: String? = null,
    val intentParam: String? = null,
    val status: String = "IDLE",
    val resultMessage: String? = null,
    val attachmentUri: String? = null
)
