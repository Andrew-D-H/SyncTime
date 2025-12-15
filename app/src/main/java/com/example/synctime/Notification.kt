package com.example.synctime

data class Notification(
    val id: String = "",
    val senderId: String = "",
    val senderProfileUrl: String? = null,
    val title: String = "",
    val meta: String = "",
    val type: String = "",
    val timestamp: Long = 0L,
    var isRead: Boolean = false
)
