package com.example.synctime

data class Friend(
    val name: String,
    val online: Boolean = false,  // offline by default
    val profileUrl: String? = null
)