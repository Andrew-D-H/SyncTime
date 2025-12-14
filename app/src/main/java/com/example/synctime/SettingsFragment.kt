package com.example.synctime

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment(R.layout.settings_screen) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind the dark mode toggle switch
        val darkModeSwitch: SwitchMaterial = view.findViewById(R.id.dark_mode_switch)

        // Get the current theme state from MainActivity
        val mainActivity = activity as MainActivity
        darkModeSwitch.isChecked = mainActivity.isDarkModeEnabled()

        // Listen for switch toggle changes
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            mainActivity.toggleDarkMode(isChecked) // Notify MainActivity to update theme
        }


    }
}