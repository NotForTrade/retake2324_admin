package com.example.retake2324.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.retake2324.core.App
import com.example.retake2324.data.Attendance
import com.example.retake2324.data.Component
import com.example.retake2324.data.Group
import com.example.retake2324.data.Schemas
import com.example.retake2324.data.TutorMapping
import com.example.retake2324.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.entity.sequenceOf


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


    private suspend fun fetchObjects(database: Database, tutorId: Int): Triple<User?, List<Component>, List<Attendance>> {

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
            } else {

                val componentGroupPairs: List<TutorMapping>
                if (tutor.role.name == "Tutor") {
                    // Fetch all the students from the user's group
                    componentGroupPairs = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.TutorMappings).filter { it.tutorId eq tutorId }.toList()
                    }
                } else { // Higher privilege has no filter restriction
                    componentGroupPairs = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.TutorMappings).toList()
                    }
                }

                val components = withContext(Dispatchers.IO) {
                    database.sequenceOf(Schemas.Components).toList()
                }
                // Fetch the student role object
                val studentRole = withContext(Dispatchers.IO) {
                    database.sequenceOf(Schemas.Roles).find { it.name eq "Student" }
                }
                // Fetch the list of students
                val students = withContext(Dispatchers.IO) {
                    database.sequenceOf(Schemas.Users).filter { it.roleId eq studentRole!!.id }.toList()
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
                        components.find {it.name == pair.component.name}
                            ?.let { assignedComponents.add(it) }
                    }

                    if (assignedComponents.isNotEmpty()) {

                        // Attribute all the students to their group
                        groups.forEach { group ->
                            group.students = students.filter { it.group.id == group.id }
                        }

                        // Attribute all the groups to the components

                        assignedComponents.forEach { assignedComponent ->
                            val assignedGroupToAssignedComponent: MutableList<Group> = mutableListOf(Group())
                            componentGroupPairs.forEach {pair ->
                                if (pair.component.id == assignedComponent.id) {
                                    assignedGroupToAssignedComponent.add(pair.group)
                                }
                            }
                            assignedComponent.groups = assignedGroupToAssignedComponent
                        }
                        return Triple(tutor, assignedComponents, attendances)

                    } else {
                        Log.i("NO ASSIGNED COMPONENT", "TUTOR MANAGES NO COMPONENT")
                        return Triple(tutor, emptyList(), emptyList())
                    }

                } else {
                    Log.e("NO COMPONENT", "NO COMPONENT FOUND IN DATABASE!")
                    return Triple(tutor, emptyList(), emptyList())
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
            val (fetchedTutor, fetchedComponents, fetchedAttendances) = fetchObjects(database, tutorId)

            // Update the states
            tutor = fetchedTutor
            components = fetchedComponents
            attendances = fetchedAttendances
            isLoading = false
        }

        if (isLoading) {
            Text(text = "Loading...", modifier = Modifier.padding(16.dp))
        } else {
            ProvideAttendanceScreen(app, tutor, components, attendances)
        }
    }

    @Composable
    fun ProvideAttendanceScreen(app: App, tutor: User?, components: List<Component>, attendances: List<Attendance>) {
        val context = LocalContext.current
        val expandedComponents = remember { mutableStateOf(components.map { it.id }.toSet()) }

        val columnWidths = listOf(200.dp) + listOf(100.dp)

        Scaffold(
            topBar = { Header("Personal Overview", app) },
            bottomBar = { Footer(tutor!!.id) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Outer Box with horizontal scrolling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Row for the group name and student name

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Group: ${student.group.name}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.width(columnWidths[0])
                            )
                            Text(
                                text = student.firstName + " " + student.lastName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.width(columnWidths[1])
                            )
                        }
                        // LazyColumn for components and skills
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(components) { component ->
                                val isExpanded = expandedComponents.value.contains(component.id)
                                // Component row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Component: ${component.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier
                                            .width(columnWidths[0])
                                            .clickable {
                                                expandedComponents.value = if (isExpanded) {
                                                    expandedComponents.value - component.id
                                                } else {
                                                    expandedComponents.value + component.id
                                                }
                                            }
                                    )
                                    val score = component.scores.find { it.student.id == student.id }?.value ?: 0.0
                                    Text(
                                        text = "$score",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.width(columnWidths[1])
                                    )
                                }

                                // Skill rows under each component
                                if (isExpanded) {
                                    component.skills.forEach { skill ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp, bottom = 2.dp)
                                        ) {
                                            Text(
                                                text = "Skill: ${skill.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier
                                                    .width(columnWidths[0])
                                                    .clickable {
                                                        val intent = Intent(
                                                            context,
                                                            SkillActivity::class.java
                                                        ).apply {
                                                            putExtra("studentId", student.id)
                                                            putExtra("skillId", skill.id)
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                            )
                                            val skillScore = skill.scores.find { it.student.id == student.id }?.value ?: 0.0
                                            Text(
                                                text = "$skillScore",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.width(columnWidths[1])
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
    }
}