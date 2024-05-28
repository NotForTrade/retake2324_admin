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
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.entity.sequenceOf


class ConsultNotesActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App

        val tutorId = intent.getIntExtra("tutorId", 12)

        setContent {
            MaterialTheme {
                ConsultNotesLoader(app, tutorId)
            }
        }
    }


    private suspend fun fetchObjects(database: Database, tutorId: Int): Pair<User?, List<Component>> {

        try {

            // get the tutor
            val tutor = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Users).find { it.id eq tutorId }
            }
            if (tutor == null) {
                Log.e("TUTOR NOT FOUND", "TUTOR NOT FOUND IN DATABASE!")
                return Pair(null, emptyList())

            } else if (tutor.role.name == "Student") {
                Log.e("STUDENT FOUND", "A STUDENT IS MATCHING THE ID")
                return Pair(null, emptyList())
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
                    val skills = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.Skills).toList()
                    }
                    val scores = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.Scores).toList()
                    }
                    val groupObservations = withContext(Dispatchers.IO) {
                        database.sequenceOf(Schemas.GroupObservations).toList()
                    }
                    if (components.isNotEmpty()) {

                        // Collect all the components the tutor is assigned to
                        val assignedComponents: MutableList<Component> = mutableListOf()
                        componentGroupPairs.forEach { pair ->
                            // Avoid retrieving duplicates
                            if (assignedComponents.find { it.id == pair.component.id } == null) {
                                components.find { it.name == pair.component.name }
                                    ?.let { assignedComponents.add(it) }
                            }
                        }
                        if (assignedComponents.isNotEmpty()) {

                            // Attribute all the groups to the components
                            assignedComponents.forEach { assignedComponent ->
                                val assignedGroupToAssignedComponent: MutableList<Group> =
                                    mutableListOf()
                                componentGroupPairs.forEach { pair ->
                                    // Avoid duplicate groups if there are duplicate entries in the database
                                    if ((assignedGroupToAssignedComponent.find { it.id == pair.group.id } == null) and (pair.component.id == assignedComponent.id)) {
                                        // Assign the student list to the the group
                                        pair.group.students =
                                            students.filter { it.group.id == pair.group.id }
                                        // add the group to the mutable list
                                        assignedGroupToAssignedComponent.add(pair.group)
                                    }
                                }
                                assignedComponent.groups = assignedGroupToAssignedComponent

                                // Assign the skills to the components
                                assignedComponent.skills =
                                    skills.filter { it.component.id == assignedComponent.id }
                                        .toList()

                                // Assign the scores and the group observations to each skill
                                assignedComponent.skills.forEach { skill ->
                                    skill.scores = scores.filter { it.skill.id == skill.id }
                                    skill.groupObservations =
                                        groupObservations.filter { it.skill.id == skill.id }
                                }
                            }
                            return Pair(tutor, assignedComponents)
                        } else {
                            Log.e("NO ASSIGNED COMPONENT", "TUTOR MANAGES NO COMPONENT")
                            return Pair(null, listOf())
                        }
                    } else {
                        Log.e("NO COMPONENT", "NO COMPONENT FOUND IN DATABASE!")
                        return Pair(null, listOf())
                    }
                } else { // tutor_mapping is empty
                    Log.e("NO MAPPING", "NO TUTOR MAPPING ENTRY")
                    return Pair(null, listOf())
                }
            }
        } catch (e: Exception) {
            Log.e("SQL FETCHING ERROR", e.toString())
            return Pair(null, listOf())
        }
    }



    @Composable
    fun ConsultNotesLoader(app: App, tutorId: Int) {
        // MutableState to hold the lists
        var tutor: User? by remember { mutableStateOf(null) }
        var components by remember { mutableStateOf(listOf<Component>()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val database = app.getDatabase() // Reuse the existing database connection
            val (fetchedTutor, fetchedComponents) = fetchObjects(database, tutorId)

            // Update the states
            tutor = fetchedTutor
            components = fetchedComponents
            isLoading = false
        }

        if (isLoading) {
            Text(text = "Loading...", modifier = Modifier.padding(16.dp))
        } else {
            ConsultNotesScreen(app, tutor!!, components)
        }
    }

    @Composable
    fun ConsultNotesScreen(app: App, tutor: User, components: List<Component>) {
        val context = LocalContext.current
        val expandedComponents = remember { mutableStateOf(components.map { it.id }.toSet()) }

        val columnWidths = listOf(200.dp) + List(students.size) { 100.dp }

        Scaffold(
            topBar = { Header("Group Overview", app) },
            bottomBar = { Footer(studentId) }
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
                        // Row for the group name and student names
                        if (students.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "Group: ${students[0].group.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.width(columnWidths[0])
                                )
                                students.forEachIndexed { index, student ->
                                    Text(
                                        text = student.firstName + " " + student.lastName,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.width(columnWidths[index + 1])
                                    )
                                }
                            }
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
                                    students.forEachIndexed { index, student ->
                                        val score = component.scores.find { it.student.id == student.id }?.value ?: 0.0
                                        Text(
                                            text = "$score",
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.width(columnWidths[index + 1])
                                        )
                                    }
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
                                                            putExtra("studentId", studentId)
                                                            putExtra("skillId", skill.id)
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                            )
                                            students.forEachIndexed { index, student ->
                                                val skillScore = skill.scores.find { it.student.id == student.id }?.value ?: 0.0
                                                Text(
                                                    text = "$skillScore",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.width(columnWidths[index + 1])
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
}