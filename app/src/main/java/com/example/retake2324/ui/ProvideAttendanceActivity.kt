package com.example.retake2324.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.retake2324.core.App
import com.example.retake2324.data.Attendance
import com.example.retake2324.data.Component
import com.example.retake2324.data.Group
import com.example.retake2324.data.Schemas
import com.example.retake2324.data.TutorMapping
import com.example.retake2324.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.entity.add
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.update


class ProvideAttendanceActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App

        val tutorId = intent.getIntExtra("tutorId", -1)

        setContent {
            MaterialTheme {
                ProvideAttendanceLoader(app, tutorId)
            }
        }
    }



    private suspend fun fetchObjects(
        database: Database,
        tutorId: Int
    ): Triple<User?, List<Component>, List<Attendance>> {

        try {
            val tutor = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Users).find { it.id eq tutorId }
            }
            if (tutor == null) {
                Log.e("TUTOR NOT FOUND", "TUTOR NOT FOUND IN DATABASE!")
                return Triple(null, emptyList(), emptyList())
            } else if (tutor.role.name == "Student") {
                Log.e("STUDENT FOUND", "A STUDENT IS MATCHING THE ID")
                return Triple(tutor, emptyList(), emptyList())
            } else { // Tutor role is valid
                val componentGroupPairs: List<TutorMapping>
                if (tutor.role.name == "Tutor") {
                    // Fetch all the students from the user's group
                    componentGroupPairs = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.TutorMappings).filter { it.tutorId eq tutorId }
                            .toList()
                    }
                } else { // Higher privilege has no filter restriction
                    componentGroupPairs = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.TutorMappings).toList()
                    }
                }

                if (componentGroupPairs.isNotEmpty()) {

                    val components = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.Components).toList()
                    }
                    // Fetch the student role object
                    val studentRole = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.Roles).find { it.name eq "Student" }
                    }
                    // Fetch the list of students
                    val students = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.Users).filter { it.roleId eq studentRole!!.id }
                            .toList()
                    }
                    val groups = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.Groups).toList()
                    }
                    val attendances = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.Attendances).toList()
                    }


                    if (components.isNotEmpty()) {

                        // Collect all the components the tutor is assigned to
                        val assignedComponents: MutableList<Component> = mutableListOf(Component())
                        componentGroupPairs.forEach { pair ->
                            components.find { it.name == pair.component.name }
                                ?.let { assignedComponents.add(it) }
                        }

                        if (assignedComponents.isNotEmpty()) {

                            // Attribute all the students to their group
                            groups.forEach { group ->
                                group.students = students.filter { it.group.id == group.id }
                            }

                            // Attribute all the groups to the components

                            assignedComponents.forEach { assignedComponent ->
                                val assignedGroupToAssignedComponent: MutableList<Group> =
                                    mutableListOf(Group())
                                componentGroupPairs.forEach { pair ->
                                    if (pair.component.id == assignedComponent.id) {
                                        assignedGroupToAssignedComponent.add(pair.group)
                                    }
                                }
                                assignedComponent.groups = assignedGroupToAssignedComponent
                            }
                            return Triple(tutor, assignedComponents, attendances)

                        } else {
                            Log.e("NO ASSIGNED COMPONENT", "TUTOR MANAGES NO COMPONENT")
                            return Triple(tutor, emptyList(), emptyList())
                        }

                    } else {
                        Log.e("NO COMPONENT", "NO COMPONENT FOUND IN DATABASE!")
                        return Triple(tutor, emptyList(), emptyList())
                    }
                } else { // tutor_mapping is empty
                    Log.e("NO MAPPING", "NO TUTOR MAPPING ENTRY")
                    return Triple(null, emptyList(), emptyList())
                }
            }
        } catch (e: Exception) {
            Log.e("SQL FETCHING ERROR", e.toString())
            return Triple(null, emptyList(), emptyList())
        }
    }


    @Composable
    fun ProvideAttendanceLoader(app: App, tutorId: Int) {
        // MutableState to hold the lists
        var tutor: User? by remember { mutableStateOf(User()) }
        var components by remember { mutableStateOf<List<Component>>(emptyList()) }
        var attendances by remember { mutableStateOf<List<Attendance>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val database = app.getDatabase() // Reuse the existing database connection
            val (fetchedTutor, fetchedComponents, fetchedAttendances) = fetchObjects(
                database,
                tutorId
            )

            // Update the states
            tutor = fetchedTutor
            components = fetchedComponents
            attendances = fetchedAttendances
            isLoading = false
        }

        if (isLoading) {
            Text(text = "Loading...", modifier = Modifier.padding(16.dp))
        } else if (tutor == null) {
            Text(text = "Either an error occurred while fetching the database, or an imperative entry is missing from the database")
        } else if (tutor!!.role.name == "Student") {
            Text(text = "The id matches a student role. Only an administration member can access this page!")
        } else {
            ProvideAttendanceScreen(app, tutor!!, components, attendances)
        }
    }

    private fun Database.attendances() = this.sequenceOf(Schemas.Attendances)

    @Composable
    fun ProvideAttendanceScreen(
        app: App,
        tutor: User,
        components: List<Component>,
        attendances: List<Attendance>
    ) {
        val context = LocalContext.current

        // Number of sessions per component
        val sessions = 1..8

        // Attendances values
        val attendanceValues = listOf("Present", "Absent without proof", "Absent with proof", "Late")

        // Components' box variables
        var selectedComponent: Component? by remember { mutableStateOf(null) }
        var componentsBoxExpanded by remember { mutableStateOf(false) }

        // Groups' box variables
        var selectedGroup: Group? by remember { mutableStateOf(null) }
        var groupsBoxExpanded by remember { mutableStateOf(false) }

        // Sessions' box variables
        var selectedSession: Int? by remember { mutableStateOf(null) }
        var sessionsBoxExpanded by remember { mutableStateOf(false) }

        // Attendances' boxes variables
        var selectedAttendanceValues: MutableList<String?> by remember { mutableStateOf(mutableListOf()) }
        var attendanceValuesBoxExpanded: MutableList<Boolean> by remember { mutableStateOf(mutableListOf()) }

        var isLoading by remember { mutableStateOf(false) }

        var showDialog by remember { mutableStateOf(false) }
        var dialogMessage by remember { mutableStateOf("") }



        LaunchedEffect(isLoading) {
            if (isLoading) {

                try {

                    withContext(Dispatchers.IO) {
                        val database = app.getDatabase()

                        selectedGroup!!.students.forEachIndexed { index, student ->

                            if (selectedAttendanceValues[index] != null) { // Don't override attendance objects that the user don't want to update

                                // Check if an attendance already exists for the triple<student, component, session>
                                val existingAttendance = attendances
                                    .filter { it.student == student }
                                    .filter { it.component == selectedComponent }
                                    .find { it.session == selectedSession }

                                if (existingAttendance != null) {
                                    existingAttendance.value = selectedAttendanceValues[index]!!
                                    existingAttendance.tutor = tutor!!
                                    database.attendances().update(existingAttendance)
                                } else {
                                    val newAttendance = Attendance {
                                        this.tutor = tutor!!
                                        this.student = student
                                        this.component = selectedComponent!!
                                        this.value = selectedAttendanceValues[index]!!
                                        this.session = selectedSession!!
                                    }
                                    database.attendances().add(newAttendance)
                                }
                            }



                            /*
                            database.insert(Schemas.Attendances) {
                                set(it.tutorId, tutor!!.id)
                                set(it.studentId, student.id)
                                set(it.componentId, selectedComponent.id)
                                set(it.value, selectedAttendanceValues[index])
                                set(it.session, selectedSession)
                            }
                            */




                        }
                    }

                    dialogMessage = "Attendances updated!"
                    showDialog = true

                } catch (e: Exception) {
                    Log.e("UPDATE VALUES", "Error: ${e.message}")
                    isLoading = false
                    dialogMessage = "An error occurred: ${e.message}"
                    showDialog = true
                }
            }

        }



        Scaffold(
            topBar = { Header("Request Reassessment", app) },
            bottomBar = { Footer(tutor!!.id) }
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {

                // Components' DropDownBox
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { componentsBoxExpanded = !componentsBoxExpanded }
                        .border(1.dp, Color.Gray)
                        .padding(16.dp)
                ) {
                    Text(text = selectedComponent?.name ?: "Select a Component")

                    DropdownMenu(
                        expanded = componentsBoxExpanded,
                        onDismissRequest = { componentsBoxExpanded = false },
                    ) {
                        components.forEach { component ->
                            DropdownMenuItem(
                                { Text(text = component.name) },
                                onClick = {
                                    selectedComponent = component
                                    componentsBoxExpanded = false
                                    selectedGroup = null


                                    Log.d("COMPONENTS", components.toString())
                                    Log.d("TUTOR", tutor.toString())
                                    Log.d("ATTENDANCES", attendances.toString())



                                }
                            )
                        }
                    }
                }

                // Groups' DropDownBox
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(selectedComponent != null) {
                            groupsBoxExpanded = !groupsBoxExpanded
                        }
                        .border(
                            1.dp,
                            if (selectedComponent != null) Color.Gray else Color.LightGray
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = selectedGroup?.name
                            ?: if (selectedComponent == null) "Select a Component first" else "Select a Group"
                    )

                    DropdownMenu(
                        expanded = groupsBoxExpanded,
                        onDismissRequest = { groupsBoxExpanded = false }
                    ) {
                        selectedComponent?.groups?.forEach { group ->
                            DropdownMenuItem(
                                { Text(text = group.name) },
                                onClick = {
                                    selectedGroup = group
                                    groupsBoxExpanded = false
                                    selectedAttendanceValues = MutableList(group.students.size) {null as String}
                                    attendanceValuesBoxExpanded = MutableList(group.students.size) {null as Boolean}
                                }
                            )
                        }
                    }
                }


                // Sessions' DropDownBox
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Color.LightGray
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (selectedSession != null) "Session: ${selectedSession.toString()}" else "Select a Group"
                    )
                    DropdownMenu(
                        expanded = sessionsBoxExpanded,
                        onDismissRequest = { sessionsBoxExpanded = false }
                    ) {
                        sessions.forEach { session ->
                            DropdownMenuItem(
                                { Text(text = "Session: ${session}") },
                                onClick = {
                                    selectedSession = session
                                    sessionsBoxExpanded = false
                                }
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.padding(bottom = 16.dp))


                // Rows for each student and each attendance
                if (selectedComponent != null &&
                    selectedGroup != null &&
                    selectedSession != null) {

                    selectedGroup!!.students.forEachIndexed { index, student ->

                        Row(

                        ){

                            Text( text = "${student.firstName} ${student.lastName}")

                            Text( text = "Attendance:")

                            // Attendance' DropDownBox
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { attendanceValuesBoxExpanded[index] = !attendanceValuesBoxExpanded[index] }
                                    .border(1.dp, Color.Gray)
                                    .padding(16.dp)
                            ) {
                                Text(text = selectedAttendanceValues[index] ?: "Select a Component")

                                DropdownMenu(
                                    expanded = attendanceValuesBoxExpanded[index],
                                    onDismissRequest = { attendanceValuesBoxExpanded[index] = false },
                                ) {
                                    attendanceValues.forEach { value ->
                                        DropdownMenuItem(
                                            { Text(text = value) },
                                            onClick = {
                                                selectedAttendanceValues[index] = value
                                                attendanceValuesBoxExpanded[index] = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Show Dialog if needed
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Submission Status") },
                        text = { Text(dialogMessage) },
                        confirmButton = {
                            Button(
                                onClick = { showDialog = false }
                            ) {
                                Text("Ok")
                            }
                        }
                    )
                }




            }
        }
    }
}