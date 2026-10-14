package com.aistudio.dexter.ui.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aistudio.dexter.R
import com.aistudio.dexter.data.model.Priority
import com.aistudio.dexter.data.model.TaskItem
import com.aistudio.dexter.data.model.TaskStatus
import com.aistudio.dexter.databinding.ItemTaskBinding

class TasksAdapter(
    private val onTaskChecked: (TaskItem) -> Unit,
    private val onTaskLongClick: (TaskItem) -> Unit
) : ListAdapter<TaskItem, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskItem) {
            binding.taskTitle.text = task.title
            binding.priorityBadge.text = task.priority.name
            binding.taskTime.text = formatRelativeTime(task.createdAt)

            val context = binding.root.context

            // Priority bar color mapping
            val priorityColor = when (task.priority) {
                Priority.CRITICAL -> ContextCompat.getColor(context, R.color.dexter_priority_critical)
                Priority.HIGH -> ContextCompat.getColor(context, R.color.dexter_priority_high)
                Priority.NORMAL -> ContextCompat.getColor(context, R.color.dexter_priority_normal)
                Priority.LOW, Priority.SOMEDAY -> ContextCompat.getColor(context, R.color.dexter_priority_low)
            }
            binding.priorityBar.setBackgroundColor(priorityColor)
            binding.priorityBadge.setTextColor(priorityColor)

            // Circular Checkbox state
            if (task.status == TaskStatus.COMPLETED) {
                binding.btnCompleteCheckbox.setImageDrawable(
                    androidx.appcompat.content.res.AppCompatResources.getDrawable(context, R.drawable.ic_check_circle)
                )
                binding.btnCompleteCheckbox.setColorFilter(
                    ContextCompat.getColor(context, R.color.dexter_success)
                )
                binding.btnCompleteCheckbox.contentDescription = "Task completed"
            } else {
                binding.btnCompleteCheckbox.setImageDrawable(
                    androidx.appcompat.content.res.AppCompatResources.getDrawable(context, R.drawable.ic_circle_outline)
                )
                binding.btnCompleteCheckbox.setColorFilter(
                    ContextCompat.getColor(context, R.color.dexter_on_surface_variant)
                )
                binding.btnCompleteCheckbox.contentDescription = "Mark '${task.title}' complete"
            }

            binding.btnCompleteCheckbox.setOnClickListener {
                onTaskChecked(task)
            }

            binding.root.setOnLongClickListener {
                onTaskLongClick(task)
                true
            }
        }

        private fun formatRelativeTime(epochSec: Long): String {
            val nowSec = System.currentTimeMillis() / 1000
            val diffSec = nowSec - epochSec
            return when {
                diffSec < 60 -> "Just now"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                diffSec < 86400 -> "${diffSec / 3600}h ago"
                diffSec < 172800 -> "Yesterday"
                else -> "${diffSec / 86400}d ago"
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskItem>() {
        override fun areItemsTheSame(oldItem: TaskItem, newItem: TaskItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskItem, newItem: TaskItem): Boolean {
            return oldItem == newItem
        }
    }
}
