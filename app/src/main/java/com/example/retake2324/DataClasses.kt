package com.example.retake2324

data class Group(
    val name: String,
    val tutor: String,
    val client: String
)

data class Student(
    val name: String,
    val group: Group
)

data class Component(
    val name: String
)

data class Skill(
    val name: String,
    val coefficient: Double,
    val component: Component
)

data class SkillScore(
    val skill: Skill,
    val student: Student,
    val score: Double
)

data class ComponentScore(
    val component: Component,
    val student: Student,
    val score: Double
)
