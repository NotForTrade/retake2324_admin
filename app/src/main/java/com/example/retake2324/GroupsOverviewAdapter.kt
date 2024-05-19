package com.example.retake2324

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupsOverviewAdapter(
    private val groups: List<Group>,
    private val students: List<Student>,
    private val components: List<Component>,
    private val skills: List<Skill>,
    private val skillScores: List<SkillScore>,
    private val componentScores: List<ComponentScore>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_GROUP = 0
    private val VIEW_TYPE_COMPONENT = 1
    private val VIEW_TYPE_SKILL = 2

    override fun getItemViewType(position: Int): Int {
        // Return appropriate view type based on position or data structure
        val item = getItem(position)
        return when (item) {
            is Group -> VIEW_TYPE_GROUP
            is Component -> VIEW_TYPE_COMPONENT
            is Skill -> VIEW_TYPE_SKILL
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_GROUP -> GroupViewHolder(inflater.inflate(R.layout.item_group, parent, false))
            VIEW_TYPE_COMPONENT -> ComponentViewHolder(inflater.inflate(R.layout.item_component, parent, false))
            VIEW_TYPE_SKILL -> SkillViewHolder(inflater.inflate(R.layout.item_skill, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupViewHolder -> holder.bind(getItem(position) as Group)
            is ComponentViewHolder -> holder.bind(getItem(position) as Component)
            is SkillViewHolder -> holder.bind(getItem(position) as Skill)
        }
    }

    override fun getItemCount(): Int {
        // Count groups, components, and skills separately
        return groups.size + components.size + skills.size
    }

    private fun getItem(position: Int): Any {
        // Determine the item at the given position
        var pos = position
        if (pos < groups.size) return groups[pos]
        pos -= groups.size
        if (pos < components.size) return components[pos]
        pos -= components.size
        return skills[pos]
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupTextView: TextView = itemView.findViewById(R.id.groupTextView)
        private val studentsTextView: TextView = itemView.findViewById(R.id.studentsTextView)

        fun bind(group: Group) {
            groupTextView.text = group.name + " (Tutor: " + group.tutor + ", Client: " + group.client + ")"
            val groupStudents = students.filter { it.group == group }
            studentsTextView.text = groupStudents.joinToString { it.name }
        }
    }

    inner class ComponentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val componentTextView: TextView = itemView.findViewById(R.id.componentTextView)
        private val componentScoresTextView: TextView = itemView.findViewById(R.id.componentScoresTextView)

        fun bind(component: Component) {
            componentTextView.text = component.name
            val componentStudentScores = componentScores.filter { it.component == component }
            componentScoresTextView.text = componentStudentScores.joinToString { "${it.student.name}: ${it.score}" }
        }
    }

    inner class SkillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val skillTextView: TextView = itemView.findViewById(R.id.skillTextView)
        private val skillScoresTextView: TextView = itemView.findViewById(R.id.skillScoresTextView)

        fun bind(skill: Skill) {
            skillTextView.text = skill.name + " (Coefficient: " + skill.coefficient + ")"
            val skillStudentScores = skillScores.filter { it.skill == skill }
            skillScoresTextView.text = skillStudentScores.joinToString { "${it.student.name}: ${it.score}" }
        }
    }
}
