package com.example.bfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class PlanListItem {
    data class Header(val title: String) : PlanListItem()
    data class PlanItem(val id: String, val type: ItemType, val text: String, var isCompleted: Boolean = false) : PlanListItem()
}

enum class ItemType {
    FOOD,
    EXERCISE
}

class PlanAdapter(
    private val planItems: List<PlanListItem>,
    private val onPlanItemCompleted: (PlanListItem.PlanItem, Boolean) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemIcon: ImageView = view.findViewById(R.id.itemIcon)
        val itemText: TextView = view.findViewById(R.id.itemText)
        val itemCheckBox: CheckBox = view.findViewById(R.id.itemCheckBox)
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerText: TextView = view.findViewById(R.id.headerText)
    }

    override fun getItemViewType(position: Int): Int {
        return when (planItems[position]) {
            is PlanListItem.Header -> TYPE_HEADER
            is PlanListItem.PlanItem -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_header_plan, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_plan, parent, false)
            PlanViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = planItems[position]) {
            is PlanListItem.Header -> {
                (holder as HeaderViewHolder).headerText.text = item.title
            }
            is PlanListItem.PlanItem -> {
                val planViewHolder = holder as PlanViewHolder
                planViewHolder.itemText.text = item.text
                planViewHolder.itemCheckBox.setOnCheckedChangeListener(null)
                planViewHolder.itemCheckBox.isChecked = item.isCompleted
                planViewHolder.itemCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    item.isCompleted = isChecked
                    onPlanItemCompleted(item, isChecked)
                }
                when (item.type) {
                    ItemType.FOOD -> planViewHolder.itemIcon.setImageResource(R.drawable.ic_baseline_restaurant_24)
                    ItemType.EXERCISE -> planViewHolder.itemIcon.setImageResource(R.drawable.ic_baseline_fitness_center_24)
                }
            }
        }
    }

    override fun getItemCount() = planItems.size

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}
