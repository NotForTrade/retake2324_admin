package com.example.retake2324

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Example profile data
        val profile = Profile(
            name = "John Doe",
            role = "Student",
            email = "john.doe@example.com",
            photoUrl = "" // Not used since we're loading from drawable
        )

        val textRole: TextView = findViewById(R.id.textRole)
        val textName: TextView = findViewById(R.id.textName)
        val textEmail: TextView = findViewById(R.id.textEmail)
        val imagePhoto: ImageView = findViewById(R.id.imagePhoto)

        textRole.text = profile.role
        textName.text = profile.name
        textEmail.text = profile.email

        // Load the profile image from a drawable resource
        imagePhoto.setImageResource(R.drawable.profile_photo)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.DashboardFragment -> {
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.ProfileFragment -> {val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.LogoutFragment -> {
                // Handle Logout navigation
                return true
            }
        }
        return false
    }
}