package com.example.synctime

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class SettingsFragment : Fragment(R.layout.settings_screen) {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val ivProfilePic: ImageView = view.findViewById(R.id.ivProfilePic)
        val btnEditProfile = view.findViewById<TextView>(R.id.btnEditProfile)
        val btnManageFriends = view.findViewById<TextView>(R.id.btnManageFriends)

        btnEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        btnManageFriends.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FriendsFragment())
                .addToBackStack(null)
                .commit()
        }
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            database = FirebaseDatabase.getInstance().getReference("users").child(userId)

            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val photoUrl = snapshot.child("profileURL").getValue(String::class.java)

                    tvUserName.text = name ?: "No Name Found"

                    ivProfilePic.imageTintList = null // clear tint before loading from Glide

                    // Load profile picture using Glide
                    Glide.with(requireContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.baseline_account_circle_24) // existing drawable
                        .error(R.drawable.baseline_account_circle_24)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .circleCrop()
                        .into(ivProfilePic)

                    // DEBUGGING
                    Log.d("SettingsFragment", "photoUrl = $photoUrl")

                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        } else {
            // pass
        }
    }
}
