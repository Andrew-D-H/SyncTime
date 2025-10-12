package com.example.synctime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Load Home first by default
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        // Handle Bottom Navigation item selection
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }

                R.id.nav_search -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SearchFragment())
                        .commit()
                    true
                }

                R.id.nav_add -> {
                    // Show the Bottom Sheet instead of a Fragment
                    val addBottomSheet = AddBottomSheet()
                    addBottomSheet.show(supportFragmentManager, "AddBottomSheet")
                    false // prevents highlighting the add icon (since it's not a real tab)
                }

                R.id.nav_notifications -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, NotificationsFragment())
                        .commit()
                    true
                }

                R.id.nav_settings -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SettingsFragment())
                        .commit()
                    true
                }

                else -> false
            }
        }
    }
}
