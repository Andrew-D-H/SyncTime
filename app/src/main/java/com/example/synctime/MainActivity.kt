package com.example.synctime

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.synctime.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        //configure google sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Get this from your Firebase project settings
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google Sign-In button click
        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        //Database testing
        val database = Firebase.database
        val myRef = database.getReference("message")
        myRef.setValue("Hello, World!")
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google Login Successful!", Toast.LENGTH_SHORT).show()

                    // Save credentials to database
                    val userId = auth.currentUser!!.uid
                    val databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    val userProfile = mapOf(
                        "name" to auth.currentUser!!.displayName,
                        "email" to auth.currentUser!!.email,
                        "profileURL" to (auth.currentUser!!.photoUrl?.toString() ?: "https://share.google/images/iLS3QJscaybUFYIRD")
                        // Add any other relevant user data here
                    )
                    // save profile data
                    databaseReference.child(userId).setValue(userProfile)
                        .addOnSuccessListener {
                            Toast.makeText(this, "User profile saved to DB!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e -> // Added 'e' for the exception
                            Toast.makeText(this, "Failed to save user profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    // You can go to your next activity here

                } else {
                    Toast.makeText(this, "Google Login Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }

}

