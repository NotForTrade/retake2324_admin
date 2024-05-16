package com.example.retake2324

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView



class MainActivity : AppCompatActivity() {
    private lateinit var componentsAdapter: ComponentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Check if user is logged in
        val isLoggedIn = checkLoginStatus()

//        if (!isLoggedIn) {
        if (false) {
            // Redirect to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close MainActivity
        } else {

            // Replace this with your actual data for components
            val components = listOf(
                Component("Component 1", listOf("Skill A", "Skill B")),
                Component("Component 2", listOf("Skill C", "Skill D", "Skill E")),
                Component("Component 3", listOf("Skill F"))
            )

            componentsAdapter = ComponentsAdapter(components)
        }
    }

    private fun checkLoginStatus(): Boolean {
        // Replace with actual login status check logic
        // For example, checking SharedPreferences or a database
        return false // Default to false for this example
    }

}

