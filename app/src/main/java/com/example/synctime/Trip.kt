package com.example.synctime

data class Trip(
    val creator: String = "",
    val name: String = "",
    val destination: String = "",
    val participants: Map<String, Boolean> = emptyMap()
)