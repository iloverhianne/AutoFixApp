package com.example.autofixapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private var repairs: List<RepairHistory>) :
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
        
        // Dynamic status color
        when (repair.status.lowercase()) {
            "completed" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#03543F"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#DEF7EC"))
            }
            "cancelled" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#9B1C1C"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FDE2E2"))
            }
            else -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#1E40AF"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#DBEAFE"))
            }
        }
    }

    override fun getItemCount() = repairs.size

    fun updateData(newRepairs: List<RepairHistory>) {
        this.repairs = newRepairs
        notifyDataSetChanged()
    }
}

