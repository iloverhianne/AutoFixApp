package com.example.autofixapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimelineAdapter(private var timelineItems: List<TimelineItem>) :
    RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatus: TextView = view.findViewById(R.id.tvTimelineStatus)
        val tvDate: TextView = view.findViewById(R.id.tvTimelineDate)
        val tvRemarks: TextView = view.findViewById(R.id.tvTimelineRemarks)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = timelineItems[position]
        holder.tvStatus.text = item.status_update.uppercase()
        holder.tvDate.text = item.created_at
        
        if (item.remarks.isNullOrEmpty()) {
            holder.tvRemarks.visibility = View.GONE
        } else {
            holder.tvRemarks.visibility = View.VISIBLE
            holder.tvRemarks.text = item.remarks
        }

        // Color based on status
        when (item.status_update.lowercase()) {
            "completed" -> holder.tvStatus.setTextColor(Color.parseColor("#03543F"))
            "in progress" -> holder.tvStatus.setTextColor(Color.parseColor("#1E40AF"))
            "pending" -> holder.tvStatus.setTextColor(Color.parseColor("#9CA3AF"))
            else -> holder.tvStatus.setTextColor(Color.parseColor("#1E40AF"))
        }
    }

    override fun getItemCount() = timelineItems.size

    fun updateData(newItems: List<TimelineItem>) {
        this.timelineItems = newItems
        notifyDataSetChanged()
    }
}
