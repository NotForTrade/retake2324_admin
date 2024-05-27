package com.example.retake2324


import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.retake2324.core.App


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if user is logged in
        // val isLoggedIn = checkLoginStatus()

        // val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity

        setContent {
            App()
        }

    }

    private fun checkLoginStatus(): Boolean {
        // Replace with actual login status check logic
        // For example, checking SharedPreferences or a database
        return false // Default to false for this example
    }
}

