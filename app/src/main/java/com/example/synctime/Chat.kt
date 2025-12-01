package com.example.synctime

data class Chat(
    val id: String = "",
    val name: String = "",
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val timestamp: Long = 0L,
    val participants: Map<String, Boolean> = emptyMap()  // Map of {userId: true}
) {
    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "", 0L, emptyMap())
}
