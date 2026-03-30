package com.example.autofixapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private var repairs: List<RepairHistory>, private val onItemClick: ((String) -> Unit)? = null) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvHistoryServiceName)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvAmount: TextView = view.findViewById(R.id.tvHistoryAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repair = repairs[position]
        holder.tvName.text = "Job #${repair.job_id} (${repair.plate_no})"
        holder.tvStatus.text = repair.status.uppercase()
        holder.tvDate.text = repair.date
        holder.tvAmount.text = "₱${repair.total_amount}"
        
        // Dynamic status colors
        val context = holder.itemView.context
        when (repair.status.lowercase()) {
            "completed" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_completed_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_success_badge)
            }
            "cancelled" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_cancelled_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_danger_badge)
            }
            else -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_active_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_active_badge)
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(repair.job_id)
        }
    }

    override fun getItemCount() = repairs.size

    fun updateData(newRepairs: List<RepairHistory>) {
        this.repairs = newRepairs
        notifyDataSetChanged()
    }
}

