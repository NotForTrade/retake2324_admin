package com.example.retake2324.ui

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.retake2324.core.App
import com.example.retake2324.data.Announcement
import com.example.retake2324.data.Schemas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList


private suspend fun fetchObjects(database: Database): List<Announcement> {
    try {
        // Fetch the announcements
        val announcements = withContext(Dispatchers.IO) {
            database.sequenceOf(Schemas.Announcements).toList()
        }
        return announcements
    } catch (e: Exception) {
        Log.e("SQL FETCHING ERROR", e.toString())
        return listOf()
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(activityName: String, app: App) {

    val context = LocalContext.current

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "APP² - Admin",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, end = 8.dp),
                )
                Text(
                    text = activityName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        },
        actions = {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ){
                IconButton(onClick = {
                    /*
                    val intent = Intent(context, AnnouncementsActivity::class.java)
                    context.startActivity(intent)
                    */
                    },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Announcements",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.height(80.dp)
    )
}
