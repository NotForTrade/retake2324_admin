package com.example.retake2324.data

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.text.SimpleDateFormat
import java.time.LocalDateTime



interface Announcement : Entity<Announcement> {
    companion object : Entity.Factory<Announcement>()
    var id: Int
    var tutor: User
    var group: Group?
    var title: String
    var content: String
    var datetime: String
}

interface Attendance : Entity<Attendance> {
    companion object : Entity.Factory<Attendance>()
    var id:  Int
    var tutor: User
    var student: User
    var component: Component
    var value: String
    var session: Int
}

interface Component : Entity<Component> {
    companion object : Entity.Factory<Component>()
    var id:  Int
    var module: Module
    var name: String
    var skills: List<Skill>
    var scores: List<Score>
    var groups: List<Group>
    var pairs: List<TutorMapping>
}

interface Module : Entity<Module> {
    companion object : Entity.Factory<Module>()
    var id:  Int
    var name: String
}

interface Role : Entity<Role> {
    companion object : Entity.Factory<Role>()
    var id:  Int
    var name: String
}


interface Group : Entity<Group> {
    companion object : Entity.Factory<Group>()
    var id:  Int
    var customer: User?
    var name: String
    var students: List<User>
}

interface User : Entity<User> {
    companion object : Entity.Factory<User>()
    var id: Int
    var role: Role
    var group: Group?
    var module: Module?
    var component: Component?
    var photo: String?
    var firstName: String
    var lastName: String
    var email: String
    var password: String

}

interface TutorMapping : Entity<TutorMapping> {
    companion object : Entity.Factory<TutorMapping>()
    var id: Int
    var tutor: User
    var group: Group
    var component: Component

}


interface Skill : Entity<Skill> {
    companion object : Entity.Factory<Skill>()
    var id:  Int
    var component: Component
    var name: String
    var description: String
    var coefficient: Int
    var scores: List<Score>
    var groupObservations: List<GroupObservation>
}

interface GroupObservation : Entity<GroupObservation> {
    companion object : Entity.Factory<GroupObservation>()
    var id: Int
    var skill: Skill
    var group: Group
    var observation: String
}

interface Score : Entity<Score> {
    companion object : Entity.Factory<Score>()
    var id: Int
    var student: User
    var skill: Skill
    var value: Double?
    var observation: String?
    var active: Boolean
    var datetime: String
}

interface Reassessment: Entity<Reassessment> {
    companion object: Entity.Factory<Reassessment>()
    var id: Int
    var student: User
    var skill: Skill
    var score: Score?
    var document: String
    var datetime: String
    var treated: Boolean
}

object Schemas {

    object Announcements : Table<Announcement>("announcement") {
        val id = int("id").primaryKey().bindTo { it.id }
        val tutorId = int("tutor_id").references(Users) { it.tutor }
        val groupId = int("group_id").references(Groups) { it.group }
        val title = varchar("title").bindTo { it.title }
        val content = varchar("content").bindTo { it.content }
        val datetime = varchar("datetime").bindTo { it.datetime }
    }


    object Attendances : Table<Attendance>("attendance") {
        val id = int("id").primaryKey().bindTo { it.id}
        val tutorId = int("tutor_id").references(Users) { it.tutor }
        val studentId = int("student_id").references(Users) { it.student }
        val componentId = int("component_id").references(Components) { it.component }
        val value = varchar("value").bindTo { it.value }
        val session = int("session").bindTo { it.session }
    }

    object Roles : Table<Role>("role") {
        val id = int("id").primaryKey().bindTo { it.id}
        val name = varchar("name").bindTo { it.name }
    }

    object Components : Table<Component>("component") {
        val id = int("id").primaryKey().bindTo { it.id}
        val moduleId = int("module_id").references(Modules) { it.module }
        val name = varchar("name").bindTo { it.name }
    }

    object Groups : Table<Group>("group") {
        val id = int("id").primaryKey().bindTo { it.id}
        val customerId = int("customer_id").references(Users) { it.customer }
        val name = varchar("name").bindTo { it.name }
    }

    object Modules : Table<Module>("module") {
        val id = int("id").primaryKey().bindTo { it.id}
        val name = varchar("name").bindTo { it.name }
    }

    object Users : Table<User>("user") {
        val id = int("id").primaryKey().bindTo { it.id }
        val roleId = int("role_id").references(Roles) { it.role }
        val groupId = int("group_id").references(Groups) { it.group }
        val moduleId = int("module_id").references(Modules) { it.module}
        val componentId = int("component_id").references(Components) { it.component }
        val photo = varchar("photo").bindTo {it.photo}
        val firstName = varchar("first_name").bindTo { it.firstName }
        val lastName = varchar("last_name").bindTo { it.lastName }
        val email = varchar("email").bindTo { it.email }
        val password = varchar("password").bindTo { it.password }
    }

    object TutorMappings : Table<TutorMapping>("tutor_mapping") {
        val id = int("id").primaryKey().bindTo { it.id }
        val tutorId = int("tutor_id").references(Users) { it.tutor }
        val groupId = int("group_id").references(Groups) { it.group }
        val componentId = int("component_id").references(Components) { it.component }
    }

    object Skills : Table<Skill>("skill") {
        val id = int("id").primaryKey().bindTo { it.id }
        val componentId = int("component_id").references(Components) { it.component }
        val name = varchar("name").bindTo { it.name }
        val description = varchar("description").bindTo { it.description }
        val coefficient = int("coefficient").bindTo { it.coefficient }
    }

    object GroupObservations : Table<GroupObservation>("group_observation") {
        val id = int("id").primaryKey().bindTo { it.id}
        val skillId = int("skill_id").references(Skills) { it.skill }
        val groupId = int("group_id").references(Groups) { it.group }
        val observation = varchar("observation").bindTo { it.observation }
    }
    object Scores : Table<Score>("score") {
        val Id = int("id").primaryKey().bindTo { it.id}
        val studentId = int("student_id").references(Users) { it.student }
        val skillId = int("skill_id").references(Skills) { it.skill }
        val value = double("value").bindTo { it.value }
        val observation = varchar("observation").bindTo { it.observation }
        val active = boolean("active").bindTo { it.active }
        val datetime = varchar("datetime").bindTo { it.datetime }
    }

    object Reassessments: Table<Reassessment>("reassessment") {
        val id = int("id").primaryKey().bindTo { it.id }
        val studentId = int("student_id").references(Users) { it.student }
        val skillId = int("skill_id").references(Skills) { it.skill }
        val scoreId = int("score_id").references(Scores) { it.score }
        val document = varchar("document").bindTo { it.document }
        val datetime = varchar("datetime").bindTo { it.datetime }
        val treated = boolean("treated").bindTo { it.treated }
    }

}