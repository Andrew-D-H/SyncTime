package com.example.synctime

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etProfileUrl = view.findViewById<EditText>(R.id.etProfileUrl)
        val btnSave = view.findViewById<Button>(R.id.btnSaveProfile)

        val uid = auth.currentUser?.uid ?: return

        // Load current values
        db.child("users").child(uid).get().addOnSuccessListener { snap ->
            etName.setText(snap.child("name").getValue(String::class.java) ?: "")
            etProfileUrl.setText(snap.child("profileURL").getValue(String::class.java) ?: "")
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val newUrl = etProfileUrl.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = hashMapOf<String, Any?>(
                "name" to newName,
                "profileURL" to (if (newUrl.isBlank()) null else newUrl)
            )

            db.child("users").child(uid).updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
