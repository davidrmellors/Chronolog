package za.co.varsitycollege.serversamurais.chronolog.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import za.co.varsitycollege.serversamurais.chronolog.R
import za.co.varsitycollege.serversamurais.chronolog.model.Task
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(private val tasks: List<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    var filteredTasks: List<Task> = tasks.toList()

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTaskName: TextView = itemView.findViewById(R.id.recentTaskNameTextView)
        val textViewTaskDescription: TextView = itemView.findViewById(R.id.recentDescriptionTextView)
        val textViewTaskDate: TextView = itemView.findViewById(R.id.recentDateTextView)
        val textViewTaskDuration: TextView = itemView.findViewById(R.id.recentDurationTextView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_task_view, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.textViewTaskName.text = task.name
        holder.textViewTaskDescription.text = task.description
        holder.textViewTaskDate.text = task.date.toString()
        holder.textViewTaskDuration.text = task.duration.toString() + "\nminutes"
        // Bind other task properties to views
    }

    override fun getItemCount(): Int = tasks.size

    fun filterByDateRange(startDate: String, endDate: String) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        try {
            val start = if (startDate.isNotEmpty()) sdf.parse(startDate) else null
            val end = if (endDate.isNotEmpty()) sdf.parse(endDate) else null
            // ... filter tasks using the parsed start and end dates ...
        } catch (e: ParseException) {
            Log.e("TaskAdapter", "Failed to parse date: ${e.message}")
        }
    }
}
