package com.example.synctime

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
private var isDarkTheme = true
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        settheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Load Home first
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_notifications -> NotificationsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> null
            }
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
                true
            } ?: false
        }

    }
    fun switchdarkmode (view: View) {
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
