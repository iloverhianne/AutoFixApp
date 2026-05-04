package com.example.autofixapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

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
        holder.tvName.text = appt.service_name ?: "Service"
        holder.tvStatus.text = appt.status?.uppercase() ?: "PENDING"
        holder.tvDate.text = appt.date ?: "--"
        val rawTime = appt.time
        val formattedTime = if (!rawTime.isNullOrEmpty()) {
            try {
                if (rawTime.contains("AM", ignoreCase = true) || rawTime.contains("PM", ignoreCase = true)) {
                    rawTime
                } else {
                    val parts = rawTime.split(":")
                    if (parts.size >= 2) {
                        var hour24 = parts[0].toInt()
                        val minute = parts[1]

                        // Smart Fix: If hour is 1-7, it's likely PM (Shop opens at 8AM)
                        if (hour24 in 1..7) hour24 += 12

                        val ampm = if (hour24 >= 12) "PM" else "AM"
                        val hour12 = when {
                            hour24 == 0 -> 12
                            hour24 > 12 -> hour24 - 12
                            else -> hour24
                        }
                        String.format("%02d:%s %s", hour12, minute, ampm)
                    } else {
                        rawTime
                    }
                }
            } catch (e: Exception) {
                rawTime
            }
        } else {
            "08:00 AM"
        }

        holder.tvTime.text = formattedTime
        holder.tvEstimate.text = "₱${appt.total_amount ?: "0.00"}"
        
        val context = holder.itemView.context
        when (appt.status?.lowercase()) {
            "pending" -> {
                holder.tvStatus.setTextColor(context.getColor(R.color.status_pending_text))
                holder.tvStatus.setBackgroundResource(R.drawable.status_badge_bg)
            }
            "confirmed", "completed" -> {
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
    }

    override fun getItemCount() = appointments.size

    fun updateData(newAppointments: List<RepairHistory>) {
        this.appointments = newAppointments
        notifyDataSetChanged()
    }
}
