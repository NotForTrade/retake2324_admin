package com.example.retake2324

import android.util.SparseBooleanArray
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

    private val items: MutableList<Any>
    private val groupExpandedState = SparseBooleanArray()
    private val componentExpandedState = SparseBooleanArray()

    init {
        items = mutableListOf<Any>().apply {
            for ((groupIndex, group) in groups.withIndex()) {
                add(group)
                groupExpandedState.put(groupIndex, false)
                val groupStudents = students.filter { it.group == group }
                for ((componentIndex, component) in components.withIndex()) {
                    add(component to groupStudents)
                    componentExpandedState.put(groupIndex * 100 + componentIndex, false)
                    val componentSkills = skills.filter { it.component == component }
                    for (skill in componentSkills) {
                        add(skill to groupStudents)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Group -> VIEW_TYPE_GROUP
            is Pair<*, *> -> {
                when ((items[position] as Pair<*, *>).first) {
                    is Component -> VIEW_TYPE_COMPONENT
                    is Skill -> VIEW_TYPE_SKILL
                    else -> throw IllegalArgumentException("Invalid item type")
                }
            }
            else -> throw IllegalArgumentException("Invalid item type")
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
            is GroupViewHolder -> holder.bind(items[position] as Group, position)
            is ComponentViewHolder -> holder.bind(items[position] as Pair<Component, List<Student>>, position)
            is SkillViewHolder -> holder.bind(items[position] as Pair<Skill, List<Student>>)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupTextView: TextView = itemView.findViewById(R.id.groupTextView)
        private val studentsTextView: TextView = itemView.findViewById(R.id.studentsTextView)

        fun bind(group: Group, position: Int) {
            groupTextView.text = group.name + " (Tutor: " + group.tutor + ", Client: " + group.client + ")"
            val groupStudents = students.filter { it.group == group }
            studentsTextView.text = groupStudents.joinToString { it.name }

            itemView.setOnClickListener {
                val isExpanded = groupExpandedState[position]
                groupExpandedState.put(position, !isExpanded)
                updateItems()
            }
        }
    }

    inner class ComponentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val componentTextView: TextView = itemView.findViewById(R.id.componentTextView)
        private val componentScoresTextView: TextView = itemView.findViewById(R.id.componentScoresTextView)

        fun bind(pair: Pair<Component, List<Student>>, position: Int) {
            val (component, students) = pair
            componentTextView.text = component.name
            val componentStudentScores = componentScores.filter { it.component == component && it.student in students }
            componentScoresTextView.text = componentStudentScores.joinToString { "${it.student.name}: ${it.score}" }

            itemView.setOnClickListener {
                val isExpanded = componentExpandedState[position]
                componentExpandedState.put(position, !isExpanded)
                updateItems()
            }
        }
    }

    inner class SkillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val skillTextView: TextView = itemView.findViewById(R.id.skillTextView)
        private val skillScoresTextView: TextView = itemView.findViewById(R.id.skillScoresTextView)

        fun bind(pair: Pair<Skill, List<Student>>) {
            val (skill, students) = pair
            skillTextView.text = skill.name + " (Coefficient: " + skill.coefficient + ")"
            val skillStudentScores = skillScores.filter { it.skill == skill && it.student in students }
            skillScoresTextView.text = skillStudentScores.joinToString { "${it.student.name}: ${it.score}" }
        }
    }

    private fun updateItems() {
        items.clear()
        for ((groupIndex, group) in groups.withIndex()) {
            items.add(group)
            val isGroupExpanded = groupExpandedState[groupIndex]
            if (isGroupExpanded) {
                val groupStudents = students.filter { it.group == group }
                for ((componentIndex, component) in components.withIndex()) {
                    items.add(component to groupStudents)
                    val isComponentExpanded = componentExpandedState[groupIndex * 100 + componentIndex]
                    if (isComponentExpanded) {
                        val componentSkills = skills.filter { it.component == component }
                        for (skill in componentSkills) {
                            items.add(skill to groupStudents)
                        }
                    }
                }
            }
        }
        notifyDataSetChanged()
    }
}
