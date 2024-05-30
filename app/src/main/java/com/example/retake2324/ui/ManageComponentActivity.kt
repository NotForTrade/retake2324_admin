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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
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
import org.ktorm.dsl.isNotNull
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.entity.sequenceOf


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


    private suspend fun fetchObjects(
        database: Database,
        tutorId: Int,
        componentId: Int
    ): Pair<User?, Component?> {

        try {

            // Fetch the tutor
            val tutor = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Users).find { it.id eq tutorId }
            }
            if (tutor == null) {
                Log.e("TUTOR NOT FOUND", "TUTOR NOT FOUND IN DATABASE!")
                return Pair(null, null)
            } else if (tutor.role.name == "Student") {
                Log.e("STUDENT FOUND", "A STUDENT IS MATCHING THE ID")
                return Pair(tutor, null)
            }/*
            else if (tutor.role.name == "Tutor") {
                Log.e("TUTOR FOUND", "A TUTOR IS MATCHING THE ID")
                return Pair(tutor, null)
            }*/

            // Fetch the component
            val component = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Components).find { it.id eq componentId }
            }
            if (component == null) {
                Log.e("COMPONENT NOT FOUND", "#####################################")
                return Pair(null, null)
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

            return Pair(tutor, component)
        } catch (e: Exception) {
            Log.e("SQL FETCHING ERROR", e.toString())
            return Pair(null, null)
        }
    }


    @Composable
    fun ManageComponentLoader(app: App, tutorId: Int, componentId: Int) {
        // MutableState to hold the lists
        var tutor: User? by remember { mutableStateOf(null) }
        var component: Component? by remember { mutableStateOf(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val database = app.getDatabase() // Reuse the existing database connection
            val (fetchedTutor, fetchedComponent) = fetchObjects(database, tutorId, componentId)

            // Update the states
            tutor = fetchedTutor
            component = fetchedComponent
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
            ManageComponentScreen(app, tutor!!, component!!)
        }
    }


    @Composable
    fun ManageComponentScreen(app: App, tutor: User, component: Component) {
        val context = LocalContext.current

        var showDialog by remember { mutableStateOf(false) }
        var skillBeingEdited by remember { mutableStateOf<Skill?>(null) } // State to keep track of the skill being edited

        val skillsToAdd = remember { mutableStateListOf<Skill>() }
        val skillsToEdit = remember { mutableStateListOf<Skill>() }
        val skillsToDelete = remember { mutableStateListOf<Skill>() }

        val pairsToAdd = remember { mutableStateListOf<TutorMapping>() }
        val pairsToEdit = remember { mutableStateListOf<TutorMapping>() }
        val pairsToDelete = remember { mutableStateListOf<TutorMapping>() }

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
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                // Update the skill in component.skills and close the dialog
                                if (skillsToEdit.find {it.id == skillBeingEdited!!.id } == null){
                                    skillsToEdit.add(skillBeingEdited!!)
                                }
                                showDialog = false
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Dismiss")
                            }
                        },
                        text = {

                            Column {
                                TextField(
                                    value = skillBeingEdited!!.name,
                                    onValueChange = { skillBeingEdited!!.name = it },
                                    label = { Text("Name") }
                                )
                                TextField(
                                    value = skillBeingEdited!!.description,
                                    onValueChange = { skillBeingEdited!!.description = it },
                                    label = { Text("Description") }
                                )
                                TextField(
                                    value = skillBeingEdited!!.coefficient.toString(),
                                    onValueChange = {skillBeingEdited!!.coefficient = it.toInt() },
                                    label = { Text("Coefficient") }
                                )
                            }

                        }
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                            Button(onClick = { /* Edit component name */ }) {
                                Text("Edit Name")
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
                                    if (!skillsToEdit.contains(skill)) {
                                        skillsToEdit.add(skill)
                                    }
                                    showDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Skill"
                                    )
                                }
                                IconButton(onClick = {
                                    component.skills -= skill
                                    skillsToDelete.add(skill)
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
                            onClick = { },
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
                                IconButton(onClick = { /* Edit pair */ }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Pair"
                                    )
                                }
                                IconButton(onClick = { component.pairs -= pair }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Pair"
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { /* Assign tutor to group */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Assign a tutor to a group")
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* APPLY MODIFICATIONS */ },
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