package com.example.synctime

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L
) {
    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "", 0L)
}
