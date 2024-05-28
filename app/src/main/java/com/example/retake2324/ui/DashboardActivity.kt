package com.example.retake2324.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.retake2324.core.App


class DashboardActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App

        setContent {
            val tutorId = intent.getIntExtra("studentId", 12)
            DashboardScreen(app, tutorId)
        }
    }

    @Composable
    fun DashboardScreen(app: App, tutorId: Int) {
        val context = LocalContext.current

        Scaffold(
            topBar = { Header("Dashboard", app) },
            bottomBar = { Footer(tutorId) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // View Announcements Button


                    Button(
                        onClick = {
                            val intent = Intent(context, ProvideAttendanceActivity::class.java)
                            intent.putExtra("tutorId", tutorId)
                            startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Announcements")
                    }

                    Spacer(modifier = Modifier.height(16.dp))


/*
                    // Frame for personal and group buttons
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = {
                                    val intent = Intent(this@DashboardActivity, PersonalOverviewActivity::class.java)
                                    intent.putExtra("studentId", studentId)
                                    startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Personal Overview")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val intent = Intent(this@DashboardActivity, PersonalSynthesisActivity::class.java)
                                    intent.putExtra("studentId", studentId)
                                    startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Personal Synthesis")
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = {
                                    val intent = Intent(this@DashboardActivity, GroupOverviewActivity::class.java)
                                    intent.putExtra("studentId", studentId)
                                    startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Group Overview")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val intent = Intent(this@DashboardActivity, GroupSynthesisActivity::class.java)
                                    intent.putExtra("studentId", studentId)
                                    startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Group Synthesis")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Request Reassessment button under the frame
                    Button(
                        onClick = {
                            val intent = Intent(this@DashboardActivity, RequestReassessmentActivity::class.java)
                            intent.putExtra("studentId", studentId)
                            startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request Reassessment")
                    }

                     */
                }
            }
        }
    }
}
