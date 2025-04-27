package com.example.synctime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.synctime.ui.theme.SyncTimeTheme
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.synctime.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var username: EditText
    lateinit var password: EditText
    lateinit var loginButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            binding.loginButton.setOnClickListener(View.OnClickListener {
                if (binding.username.text.toString() == "SyncTimeUser" && binding.password.text.toString() == "eta") {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Login Failed!", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

