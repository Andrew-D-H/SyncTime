package com.example.synctime

import com.google.firebase.database.PropertyName

data class User(
    var uid: String = "",
    var email: String = "",

    // Handle both "displayName" and "name" from Firebase
    @get:PropertyName("displayName")
    @set:PropertyName("displayName")
    var displayName: String = "",

    // This will catch the "name" field from Firebase
    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    // Handle both "profilePicUrl" and "profileURL" from Firebase
    @get:PropertyName("profilePicUrl")
    @set:PropertyName("profilePicUrl")
    var profilePicUrl: String = "",

    @get:PropertyName("profileURL")
    @set:PropertyName("profileURL")
    var profileURL: String = ""
) {
    // No-argument constructor required for Firebase
    constructor() : this("", "", "", "", "", "")

    // Helper to get the actual display name (whichever field is populated)
    fun getActualDisplayName(): String {
        return when {
            displayName.isNotBlank() -> displayName
            name.isNotBlank() -> name
            email.isNotBlank() -> email
            else -> "Unknown User"
        }
    }

    // Helper to get the actual profile picture URL
    fun getActualProfilePicUrl(): String {
        return when {
            profilePicUrl.isNotBlank() -> profilePicUrl
            profileURL.isNotBlank() -> profileURL
            else -> ""
        }
    }
}