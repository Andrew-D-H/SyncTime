package com.example.synctime

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val profilePicUrl: String = ""
) {
    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "")
}
