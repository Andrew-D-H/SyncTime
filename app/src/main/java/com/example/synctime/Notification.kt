package com.example.synctime

data class Notification(
    val id: Int,
    val avatarRes: Int,
    val title: String,
    val meta: String,
    var isRead: Boolean
)