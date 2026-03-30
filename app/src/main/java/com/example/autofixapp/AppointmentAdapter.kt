package com.example.autofixapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppointmentAdapter(private var appointments: List<RepairHistory>) :
    RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvApptServiceName)
        val tvStatus: TextView = view.findViewById(R.id.tvApptStatus)
        val tvDate: TextView = view.findViewById(R.id.tvApptDate)
        val tvTime: TextView = view.findViewById(R.id.tvApptTime)
        val tvEstimate: TextView = view.findViewById(R.id.tvApptEstimate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appt = appointments[position]
        holder.tvName.text = "Service for ${appt.plate_no}"
        holder.tvStatus.text = appt.status.uppercase()
        holder.tvDate.text = appt.date
        holder.tvTime.text = if (appt.status.lowercase() == "confirmed") "Confirmed Slot" else "Pending Review"
        holder.tvEstimate.text = "₱${appt.total_amount}"
        
        // Dynamic status colors
        val context = holder.itemView.context
        when (appt.status.lowercase()) {
            "pending" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_pending_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_bg)
            }
            "confirmed" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_completed_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_success_badge)
            }
            else -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_active_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_active_badge)
            }
        }
    }

    override fun getItemCount() = appointments.size

    fun updateData(newAppointments: List<RepairHistory>) {
        this.appointments = newAppointments
        notifyDataSetChanged()
    }
}
