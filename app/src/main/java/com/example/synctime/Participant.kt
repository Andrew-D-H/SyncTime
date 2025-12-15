package com.example.synctime

data class Participant(
    val name: String = "",
    val email: String = "",
    val location: Location = Location()
)

data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)