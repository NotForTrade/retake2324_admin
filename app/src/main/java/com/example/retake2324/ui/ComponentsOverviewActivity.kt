package com.example.retake2324.ui

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.retake2324.core.App
import com.example.retake2324.data.Component
import com.example.retake2324.data.Group
import com.example.retake2324.data.Schemas
import com.example.retake2324.data.TutorMapping
import com.example.retake2324.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.isNotNull
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.entity.sequenceOf


class ComponentsOverviewActivity : ComponentActivity() {


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
                        database.sequenceOf(Schemas.Users)
                            .filter { it.roleId eq studentRole!!.id }
                            .filter { it.groupId.isNotNull()}
                            .toList()
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
                                        pair.group.students = students.filter { it.group!!.id == pair.group.id }
                                        // add the group to the mutable list
                                        assignedGroupToAssignedComponent.add(pair.group)
                                    }
                                }
                                assignedComponent.groups = assignedGroupToAssignedComponent
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

                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    components.forEach { component ->
                        item {
                            var isExpanded by remember { mutableStateOf(true) }
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            enabled = tutor.role.name != "Tutor",
                                            onClick = {
                                                if (tutor.role.name != "Tutor") {

                                                // todo    navigateToManageComponent(context, tutorId, component.name)

                                                }
                                            }
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = component.name,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    IconButton(onClick = { isExpanded = !isExpanded }) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                }
                                if (isExpanded) {
                                    Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
                                        Text(
                                            text = "Skills: ",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        component.skills.forEach { skill ->
                                            Text(
                                                text = skill.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .clickable {
                                                        // todo navigateToSkill(context, tutorId, skill.id)
                                                    }
                                            )
                                        }
                                    }
                                    component.groups.forEach { group ->
                                        Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = group.name,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                var expanded by remember { mutableStateOf(false) }
                                                Box {
                                                    IconButton(onClick = { expanded = true }) {
                                                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More actions")
                                                    }
                                                    DropdownMenu(
                                                        expanded = expanded,
                                                        onDismissRequest = { expanded = false },
                                                        offset = DpOffset(x = 0.dp, y = 0.dp),
                                                        properties = PopupProperties(focusable = true)
                                                    ) {
                                                        DropdownMenuItem(
                                                            onClick = {
                                                                expanded = false
                                                                val intent = Intent(context, GroupOverviewActivity::class.java)
                                                                intent.putExtra("tutorId", tutor.id)
                                                                intent.putExtra("groupId", group.id)
                                                                startActivity(intent)
                                                            },
                                                            text = { Text("View Group Overview") }
                                                        )
                                                        DropdownMenuItem(
                                                            onClick = {
                                                                // todo navigateToGroupSynthesis(context, tutorId, group.name)
                                                                expanded = false
                                                            },
                                                            text = { Text("View Group Synthesis") }
                                                        )
                                                        DropdownMenuItem(
                                                            onClick = {
                                                                // todo navigateToManageGroup(context, tutorId, group.name)
                                                                expanded = false
                                                            },
                                                            text = { Text("Manage Group") }
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))
                                            group.students.forEach { student ->
                                                Text(
                                                    text = "${student.firstName} ${student.lastName}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier
                                                        .padding(start = 32.dp)
                                                        .clickable {
                                                            // todo navigateToProfile(context, tutorId, student.name)
                                                        }
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