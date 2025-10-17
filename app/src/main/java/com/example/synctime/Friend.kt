package com.example.synctime

data class Friend(
    val uid: String = "",
    val name: String = "",
    val online: Boolean = false,  // offline by default
    val profileUrl: String? = null,
    var isPending: Boolean = false
)