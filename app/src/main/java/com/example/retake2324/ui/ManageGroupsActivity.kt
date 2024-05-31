package com.example.retake2324.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.retake2324.core.App
import com.example.retake2324.data.Component
import com.example.retake2324.data.Group
import com.example.retake2324.data.Schemas
import com.example.retake2324.data.Score
import com.example.retake2324.data.TutorMapping
import com.example.retake2324.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.isNotNull
import org.ktorm.entity.add
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.update


class ManageGroupsActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App

        val tutorId = intent.getIntExtra("tutorId", 12)

        setContent {
            MaterialTheme {
                ManageGroupsLoader(app, tutorId)
            }
        }
    }


    private suspend fun fetchObjects(database: Database, tutorId: Int): Triple<User?, List<Group>, List<User>> {

        try {

            // get the tutor
            val tutor = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Users).find { it.id eq tutorId }
            }
            if (tutor == null) {
                Log.e("TUTOR NOT FOUND", "TUTOR NOT FOUND IN DATABASE!")
                return Triple(null, emptyList(), emptyList())

            } else if (tutor.role.name == "Student") {
                Log.e("STUDENT FOUND", "A STUDENT IS MATCHING THE ID")
                return Triple(tutor, emptyList(), emptyList())
            }
            Log.d("TUTOR FOUND", "###########################################")


            val users = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Users).toList()
            }

            val students = users.filter { it.role.name == "Student" }.toMutableList()

            val groups = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Groups).toList()
            }
            if (groups.isEmpty()) {
                return Triple(tutor, emptyList(), students)
            }

            groups.forEach { group ->
                val thisGroupStudents = students.filter { it.group?.id == group.id }.toList()
                students.removeAll(thisGroupStudents)
                group.students = thisGroupStudents
            }

            // Residual students either have no group or a deleted group
            val studentsWithoutGroup = students.toList()

            return Triple(tutor, groups, studentsWithoutGroup)

        } catch (e: Exception) {
            Log.e("SQL FETCHING ERROR", e.toString())
            return Triple(null, emptyList(), emptyList())
        }
    }



    @Composable
    fun ManageGroupsLoader(app: App, tutorId: Int) {
        // MutableState to hold the lists
        var tutor: User? by remember { mutableStateOf(null) }
        var groups by remember { mutableStateOf(listOf<Group>()) }
        var studentsWithoutGroups by remember { mutableStateOf(listOf<User>()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val database = app.getDatabase() // Reuse the existing database connection
            val (fetchedTutor, fetchedGroups, fetchedStudents) = fetchObjects(database, tutorId)

            // Update the states
            tutor = fetchedTutor
            groups = fetchedGroups
            studentsWithoutGroups = fetchedStudents
            isLoading = false
        }

        if (isLoading) {
            Text(text = "Loading...", modifier = Modifier.padding(16.dp))
        } else if (tutor == null) {
            Text(text = "TUTOR NOT FOUND", modifier = Modifier.padding(16.dp))
        } else if (tutor!!.role.name == "Student") {
            Text(text = "Tutor has student role!", modifier = Modifier.padding(16.dp))
        } else {
            ManageGroupsScreen(app, tutor!!, groups, studentsWithoutGroups)
        }
    }




    @SuppressLint("MutableCollectionMutableState")
    @Composable
    fun ManageGroupsScreen(app: App, tutor: User, groups: List<Group>, studentsWithoutGroup: List<User>) {
        val context = LocalContext.current

        var groupName by remember { mutableStateOf("") }
        val customerList = listOf("Tony Stark", "Jeff Bezos", "Elon Musk", "Albert Einstein")


        var createGroup by remember { mutableStateOf(false) }
        var editGroup by remember { mutableStateOf(false) }
        var deleteGroup by remember { mutableStateOf(false) }
        var editStudent by remember { mutableStateOf(false) }

        var selectedGroup by remember { mutableStateOf<Group?>(null) }
        var selectedCustomer by remember { mutableStateOf<String?>(null) }
        var selectedStudent by remember { mutableStateOf<User?>(null) }


        var showEditStudentDialog by remember { mutableStateOf(false) }

        var showAddGroupDialog by remember { mutableStateOf(false) }
        var showEditGroupDialog by remember { mutableStateOf(false) }
        var showDeleteGroupDialog by remember { mutableStateOf(false) }

        var expandedCustomer by remember { mutableStateOf(false) }


        Scaffold(
            topBar = { Header("Components Overview", app) },
            bottomBar = { Footer(tutor.id) }
        ) { innerPadding ->
            Box (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {


                LaunchedEffect(createGroup) {

                    if (createGroup) {
                        try {

                            val database = app.getDatabase()
                            withContext(Dispatchers.IO) {
                                database.sequenceOf(Schemas.Groups).add(Group{
                                    this.name = groupName
                                    this.customer = selectedCustomer
                                })

                            }
                            Log.d("ADD GROUP SUCCESS", "##################################")

                            // Reload the activity
                            val intent = Intent(context, ManageGroupsActivity::class.java)
                            intent.putExtra("tutorId", tutor.id)
                            startActivity(intent)

                            createGroup = false
                        }catch (e: Exception){
                            Log.e("ADD COMPONENT ERROR", e.toString())
                            createGroup = false
                        }

                    }

                }


                // Dialog to add a group
                if (showAddGroupDialog) {
                    groupName = ""
                    AlertDialog(
                        onDismissRequest = { showAddGroupDialog = false },
                        confirmButton = {
                            TextButton(
                                modifier = Modifier.clickable { groupName != ""},
                                onClick = {
                                createGroup = true
                                showAddGroupDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAddGroupDialog = false
                            }) {
                                Text("Dismiss")
                            }
                        },
                        text = {

                            Column {
                                TextField(
                                    value = groupName,
                                    onValueChange = {
                                        groupName = it
                                    },
                                    label = { Text("Component name") }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(text = "Customer: ")
                                Box {
                                    Text(
                                        text = selectedCustomer ?: "Select a customer",
                                        modifier = Modifier
                                            .clickable { expandedCustomer = true }
                                            .padding(8.dp),
                                        color = Color.Black
                                    )
                                    DropdownMenu(
                                        expanded = expandedCustomer,
                                        onDismissRequest = { expandedCustomer = false }
                                    ) {
                                        customerList.forEach { customer ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = customer,
                                                        color = Color.Black
                                                    )
                                                },
                                                onClick = {
                                                    selectedCustomer = customer
                                                    expandedCustomer = false
                                                }

                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }





                LazyColumn(modifier = Modifier.padding(16.dp)) {

                    if (groups.isNotEmpty()){
                        groups.forEach { group ->
                            item {
                                var isExpanded by remember { mutableStateOf(true) }
                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = group.name,
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        IconButton(onClick = {
                                            showEditGroupDialog = true
                                            selectedGroup = group
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Group"
                                            )
                                        }
                                        IconButton(onClick = { isExpanded = !isExpanded }) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowLeft,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Customer: ${ if (group.customer == null) "-" else group.customer!!}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (isExpanded) {

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Column {
                                            group.students.forEach { student ->
                                                Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
                                                    Text(
                                                        text = "${student.firstName} ${student.lastName}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier
                                                            .padding(horizontal = 4.dp)
                                                            .clickable {
                                                                // todo navigateToProfile
                                                            }
                                                    )
                                                    IconButton(onClick = {
                                                        showEditStudentDialog = true
                                                        selectedStudent = student
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Student"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        var isExpanded by remember { mutableStateOf(true) }
                       Column {
                           Row(
                               modifier = Modifier
                                   .fillMaxWidth(),
                               horizontalArrangement = Arrangement.SpaceBetween
                           ) {
                                Text(
                                    text = "Not in a group!",
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                IconButton(onClick = { isExpanded = !isExpanded }) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowLeft,
                                        contentDescription = null
                                    )
                                }
                           }
                           studentsWithoutGroup.forEach { student ->
                                Row(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        top = 4.dp,
                                        bottom = 4.dp
                                    )
                                ) {
                                    Text(
                                        text = "${student.firstName} ${student.lastName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clickable {
                                                // todo navigateToProfile
                                            }
                                    )
                                    IconButton(onClick = {
                                        showEditStudentDialog = true
                                        selectedStudent = student
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Pair"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showAddGroupDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create new group")
                        }
                    }
                }
            }
        }
    }






}