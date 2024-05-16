package com.example.retake2324

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Component(val name: String, val skills: List<String>)
class ComponentsAdapter(private val components: List<Component>) :
    RecyclerView.Adapter<ComponentsAdapter.ComponentViewHolder>() {

    inner class ComponentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val componentNameTextView: TextView = itemView.findViewById(R.id.componentNameTextView)
        val skillsContainer: LinearLayout = itemView.findViewById(R.id.skillsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComponentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_component, parent, false)
        return ComponentViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComponentViewHolder, position: Int) {
        val component = components[position]
        holder.componentNameTextView.text = component.name

        // Clear previous skills if any
        holder.skillsContainer.removeAllViews()

        // Add skills dynamically
        for (skill in component.skills) {
            val skillTextView = TextView(holder.itemView.context)
            skillTextView.text = skill
            holder.skillsContainer.addView(skillTextView)
        }
    }

    override fun getItemCount(): Int {
        return components.size
    }
}
