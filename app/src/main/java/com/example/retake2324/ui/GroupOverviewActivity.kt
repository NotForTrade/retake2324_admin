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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.retake2324.core.App
import com.example.retake2324.data.Component
import com.example.retake2324.data.Schemas
import com.example.retake2324.data.Score
import com.example.retake2324.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.entity.sequenceOf
import java.math.BigDecimal
import java.math.RoundingMode


class GroupOverviewActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App

        val tutorId = intent.getIntExtra("tutorId", 12)
        val groupId = intent.getIntExtra("groupId", -1)

        setContent {
            MaterialTheme {
                GroupOverviewLoader(app, tutorId, groupId)
            }
        }
    }


    private suspend fun fetchObjects(database: Database, tutorId: Int, groupId: Int): Triple<User?, List<User>, List<Component>> {

        try {

            // fetch the tutor
            val tutor = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Users).find { it.id eq tutorId }
            }
            if (tutor == null) {
                Log.d("FETCH ERROR", "########################## TUTOR MISSING ##########################")
                return Triple(null, emptyList(), emptyList())
            } else if (tutor.role.name == "Student") {
                Log.d("FETCH ERROR", "########################## TUTOR IS STUDENT ##########################")
                return Triple(tutor, emptyList(), emptyList())
            }

            // Fetch the group
            val group = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Groups).find { it.id eq groupId }
            }
            if (group == null) {
                Log.d("FETCH ERROR", "########################## GROUP MISSING ##########################")
                return Triple(tutor, emptyList(), emptyList())
            }

            // Fetch the students from the group
            val students = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Users).filter { it.groupId eq group.id }
                    .toList()
            }

            // Fetch the components of the group
            val tutorMapping = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.TutorMappings)
                    .filter { it.tutorId eq tutor.id }
                    .filter { it.groupId eq group.id }
                    .toList()
            }
            if (tutorMapping.isEmpty()) {
                Log.d("FETCH ERROR", "########################## MAPPING MISSING ##########################")
                return Triple(tutor, emptyList(), emptyList())
            }
            val components: MutableList<Component> = mutableListOf()
            tutorMapping.forEach { pair ->
                // Prevent duplicate entries to generate duplicate items
                if (components.find { it.id == pair.component.id} == null)
                components.add(pair.component)
            }
            if (components.isEmpty()) {
                Log.d("FETCH ERROR", "########################## NO ASSIGNED COMPONENT ##########################")
                return Triple(tutor, emptyList(), emptyList())
            }

            // Fetch the skill list
            val skills = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Skills).toList()
            }
            if (skills.isEmpty()) {
                Log.d("FETCH ERROR", "########################## NO SKILLS ##########################")
                return Triple(tutor, emptyList(), emptyList())
            }

            // Fetch the score list -- ignore these for history
            val scores = withContext(Dispatchers.IO) {
                database.sequenceOf(Schemas.Scores).filter { it.active eq true }.toList()
            }

            // Set-up the studentComponentScore mutable list to build the list of students' score per component
            val studentComponentScore: MutableList<Score> = mutableListOf()

            // attribute skills to their corresponding component
            components.forEach { component ->
                component.skills =
                    skills.filter { it.component.id == component.id }.toList()
                if (component.skills.isNotEmpty()) {
                    // attribute scores to each skills
                    component.skills.forEach { skill ->
                        skill.scores = scores.filter { it.skill.id == skill.id }.toList()
                    }
                    if (students.isNotEmpty() and scores.isNotEmpty()) {
                        // For each student, calculate their weighted average score of the currently explored component
                        students.forEach { student ->
                            val studentScores =
                                scores.filter { (it.student.id == student.id) and (it.skill.component.id == component.id) }
                            if (studentScores.isNotEmpty()) {
                                var weightedScoreSum = 0.0
                                var coefficientSum = 0.0
                                studentScores.forEach { studentScore ->
                                    if (studentScore.value != null) {
                                        weightedScoreSum += studentScore.value!! * studentScore.skill.coefficient
                                        coefficientSum += studentScore.skill.coefficient
                                    }
                                }
                                // Create and add a new Score object with the weighted average score for the component
                                studentComponentScore.add(Score {
                                    id // not used
                                    this.student = student
                                    skill // not used
                                    this.value =
                                        if (coefficientSum != 0.0) weightedScoreSum / coefficientSum else 0.0
                                    observation // not used
                                })
                            }
                        }
                        // Assign the new built score list to the component
                        component.scores = studentComponentScore.toList()
                        // Clear studentComponentScore for future calculation
                        studentComponentScore.clear()
                        // Log.i("COMPONENT SCORE", component.scores.toString())
                    }
                }
            }

            // Create empty score when entries are missing
            components.forEach {component ->
                component.skills.forEach {skill ->
                    students.forEach {student ->
                        var score = scores.filter { it.skill.id == skill.id }.find { it.student.id == student.id }
                        if (score == null) {
                            score = Score {
                                this.student = student
                                this.skill = skill
                                this.value = null
                                this.observation = null
                                this.active = true
                            }
                            withContext(Dispatchers.IO) {
                                database.sequenceOf(Schemas.Scores).add(score)
                            }
                            Log.d("EMPTY SCORE ADDED", score.toString())

                            // get the score from the database
                            val newScore = withContext(Dispatchers.IO) {
                                database.sequenceOf(Schemas.Scores)
                                    .filter { it.studentId eq score.student.id }
                                    .filter { it.skillId eq score.skill.id }
                                    .find { it.active eq true }
                            }

                            // Update the score list
                            val newList = components.find { it.id == score.skill.component.id }!!.skills.find {it.id == score.skill.id}!!.scores.toMutableList()
                            newList.add(newScore!!)
                            components.find { it.id == score.skill.component.id }!!.skills.find {it.id == score.skill.id}!!.scores = newList
                        }
                    }
                }
            }


            Log.d("FETCH SUCCESS", "########################## SUCCESS ##########################")
            Log.d("TUTOR", tutor.toString())
            Log.d("STUDENTS", students.toString())
            Log.d("COMPONENTS", components.toString())
            return Triple(tutor, students, components)

        } catch (e: Exception) {
            Log.e("SQL FETCHING ERROR", e.toString())
            return Triple(null, emptyList(), emptyList())
        }
    }


    @Composable
    fun GroupOverviewLoader(app: App, tutorId: Int, groupId: Int) {
        // MutableState to hold the lists
        var tutor: User? by remember { mutableStateOf(null) }
        var students by remember { mutableStateOf(listOf<User>()) }
        var components by remember { mutableStateOf(listOf<Component>()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val database = app.getDatabase() // Reuse the existing database connection
            val (fetchedTutor, fetchedStudents, fetchedComponents) = fetchObjects(database, tutorId, groupId)

            // Update the states
            tutor = fetchedTutor
            students = fetchedStudents
            components = fetchedComponents
            isLoading = false
        }

        if (isLoading) {
            Text(text = "Loading...", modifier = Modifier.padding(16.dp))
        } else if (tutor == null) {
            Text(text = "An error occurred while retrieving data on the database!", modifier = Modifier.padding(16.dp))
        } else if (tutor!!.role.name == "Student") {
            Text(text = "The user matches a student, not an administration member!", modifier = Modifier.padding(16.dp))
        } else {
            GroupOverviewScreen(app, tutor!!, students, components)
        }
    }

    @Composable
    fun GroupOverviewScreen(app: App, tutor: User, students: List<User>, components: List<Component>) {
        val context = LocalContext.current
        val expandedComponents = remember { mutableStateOf(components.map { it.id }.toSet()) }

        val scoreList = listOf(null, 0, 7, 10, 13, 16, 20)

        var updateComponentScores by remember { mutableStateOf(false) }

        val initialScores = remember { mutableStateListOf<Score>() }
        val scoreUpdates = remember { mutableStateListOf<Score>() }

        val columnWidths = listOf(200.dp) + List(students.size) { 100.dp }




        LaunchedEffect(Unit) {

        }


        Scaffold(
            topBar = { Header("${students[0].group?.name} Overview", app) },
            bottomBar = { Footer(tutor.id) }
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
                                    text = "Group: ${students[0].group?.name}",
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
                                            text = BigDecimal(score).setScale(1, RoundingMode.HALF_UP).toString(),
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
                                                            putExtra("tutorId", tutor.id)
                                                            putExtra("skillId", skill.id)
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                            )

                                            students.forEachIndexed { index, student ->
                                                val score = skill.scores.find { it.student.id == student.id }
                                                var expanded by remember { mutableStateOf(false) }

                                                Box {
                                                    Text(
                                                        text = "${score?.value ?: "-"}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier
                                                            .width(columnWidths[index + 1])
                                                            .clickable { expanded = true }
                                                    )

                                                    DropdownMenu(
                                                        expanded = expanded,
                                                        onDismissRequest = { expanded = false },
                                                        modifier = Modifier.width(columnWidths[index + 1])
                                                    ) {
                                                        scoreList.forEach { value ->
                                                            DropdownMenuItem(
                                                                text = { Text(value?.toString() ?: "-") },
                                                                onClick = {

                                                                    val newScore = score!!.copy()
                                                                    newScore.value = value?.toDouble()

                                                                    // Check if the score's value changes
                                                                    if (score != newScore) {
                                                                        // Check if the score already exists in initialScores
                                                                        if (initialScores.find { it.id == skill.id } == null){
                                                                            // add the score to the initialScores
                                                                            initialScores.add(score.copy())
                                                                        }
                                                                        // Check if the score already exists in the scoreUpdates
                                                                        if (scoreUpdates.find { it.id == skill.id } == null){
                                                                            // remove the score to the scoreUpdates
                                                                            scoreUpdates.add(score)
                                                                        }

                                                                        // Update the score's value
                                                                        score.value = value?.toDouble()

                                                                        // Update the component scores
                                                                        updateComponentScores = true

                                                                        // there might be a simple mathematic rule for this

                                                                    }

                                                                    expanded = false
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
        }
    }
}