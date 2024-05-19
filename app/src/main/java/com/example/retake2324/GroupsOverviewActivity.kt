package com.example.retake2324

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupsOverviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups_overview)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val groups: List<Group> = getGroups()
        val students: List<Student> = getStudents()
        val components: List<Component> = getComponents()
        val skills: List<Skill> = getSkills()
        val skillScores: List<SkillScore> = getSkillScores()
        val componentScores: List<ComponentScore> = getComponentScores()

        val adapter = GroupsOverviewAdapter(groups, students, components, skills, skillScores, componentScores)
        recyclerView.adapter = adapter
    }

    private fun getGroups(): List<Group> {
        return listOf(
            Group(name = "Group A", tutor = "Tutor 1", client = "Client 1"),
            Group(name = "Group B", tutor = "Tutor 2", client = "Client 2")
        )
    }

    private fun getStudents(): List<Student> {
        val groups = getGroups()
        return listOf(
            Student(name = "Student 1", group = groups[0]),
            Student(name = "Student 2", group = groups[0]),
            Student(name = "Student 3", group = groups[0]),
            Student(name = "Student 4", group = groups[0]),
            Student(name = "Student 5", group = groups[1]),
            Student(name = "Student 6", group = groups[1]),
            Student(name = "Student 7", group = groups[1]),
            Student(name = "Student 8", group = groups[1])
        )
    }

    private fun getComponents(): List<Component> {
        return listOf(
            Component(name = "Component 1"),
            Component(name = "Component 2")
        )
    }

    private fun getSkills(): List<Skill> {
        val components = getComponents()
        return listOf(
            Skill(name = "Skill 1", coefficient = 1.0, component = components[0]),
            Skill(name = "Skill 2", coefficient = 1.5, component = components[0]),
            Skill(name = "Skill 3", coefficient = 2.0, component = components[0]),
            Skill(name = "Skill 4", coefficient = 1.0, component = components[1]),
            Skill(name = "Skill 5", coefficient = 1.5, component = components[1]),
            Skill(name = "Skill 6", coefficient = 2.0, component = components[1])
        )
    }

    private fun getSkillScores(): List<SkillScore> {
        val students = getStudents()
        val skills = getSkills()
        return listOf(
            SkillScore(skill = skills[0], student = students[0], score = 85.0),
            SkillScore(skill = skills[0], student = students[1], score = 90.0),
            SkillScore(skill = skills[0], student = students[2], score = 75.0),
            SkillScore(skill = skills[0], student = students[3], score = 80.0),
            SkillScore(skill = skills[1], student = students[0], score = 88.0),
            SkillScore(skill = skills[1], student = students[1], score = 85.0),
            SkillScore(skill = skills[1], student = students[2], score = 92.0),
            SkillScore(skill = skills[1], student = students[3], score = 78.0),
            SkillScore(skill = skills[2], student = students[0], score = 79.0),
            SkillScore(skill = skills[2], student = students[1], score = 82.0),
            SkillScore(skill = skills[2], student = students[2], score = 91.0),
            SkillScore(skill = skills[2], student = students[3], score = 84.0),
            SkillScore(skill = skills[3], student = students[4], score = 80.0),
            SkillScore(skill = skills[3], student = students[5], score = 85.0),
            SkillScore(skill = skills[3], student = students[6], score = 90.0),
            SkillScore(skill = skills[3], student = students[7], score = 75.0),
            SkillScore(skill = skills[4], student = students[4], score = 82.0),
            SkillScore(skill = skills[4], student = students[5], score = 78.0),
            SkillScore(skill = skills[4], student = students[6], score = 88.0),
            SkillScore(skill = skills[4], student = students[7], score = 85.0),
            SkillScore(skill = skills[5], student = students[4], score = 90.0),
            SkillScore(skill = skills[5], student = students[5], score = 85.0),
            SkillScore(skill = skills[5], student = students[6], score = 79.0),
            SkillScore(skill = skills[5], student = students[7], score = 84.0)
        )
    }

    private fun getComponentScores(): List<ComponentScore> {
        val students = getStudents()
        val components = getComponents()
        return listOf(
            ComponentScore(component = components[0], student = students[0], score = 80.0),
            ComponentScore(component = components[0], student = students[1], score = 85.0),
            ComponentScore(component = components[0], student = students[2], score = 90.0),
            ComponentScore(component = components[0], student = students[3], score = 75.0),
            ComponentScore(component = components[1], student = students[4], score = 88.0),
            ComponentScore(component = components[1], student = students[5], score = 80.0),
            ComponentScore(component = components[1], student = students[6], score = 85.0),
            ComponentScore(component = components[1], student = students[7], score = 90.0)
        )
    }
}
