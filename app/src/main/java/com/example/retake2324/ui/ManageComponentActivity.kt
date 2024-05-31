package com.example.retake2324.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.retake2324.core.App
import com.example.retake2324.data.Component
import com.example.retake2324.data.Group
import com.example.retake2324.data.Schemas
import com.example.retake2324.data.Skill
import com.example.retake2324.data.TutorMapping
import com.example.retake2324.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.removeIf
import org.ktorm.entity.toList
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.update


class ManageComponentActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App

        val tutorId = intent.getIntExtra("tutorId", 12)
        val componentId = intent.getIntExtra("componentId", -1)

        setContent {
            MaterialTheme {
                ManageComponentLoader(app, tutorId, componentId)
            }
        }
    }


    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )


    private suspend fun fetchObjects(
        database: Database,
        tutorId: Int,
        componentId: Int
    ): Quadruple<User?, Component?, List<Group>, List<User>> {

        try {

            // Fetch the tutor
            val tutor = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Users).find { it.id eq tutorId }
            }
            if (tutor == null) {
                Log.e("TUTOR NOT FOUND", "TUTOR NOT FOUND IN DATABASE!")
                return Quadruple(null, null, emptyList(), emptyList())
            } else if (tutor.role.name == "Student") {
                Log.e("STUDENT FOUND", "A STUDENT IS MATCHING THE ID")
                return Quadruple(null, null, emptyList(), emptyList())
            }
            else if (tutor.role.name == "Tutor") {
                Log.e("TUTOR FOUND", "A TUTOR IS MATCHING THE ID")
                return Quadruple(tutor, null, emptyList(), emptyList())
            }

            // Fetch the component
            val component = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Components).find { it.id eq componentId }
            }
            if (component == null) {
                Log.e("COMPONENT NOT FOUND", "#####################################")
                return Quadruple(tutor, null, emptyList(), emptyList())
            }

            // Fetch the skills and assign them to the component
            component.skills = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Skills).filter { it.componentId eq component.id }
                    .toList()
            }

            // Fetch the pairs for the component
            component.pairs = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.TutorMappings).filter { it.componentId eq component.id }
                    .toList()
            }

            // Fetch all the groups
            val groups = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Groups).toList()
            }

            // Fetch the tutor role
            val tutorRole = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Roles).find { it.name eq "Tutor" }
            }
            var tutors: List<User> = listOf()
            if (tutorRole != null) {
                tutors = withContext(Dispatchers.IO) {
                    database.sequenceOf(Schemas.Users).filter { it.roleId eq tutorRole.id}.toList()
                }
            }


            return Quadruple(tutor, component, groups, tutors)
        } catch (e: Exception) {
            Log.e("SQL FETCHING ERROR", e.toString())
            return Quadruple(null, null, emptyList(), emptyList())
        }
    }


    @Composable
    fun ManageComponentLoader(app: App, tutorId: Int, componentId: Int) {
        // MutableState to hold the lists
        var tutor: User? by remember { mutableStateOf(null) }
        var component: Component? by remember { mutableStateOf(null) }
        var groups by remember { mutableStateOf(listOf<Group>()) }
        var tutors by remember { mutableStateOf(listOf<User>()) }

        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val database = app.getDatabase() // Reuse the existing database connection
            val (fetchedTutor, fetchedComponent, fetchedGroups, fetchedTutors) = fetchObjects(database, tutorId, componentId)

            // Update the states
            tutor = fetchedTutor
            component = fetchedComponent
            groups = fetchedGroups
            tutors = fetchedTutors

            isLoading = false
        }

        if (isLoading) {
            Text(text = "Loading...", modifier = Modifier.padding(16.dp))
        } else if (tutor == null) {
            Text(text = "Tutor not found!", modifier = Modifier.padding(16.dp))
        } else if (tutor!!.role.name == "Student") {
            Text(text = "Tutor has Student role!", modifier = Modifier.padding(16.dp))
        } else if (component == null) {
            Text(text = "No component found!", modifier = Modifier.padding(16.dp))
        } else { // TODO block access to tutors
            ManageComponentScreen(app, tutor!!, component!!, groups, tutors)
        }
    }


    @Composable
    fun ManageComponentScreen(app: App, tutor: User, component: Component, groups: List<Group>, tutors: List<User>) {
        val context = LocalContext.current

        var showDeleteComponentDialog by remember { mutableStateOf(false) }
        var showEditComponentDialog by remember { mutableStateOf(false) }
        var deleteComponent by remember { mutableStateOf(false) }
        var renameComponent by remember { mutableStateOf(false) }
        var componentName by remember { mutableStateOf("") }


        var skillName by remember { mutableStateOf("") }
        var skillDescription by remember { mutableStateOf("") }
        var skillCoefficient by remember { mutableIntStateOf(1) }

        var showEditSkillDialog by remember { mutableStateOf(false) }
        var skillBeingEdited by remember { mutableStateOf<Skill?>(null) }

        var showDeleteSkillDialog by remember { mutableStateOf(false) }
        var skillBeingDeleted by remember { mutableStateOf<Skill?>(null) }

        var showAddingSkillDialog by remember { mutableStateOf(false) }
        var skillBeingAdded by remember { mutableStateOf<Skill?>(null) }

        val skillsToAdd = remember { mutableStateListOf<Skill>() }
        val skillsToEdit = remember { mutableStateListOf<Skill>() }
        val skillsToDelete = remember { mutableStateListOf<Skill>() }

        var expandedGroup by remember { mutableStateOf(false) }
        var selectedGroup by remember { mutableStateOf<Group?>(null) }

        var expandedTutor by remember { mutableStateOf(false) }
        var selectedTutor by remember { mutableStateOf<User?>(null) }

        var showEditPairDialog by remember { mutableStateOf(false) }
        var pairBeingEdited by remember { mutableStateOf<TutorMapping?>(null) }

        var showDeletePairDialog by remember { mutableStateOf(false) }
        var pairBeingDeleted by remember { mutableStateOf<TutorMapping?>(null) }

        var showAddingPairDialog by remember { mutableStateOf(false) }
        var pairBeingAdded by remember { mutableStateOf<TutorMapping?>(null) }

        val pairsToAdd = remember { mutableStateListOf<TutorMapping>() }
        val pairsToEdit = remember { mutableStateListOf<TutorMapping>() }
        val pairsToDelete = remember { mutableStateListOf<TutorMapping>() }

        var showApplyDialog by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }


        Scaffold(
            topBar = { Header("Manage Component: ${component.name}", app) },
            bottomBar = { Footer(tutor.id) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {



                LaunchedEffect(deleteComponent) {
                    if (deleteComponent) {
                        try {
                            val database = app.getDatabase()
                            withContext(Dispatchers.IO) {

                                // Delete the scores and group observations
                                component.skills.forEach { skill ->
                                    database.sequenceOf(Schemas.Scores).removeIf { it.skillId eq skill.id }
                                    database.sequenceOf(Schemas.GroupObservations).removeIf { it.skillId eq skill.id }
                                }
                                // Delete the pairs
                                database.sequenceOf(Schemas.TutorMappings).removeIf { it.componentId eq component.id }

                                // delete the skills
                                database.sequenceOf(Schemas.Skills).removeIf { it.componentId eq component.id }

                                // Delete the component
                                database.sequenceOf(Schemas.Components).removeIf { it.id eq component.id }
                                Log.d("DELETE SUCCESS", "COMPONENT DELETION SUCCESS")

                                renameComponent = false

                                // Redirect to components overview
                                val intent = Intent(context, ComponentsOverviewActivity::class.java)
                                intent.putExtra("tutorId", tutor.id)
                                startActivity(intent)
                            }

                        } catch(e: Exception) {
                            Log.e("SQL UPDATE ERROR", e.toString())
                            renameComponent = false
                        }
                    }
                }


                LaunchedEffect(isLoading) {
                    if (isLoading) {
                        try {
                            val database = app.getDatabase()

                            withContext(Dispatchers.IO) {

                                if (renameComponent) {
                                    database.sequenceOf(Schemas.Components).update(component)
                                    Log.d("SUCCESS", "COMPONENT NAME UPDATE SUCCESS")
                                }

                                // Update the skill table
                                if (skillsToEdit.isNotEmpty()) {
                                    skillsToEdit.forEach { skill ->
                                        database.sequenceOf(Schemas.Skills).update(skill)
                                    }
                                }
                                if (skillsToDelete.isNotEmpty()) {
                                    skillsToDelete.forEach { skill ->
                                        // First delete the scores referencing to the skill
                                        database.sequenceOf(Schemas.Scores).removeIf {it.skillId eq skill.id }
                                        database.sequenceOf(Schemas.GroupObservations).removeIf {it.skillId eq skill.id }
                                        // Then delete the skills
                                        database.sequenceOf(Schemas.Skills).removeIf {it.id eq skill.id }
                                    }
                                }
                                if (skillsToAdd.isNotEmpty()) {
                                    skillsToAdd.forEach { skill ->
                                        database.sequenceOf(Schemas.Skills).add(skill)
                                    }
                                }
                                Log.d("SUCCESS UPDATING SKILLS", "#########################")

                                // Update the tutor_mapping table
                                if (pairsToEdit.isNotEmpty()) {
                                    pairsToEdit.forEach { pair ->
                                        database.sequenceOf(Schemas.TutorMappings).update(pair)
                                    }
                                }
                                if (pairsToDelete.isNotEmpty()) {
                                    pairsToDelete.forEach { pair ->
                                        database.sequenceOf(Schemas.TutorMappings).removeIf {it.id eq pair.id }
                                    }
                                }
                                if (pairsToAdd.isNotEmpty()) {
                                    pairsToAdd.forEach { pair ->
                                        database.sequenceOf(Schemas.TutorMappings).add(pair)
                                    }
                                }
                                Log.d("SUCCESS UPDATING PAIRS", "#########################")
                            }
                            isLoading = false

                            // Redirect to components overview
                            val intent = Intent(context, ComponentsOverviewActivity::class.java)
                            intent.putExtra("tutorId", tutor.id)
                            startActivity(intent)

                        } catch(e: Exception) {
                            Log.e("SQL UPDATE ERROR", e.toString())
                            isLoading = false
                        }
                    }
                }


                // Dialog to delete component
                if (showDeleteComponentDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteComponentDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                deleteComponent = true
                                showDeleteComponentDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteComponentDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to delete the component?\nAll the data will be lost forever.",
                                color = Color.Black
                            )
                        }
                    )
                }



                // Dialog to edit component name
                if (showEditComponentDialog) {
                    componentName = component.name

                    AlertDialog(
                        onDismissRequest = { showEditComponentDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                component.name = componentName
                                renameComponent = true
                                showEditComponentDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditComponentDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {
                            TextField(
                                value = componentName,
                                onValueChange = {
                                    componentName = it
                                },
                                label = { Text("Name") }
                            )
                        }
                    )
                }


                // Dialog to confirm database changes
                if (showApplyDialog) {
                    AlertDialog(
                        onDismissRequest = { showApplyDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                isLoading = true
                                showApplyDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showApplyDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to apply the modification to the database?\nAll lost data will be lost forever.",
                                color = Color.Black
                            )
                        }
                    )
                }



                // AlertDialog for modifying skills
                if (showEditSkillDialog) {

                    // Mandatory to have these variables because Text fields don't support object's attributes modification
                    skillName = skillBeingEdited!!.name
                    skillDescription = skillBeingEdited!!.description
                    skillCoefficient = skillBeingEdited!!.coefficient

                    AlertDialog(
                        onDismissRequest = { showEditSkillDialog = false },
                        confirmButton = {
                            TextButton(onClick = {

                                skillBeingEdited!!.name = skillName
                                skillBeingEdited!!.description = skillDescription
                                skillBeingEdited!!.coefficient = skillCoefficient

                                skillsToEdit.add(skillBeingEdited!!)

                                showEditSkillDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditSkillDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {

                            Column {
                                TextField(
                                    value = skillName,
                                    onValueChange = {
                                        skillName = it
                                    },
                                    label = { Text("Name") }
                                )
                                TextField(
                                    value = skillDescription,
                                    onValueChange = { skillDescription = it },
                                    label = { Text("Description") }
                                )
                                TextField(
                                    value = if (skillCoefficient != 0) skillCoefficient.toString() else "",
                                    onValueChange = { skillCoefficient = if (it.isNotEmpty()) it.toInt() else 0 },
                                    label = { Text("Coefficient") }
                                )
                            }
                        }

                    )
                }

                // AlertDialog for deleting skills
                if (showDeleteSkillDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteSkillDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                skillsToDelete.add(skillBeingDeleted!!)
                                component.skills -= skillBeingDeleted!!
                                showDeleteSkillDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteSkillDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = { Text(
                            text = "Delete the skill: ${skillBeingDeleted!!.name}?",
                            color = Color.Black
                        )}
                    )
                }


                // AlertDialog for adding skills
                if (showAddingSkillDialog) {

                    // Mandatory to have these variables because Text fields don't support object's attributes modification
                    skillName = ""
                    skillDescription = ""
                    skillCoefficient = 1

                    AlertDialog(
                        onDismissRequest = { showAddingSkillDialog = false },
                        confirmButton = {
                            TextButton(onClick = {

                                skillBeingAdded = Skill {
                                    this.component = component
                                    this.name = skillName
                                    this.description = skillDescription
                                    this.coefficient = skillCoefficient
                                }
                                skillsToAdd.add(skillBeingAdded!!)
                                component.skills += skillBeingAdded!!
                                showAddingSkillDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddingSkillDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {


                            Column {
                                TextField(
                                    value = skillName,
                                    onValueChange = {
                                        skillName = it
                                    },
                                    label = { Text("Name") }
                                )
                                TextField(
                                    value = skillDescription,
                                    onValueChange = { skillDescription = it },
                                    label = { Text("Description") }
                                )
                                TextField(
                                    value = if (skillCoefficient != 0) skillCoefficient.toString() else "",
                                    onValueChange = { skillCoefficient = if (it.isNotEmpty()) it.toInt() else 0 },
                                    label = { Text("Coefficient") }
                                )
                            }
                        }



                    )
                }


                // AlertDialog for editing pair
                if (showEditPairDialog) {

                    AlertDialog(
                        onDismissRequest = { showEditPairDialog = false },
                        confirmButton = {
                            TextButton(onClick = {

                                pairBeingEdited!!.group = selectedGroup!!
                                pairBeingEdited!!.tutor = selectedTutor!!
                                pairsToEdit.add(pairBeingEdited!!)

                                showEditPairDialog = false
                            },
                                enabled = (selectedGroup != null) and (selectedTutor != null)

                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditPairDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Group: ")
                                    Box {
                                        Text(
                                            text = selectedGroup?.name ?: "Select a group",
                                            modifier = Modifier
                                                .clickable { expandedGroup = true }
                                                .padding(8.dp),
                                            color = Color.Black
                                        )
                                        DropdownMenu(
                                            expanded = expandedGroup,
                                            onDismissRequest = { expandedGroup = false }
                                        ) {
                                            groups.forEach { group ->
                                                DropdownMenuItem(
                                                    text = { Text(
                                                        text = group.name,
                                                        color = Color.Black
                                                        )},
                                                    onClick = {
                                                        selectedGroup = group
                                                        expandedGroup = false
                                                    }

                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Tutor: ")
                                    Box {
                                        Text(
                                            text = if (selectedTutor != null) "${selectedTutor!!.firstName} ${selectedTutor!!.lastName}" else "Select a Tutor",
                                            modifier = Modifier
                                                .clickable { expandedTutor = true }
                                                .padding(8.dp),
                                            color = Color.Black
                                        )
                                        DropdownMenu(
                                            expanded = expandedTutor,
                                            onDismissRequest = { expandedTutor = false }
                                        ) {
                                            tutors.forEach { tutor ->
                                                DropdownMenuItem(
                                                    text = { Text(
                                                     text = "${tutor.firstName} ${tutor.lastName}",
                                                        color = Color.Black
                                                    )},
                                                    onClick = {
                                                        selectedTutor = tutor
                                                        expandedTutor = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }


                // AlertDialog for deleting pair
                if (showDeletePairDialog) {

                    AlertDialog(
                        onDismissRequest = { showDeletePairDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                if (pairsToDelete.find { it.id != pairBeingDeleted!!.id } == null) {
                                    pairsToDelete.add(pairBeingDeleted!!)
                                }

                                component.pairs -= pairBeingDeleted!!
                                showDeletePairDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeletePairDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {
                            Text(
                                text = "De-assign the tutor ${pairBeingDeleted!!.tutor.firstName} ${pairBeingDeleted!!.tutor.lastName} to the group ${pairBeingDeleted!!.group.name}?"
                            )

                        }
                    )
                }


                // AlertDialog for adding pair
                if (showAddingPairDialog) {

                    AlertDialog(
                        onDismissRequest = { showAddingPairDialog = false },
                        confirmButton = {
                            TextButton(onClick = {

                                pairBeingAdded = TutorMapping {
                                    this.component = component
                                    this.group = selectedGroup!!
                                    this.tutor = selectedTutor!!
                                }

                                if (pairsToAdd.filter { it.component == component }.filter { it.group == selectedGroup }.find { it.tutor == selectedTutor } == null) {
                                    pairsToEdit.add(pairBeingAdded!!)
                                }
                                val obsoletePairDelete = pairsToDelete.filter { it.component == component }.filter { it.group == selectedGroup }.find { it.tutor == selectedTutor }
                                if (obsoletePairDelete != null) {
                                    pairsToDelete.remove(obsoletePairDelete)
                                }
                                component.pairs += pairBeingAdded!!
                                showAddingPairDialog = false
                            },
                                enabled = (selectedGroup != null) and (selectedTutor != null)

                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddingPairDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Group: ")
                                    Box {
                                        Text(
                                            text = selectedGroup?.name ?: "Select a group",
                                            modifier = Modifier
                                                .clickable { expandedGroup = true }
                                                .padding(8.dp)
                                        )
                                        DropdownMenu(
                                            expanded = expandedGroup,
                                            onDismissRequest = { expandedGroup = false }
                                        ) {
                                            groups.forEach { group ->
                                                DropdownMenuItem(
                                                    text = { Text(text = group.name)},
                                                    onClick = {
                                                        selectedGroup = group
                                                        expandedGroup = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Tutor: ")
                                    Box {
                                        Text(
                                            text = if (selectedTutor != null) "${selectedTutor!!.firstName} ${selectedTutor!!.lastName}" else "Select a Tutor",
                                            modifier = Modifier
                                                .clickable { expandedTutor = true }
                                                .padding(8.dp)
                                        )
                                        DropdownMenu(
                                            expanded = expandedTutor,
                                            onDismissRequest = { expandedTutor = false }
                                        ) {
                                            tutors.forEach { tutor ->
                                                DropdownMenuItem(
                                                    text = { Text(text = "${tutor.firstName} ${tutor.lastName}")},
                                                    onClick = {
                                                        selectedTutor = tutor
                                                        expandedTutor = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }





                LazyColumn(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = component.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                showEditComponentDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Component"
                                )
                            }
                            IconButton(onClick = {
                                showDeleteComponentDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Component"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(component.skills) { skill ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = skill.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = skill.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Coefficient: ${skill.coefficient}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    skillBeingEdited = skill
                                    showEditSkillDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Skill"
                                    )
                                }
                                IconButton(onClick = {
                                    skillBeingDeleted = skill
                                    showDeleteSkillDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Skill"
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                      showAddingSkillDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Skill")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Assigned tutors",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (component.pairs.isNotEmpty()) {
                        items(component.pairs) { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pair.group.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${pair.tutor.firstName} ${pair.tutor.lastName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        showEditPairDialog = true
                                        pairBeingEdited = pair
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Pair"
                                        )
                                    }
                                    IconButton(onClick = {
                                        pairBeingDeleted = pair
                                        showDeletePairDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Pair"
                                        )
                                    }
                                }
                            }
                        }
                    }


                    item {
                        Button(
                            onClick = { showAddingPairDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Assign a tutor to a group")
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showApplyDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply modifications")
                        }
                    }
                }
            }
        }
    }


}
