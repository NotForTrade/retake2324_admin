package com.example.retake2324.core

import android.app.Application
import com.example.retake2324.data.connectDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.ktorm.database.Database

class App : Application() {

    private val _exceptionFlow = MutableStateFlow<Exception?>(null)
    val exceptionFlow = _exceptionFlow.asStateFlow()

    // connections
    private val _databaseDeferred = CompletableDeferred<Database>()
    val databaseDeferred = _databaseDeferred

    override fun onCreate() {
        super.onCreate()

        // Initialize notification channels here
        //val notificationHelper = NotificationHelper(this)
        //notificationHelper.createNotificationChannel()

        // Initialize the database connection lazily
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = connectDatabase()
                _databaseDeferred.complete(database)
            } catch (e: Exception) {
                displayException(e)
            }
        }
    }

    suspend fun getDatabase(): Database {
        return _databaseDeferred.await()
    }


    fun displayException(exception: Exception) {
        _exceptionFlow.value = exception
        exception.printStackTrace()
    }

}