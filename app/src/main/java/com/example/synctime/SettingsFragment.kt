package com.example.synctime

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SettingsFragment : Fragment(R.layout.settings_screen) {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userListener: ValueEventListener // Firebase listener reference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind UI views
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val ivProfilePic: ImageView = view.findViewById(R.id.ivProfilePic)
        val darkModeSwitch: SwitchMaterial = view.findViewById(R.id.dark_mode_switch)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Ensure current user is not null
        if (currentUser != null) {
            val userId = currentUser.uid
            database = FirebaseDatabase.getInstance().getReference("users").child(userId)

            // Define the ValueEventListener
            userListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Check if the fragment is still added to its activity before updating the UI
                    if (!isAdded) {
                        Log.w("SettingsFragment", "Fragment not attached to context, ignoring data update")
                        return
                    }

                    val name = snapshot.child("name").getValue(String::class.java)
                    val photoUrl = snapshot.child("profileURL").getValue(String::class.java)

                    tvUserName.text = name ?: "No Name Found"
                    ivProfilePic.imageTintList = null // Clear tint to allow loaded image colors

                    // Load profile picture using Glide
                    Glide.with(requireContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.baseline_account_circle_24) // Default placeholder
                        .error(R.drawable.baseline_account_circle_24) // Fallback if image fails to load
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .circleCrop()
                        .into(ivProfilePic)
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!isAdded) {
                        Log.w("SettingsFragment", "Fragment not attached to context, ignoring error handling")
                        return
                    }
                    Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
            }

            // Attach the listener
            database.addListenerForSingleValueEvent(userListener)
        } else {
            tvUserName.text = "Guest"
        }

        // Handle Dark Mode Toggle Logic
        val mainActivity = activity as MainActivity

        // Set initial state of dark mode toggle based on current app theme
        darkModeSwitch.isChecked = mainActivity.isDarkModeEnabled()

        // Listen for toggle changes
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d("SettingsFragment", "Dark Mode toggled: $isChecked")
            mainActivity.toggleDarkMode(isChecked) // Trigger dark mode toggle in MainActivity
        }
    }

    // Cleanup: Remove Firebase listener when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        if (::userListener.isInitialized) {
            database.removeEventListener(userListener) // Detach the active Firebase listener
        }
    }
}