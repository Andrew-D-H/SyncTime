package com.example.synctime

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

private var isDarkTheme = true

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        settheme()

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
                    val addBottomSheet = AddBottomSheet()
                    addBottomSheet.show(supportFragmentManager, "AddBottomSheet")
                    false
                }

                R.id.nav_notifications -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, FriendsFragment())
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

    fun switchdarkmode(view: View) {
        isDarkTheme = !isDarkTheme // Toggle the theme flag
        settheme()
        recreate() // Recreate the activity to apply the new theme
    }

    fun settheme() {
        if (isDarkTheme) {
            setTheme(R.style.Theme_SyncTimeDark)
        } else {
            setTheme(R.style.Theme_SyncTime)
        }
    }
}