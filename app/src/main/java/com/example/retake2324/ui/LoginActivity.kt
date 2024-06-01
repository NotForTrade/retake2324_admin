package com.example.retake2324.ui


import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.retake2324.core.App
import com.example.retake2324.data.Schemas
import com.example.retake2324.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import java.security.MessageDigest

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App
        setContent {
            val signupEmail = intent.getStringExtra("email")
            LoginScreen(app, signupEmail)
        }
    }

    private fun hashPassword(password: String): String {

        // Create a MessageDigest instance for SHA-512
        val digest = MessageDigest.getInstance("SHA-512")

        // Apply the hash to the input password bytes
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))

        // Convert the hash bytes to a hexadecimal string
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    @Composable
    fun LoginScreen(app: App, signupEmail: String?) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var tutorId by remember { mutableIntStateOf(-1) }
        var tutor: User? by remember { mutableStateOf(null) }
        var success by remember { mutableStateOf(false) }

        var email by remember { mutableStateOf("") }
        if (signupEmail != null) {
            email = signupEmail
        }
        var password by remember { mutableStateOf("") }

        var isLoading by remember { mutableStateOf(false) }



        LaunchedEffect(isLoading) {
            if (isLoading) {

                try {
                    val hashedPassword = hashPassword(password)

                    withContext(Dispatchers.IO) {
                        val database = app.getDatabase()

                        // email is a unique key and used as "username"
                        tutor = database.sequenceOf(Schemas.Users).find { it.email eq email }
                    }


                    if (tutor != null) {
                        if (tutor!!.role.name != "Student") {
                            if (tutor!!.password == hashedPassword) {
                                tutorId = tutor!!.id
                                success = true
                            } else {
                                // Wrong password
                                isLoading = false
                            }
                        } else {
                            // User is student, not an admin member
                            isLoading = false
                        }
                    } else {
                        // User not found
                        isLoading = false
                    }
                } catch (e: Exception) {
                    Log.e("SUBMIT", "Error submitting file: ${e.message}")
                    isLoading = false
                }
            }
            if (success) {
                val intent = Intent(context, DashboardActivity::class.java)
                intent.putExtra("tutorId", tutorId)
                startActivity(intent)
                finish()
            }

        }




        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Login", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
                onClick = {
                    isLoading = true
                },
                enabled = (email.isNotEmpty() && password.isNotEmpty()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Login")
            }

            TextButton(
                onClick = {
                    val intent = Intent(this@LoginActivity, SignupActivity::class.java)
                    startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Signup")
            }
        }
    }
}