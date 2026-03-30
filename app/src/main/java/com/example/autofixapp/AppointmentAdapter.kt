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
        val tvName: TextView = view.findViewById(R.id.tvHistoryServiceName)
        val tvStatus: TextView = view.findViewById(R.id.tvHistoryStatus)
        val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
        val tvAmount: TextView = view.findViewById(R.id.tvHistoryAmount)
        val btnPay: MaterialButton = view.findViewById(R.id.btnHistoryPay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appt = appointments[position]
        holder.tvName.text = "Service for ${appt.plate_no}"
        holder.tvStatus.text = appt.status.uppercase()
        holder.tvDate.text = appt.date
        holder.tvAmount.text = "₱${appt.total_amount}"
        
        // Dynamic status colors
        val context = holder.itemView.context
        
        if (appt.status.equals("COMPLETED", ignoreCase = true)) {
            holder.btnPay.visibility = View.VISIBLE
            holder.btnPay.setOnClickListener {
                val intent = Intent(context, PaymentActivity::class.java).apply {
                    putExtra("AMOUNT", appt.total_amount)
                    putExtra("JOB_ID", appt.job_id)
                }
                context.startActivity(intent)
            }
        } else {
            holder.btnPay.visibility = View.GONE
        }

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
