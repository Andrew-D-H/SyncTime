package com.example.synctime

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    private var isDarkTheme: Boolean = false // Default to light theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the saved dark mode preference
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        isDarkTheme = preferences.getBoolean("dark_theme", false)

        // Log theme status
        Log.d("MainActivity", "Theme loaded: Dark mode is $isDarkTheme")

        // Apply the saved theme
        val mode = if (isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Set the content view
        setContentView(R.layout.main_menu)
        setupBottomNavigation(findViewById(R.id.bottom_navigation))
    }

    /**
     * Applies the theme (Light or Dark) using AppCompatDelegate.
     * This ensures that it doesn't reapply the same mode, avoiding infinite loops.
     */
    private fun applyTheme(darkTheme: Boolean) {
        val currentMode = if (darkTheme) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        // Only apply the theme if the mode is different
        if (AppCompatDelegate.getDefaultNightMode() != currentMode) {
            Log.d("MainActivity", "Applying theme: ${if (darkTheme) "Dark Mode" else "Light Mode"}")
            AppCompatDelegate.setDefaultNightMode(currentMode) // Change the theme mode
        }
    }

    /**
     * Toggles the theme (Light or Dark) when the user changes it, e.g., via settings.
     */
    fun toggleDarkMode(enableDarkMode: Boolean) {
        if (isDarkTheme != enableDarkMode) { // Avoid unnecessary changes
            Log.d("MainActivity", "Toggling theme to: ${if (enableDarkMode) "Dark Mode" else "Light Mode"}")

            // Update dark theme state
            isDarkTheme = enableDarkMode

            // Save the new preference
            preferences.edit().putBoolean("dark_theme", isDarkTheme).apply()

            // Apply the new theme
            val mode = if (isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)

            // Recreate the activity to reflect changes
            recreate()
        } else {
            Log.d("MainActivity", "Theme change ignored as it's already set to: ${if (enableDarkMode) "Dark Mode" else "Light Mode"}")
        }
    }

    /**
     * Returns whether dark mode is currently enabled.
     * Checks SharedPreferences value to determine the mode.
     */
    fun isDarkModeEnabled(): Boolean {
        return isDarkTheme
    }

    /**
     * Set up BottomNavigationView to switch fragments.
     */
    private fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        // Show HomeFragment by default
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val fragment = when (menuItem.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_search -> SearchFragment()
                R.id.nav_add -> {
                    AddBottomSheet().show(supportFragmentManager, "AddBottomSheet")
                    return@setOnItemSelectedListener false
                }
                R.id.nav_notifications -> NotificationsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> null
            }

            if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
                true
            } else {
                false
            }
        }
    }
}