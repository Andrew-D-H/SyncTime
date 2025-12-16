package com.example.synctime

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    private var isDarkTheme: Boolean = false // Default to light theme

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load saved preferences BEFORE calling super.onCreate()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        isDarkTheme = preferences.getBoolean("dark_theme", false)

        // Apply the saved theme before initializing the activity
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        Log.d("MainActivity", "Launching in ${if (isDarkTheme) "Dark Mode" else "Light Mode"}")

        // Now call super.onCreate()
        super.onCreate(savedInstanceState)

        // Initialize views and activity logic
        supportActionBar?.hide()
        setContentView(R.layout.main_menu)

        Log.d("MainActivity", "Theme loaded: Dark mode is $isDarkTheme")

        // Set up bottom navigation
        setupBottomNavigation(findViewById(R.id.bottom_navigation))
    }

    /**
     * Toggles the theme (Light or Dark) and saves the state.
     */
    fun toggleDarkMode(enableDarkMode: Boolean) {
        if (isDarkTheme == enableDarkMode) {
            Log.d("MainActivity", "Theme is already set to: ${if (enableDarkMode) "Dark Mode" else "Light Mode"}")
            return
        }

        // Update theme state and preferences
        isDarkTheme = enableDarkMode
        preferences.edit().putBoolean("dark_theme", isDarkTheme).apply()

        // Apply the theme and recreate the activity to reflect changes
        AppCompatDelegate.setDefaultNightMode(
            if (enableDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        recreate()
    }

    /**
     * Checks if dark mode is currently active.
     */
    fun isDarkModeEnabled(): Boolean {
        return isDarkTheme
    }

    /**
     * Set up BottomNavigationView to switch fragments.
     */
    private fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        // Default fragment is HomeFragment
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
                R.id.nav_notifications -> FriendsFragment()
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

    /**
     * Toggle language between English and Spanish.
     * Called from the settings UI when the user clicks the language button.
     */
    fun setlanguage(view: View) {
        // set app locale given the user's selected locale
        val lang = AppCompatDelegate.getApplicationLocales()
        if (lang.toLanguageTags().toString() == "en") {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    "es" // ISO for Spanish
                )
            )
        } else if (lang.toLanguageTags().toString() == "es") {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    "en" // ISO for English
                )
            )
        }
    }
}